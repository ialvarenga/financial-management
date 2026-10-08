package com.example.gerenciadorfinanceiro.data.csv

import org.xml.sax.Attributes
import org.xml.sax.InputSource
import org.xml.sax.helpers.DefaultHandler
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader
import java.math.BigDecimal
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory

/**
 * A cell value read from a bill file. CSV fields are always Text; .xlsx cells keep
 * their stored type so dates and amounts don't depend on the configured text formats.
 */
sealed class SheetCell {
    data class Text(val value: String) : SheetCell()
    data class Number(val value: BigDecimal) : SheetCell()
}

/**
 * @param rowNumber 1-based row/line number as the user sees it in the file
 * @param cells Cells by column (index 0 = column 1); null where the cell is empty
 * @param content Human-readable row, shown for skipped lines
 */
data class SheetRow(val rowNumber: Int, val cells: List<SheetCell?>, val content: String)

class XlsxSheet(val rows: List<SheetRow>, val date1904: Boolean)

/**
 * Minimal .xlsx reader: unzips the workbook and reads the first sheet's cell values with SAX
 * (available both on Android and in JVM unit tests). Formatting, formulas and other sheets
 * are ignored.
 */
internal object XlsxReader {

    // Bill exports are tiny; this only guards against corrupt or malicious archives.
    private const val MAX_ENTRY_BYTES = 20L * 1024 * 1024
    private const val DEFAULT_SHEET = "xl/worksheets/sheet1.xml"

    fun read(bytes: ByteArray): XlsxSheet {
        val entries = unzip(bytes)
        val workbook = WorkbookHandler().also { parseXml(entries["xl/workbook.xml"], it) }
        val relationships = RelationshipsHandler().also { parseXml(entries["xl/_rels/workbook.xml.rels"], it) }

        val sheetPath = workbook.firstSheetRelId
            ?.let { relationships.targets[it] }
            ?.let { target -> if (target.startsWith("/")) target.drop(1) else "xl/$target" }
            ?.takeIf { it in entries }
            ?: DEFAULT_SHEET
        val sheetXml = entries[sheetPath]
            ?: throw IllegalArgumentException("Planilha inválida: nenhuma aba encontrada")

        val sharedStrings = SharedStringsHandler().also { parseXml(entries["xl/sharedStrings.xml"], it) }.strings
        val sheet = SheetHandler(sharedStrings).also { parseXml(sheetXml, it) }
        return XlsxSheet(sheet.rows, workbook.date1904)
    }

