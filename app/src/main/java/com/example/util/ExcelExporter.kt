package com.example.util

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import com.example.data.models.Labour
import com.example.data.models.Payment
import com.example.data.models.Site
import com.example.data.models.UserProfile
import com.example.data.models.Attendance
import com.example.data.models.SiteExpense
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Enterprise-grade Excel File Exporter for Labour Tracker.
 * Generates structured, styled OpenXML spreadsheet packages (.xlsx) natively.
 */
object ExcelExporter {

    private fun escapeXml(value: String): String {
        return value.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }

    private fun getColumnLetter(colIndex: Int): String {
        var temp = colIndex
        val letter = StringBuilder()
        while (temp >= 0) {
            letter.insert(0, ('A'.toInt() + (temp % 26)).toChar())
            temp = (temp / 26) - 1
        }
        return letter.toString()
    }

    private fun writeCell(rowIdx: Int, colIdx: Int, value: Any, styleIndex: Int): String {
        val cellRef = getColumnLetter(colIdx) + rowIdx
        return when (value) {
            is Number -> "<c r=\"$cellRef\" s=\"$styleIndex\"><v>$value</v></c>"
            else -> {
                val escapedStr = escapeXml(value.toString())
                "<c r=\"$cellRef\" s=\"$styleIndex\" t=\"inlineStr\"><is><t>$escapedStr</t></is></c>"
            }
        }
    }

    fun generateLabourExcelReport(
        context: Context,
        user: UserProfile,
        sites: List<Site>,
        labours: List<Labour>,
        payments: List<Payment>,
        attendances: List<Attendance> = emptyList(),
        siteExpenses: List<SiteExpense> = emptyList(),
        filterSiteId: Int? = null,
        filterLabourId: Int? = null,
        startDateStr: String? = null, // "YYYY-MM-DD"
        endDateStr: String? = null,  // "YYYY-MM-DD"
        isHindi: Boolean = false
    ): File {
        // Apply filters
        var filteredPayments = payments
        var filteredAttendances = attendances
        var filteredSiteExpenses = siteExpenses
        if (filterSiteId != null) {
            filteredPayments = filteredPayments.filter { it.siteId == filterSiteId }
            filteredAttendances = filteredAttendances.filter { it.siteId == filterSiteId }
            filteredSiteExpenses = filteredSiteExpenses.filter { it.siteId == filterSiteId }
        }
        if (filterLabourId != null) {
            filteredPayments = filteredPayments.filter { it.labourId == filterLabourId }
            filteredAttendances = filteredAttendances.filter { it.labourId == filterLabourId }
        }
        if (!startDateStr.isNullOrBlank()) {
            filteredPayments = filteredPayments.filter { it.date >= startDateStr }
            filteredAttendances = filteredAttendances.filter { it.date >= startDateStr }
            filteredSiteExpenses = filteredSiteExpenses.filter { it.expenseDate >= startDateStr }
        }
        if (!endDateStr.isNullOrBlank()) {
            filteredPayments = filteredPayments.filter { it.date <= endDateStr }
            filteredAttendances = filteredAttendances.filter { it.date <= endDateStr }
            filteredSiteExpenses = filteredSiteExpenses.filter { it.expenseDate <= endDateStr }
        }

        // Setup File Name
        val reportPeriodStr = if (!startDateStr.isNullOrBlank() && !endDateStr.isNullOrBlank()) {
            "${startDateStr}_to_${endDateStr}"
        } else if (!startDateStr.isNullOrBlank()) {
            "From_${startDateStr}"
        } else {
            "AllTime"
        }

        val siteNameClean = if (filterSiteId != null) {
            sites.find { it.id == filterSiteId }?.name?.replace(" ", "_")?.take(15) ?: "Site"
        } else null

        val workerNameClean = if (filterLabourId != null) {
            labours.find { it.id == filterLabourId }?.name?.replace(" ", "_")?.take(15) ?: "Labour"
        } else null

        val baseFileName = when {
            siteNameClean != null -> "Site_Report_${siteNameClean}.xlsx"
            workerNameClean != null -> "Labour_Register_${workerNameClean}.xlsx"
            else -> "Nirmaan_Labour_Report_${reportPeriodStr}.xlsx"
        }

        val destinationFile = File(context.cacheDir, baseFileName)
        val zos = ZipOutputStream(FileOutputStream(destinationFile))

        // Write _rels/.rels
        zos.putNextEntry(ZipEntry("_rels/.rels"))
        zos.write(getRelsXml().toByteArray())

        // Write [Content_Types].xml
        zos.putNextEntry(ZipEntry("[Content_Types].xml"))
        zos.write(getContentTypesXml().toByteArray())

        // Write xl/styles.xml
        zos.putNextEntry(ZipEntry("xl/styles.xml"))
        zos.write(getStylesXml().toByteArray())

        // Write xl/workbook.xml
        zos.putNextEntry(ZipEntry("xl/workbook.xml"))
        zos.write(getWorkbookXml().toByteArray())

        // Write xl/_rels/workbook.xml.rels
        zos.putNextEntry(ZipEntry("xl/_rels/workbook.xml.rels"))
        zos.write(getWorkbookRelsXml().toByteArray())

        // Calculate statistics for Sheet 1 (Summary)
        val activeLabours = labours.filter { it.status == "Active" }
        val activeSites = sites.filter { it.status == "Active" }

        val kharchiAmount = filteredPayments.filter { it.paymentType.contains("Kharchi") || it.paymentType.contains("Daily") }.sumOf { it.amount }
        val extraExpenseAmount = filteredPayments.filter { it.paymentType.contains("Extra") }.sumOf { it.amount }
        val advanceAmount = filteredPayments.filter { it.paymentType.contains("Advance") || it.paymentType.contains("Weekly") }.sumOf { it.amount }
        val bonusAmount = filteredPayments.filter { it.paymentType.contains("Bonus") }.sumOf { it.amount }
        val deductionAmount = filteredPayments.filter { it.paymentType.contains("Deduction") }.sumOf { it.amount }
        val totalRemittances = filteredPayments.sumOf { it.amount }
        
        // Net financial operation cost: payments which actually spent capital (excluding deductions which are credit back, plus extra expense)
        // Let's compute: Net Expense = Kharchi + Extra Expense + Weekly Advance + Bonus - Deductions
        val netExpense = kharchiAmount + extraExpenseAmount + advanceAmount + bonusAmount - deductionAmount

        // Generate Sheet 1
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet1.xml"))
        zos.write(generateSheet1Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            totalLabourCount = activeLabours.size,
            totalSitesCount = activeSites.size,
            totalPaymentsCount = filteredPayments.size,
            totalPaymentsAmount = totalRemittances,
            kharchiAmount = kharchiAmount,
            extraExpenseAmount = extraExpenseAmount,
            advanceAmount = advanceAmount,
            bonusAmount = bonusAmount,
            deductionAmount = deductionAmount,
            netExpense = netExpense
        ).toByteArray())

