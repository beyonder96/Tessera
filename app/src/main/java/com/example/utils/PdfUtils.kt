package com.example.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.example.data.MarketItem
import java.util.Locale

object PdfUtils {
    fun generateMarketPdf(context: Context, uri: Uri, items: List<MarketItem>, totalAmount: Double) {
        val document = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        var pageNumber = 1

        var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
        var page = document.startPage(pageInfo)
        var canvas: Canvas = page.canvas
        val paint = Paint()

        paint.color = Color.BLACK
        paint.textSize = 24f
        paint.isFakeBoldText = true
        canvas.drawText("Resumo de Compras - Tessera", 50f, 50f, paint)

        paint.textSize = 14f
        paint.isFakeBoldText = false
        var yPos = 100f
        val startX = 50f

        items.forEach { item ->
            if (yPos > pageHeight - 100f) {
                document.finishPage(page)
                pageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                yPos = 60f
            }

            val line = "${item.quantity}x ${item.unit} - ${item.name} | R$ ${String.format(Locale("pt", "BR"), "%.2f", item.price * item.quantity)}"
            canvas.drawText(line, startX, yPos, paint)
            yPos += 25f
        }

        if (yPos > pageHeight - 80f) {
            document.finishPage(page)
            pageNumber++
            pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            page = document.startPage(pageInfo)
            canvas = page.canvas
            yPos = 60f
        }

        yPos += 20f
        paint.isFakeBoldText = true
        paint.textSize = 18f
        val totalText = "TOTAL: R$ ${String.format(Locale("pt", "BR"), "%,.2f", totalAmount)}"
        canvas.drawText(totalText, startX, yPos, paint)

        document.finishPage(page)

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            document.writeTo(outputStream)
        }
        document.close()
    }
}
