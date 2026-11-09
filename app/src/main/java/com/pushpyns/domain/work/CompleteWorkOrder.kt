package com.pushpyns.domain.work

import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderStatus
import com.pushpyns.domain.pdf.WorkOrderPdfWriter
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.domain.repository.WorkOrderFiles
import com.pushpyns.domain.repository.WorkOrderRepository

fun interface TimeSource {
    fun nowMillis(): Long
}

class CompleteWorkOrder(
    private val workOrders: WorkOrderRepository,
    private val profiles: ProfileRepository,
    private val pdfWriter: WorkOrderPdfWriter,
    private val files: WorkOrderFiles,
    private val time: TimeSource,
) {
    suspend fun acceptTerms(id: Long) {
        val order = workOrders.get(id) ?: error("Work order $id was not found")
        if (order.termsAcceptedAt == null) {
            workOrders.recordTermsAccepted(id, time.nowMillis())
        }
    }

    suspend fun complete(id: Long, signaturePng: ByteArray): WorkOrder {
        val order = workOrders.get(id) ?: error("Work order $id was not found")
        if (order.termsAcceptedAt == null) error("Terms were not accepted")
        if (signaturePng.isEmpty()) error("Signature is empty")
        if (order.customerName.isBlank() || order.name.isBlank()) {
            error("Customer and job name are required")
        }
        val signaturePath = files.saveSignature(id, signaturePng)
        val completedAt = time.nowMillis()
        val pdfPath = files.pdfRelativePath(id)
        val profile = profiles.get()
        val forPdf =
            order.copy(
                status = WorkOrderStatus.COMPLETE,
                signaturePath = signaturePath,
                completedAt = completedAt,
                pdfPath = pdfPath,
            )
        pdfWriter.write(forPdf, profile, pdfPath)
        workOrders.markComplete(id, signaturePath, pdfPath, completedAt)
        return workOrders.get(id) ?: forPdf
    }

    suspend fun ensurePdf(id: Long): WorkOrder {
        val order = workOrders.get(id) ?: error("Work order $id was not found")
        if (order.status != WorkOrderStatus.COMPLETE) error("Work order is not complete")
        val existing = order.pdfPath
        if (existing != null && files.exists(existing)) return order
        val pdfPath = files.pdfRelativePath(id)
        pdfWriter.write(order, profiles.get(), pdfPath)
        workOrders.updatePdfPath(id, pdfPath)
        return workOrders.get(id) ?: order.copy(pdfPath = pdfPath)
    }
}