        // Generate Sheet 2
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet2.xml"))
        zos.write(generateSheet2Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            payments = filteredPayments,
            sites = sites
        ).toByteArray())

        // Generate Sheet 3
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet3.xml"))
        zos.write(generateSheet3Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            sites = sites,
            labours = labours,
            payments = filteredPayments,
            siteExpenses = filteredSiteExpenses
        ).toByteArray())

        // Generate Sheet 4 (Daily Attendance Log)
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet4.xml"))
        zos.write(generateSheet4Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            attendances = filteredAttendances,
            labours = labours,
            sites = sites,
            payments = filteredPayments
        ).toByteArray())

        // Generate Sheet 5 (Monthly Analytics)
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet5.xml"))
        zos.write(generateSheet5Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            payments = filteredPayments
        ).toByteArray())

        // Generate Sheet 6 (Site Non-Labour Expenses)
        zos.putNextEntry(ZipEntry("xl/worksheets/sheet6.xml"))
        zos.write(generateSheet6Xml(
            user = user,
            reportPeriodStr = reportPeriodStr,
            isHindi = isHindi,
            siteExpenses = filteredSiteExpenses,
            sites = sites
        ).toByteArray())

        zos.close()
        return destinationFile
    }

    private fun getRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""
    }

    private fun getContentTypesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
  <Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
  <Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet2.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet3.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet4.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet5.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
  <Override PartName="/xl/worksheets/sheet6.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
