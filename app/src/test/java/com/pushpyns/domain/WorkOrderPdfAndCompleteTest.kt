package com.pushpyns.domain

import com.google.common.truth.Truth.assertThat
import com.pushpyns.FakePdfWriter
import com.pushpyns.FakeProfileRepository
import com.pushpyns.FakeWorkOrderRepository
import com.pushpyns.MemoryWorkOrderFiles
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.UseType
import com.pushpyns.domain.model.WorkOrderStatus
import com.pushpyns.domain.pdf.PdfBlock
import com.pushpyns.domain.pdf.WorkOrderPdfComposer
import com.pushpyns.domain.work.CompleteWorkOrder
import com.pushpyns.domain.work.TimeSource
import com.pushpyns.sampleOrder
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertThrows
import org.junit.Test

class WorkOrderPdfAndCompleteTest {
    private val composer = WorkOrderPdfComposer()

    @Test
    fun composerIncludesBusinessCustomerChargesAndSignature() {
        val order =
            sampleOrder().copy(
                signaturePath = "signatures/wo_7.png",
                completedAt = 1_700_000_000_000L,
                status = WorkOrderStatus.COMPLETE,
            )
        val blocks =
            composer.compose(
                order,
                Profile("Matthew Nielsen", "Nielsen Field Service", UseType.BUSINESS),
                generatedAt = 1_700_000_000_000L,
            )
        val text =
            blocks.joinToString("\n") { block ->
                when (block) {
                    is PdfBlock.Title -> block.text
                    is PdfBlock.Subtitle -> block.text
                    is PdfBlock.Section -> block.text
                    is PdfBlock.KeyValue -> "${block.key}=${block.value}"
                    is PdfBlock.Paragraph -> block.text
                    is PdfBlock.TableRow -> block.cells.joinToString("|")
                    is PdfBlock.Total -> "${block.label}=${block.value}"
                    is PdfBlock.Image -> block.caption
                    is PdfBlock.Signature -> block.relativePath
                }
            }
        assertThat(text).contains("Nielsen Field Service")
        assertThat(text).contains("Joe D.")
        assertThat(text).contains("Garage door maintenance")
        assertThat(text).contains("Labor")
        assertThat(text).contains("Total=$150.00")
        assertThat(text).contains("signatures/wo_7.png")
        assertThat(composer.grandTotalCents(order)).isEqualTo(15_000L)
    }

    @Test
    fun completeRequiresTermsAndWritesPdf() = runTest {
        val orders = FakeWorkOrderRepository(listOf(sampleOrder(termsAt = null)))
        val files = MemoryWorkOrderFiles()
        val writer = FakePdfWriter()
        val useCase =
            CompleteWorkOrder(
                workOrders = orders,
                profiles = FakeProfileRepository(),
                pdfWriter = writer,
                files = files,
                time = TimeSource { 99L },
            )
        assertThrows(IllegalStateException::class.java) {
            kotlinx.coroutines.runBlocking { useCase.complete(7L, byteArrayOf(1, 2, 3)) }
        }
        useCase.acceptTerms(7L)
        val done = useCase.complete(7L, byteArrayOf(1, 2, 3))
        assertThat(done.status).isEqualTo(WorkOrderStatus.COMPLETE)
        assertThat(done.completedAt).isEqualTo(99L)
        assertThat(done.pdfPath).isEqualTo("pdfs/work-order-7.pdf")
        assertThat(writer.writes).isEqualTo(1)
        assertThat(writer.lastBusiness).isEqualTo("Ada Field Co")
        assertThat(files.signatures.getValue(7L)).hasLength(3)
    }

    @Test
    fun ensurePdfRewritesWhenFileIsMissing() = runTest {
        val completed =
            sampleOrder().copy(
                status = WorkOrderStatus.COMPLETE,
                signaturePath = "signatures/wo_7.png",
                pdfPath = "pdfs/missing.pdf",
                completedAt = 10L,
            )
        val orders = FakeWorkOrderRepository(listOf(completed))
        val writer = FakePdfWriter()
        val useCase =
            CompleteWorkOrder(
                orders,
                FakeProfileRepository(),
                writer,
                MemoryWorkOrderFiles(),
                TimeSource { 1L },
            )
        val refreshed = useCase.ensurePdf(7L)
        assertThat(writer.writes).isEqualTo(1)
        assertThat(refreshed.pdfPath).isEqualTo("pdfs/work-order-7.pdf")
    }
}
