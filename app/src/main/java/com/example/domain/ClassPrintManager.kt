package com.example.domain

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.PageRange
import android.print.PrintAttributes
import android.print.PrintDocumentAdapter
import android.print.PrintDocumentInfo
import android.print.PrintManager
import com.example.data.model.Student
import java.io.FileOutputStream
import java.io.IOException

/**
 * Handles class-wise QR Code badge grid printing formatted for standard A4 paper size.
 * Utilizes the native Android PrintManager and PdfDocument APIs.
 */
object ClassPrintManager {

    private const val PAGE_WIDTH = 595 // Standard A4 width in PostScript points (72 dpi)
    private const val PAGE_HEIGHT = 842 // Standard A4 height in PostScript points (72 dpi)

    private const val COLUMNS_PER_PAGE = 3
    private const val ROWS_PER_PAGE = 4
    private const val BADGES_PER_PAGE = COLUMNS_PER_PAGE * ROWS_PER_PAGE // 12 badges per A4 sheet

    fun printClassBadges(
        context: Context,
        className: String,
        students: List<Student>
    ) {
        val printManager = context.getSystemService(Context.PRINT_SERVICE) as? PrintManager
            ?: return

        val jobName = "بطاقات_تلاميذ_${className}_QR"
        val printAdapter = StudentBadgePrintAdapter(context, className, students)

        val printAttributes = PrintAttributes.Builder()
            .setMediaSize(PrintAttributes.MediaSize.ISO_A4)
            .setColorMode(PrintAttributes.COLOR_MODE_COLOR)
            .setMinMargins(PrintAttributes.Margins.NO_MARGINS)
            .build()

        printManager.print(jobName, printAdapter, printAttributes)
    }