</Types>"""
    }

    private fun getWorkbookXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheets>
    <sheet name="Summary" sheetId="1" r:id="rId1"/>
    <sheet name="Labour Payment Register" sheetId="2" r:id="rId2"/>
    <sheet name="Site Expense Ledger" sheetId="3" r:id="rId3"/>
    <sheet name="Daily Attendance Log" sheetId="4" r:id="rId4"/>
    <sheet name="Monthly Analytics" sheetId="5" r:id="rId5"/>
    <sheet name="Material &amp; Site Expenses" sheetId="6" r:id="rId6"/>
  </sheets>
</workbook>"""
    }

    private fun getWorkbookRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
  <Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet2.xml"/>
  <Relationship Id="rId3" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet3.xml"/>
  <Relationship Id="rId4" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet4.xml"/>
  <Relationship Id="rId5" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet5.xml"/>
  <Relationship Id="rId6" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet6.xml"/>
  <Relationship Id="rId7" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""
    }

    private fun getStylesXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
  <numFmts count="3">
    <numFmt numFmtId="164" formatCode="[$₹-4009] #,##0.00"/>
    <numFmt numFmtId="165" formatCode="dd/mm/yyyy"/>
    <numFmt numFmtId="166" formatCode="[$₹-4009] #,##0"/>
  </numFmts>
  
  <fonts count="9">
    <font><sz val="11"/><color rgb="FF000000"/><name val="Segoe UI"/></font>                                                <!-- 0: Regular -->
    <font><b/><sz val="11"/><color rgb="FF000000"/><name val="Segoe UI"/></font>                                             <!-- 1: Bold standard -->
    <font><b/><sz val="15"/><color rgb="FFFFFFFF"/><name val="Segoe UI"/></font>                                             <!-- 2: Title Large White -->
    <font><b/><sz val="13"/><color rgb="FF1E3A8A"/><name val="Segoe UI"/></font>                                             <!-- 3: Subheader Navy bold -->
    <font><b/><sz val="11"/><color rgb="FFFFFFFF"/><name val="Segoe UI"/></font>                                             <!-- 4: Header White bold -->
    <font><b/><sz val="11"/><color rgb="FF1E3A8A"/><name val="Segoe UI"/></font>                                             <!-- 5: Kharchi Dark Blue -->
    <font><b/><sz val="11"/><color rgb="FF14532D"/><name val="Segoe UI"/></font>                                             <!-- 6: Extra Exp Dark Green -->
    <font><b/><sz val="11"/><color rgb="FF7C2D12"/><name val="Segoe UI"/></font>                                             <!-- 7: Advance Dark Red/Orange -->
    <font><sz val="11"/><color rgb="FF8C8C8C"/><name val="Segoe UI"/></font>                                                <!-- 8: Muted Gray text for Absent -->
  </fonts>
  
  <fills count="11">
    <fill><patternFill patternType="none"/></fill>                                                                           <!-- 0: Default None -->
    <fill><patternFill patternType="gray125"/></fill>                                                                        <!-- 1: Grey scale default -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF0F172A"/><bgColor indexed="64"/></patternFill></fill>             <!-- 2: Title Card Slate Background -->
    <fill><patternFill patternType="solid"><fgColor rgb="FF1E3A8A"/><bgColor indexed="64"/></patternFill></fill>             <!-- 3: Table Header Royal Blue Bg -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFD2E4FC"/><bgColor indexed="64"/></patternFill></fill>             <!-- 4: Light Blue (Kharchi) -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFDCFCE7"/><bgColor indexed="64"/></patternFill></fill>             <!-- 5: Light Green (Extra Expense) -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFFEDD5"/><bgColor indexed="64"/></patternFill></fill>             <!-- 6: Light Orange (Weekly Advance) -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF3E8FF"/><bgColor indexed="64"/></patternFill></fill>             <!-- 7: Light Purple (Bonus) -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFFEE2E2"/><bgColor indexed="64"/></patternFill></fill>             <!-- 8: Light Red (Deduction) -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF9FAFB"/><bgColor indexed="64"/></patternFill></fill>             <!-- 9: Alternating light row space -->
    <fill><patternFill patternType="solid"><fgColor rgb="FFF2F4F7"/><bgColor indexed="64"/></patternFill></fill>             <!-- 10: Light Gray (Absent) -->
  </fills>
  
  <borders count="2">
    <border><left/><right/><top/><bottom/></border>                                                                          <!-- 0: None -->
    <border>                                                                                                                 <!-- 1: Thin Grey Gridlines -->
      <left style="thin"><color rgb="FFD1D5DB"/></left>
      <right style="thin"><color rgb="FFD1D5DB"/></right>
      <top style="thin"><color rgb="FFD1D5DB"/></top>
      <bottom style="thin"><color rgb="FFD1D5DB"/></bottom>
    </border>
  </borders>
  
  <cellStyleXfs count="1">
    <xf numFmtId="0" fontId="0" fillId="0" borderId="0"/>
  </cellStyleXfs>
  
  <cellXfs count="18">
    <!-- Index 0: Normal regular text Left styled with border -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center" wrapText="1"/>
    </xf>
    <!-- Index 1: Bold text Left aligned -->
    <xf numFmtId="0" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- Index 2: Title row style banner (Header White Font, Dark Background Slate) -->
    <xf numFmtId="0" fontId="2" fillId="2" borderId="0" xfId="0" applyFont="1" applyFill="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 3: Table main headers style -->
    <xf numFmtId="0" fontId="4" fillId="3" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center" wrapText="1"/>
    </xf>
    <!-- Index 4: Centered Date Format -->
    <xf numFmtId="165" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1" applyNumberFormat="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 5: INR Currency format (Right aligned regular) -->
    <xf numFmtId="164" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1" applyNumberFormat="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- Index 6: INR Currency format (Right aligned bold) -->
    <xf numFmtId="164" fontId="1" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1" applyNumberFormat="1">
      <alignment horizontal="right" vertical="center"/>
    </xf>
    <!-- Index 7: Payment Daily Wage (Kharchi) cell style -->
    <xf numFmtId="0" fontId="5" fillId="4" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 8: Payment Extra Expense cell style -->
    <xf numFmtId="0" fontId="6" fillId="5" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 9: Payment Weekly Advance cell style -->
    <xf numFmtId="0" fontId="7" fillId="6" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 10: Payment Bonus cell style -->
    <xf numFmtId="0" fontId="7" fillId="7" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 11: Payment Deduction cell style -->
    <xf numFmtId="0" fontId="7" fillId="8" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 12: Sub-title Block (Left bold navy) -->
    <xf numFmtId="0" fontId="3" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- Index 13: Normal text Center aligned -->
    <xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyFont="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 14: Alternating Row Style normal text left alignment -->
    <xf numFmtId="0" fontId="0" fillId="9" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center" wrapText="1"/>
    </xf>
    <!-- Index 15: Centered normal text with gray alternating background -->
    <xf numFmtId="0" fontId="0" fillId="9" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
    <!-- Index 16: Absent row regular left aligned (font 8, fill 10) -->
    <xf numFmtId="0" fontId="8" fillId="10" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="left" vertical="center"/>
    </xf>
    <!-- Index 17: Absent row regular center aligned (font 8, fill 10) -->
    <xf numFmtId="0" fontId="8" fillId="10" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1">
      <alignment horizontal="center" vertical="center"/>
    </xf>
  </cellXfs>
