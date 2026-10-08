package com.example.gerenciadorfinanceiro.data.csv

import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

private const val MAIN_NS = "http://schemas.openxmlformats.org/spreadsheetml/2006/main"

/** Wraps <row> elements in a worksheet document. */
fun sheetXml(vararg rows: String): String =
    """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="$MAIN_NS"><sheetData>${rows.joinToString("")}</sheetData></worksheet>"""

/**
 * Builds a minimal .xlsx archive in memory, laid out like the files Excel writes.
 * @param sheetTarget Sheet path relative to xl/, as referenced by the workbook relationship
 */
fun xlsx(
    sheet: String,
    sharedStrings: List<String> = emptyList(),
    date1904: Boolean = false,
    sheetTarget: String = "worksheets/sheet1.xml",
    extraEntries: Map<String, String> = emptyMap()
): ByteArray {
    val workbookPr = if (date1904) """<workbookPr date1904="1"/>""" else "<workbookPr/>"
    val entries = mapOf(
        "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"/>""",
        "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="$MAIN_NS" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">$workbookPr<sheets><sheet name="Fatura" sheetId="1" r:id="rId1"/></sheets></workbook>""",
        "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="$sheetTarget"/></Relationships>""",
        "xl/sharedStrings.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<sst xmlns="$MAIN_NS">${sharedStrings.joinToString("") { "<si><t>${it.escapeXml()}</t></si>" }}</sst>""",
        "xl/$sheetTarget" to sheet
    ) + extraEntries

    val out = ByteArrayOutputStream()
    ZipOutputStream(out).use { zip ->
        for ((name, content) in entries) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray(Charsets.UTF_8))
            zip.closeEntry()
        }
    }
    return out.toByteArray()
}

private fun String.escapeXml(): String =
    replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
