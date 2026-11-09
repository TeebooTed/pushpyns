package com.pushpyns.domain.pdf

import com.pushpyns.domain.geo.formatAddress
import com.pushpyns.domain.geo.formatCoordinates
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.money.Money
import com.pushpyns.domain.time.formatDateTime

class WorkOrderPdfComposer {
    fun compose(order: WorkOrder, profile: Profile?, generatedAt: Long): List<PdfBlock> {
        val business =
            profile?.businessName?.takeIf { it.isNotBlank() }
                ?: profile?.name?.takeIf { it.isNotBlank() }
                ?: "Pushpyns"
        val blocks = mutableListOf<PdfBlock>()
        blocks += PdfBlock.Title("Work order")
        blocks += PdfBlock.Subtitle(business)
        blocks +=
            PdfBlock.KeyValue(
                "Status",
                order.status.name
                    .lowercase()
                    .replaceFirstChar { it.titlecase() },
            )
        order.completedAt?.let { blocks += PdfBlock.KeyValue("Completed", formatDateTime(it)) }
        blocks += PdfBlock.KeyValue("Generated", formatDateTime(generatedAt))
        profile?.name?.takeIf { it.isNotBlank() }?.let {
            blocks += PdfBlock.KeyValue("Technician", it)
        }
        if (!profile?.businessName.isNullOrBlank() && profile.name.isNotBlank()) {
            blocks += PdfBlock.KeyValue("Business", profile.businessName)
        }

        blocks += PdfBlock.Section("Customer")
        blocks += PdfBlock.KeyValue("Customer", order.customerName.ifBlank { "—" })
        blocks += PdfBlock.KeyValue("Job", order.name.ifBlank { "—" })
        order.scheduledAt?.let { blocks += PdfBlock.KeyValue("Scheduled", formatDateTime(it)) }
        order.linkedPynName?.takeIf { it.isNotBlank() }?.let {
            blocks += PdfBlock.KeyValue("Linked pyn", it)
        }

        blocks += PdfBlock.Section("Location")
        val address = formatAddress(order.address, order.city, order.state, order.zip)
        blocks += PdfBlock.Paragraph(address.ifBlank { "No address recorded." })
        val lat = order.latitude
        val lng = order.longitude
        if (lat != null && lng != null) {
            blocks += PdfBlock.KeyValue("Coordinates", formatCoordinates(lat, lng))
        }

        if (order.description.isNotBlank() || order.notes.isNotBlank()) {
            blocks += PdfBlock.Section("Work")
            if (order.description.isNotBlank()) blocks += PdfBlock.Paragraph(order.description.trim())
            if (order.notes.isNotBlank()) blocks += PdfBlock.KeyValue("Notes", order.notes.trim())
        }

        if (order.lineItems.isNotEmpty()) {
            blocks += PdfBlock.Section("Charges")
            blocks += PdfBlock.TableRow(listOf("Description", "Qty", "Price", "Amount"), header = true)
            var total = 0L
            order.lineItems.forEach { item ->
                val line = Money.lineTotalCents(item.quantity, item.unitPriceCents)
                total += line
                blocks +=
                    PdfBlock.TableRow(
                        listOf(
                            item.description,
                            Money.displayQuantity(item.quantity),
                            Money.formatCents(item.unitPriceCents),
                            Money.formatCents(line),
                        ),
                    )
            }
            blocks += PdfBlock.Total("Total", Money.formatCents(total))
        }

        if (order.photos.isNotEmpty()) {
            blocks += PdfBlock.Section("Photos")
            order.photos.forEachIndexed { index, photo ->
                blocks += PdfBlock.Image(photo.relativePath, "Photo ${index + 1}")
            }
        }

        blocks += PdfBlock.Section("Acceptance")
        order.termsAcceptedAt?.let {
            blocks += PdfBlock.KeyValue("Terms accepted", formatDateTime(it))
        }
        blocks +=
            PdfBlock.Paragraph(
                "The customer accepted the in-app terms and signed below. " +
                    "This copy was created on the technician's phone by Pushpyns.",
            )
        order.signaturePath?.let { blocks += PdfBlock.Signature(it) }
        return blocks
    }

    fun grandTotalCents(order: WorkOrder): Long = order.lineItems.sumOf { Money.lineTotalCents(it.quantity, it.unitPriceCents) }
}