</styleSheet>"""
    }

    private fun generateSheet1Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        totalLabourCount: Int,
        totalSitesCount: Int,
        totalPaymentsCount: Int,
        totalPaymentsAmount: Double,
        kharchiAmount: Double,
        extraExpenseAmount: Double,
        advanceAmount: Double,
        bonusAmount: Double,
        deductionAmount: Double,
        netExpense: Double
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        
        val metricTitle = if (isHindi) "वित्तीय रिपोर्ट सारांश" else "NIRMAAN LABOUR FINANCE REPORT"
        val subtitle = if (isHindi) 
            "जनरेट तिथि: $todayStr | व्यवस्थापक: ${user.displayName} | अवधि: $reportPeriodStr"
            else "Generated On: $todayStr | Manager: ${user.displayName} | Period: $reportPeriodStr"

        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <cols>
    <col min="1" max="1" width="38" customWidth="1"/>
    <col min="2" max="2" width="22" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Row 1 & 2: Header Section (to be merged)
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, metricTitle, 2))
        sb.append(writeCell(1, 1, "", 2))
        sb.append("</row>")
        
        sb.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, subtitle, 0))
        sb.append("</row>")

        sb.append("<row r=\"3\" ht=\"15\"/>") // blank spacer row

        // Section Title: Metrics Summary
        sb.append("<row r=\"4\" ht=\"24\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "प्रमुख परिचालन मेट्रिक्स" else "OPERATIONAL STATISTICS", 12))
        sb.append("</row>")

        // Table Header
        sb.append("<row r=\"5\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(5, 0, if (isHindi) "परिचालन संकेतक" else "Operational Metrics Indicator", 3))
        sb.append(writeCell(5, 1, if (isHindi) "मूल्य / डेटा" else "Recorded Value", 3))
        sb.append("</row>")

        // Row metrics listing
        val metrics = listOf(
            Pair(if (isHindi) "कुल सक्रिय पंजीकृत मजदूर" else "Total Active Registered Labour", totalLabourCount),
            Pair(if (isHindi) "कुल सक्रिय निर्माण स्थल (Sites)" else "Total Active Sites Operated", totalSitesCount),
            Pair(if (isHindi) "कुल प्रविष्ट भुगतान स्थानान्तरण" else "Total Remittances Recorded", totalPaymentsCount),
            Pair(if (isHindi) "कुल भुगतान वितरण राशि (Overall)" else "Total Distributed Cash flow", totalPaymentsAmount),
            Pair(if (isHindi) "दैनिक मजदूरी खर्च (Kharchi)" else "Total Daily Wage (Kharchi)", kharchiAmount),
            Pair(if (isHindi) "अतिरिक्त निर्माण खर्च" else "Total Extra Construction Expense", extraExpenseAmount),
            Pair(if (isHindi) "साप्ताहिक एडवांस स्थानान्तरण" else "Total Weekly Advances Distributed", advanceAmount),
            Pair(if (isHindi) "श्रमिक बोनस प्रोत्साहन भुगतान" else "Total Bonus Payments Credited", bonusAmount),
            Pair(if (isHindi) "वेतन कटौती संग्रह (Salary Deductions)" else "Total Penalty/Salary Deductions", deductionAmount),
            Pair(if (isHindi) "कुल शुद्ध निर्माण संचालन व्यय" else "Net Site Operational Expense", netExpense)
        )

        var rIdx = 6
        metrics.forEach { (label, value) ->
            val bgStyle = if (rIdx % 2 == 1) 14 else 0 // Alternating gray rows
            val valStyle = if (value is Number && value.toDouble() > 100) {
                if (rIdx == 15) 6 else 5 // Net Expense or financial value is bold
            } else {
                if (bgStyle == 14) 15 else 13 // simple center aligned centered alternating values
            }

            sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, label, if (rIdx == 15) 1 else bgStyle)) // bold label for net expense
            sb.append(writeCell(rIdx, 1, value, valStyle))
            sb.append("</row>")
            rIdx++
        }

        sb.append("""  </sheetData>
  <mergeCells count="1">
    <mergeCell ref="A1:B1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    private fun generateSheet2Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        payments: List<Payment>,
        sites: List<Site>
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val titleText = if (isHindi) "मजदूर भुगतान रजिस्टर" else "LABOUR REMITTANCE REGISTER"
        val subtitle = if (isHindi) 
            "सक्रिय प्रविष्टियाँ: ${payments.size} | जनरेट तिथि: $todayStr | व्यवस्थापक: ${user.displayName}"
            else "Active Settlements: ${payments.size} | Generated On: $todayStr | Manager: ${user.displayName}"

        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="4" topLeftCell="A5" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="15" customWidth="1"/>
    <col min="2" max="2" width="24" customWidth="1"/>
    <col min="3" max="3" width="25" customWidth="1"/>
    <col min="4" max="4" width="22" customWidth="1"/>
    <col min="5" max="5" width="18" customWidth="1"/>
    <col min="6" max="6" width="30" customWidth="1"/>
    <col min="7" max="7" width="18" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Merged title block
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, titleText, 2))
        for (i in 1..6) sb.append(writeCell(1, i, "", 2))
        sb.append("</row>")
        
        sb.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, subtitle, 0))
        sb.append("</row>")
        
        sb.append("<row r=\"3\" ht=\"15\"/>")

        // Main Table headers
        sb.append("<row r=\"4\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "तारीख (Date)" else "Settlement Date", 3))
        sb.append(writeCell(4, 1, if (isHindi) "मजदूर नाम" else "Labour Employee Name", 3))
        sb.append(writeCell(4, 2, if (isHindi) "साइट का नाम" else "Site Destination", 3))
        sb.append(writeCell(4, 3, if (isHindi) "भुगतान प्रकार (Type)" else "Remittance Category", 3))
        sb.append(writeCell(4, 4, if (isHindi) "राशि" else "Amount (INR)", 3))
        sb.append(writeCell(4, 5, if (isHindi) "टिप्पणी / विवरण" else "Memo / Notes", 3))
        sb.append(writeCell(4, 6, if (isHindi) "प्रबंधक (Added By)" else "Added By (UID)", 3))
        sb.append("</row>")

        // Data listing
        var rIdx = 5
        if (payments.isEmpty()) {
            sb.append("<row r=\"$rIdx\" ht=\"25\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, if (isHindi) "कोई डेटा उपलब्ध नहीं है" else "No Data Available", 0))
            for (i in 1..6) sb.append(writeCell(rIdx, i, "", 0))
            sb.append("</row>")
        } else {
            payments.forEach { payment ->
                val siteName = sites.find { it.id == payment.siteId }?.name ?: "Main Site Office"
                
                // Color Code styling map based on payment type
                val styleType = when (payment.paymentType) {
                    "Daily Wage (Kharchi)" -> 7
                    "Light Blue (Daily Wage)", "Kharchi" -> 7
                    "Extra Expense" -> 8
                    "Weekly Advance", "Advance" -> 9
                    "Bonus" -> 10
                    "Deduction" -> 11
                    else -> {
                        // check matching remarks if type is fuzzy
                        val rem = payment.remarks.lowercase()
                        if (rem.contains("advance")) 9
                        else if (rem.contains("bonus") || rem.contains("extra")) 10
                        else if (rem.contains("deduct") || rem.contains("cut")) 11
                        else if (rIdx % 2 == 1) 14 else 0
                    }
                }

                // Format: Date DD/MM/YYYY Center parsed from YYYY-MM-DD
                val formattedDate = try {
                    val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(payment.date)
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(originalDate)
                } catch (e: Exception) {
                    payment.date
                }

                val altRowStyle = if (rIdx % 2 == 1) 14 else 0

                sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
                sb.append(writeCell(rIdx, 0, formattedDate, if (rIdx % 2 == 1) 15 else 4)) // Date
                sb.append(writeCell(rIdx, 1, payment.labourName, 1))                      // Labour (Bold)
                sb.append(writeCell(rIdx, 2, siteName, altRowStyle))                      // Site Name
                sb.append(writeCell(rIdx, 3, payment.paymentType, styleType))             // Payment type color
                sb.append(writeCell(rIdx, 4, payment.amount, 6))                          // Amount (right aligned bold currency)
                sb.append(writeCell(rIdx, 5, payment.remarks, altRowStyle))               // remarks
                sb.append(writeCell(rIdx, 6, user.displayName, altRowStyle))              // Admin Manager
                sb.append("</row>")
                rIdx++
            }
        }

        // Apply filters & closing tags
        val autoEndRow = rIdx - 1
        sb.append("""  </sheetData>
  <autoFilter ref="A4:G$autoEndRow"/>
  <mergeCells count="1">
    <mergeCell ref="A1:G1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    private fun generateSheet3Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        sites: List<Site>,
        labours: List<Labour>,
        payments: List<Payment>,
        siteExpenses: List<SiteExpense> = emptyList()
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val titleText = if (isHindi) "साइटवार व्यय लेजर" else "SITE OPERATIONAL EXPENSE LEDGER"
        
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="4" topLeftCell="A5" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="28" customWidth="1"/>
    <col min="2" max="2" width="16" customWidth="1"/>
    <col min="3" max="3" width="20" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="18" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="16" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Title Block info
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, titleText, 2))
        for (i in 1..6) sb.append(writeCell(1, i, "", 2))
        sb.append("</row>")

        sb.append("<row r=\"2\" ht=\"18\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, "Generated On: $todayStr | Active Sites: ${sites.size} | Manager: ${user.displayName}", 0))
        sb.append("</row>")
        
        sb.append("<row r=\"3\" ht=\"15\"/>")

        // Headers
        sb.append("<row r=\"4\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "साइट का नाम" else "Site Project Destination", 3))
        sb.append(writeCell(4, 1, if (isHindi) "सक्रिय मजदूर" else "Assigned Labours", 3))
        sb.append(writeCell(4, 2, if (isHindi) "कुल व्यय (Expense)" else "Total Expense (INR)", 3))
        sb.append(writeCell(4, 3, if (isHindi) "खर्ची (Kharchi)" else "Kharchi Distributed", 3))
        sb.append(writeCell(4, 4, if (isHindi) "एडवांस (Advance)" else "Advances Paid", 3))
        sb.append(writeCell(4, 5, if (isHindi) "कटौती (Deduction)" else "Deductions Deducted", 3))
        sb.append(writeCell(4, 6, if (isHindi) "स्थिति (Status)" else "Project Status", 3))
        sb.append("</row>")

        var rIdx = 5
        var maxExpense = 0.0
        var maxExpenseSiteName = "N/A"

        if (sites.isEmpty()) {
            sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, if (isHindi) "कोई साइट उपलब्ध नहीं है" else "No Construction Sites Created", 0))
            for (i in 1..6) sb.append(writeCell(rIdx, i, "", 0))
            sb.append("</row>")
            rIdx++
        } else {
            sites.forEach { site ->
                val siteWorkers = labours.filter { it.siteId == site.id && it.status == "Active" }
                val sitePayments = payments.filter { it.siteId == site.id }
                val siteNonLabourExpenses = siteExpenses.filter { it.siteId == site.id }
                
                val siteKharchi = sitePayments.filter { it.paymentType.contains("Kharchi") || it.paymentType.contains("Daily") }.sumOf { it.amount }
                val siteAdvance = sitePayments.filter { it.paymentType.contains("Advance") || it.paymentType.contains("Weekly") }.sumOf { it.amount }
                val siteDeduction = sitePayments.filter { it.paymentType.contains("Deduction") }.sumOf { it.amount }
                val siteBonus = sitePayments.filter { it.paymentType.contains("Bonus") }.sumOf { it.amount }
                val siteExtra = sitePayments.filter { it.paymentType.contains("Extra") }.sumOf { it.amount }
                val nonLabourExpSum = siteNonLabourExpenses.sumOf { it.amount }

                val siteTotalExpense = siteKharchi + siteExtra + siteAdvance + siteBonus - siteDeduction + nonLabourExpSum

                if (siteTotalExpense > maxExpense) {
                    maxExpense = siteTotalExpense
                    maxExpenseSiteName = site.name
                }

                val altRowStyle = if (rIdx % 2 == 1) 14 else 0
                val altCenterStyle = if (rIdx % 2 == 1) 15 else 13

                sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
                sb.append(writeCell(rIdx, 0, site.name, 1))                               // Site Name (Bold)
                sb.append(writeCell(rIdx, 1, siteWorkers.size, altCenterStyle))           // Active workers count
                sb.append(writeCell(rIdx, 2, siteTotalExpense, 6))                        // Total Expense Bold currency
                sb.append(writeCell(rIdx, 3, siteKharchi, 5))                             // Kharchi
                sb.append(writeCell(rIdx, 4, siteAdvance, 5))                             // Advance
                sb.append(writeCell(rIdx, 5, siteDeduction, 5))                           // Deduction
                sb.append(writeCell(rIdx, 6, site.status.uppercase(), altCenterStyle))     // Status (Active / Archived)
                sb.append("</row>")
                rIdx++
            }
        }

        // Print analytics card 3 rows under sheet
        val autoEndRow = rIdx - 1
        rIdx += 2

        sb.append("<row r=\"$rIdx\" ht=\"24\" customHeight=\"1\">")
        sb.append(writeCell(rIdx, 0, if (isHindi) "साइट विश्लेषण सारांश" else "SITE PERFORMANCE ANALYTICS SUMMARY", 12))
        sb.append("</row>")
        rIdx++

        val analyticsList = listOf(
            Pair(if (isHindi) "उच्चतम खर्च वाली साइट" else "Highest Construction Expense Site", maxExpenseSiteName),
            Pair(if (isHindi) "उच्चतम साइट परिचालन व्यय" else "Highest Site Capital Spent", if (maxExpense > 0) maxExpense else "₹ 0.00"),
            Pair(if (isHindi) "कुल सक्रिय निर्माण साइटें" else "Total Active Site Count", sites.count { it.status == "Active" }),
            Pair(if (isHindi) "मजदूरों का भौगोलिक वितरण" else "Operational Site Distribution Rate", "${labours.filter { it.siteId != null }.size} Workers Assigned")
        )

        analyticsList.forEach { (label, value) ->
            sb.append("<row r=\"$rIdx\" ht=\"20\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, label, 0))
            sb.append(writeCell(rIdx, 1, value, 1))
            sb.append("</row>")
            rIdx++
        }

        sb.append("""  </sheetData>
  <autoFilter ref="A4:G$autoEndRow"/>
  <mergeCells count="1">
    <mergeCell ref="A1:G1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    private fun generateSheet4Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        attendances: List<Attendance>,
        labours: List<Labour>,
        sites: List<Site>,
        payments: List<Payment>
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val titleText = if (isHindi) "दैनिक मजदूर उपस्थिति लॉग" else "DAILY LABOUR ATTENDANCE REGISTER"
        val subtitle = if (isHindi) 
            "सक्रिय प्रविष्टियाँ: ${attendances.size} | जनरेट तिथि: $todayStr | व्यवस्थापक: ${user.displayName}"
            else "Active Enrolments: ${attendances.size} | Generated On: $todayStr | Manager: ${user.displayName}"

        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="4" topLeftCell="A5" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="15" customWidth="1"/>
    <col min="2" max="2" width="24" customWidth="1"/>
    <col min="3" max="3" width="25" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="22" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Merged title block
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, titleText, 2))
        for (i in 1..5) sb.append(writeCell(1, i, "", 2))
        sb.append("</row>")
        
        sb.append("<row r=\"2\" ht=\"20\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, subtitle, 0))
        sb.append("</row>")
        
        sb.append("<row r=\"3\" ht=\"15\"/>")

        // Main Table headers
        sb.append("<row r=\"4\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "तारीख (Date)" else "Date", 3))
        sb.append(writeCell(4, 1, if (isHindi) "मजदूर का नाम" else "Labour Name", 3))
        sb.append(writeCell(4, 2, if (isHindi) "साइट" else "Site", 3))
        sb.append(writeCell(4, 3, if (isHindi) "स्थिति (Status)" else "Status", 3))
        sb.append(writeCell(4, 4, if (isHindi) "भुगतान प्रकार" else "Payment Type", 3))
        sb.append(writeCell(4, 5, if (isHindi) "राशि" else "Amount", 3))
        sb.append("</row>")

        // Data listing
        var rIdx = 5
        if (attendances.isEmpty()) {
            sb.append("<row r=\"$rIdx\" ht=\"25\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, if (isHindi) "कोई डेटा उपलब्ध नहीं है" else "No Attendance Records", 0))
            for (i in 1..5) sb.append(writeCell(rIdx, i, "", 0))
            sb.append("</row>")
        } else {
            attendances.forEach { attendance ->
                val labourObj = labours.find { it.id == attendance.labourId }
                val labourName = labourObj?.name ?: "Unknown"
                val siteName = sites.find { it.id == attendance.siteId }?.name ?: "Main Site Office"
                
                val formattedDate = try {
                    val originalDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).parse(attendance.date)
                    SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(originalDate)
                } catch (e: Exception) {
                    attendance.date
                }

                val isAbsent = attendance.status == "Absent"
                val isHalfDay = attendance.status == "Half Day"
                val statusStr = if (isHindi) {
                    when {
                        isAbsent -> "अनुपस्थित (Absent)"
                        isHalfDay -> "हाफ डे (Half Day)"
                        else -> "उपस्थित (Present)"
                    }
                } else {
                    attendance.status
                }

                val rowStyleLeft = if (isAbsent) 16 else 1
                val rowStyleCenter = if (isAbsent) 17 else 15
                val cellAltStyle = if (isAbsent) 16 else (if (rIdx % 2 == 1) 14 else 0)

                val paymentTypeStr = if (isAbsent) "—" else "Kharchi"
                val amountVal = when {
                    isAbsent -> "—"
                    isHalfDay -> (labourObj?.dailyWage ?: 0.0) / 2.0
                    else -> (labourObj?.dailyWage ?: 0.0)
                }

                sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
                sb.append(writeCell(rIdx, 0, formattedDate, rowStyleCenter)) // Date
                sb.append(writeCell(rIdx, 1, labourName, rowStyleLeft))       // Labour Name
                sb.append(writeCell(rIdx, 2, siteName, cellAltStyle))         // Site
                sb.append(writeCell(rIdx, 3, statusStr, rowStyleCenter))      // Status
                sb.append(writeCell(rIdx, 4, paymentTypeStr, cellAltStyle))   // Payment Type
                sb.append(writeCell(rIdx, 5, if (isAbsent) "—" else amountVal, if (isAbsent) rowStyleCenter else 6)) // Amount
                sb.append("</row>")
                rIdx++
            }
        }

        sb.append("""  </sheetData>
  <mergeCells count="1">
    <mergeCell ref="A1:F1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    private fun generateSheet5Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        payments: List<Payment>
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val titleText = if (isHindi) "मासिक व्यय विश्लेषण" else "MONTHLY OPERATION ANALYTICS"

        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="4" topLeftCell="A5" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="22" customWidth="1"/>
    <col min="2" max="2" width="18" customWidth="1"/>
    <col min="3" max="3" width="18" customWidth="1"/>
    <col min="4" max="4" width="18" customWidth="1"/>
    <col min="5" max="5" width="18" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="20" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Title Block Header
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, titleText, 2))
        for (i in 1..6) sb.append(writeCell(1, i, "", 2))
        sb.append("</row>")

        sb.append("<row r=\"2\" ht=\"18\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, "Report Period: $reportPeriodStr | Print Date: $todayStr | Audit Specialist: ${user.displayName}", 0))
        sb.append("</row>")
        
        sb.append("<row r=\"3\" ht=\"15\"/>")

        // Table Columns headers
        sb.append("<row r=\"4\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "महीना (Month Summary)" else "Operating Month", 3))
        sb.append(writeCell(4, 1, if (isHindi) "खर्ची (Kharchi)" else "Kharchi Amount", 3))
        sb.append(writeCell(4, 2, if (isHindi) "अतिरिक्त खर्च (Extra)" else "Extra Expenses", 3))
        sb.append(writeCell(4, 3, if (isHindi) "एडवांस (Advance)" else "Advances Allocated", 3))
        sb.append(writeCell(4, 4, if (isHindi) "बोनस (Bonus)" else "Bonus Promos", 3))
        sb.append(writeCell(4, 5, if (isHindi) "कटौती (Deduction)" else "Penalty Deductions", 3))
        sb.append(writeCell(4, 6, if (isHindi) "शुद्ध कुल खर्च" else "Net Monthly Capital", 3))
        sb.append("</row>")

        // Group payments by Month YYYY-MM
        val monthlyGroup = payments.groupBy { payment ->
            try {
                if (payment.date.length >= 7) {
                    payment.date.substring(0, 7) // YYYY-MM
                } else {
                    "Unknown"
                }
            } catch (e: Exception) {
                "Unknown"
            }
        }.toSortedMap()

        var rIdx = 5
        if (monthlyGroup.isEmpty()) {
            sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, if (isHindi) "कोई भुगतान लेनदेन विवरण उपलब्ध नहीं" else "No Monthly Transmissions Issued", 0))
            for (i in 1..6) sb.append(writeCell(rIdx, i, "", 0))
            sb.append("</row>")
            rIdx++
        } else {
            monthlyGroup.forEach { (yearMonth, monthPayments) ->
                val displayMonthName = getDisplayMonthName(yearMonth, isHindi)

                val payKharchi = monthPayments.filter { it.paymentType.contains("Kharchi") || it.paymentType.contains("Daily") }.sumOf { it.amount }
                val payExtra = monthPayments.filter { it.paymentType.contains("Extra") }.sumOf { it.amount }
                val payAdvance = monthPayments.filter { it.paymentType.contains("Advance") || it.paymentType.contains("Weekly") }.sumOf { it.amount }
                val payBonus = monthPayments.filter { it.paymentType.contains("Bonus") }.sumOf { it.amount }
                val payDeduct = monthPayments.filter { it.paymentType.contains("Deduction") }.sumOf { it.amount }

                val netMonthExpense = payKharchi + payExtra + payAdvance + payBonus - payDeduct

                val altRowStyle = if (rIdx % 2 == 1) 14 else 0

                sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
                sb.append(writeCell(rIdx, 0, displayMonthName, 1))                      // Operating Month
                sb.append(writeCell(rIdx, 1, payKharchi, 5))                            // Kharchi
                sb.append(writeCell(rIdx, 2, payExtra, 5))                              // Extra Expense
                sb.append(writeCell(rIdx, 3, payAdvance, 5))                            // Advance
                sb.append(writeCell(rIdx, 4, payBonus, 5))                              // Bonus
                sb.append(writeCell(rIdx, 5, payDeduct, 5))                            // Deduction
                sb.append(writeCell(rIdx, 6, netMonthExpense, 6))                       // Net Expense (bold)
                sb.append("</row>")
                rIdx++
            }
        }

        val autoEndRow = rIdx - 1
        sb.append("""  </sheetData>
  <autoFilter ref="A4:G$autoEndRow"/>
  <mergeCells count="1">
    <mergeCell ref="A1:G1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    private fun getDisplayMonthName(yearMonth: String, isHindi: Boolean): String {
        if (yearMonth == "Unknown") return if (isHindi) "अज्ञात" else "Unknown Date"
        val parts = yearMonth.split("-")
        if (parts.size != 2) return yearMonth
        val year = parts[0]
        val monthCode = parts[1]

        val monthName = when (monthCode) {
            "01" -> if (isHindi) "जनवरी" else "January"
            "02" -> if (isHindi) "फ़रवरी" else "February"
            "03" -> if (isHindi) "मार्च" else "March"
            "04" -> if (isHindi) "अप्रैल" else "April"
            "05" -> if (isHindi) "मई" else "May"
            "06" -> if (isHindi) "जून" else "June"
            "07" -> if (isHindi) "जुलाई" else "July"
            "08" -> if (isHindi) "अगस्त" else "August"
            "09" -> if (isHindi) "सितंबर" else "September"
            "10" -> if (isHindi) "अक्टूबर" else "October"
            "11" -> if (isHindi) "नवंबर" else "November"
            "12" -> if (isHindi) "दिसंबर" else "December"
            else -> monthCode
        }
        return "$monthName $year"
    }

    private fun generateSheet6Xml(
        user: UserProfile,
        reportPeriodStr: String,
        isHindi: Boolean,
        siteExpenses: List<SiteExpense>,
        sites: List<Site>
    ): String {
        val todayStr = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date())
        val titleText = if (isHindi) "गैर-मजदूर साइट व्यय रजिस्टर" else "NON-LABOUR SITE EXPENSE REGISTER"
        
        val sb = java.lang.StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<worksheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
  <sheetViews>
    <sheetView tabSelected="0" workbookViewId="0">
      <pane ySplit="4" topLeftCell="A5" activePane="bottomLeft" state="frozen"/>
    </sheetView>
  </sheetViews>
  <cols>
    <col min="1" max="1" width="16" customWidth="1"/>
    <col min="2" max="2" width="25" customWidth="1"/>
    <col min="3" max="3" width="22" customWidth="1"/>
    <col min="4" max="4" width="16" customWidth="1"/>
    <col min="5" max="5" width="20" customWidth="1"/>
    <col min="6" max="6" width="18" customWidth="1"/>
    <col min="7" max="7" width="28" customWidth="1"/>
  </cols>
  <sheetData>""")

        // Title Block info
        sb.append("<row r=\"1\" ht=\"28\" customHeight=\"1\">")
        sb.append(writeCell(1, 0, titleText, 2))
        for (i in 1..6) sb.append(writeCell(1, i, "", 2))
        sb.append("</row>")

        sb.append("<row r=\"2\" ht=\"18\" customHeight=\"1\">")
        sb.append(writeCell(2, 0, "Generated On: $todayStr | Total Expense Records: ${siteExpenses.size} | Manager: ${user.displayName}", 0))
        sb.append("</row>")
        
        sb.append("<row r=\"3\" ht=\"15\"/>")

        // Headers
        sb.append("<row r=\"4\" ht=\"25\" customHeight=\"1\">")
        sb.append(writeCell(4, 0, if (isHindi) "दिनांक" else "Date", 3))
        sb.append(writeCell(4, 1, if (isHindi) "साइट" else "Site", 3))
        sb.append(writeCell(4, 2, if (isHindi) "खर्च विवरण" else "Expense Name", 3))
        sb.append(writeCell(4, 3, if (isHindi) "राशि" else "Amount (INR)", 3))
        sb.append(writeCell(4, 4, if (isHindi) "किसे भुगतान किया" else "Paid To", 3))
        sb.append(writeCell(4, 5, if (isHindi) "श्रेणी" else "Category", 3))
        sb.append(writeCell(4, 6, if (isHindi) "टिप्पणी / विवरण" else "Description Details", 3))
        sb.append("</row>")

        var rIdx = 5
        if (siteExpenses.isEmpty()) {
            sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
            sb.append(writeCell(rIdx, 0, if (isHindi) "कोई साइट खर्च उपलब्ध नहीं है" else "No Site Expense Records Available", 0))
            for (i in 1..6) sb.append(writeCell(rIdx, i, "", 0))
            sb.append("</row>")
            rIdx++
        } else {
            siteExpenses.forEach { exp ->
                val siteName = sites.find { it.id == exp.siteId }?.name ?: "Unknown Site"
                val altRowStyle = if (rIdx % 2 == 1) 14 else 0
                val altCenterStyle = if (rIdx % 2 == 1) 15 else 13

                sb.append("<row r=\"$rIdx\" ht=\"22\" customHeight=\"1\">")
                sb.append(writeCell(rIdx, 0, exp.expenseDate, altCenterStyle))
                sb.append(writeCell(rIdx, 1, siteName, 0))
                sb.append(writeCell(rIdx, 2, exp.expenseName, 1))
                sb.append(writeCell(rIdx, 3, exp.amount, 6)) // Style 6 is bold currency right-aligned
                sb.append(writeCell(rIdx, 4, exp.paidTo, 0))
                sb.append(writeCell(rIdx, 5, exp.category, altCenterStyle))
                sb.append(writeCell(rIdx, 6, exp.description, 0))
                sb.append("</row>")
                rIdx++
            }
        }

        val autoEndRow = rIdx - 1

        sb.append("""  </sheetData>
  <autoFilter ref="A4:G$autoEndRow"/>
  <mergeCells count="1">
    <mergeCell ref="A1:G1"/>
  </mergeCells>
</worksheet>""")
        return sb.toString()
    }

    /**
     * Share the generated Excel file via generic Android system Chooser.
     */
    fun shareGeneratedExcel(context: Context, excelFile: File, title: String) {
        try {
            val authority = "${context.packageName}.fileprovider"
            val uri = FileProvider.getUriForFile(context, authority, excelFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(intent, title).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            android.util.Log.e("ExcelExporter", "Failed to share Excel report", e)
        }
    }
}