    private class StudentBadgePrintAdapter(
        private val context: Context,
        private val className: String,
        private val students: List<Student>
    ) : PrintDocumentAdapter() {

        private var totalPages = 1

        override fun onLayout(
            oldAttributes: PrintAttributes?,
            newAttributes: PrintAttributes,
            cancellationSignal: CancellationSignal?,
            callback: LayoutResultCallback,
            extras: Bundle?
        ) {
            if (cancellationSignal?.isCanceled == true) {
                callback.onLayoutCancelled()
                return
            }

            totalPages = if (students.isEmpty()) 1 else (students.size + BADGES_PER_PAGE - 1) / BADGES_PER_PAGE

            val info = PrintDocumentInfo.Builder("بطاقات_$className.pdf")
                .setContentType(PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                .setPageCount(totalPages)
                .build()

            callback.onLayoutFinished(info, true)
        }

        override fun onWrite(
            pages: Array<out PageRange>?,
            destination: ParcelFileDescriptor,
            cancellationSignal: CancellationSignal?,
            callback: WriteResultCallback
        ) {
            val pdfDocument = PdfDocument()

            try {
                val headerPaint = Paint().apply {
                    color = Color.rgb(11, 59, 36) // Algerian deep green
                    textSize = 10f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val titlePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 12f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val borderPaint = Paint().apply {
                    color = Color.rgb(180, 190, 180)
                    style = Paint.Style.STROKE
                    strokeWidth = 1f
                    isAntiAlias = true
                }

                val namePaint = Paint().apply {
                    color = Color.BLACK
                    textSize = 9f
                    textAlign = Paint.Align.CENTER
                    isFakeBoldText = true
                    isAntiAlias = true
                }

                val detailPaint = Paint().apply {
                    color = Color.rgb(80, 80, 80)
                    textSize = 7.5f
                    textAlign = Paint.Align.CENTER
                    isAntiAlias = true
                }

                val marginX = 24f
                val marginY = 32f
                val headerHeight = 44f
                val availableWidth = PAGE_WIDTH - (marginX * 2)
                val availableHeight = PAGE_HEIGHT - (marginY * 2) - headerHeight

                val badgeWidth = (availableWidth - ((COLUMNS_PER_PAGE - 1) * 12f)) / COLUMNS_PER_PAGE
                val badgeHeight = (availableHeight - ((ROWS_PER_PAGE - 1) * 12f)) / ROWS_PER_PAGE

                for (pageIndex in 0 until totalPages) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback.onWriteCancelled()
                        pdfDocument.close()
                        return
                    }

                    val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageIndex + 1).create()
                    val page = pdfDocument.startPage(pageInfo)
                    val canvas = page.canvas

                    // 1. Draw Page Header (Ministry & Class info)
                    canvas.drawText(
                        "الجمهورية الجزائرية الديمقراطية الشعبية - وزارة التربية الوطنية",
                        PAGE_WIDTH / 2f,
                        marginY + 10f,
                        headerPaint
                    )
                    canvas.drawText(
                        "بطاقات رمز الاستجابة السريعة (QR Code) لتسيير النقاط - الفوج: $className (صفحة ${pageIndex + 1}/$totalPages)",
                        PAGE_WIDTH / 2f,
                        marginY + 26f,
                        titlePaint
                    )

                    // 2. Draw Badges for this page
                    val startIndex = pageIndex * BADGES_PER_PAGE
                    val endIndex = minOf(startIndex + BADGES_PER_PAGE, students.size)

                    for (i in startIndex until endIndex) {
                        val student = students[i]
                        val badgeIndexOnPage = i - startIndex
                        val col = badgeIndexOnPage % COLUMNS_PER_PAGE
                        val row = badgeIndexOnPage / COLUMNS_PER_PAGE

                        val left = marginX + (col * (badgeWidth + 12f))
                        val top = marginY + headerHeight + (row * (badgeHeight + 12f))
                        val right = left + badgeWidth
                        val bottom = top + badgeHeight

                        // Badge background and border
                        val badgeRect = RectF(left, top, right, bottom)
                        canvas.drawRoundRect(badgeRect, 6f, 6f, borderPaint)

                        // Badge top header bar
                        val badgeHeaderRect = RectF(left, top, right, top + 16f)
                        val headerBgPaint = Paint().apply {
                            color = Color.rgb(235, 245, 238)
                            style = Paint.Style.FILL
                        }
                        canvas.drawRoundRect(badgeHeaderRect, 6f, 6f, headerBgPaint)

                        val badgeHeaderTxtPaint = Paint().apply {
                            color = Color.rgb(11, 59, 36)
                            textSize = 7f
                            textAlign = Paint.Align.CENTER
                            isFakeBoldText = true
                            isAntiAlias = true
                        }
                        canvas.drawText(
                            "التعليم المتوسط | ${student.className}",
                            badgeRect.centerX(),
                            top + 11f,
                            badgeHeaderTxtPaint
                        )

                        // Generate & Draw QR Code
                        val qrSizePx = (badgeWidth * 0.58f).toInt()
                        val qrBitmap = QrCodeHelper.generateQrBitmap(student.qrCodeContent, qrSizePx, qrSizePx)
                        val qrLeft = badgeRect.centerX() - (qrSizePx / 2f)
                        val qrTop = top + 20f

                        canvas.drawBitmap(qrBitmap, qrLeft, qrTop, null)

                        // Bottom Student Info in Arabic
                        val textYStart = qrTop + qrSizePx + 11f
                        canvas.drawText(
                            student.fullName,
                            badgeRect.centerX(),
                            textYStart,
                            namePaint
                        )

                        val idLabel = if (student.isManualEntry || student.nationalId.isBlank()) "الرمز: ${student.fullName}" else "الرقم: ${student.nationalId}"
                        canvas.drawText(
                            idLabel,
                            badgeRect.centerX(),
                            textYStart + 11f,
                            detailPaint
                        )

                        val orderTxt = if (student.orderNumber > 0) "رقم القائمة: ${student.orderNumber}" else ""
                        if (orderTxt.isNotBlank()) {
                            canvas.drawText(
                                orderTxt,
                                badgeRect.centerX(),
                                textYStart + 21f,
                                detailPaint
                            )
                        }
                    }

                    pdfDocument.finishPage(page)
                }

                // Write document to stream
                FileOutputStream(destination.fileDescriptor).use { outputStream ->
                    pdfDocument.writeTo(outputStream)
                }
                callback.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            } catch (e: IOException) {
                callback.onWriteFailed(e.message)
            } finally {
                pdfDocument.close()
            }
        }
    }
}
