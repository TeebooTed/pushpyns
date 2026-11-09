package com.pushpyns.data.pdf

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.pushpyns.data.media.AppFiles
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.pdf.PdfBlock
import com.pushpyns.domain.pdf.WorkOrderPdfComposer
import com.pushpyns.domain.pdf.WorkOrderPdfWriter
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

@Singleton
class AndroidWorkOrderPdfWriter
@Inject
constructor(
    private val composer: WorkOrderPdfComposer,
    private val files: AppFiles,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WorkOrderPdfWriter {
    private lateinit var activeCanvas: android.graphics.Canvas

    override suspend fun write(order: WorkOrder, profile: Profile?, relativePath: String) {
        withContext(io) {
            val blocks =
                composer.compose(
                    order = order,
                    profile = profile,
                    generatedAt = order.completedAt ?: System.currentTimeMillis(),
                )
            val target = files.file(relativePath)
            target.parentFile?.mkdirs()
            render(blocks, target)
        }
    }

    private fun render(blocks: List<PdfBlock>, output: File) {
        val document = PdfDocument()
        val titlePaint = paint(22f, bold = true, color = Color.rgb(12, 111, 190))
        val subtitlePaint = paint(14f, bold = true, color = Color.rgb(38, 50, 56))
        val sectionPaint = paint(13f, bold = true, color = Color.rgb(12, 111, 190))
        val bodyPaint = paint(11f, bold = false, color = Color.rgb(33, 33, 33))
        val keyPaint = paint(11f, bold = true, color = Color.rgb(69, 90, 100))
        val smallPaint = paint(9f, bold = false, color = Color.rgb(96, 125, 139))
        val headerPaint = paint(11f, bold = true, color = Color.WHITE)
        val barPaint = Paint().apply { color = Color.rgb(12, 111, 190) }
        var pageNumber = 0
        lateinit var page: PdfDocument.Page
        lateinit var canvas: android.graphics.Canvas
        var y = 0f

        fun finish() {
            canvas.drawText(
                "Pushpyns  ·  page $pageNumber  ·  stored on this phone",
                MARGIN,
                PAGE_HEIGHT - 28f,
                smallPaint,
            )
            document.finishPage(page)
        }

        fun openPage(branded: Boolean) {
            if (pageNumber > 0) finish()
            pageNumber += 1
            page = document.startPage(PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create())
            canvas = page.canvas
            activeCanvas = canvas
            if (branded) {
                canvas.drawRect(0f, 0f, PAGE_WIDTH.toFloat(), 36f, barPaint)
                canvas.drawText("PUSHPYNS", MARGIN, 24f, headerPaint)
                y = 56f
            } else {
                y = MARGIN
            }
        }

        fun ensure(height: Float) {
            if (y + height > PAGE_HEIGHT - MARGIN - 24f) openPage(branded = false)
        }

        openPage(branded = true)
        blocks.forEach { block ->
            when (block) {
                is PdfBlock.Title -> {
                    ensure(30f)
                    canvas.drawText(block.text, MARGIN, y, titlePaint)
                    y += 28f
                }
                is PdfBlock.Subtitle -> {
                    ensure(22f)
                    canvas.drawText(block.text, MARGIN, y, subtitlePaint)
                    y += 22f
                }
                is PdfBlock.Section -> {
                    ensure(32f)
                    y += 8f
                    canvas.drawText(block.text, MARGIN, y, sectionPaint)
                    y += 6f
                    canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, sectionPaint)
                    y += 16f
                }
                is PdfBlock.KeyValue -> {
                    val valueLines = wrap(block.value, bodyPaint, CONTENT_WIDTH - 140f)
                    ensure(16f * valueLines.size.coerceAtLeast(1))
                    canvas.drawText(block.key, MARGIN, y, keyPaint)
                    valueLines.forEachIndexed { index, line ->
                        canvas.drawText(line, MARGIN + 140f, y, bodyPaint)
                        if (index < valueLines.lastIndex) y += 14f
                    }
                    y += 16f
                }
                is PdfBlock.Paragraph -> {
                    val lines = wrap(block.text, bodyPaint, CONTENT_WIDTH)
                    ensure(14f * lines.size + 6f)
                    lines.forEach { line ->
                        canvas.drawText(line, MARGIN, y, bodyPaint)
                        y += 14f
                    }
                    y += 6f
                }
                is PdfBlock.TableRow -> {
                    val widths =
                        listOf(
                            CONTENT_WIDTH * 0.46f,
                            CONTENT_WIDTH * 0.14f,
                            CONTENT_WIDTH * 0.2f,
                            CONTENT_WIDTH * 0.2f,
                        )
                    ensure(18f)
                    var x = MARGIN
                    val rowPaint = if (block.header) keyPaint else bodyPaint
                    block.cells.forEachIndexed { index, cell ->
                        val width = widths.getOrElse(index) { 80f }
                        canvas.drawText(ellipsize(cell, rowPaint, width - 6f), x, y, rowPaint)
                        x += width
                    }
                    y += 16f
                }
                is PdfBlock.Total -> {
                    ensure(22f)
                    canvas.drawText("${block.label}  ${block.value}", MARGIN, y, subtitlePaint)
                    y += 20f
                }
                is PdfBlock.Image -> {
                    y = drawPicture(block.relativePath, block.caption, y, bodyPaint, ::ensure)
                }
                is PdfBlock.Signature -> {
                    y = drawPicture(block.relativePath, "Customer signature", y, bodyPaint, ::ensure)
                }
            }
        }
        finish()
        output.outputStream().use { document.writeTo(it) }
        document.close()
    }

    private fun drawPicture(relativePath: String, caption: String, startY: Float, captionPaint: Paint, ensure: (Float) -> Unit): Float {
        val bitmap = decodeScaled(files.file(relativePath), CONTENT_WIDTH.toInt(), 220)
        if (bitmap == null) {
            ensure(16f)
            val top = if (startY + 16f > PAGE_HEIGHT - MARGIN - 24f) MARGIN else startY
            activeCanvas.drawText("$caption (not available)", MARGIN, top, captionPaint)
            return top + 16f
        }
        val blockHeight = bitmap.height + 32f
        ensure(blockHeight)
        val top = if (startY + blockHeight > PAGE_HEIGHT - MARGIN - 24f) MARGIN else startY
        activeCanvas.drawBitmap(bitmap, MARGIN, top, null)
        val captionY = top + bitmap.height + 14f
        activeCanvas.drawText(caption, MARGIN, captionY, captionPaint)
        if (!bitmap.isRecycled) bitmap.recycle()
        return captionY + 18f
    }

    private fun decodeScaled(file: File, maxWidth: Int, maxHeight: Int): Bitmap? {
        if (!file.exists()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > maxWidth * 2 || bounds.outHeight / sample > maxHeight * 2) {
            sample *= 2
        }
        val decoded =
            BitmapFactory.decodeFile(
                file.absolutePath,
                BitmapFactory.Options().apply { inSampleSize = sample },
            ) ?: return null
        val scale = minOf(maxWidth / decoded.width.toFloat(), maxHeight / decoded.height.toFloat(), 1f)
        if (scale >= 0.99f) return decoded
        val scaled =
            Bitmap.createScaledBitmap(
                decoded,
                (decoded.width * scale).toInt().coerceAtLeast(1),
                (decoded.height * scale).toInt().coerceAtLeast(1),
                true,
            )
        if (scaled != decoded) decoded.recycle()
        return scaled
    }

    private fun paint(size: Float, bold: Boolean, color: Int): Paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = size
        this.color = color
        typeface = if (bold) Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD) else Typeface.SANS_SERIF
    }

    internal fun wrap(text: String, paint: Paint, width: Float): List<String> {
        if (text.isBlank()) return listOf("")
        val lines = mutableListOf<String>()
        text.split('\n').forEach { paragraph ->
            var current = ""
            paragraph.split(Regex("\\s+")).forEach { word ->
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (paint.measureText(candidate) <= width) {
                    current = candidate
                } else {
                    if (current.isNotEmpty()) lines += current
                    current = word
                }
            }
            if (current.isNotEmpty()) lines += current
        }
        return lines.ifEmpty { listOf("") }
    }

    private fun ellipsize(text: String, paint: Paint, width: Float): String {
        if (paint.measureText(text) <= width) return text
        var trimmed = text
        while (trimmed.isNotEmpty() && paint.measureText("$trimmed…") > width) {
            trimmed = trimmed.dropLast(1)
        }
        return "$trimmed…"
    }

    private companion object {
        const val PAGE_WIDTH = 612
        const val PAGE_HEIGHT = 792
        const val MARGIN = 48f
        const val CONTENT_WIDTH = PAGE_WIDTH - MARGIN * 2
    }
}
