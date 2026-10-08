package com.example.gerenciadorfinanceiro.data.csv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.math.BigDecimal

class XlsxReaderTest {

    @Test
    fun readsSharedInlineFormulaAndNumberCells() {
        val sheet = sheetXml(
            """<row r="1">
                <c r="A1" t="s"><v>0</v></c>
                <c r="B1" t="inlineStr"><is><t>INLINE</t></is></c>
                <c r="C1" t="str"><f>A1&amp;"!"</f><v>FORMULA</v></c>
                <c r="D1"><v>1234.56</v></c>
            </row>"""
        )

        val rows = XlsxReader.read(xlsx(sheet, sharedStrings = listOf("SHARED & CO"))).rows

        assertEquals(1, rows.size)
        assertEquals(
            listOf(
                SheetCell.Text("SHARED & CO"),
                SheetCell.Text("INLINE"),
                SheetCell.Text("FORMULA"),
                SheetCell.Number(BigDecimal("1234.56"))
            ),
            rows[0].cells
        )
        assertEquals("SHARED & CO | INLINE | FORMULA | 1234.56", rows[0].content)
    }

    @Test
    fun emptyCellsKeepColumnPositions() {
        val sheet = sheetXml("""<row r="1"><c r="A1"><v>1</v></c><c r="C1"><v>3</v></c></row>""")

        val cells = XlsxReader.read(xlsx(sheet)).rows[0].cells

        assertEquals(3, cells.size)
        assertEquals(SheetCell.Number(BigDecimal("1")), cells[0])
        assertNull(cells[1])
        assertEquals(SheetCell.Number(BigDecimal("3")), cells[2])
    }

    @Test
    fun cellsWithoutReferenceFollowPreviousCell() {
        val sheet = sheetXml("""<row><c t="inlineStr"><is><t>A</t></is></c><c><v>2</v></c></row>""")

        val rows = XlsxReader.read(xlsx(sheet)).rows

        assertEquals(1, rows[0].rowNumber)
        assertEquals(listOf(SheetCell.Text("A"), SheetCell.Number(BigDecimal("2"))), rows[0].cells)
    }

    @Test
    fun rowNumbersComeFromFileAndEmptyRowsAreDropped() {
        val sheet = sheetXml(
            """<row r="1"><c r="A1"><v>1</v></c></row>""",
            // Styled but valueless cells don't make a row
            """<row r="2"><c r="A2" s="3"/></row>""",
            """<row r="5"><c r="A5"><v>5</v></c></row>"""
        )

        val rows = XlsxReader.read(xlsx(sheet)).rows

        assertEquals(listOf(1, 5), rows.map { it.rowNumber })
    }

    @Test
    fun richTextSharedStringIgnoresPhoneticRuns() {
        val sharedStrings = """<?xml version="1.0" encoding="UTF-8"?>
<sst xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><si><r><rPr><b/></rPr><t>SUPER</t></r><r><t xml:space="preserve">MERCADO</t></r><rPh sb="0" eb="1"><t>X</t></rPh></si></sst>"""
        val sheet = sheetXml("""<row r="1"><c r="A1" t="s"><v>0</v></c></row>""")

        val rows = XlsxReader.read(xlsx(sheet, extraEntries = mapOf("xl/sharedStrings.xml" to sharedStrings))).rows

        assertEquals(SheetCell.Text("SUPERMERCADO"), rows[0].cells[0])
    }

    @Test
    fun errorCellsAreEmpty() {
        val sheet = sheetXml("""<row r="1"><c r="A1" t="e"><v>#N/A</v></c><c r="B1"><v>1</v></c></row>""")

        val cells = XlsxReader.read(xlsx(sheet)).rows[0].cells

        assertNull(cells[0])
    }

    @Test
    fun followsWorkbookRelationshipToFirstSheet() {
        val sheet = sheetXml("""<row r="1"><c r="A1" t="inlineStr"><is><t>FATURA</t></is></c></row>""")
        val decoy = sheetXml("""<row r="1"><c r="A1" t="inlineStr"><is><t>DECOY</t></is></c></row>""")

        val rows = XlsxReader.read(
            xlsx(
                sheet,
                sheetTarget = "worksheets/fatura.xml",
                extraEntries = mapOf("xl/worksheets/sheet1.xml" to decoy)
            )
        ).rows

        assertEquals(SheetCell.Text("FATURA"), rows[0].cells[0])
    }

    @Test
    fun readsPrefixedElements() {
        // Some generators (e.g. the Open XML SDK) prefix every element
        val sheet = """<?xml version="1.0" encoding="UTF-8"?>
<x:worksheet xmlns:x="http://schemas.openxmlformats.org/spreadsheetml/2006/main"><x:sheetData><x:row r="1"><x:c r="A1" t="inlineStr"><x:is><x:t>PREFIXED</x:t></x:is></x:c><x:c r="B1"><x:v>10</x:v></x:c></x:row></x:sheetData></x:worksheet>"""

        val rows = XlsxReader.read(xlsx(sheet)).rows

        assertEquals(listOf(SheetCell.Text("PREFIXED"), SheetCell.Number(BigDecimal("10"))), rows[0].cells)
    }

    @Test
    fun detectsDateSystem() {
        val sheet = sheetXml("""<row r="1"><c r="A1"><v>1</v></c></row>""")

        assertFalse(XlsxReader.read(xlsx(sheet)).date1904)
        assertTrue(XlsxReader.read(xlsx(sheet, date1904 = true)).date1904)
    }
}