    private fun unzip(bytes: ByteArray): Map<String, ByteArray> {
        val entries = mutableMapOf<String, ByteArray>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                val name = entry.name
                if (!entry.isDirectory && name.startsWith("xl/") && (name.endsWith(".xml") || name.endsWith(".rels"))) {
                    entries[name] = readEntry(zip)
                }
            }
        }
        return entries
    }

    private fun readEntry(zip: ZipInputStream): ByteArray {
        val out = ByteArrayOutputStream()
        val buffer = ByteArray(8192)
        var total = 0L
        while (true) {
            val read = zip.read(buffer)
            if (read < 0) break
            total += read
            if (total > MAX_ENTRY_BYTES) throw IllegalArgumentException("Planilha muito grande")
            out.write(buffer, 0, read)
        }
        return out.toByteArray()
    }

    private fun parseXml(xml: ByteArray?, handler: XmlHandler) {
        if (xml == null) return
        SAXParserFactory.newInstance().newSAXParser().parse(ByteArrayInputStream(xml), handler)
    }

    /** 1-based column index from a cell reference such as "AB12". */
    private fun columnIndex(reference: String): Int? {
        val letters = reference.takeWhile { it.isLetter() }.uppercase()
        if (letters.isEmpty()) return null
        return letters.fold(0) { acc, c -> acc * 26 + (c - 'A' + 1) }
    }

    private abstract class XmlHandler : DefaultHandler() {
        // Never fetch external entities/DTDs referenced by the file.
        override fun resolveEntity(publicId: String?, systemId: String?) = InputSource(StringReader(""))

        // Parsed without namespace processing, which behaves the same on Android and the JVM;
        // prefixes such as "x:c" are dropped so prefixed and unprefixed files match alike.
        final override fun startElement(uri: String?, localName: String?, qName: String, attributes: Attributes) =
            start(qName.substringAfterLast(':'), attributes)

        final override fun endElement(uri: String?, localName: String?, qName: String) =
            end(qName.substringAfterLast(':'))

        open fun start(name: String, attributes: Attributes) {}
        open fun end(name: String) {}
    }

    private class WorkbookHandler : XmlHandler() {
        var firstSheetRelId: String? = null
        var date1904 = false

        override fun start(name: String, attributes: Attributes) {
            when (name) {
                "workbookPr" -> date1904 = attributes.getValue("date1904").let { it == "1" || it == "true" }
                "sheet" -> if (firstSheetRelId == null) {
                    // r:id; matched without the prefix, which isn't fixed by the format
                    firstSheetRelId = (0 until attributes.length)
                        .firstOrNull { attributes.getQName(it).substringAfterLast(':') == "id" }
                        ?.let { attributes.getValue(it) }
                }
            }
        }
    }

    private class RelationshipsHandler : XmlHandler() {
        val targets = mutableMapOf<String, String>()

        override fun start(name: String, attributes: Attributes) {
            if (name == "Relationship") {
                val id = attributes.getValue("Id") ?: return
                val target = attributes.getValue("Target") ?: return
                targets[id] = target
            }
        }
    }

    private class SharedStringsHandler : XmlHandler() {
        val strings = mutableListOf<String>()
        private val current = StringBuilder()
        private var inText = false
        private var inPhonetic = false

        override fun start(name: String, attributes: Attributes) {
            when (name) {
                "si" -> current.setLength(0)
                "rPh" -> inPhonetic = true
                "t" -> inText = !inPhonetic
            }
        }

        override fun end(name: String) {
            when (name) {
                "si" -> strings.add(current.toString())
                "rPh" -> inPhonetic = false
                "t" -> inText = false
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (inText) current.appendRange(ch, start, start + length)
        }
    }

    private class SheetHandler(private val sharedStrings: List<String>) : XmlHandler() {
        val rows = mutableListOf<SheetRow>()
        private var rowNumber = 0
        private var rowCells = mutableMapOf<Int, SheetCell>()
        private var column = 0
        private var cellType: String? = null
        private val value = StringBuilder()
        private var inValue = false

        override fun start(name: String, attributes: Attributes) {
            when (name) {
                "row" -> {
                    // Empty rows are omitted from the file, so trust the stored row number when present.
                    rowNumber = attributes.getValue("r")?.toIntOrNull() ?: (rowNumber + 1)
                    rowCells = mutableMapOf()
                    column = 0
                }
                "c" -> {
                    // Empty cells are omitted too; the reference keeps the remaining cells in their columns.
                    column = attributes.getValue("r")?.let(::columnIndex) ?: (column + 1)
                    cellType = attributes.getValue("t")
                    value.setLength(0)
                }
                // <v> holds the stored value; <t> holds inline string text
                "v", "t" -> inValue = true
            }
        }

        override fun end(name: String) {
            when (name) {
                "v", "t" -> inValue = false
                "c" -> toCell(value.toString())?.let { rowCells[column] = it }
                "row" -> if (rowCells.isNotEmpty()) {
                    val cells = (1..rowCells.keys.max()).map { rowCells[it] }
                    rows.add(SheetRow(rowNumber, cells, cells.joinToString(" | ") { it.display() }))
                }
            }
        }

        override fun characters(ch: CharArray, start: Int, length: Int) {
            if (inValue) value.appendRange(ch, start, start + length)
        }

        private fun toCell(raw: String): SheetCell? = when (cellType) {
            "s" -> raw.trim().toIntOrNull()?.let { sharedStrings.getOrNull(it) }?.let { SheetCell.Text(it) }
            "str", "inlineStr" -> SheetCell.Text(raw)
            "e" -> null // #N/A, #REF! and other formula errors
            else -> raw.trim().takeIf { it.isNotEmpty() }?.let { text ->
                text.toBigDecimalOrNull()?.let { SheetCell.Number(it) } ?: SheetCell.Text(text)
            }
        }

        private fun SheetCell?.display(): String = when (this) {
            is SheetCell.Text -> value
            is SheetCell.Number -> value.toPlainString()
            null -> ""
        }
    }
}
