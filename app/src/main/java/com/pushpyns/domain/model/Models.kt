package com.pushpyns.domain.model

enum class UseType {
    PERSONAL,
    BUSINESS,
    BOTH,
}

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
}

enum class PynSort {
    CATEGORY,
    NAME,
    DISTANCE,
    DATE,
}

enum class WorkOrderStatus {
    OPEN,
    COMPLETE,
}

data class Profile(val name: String, val businessName: String, val useType: UseType)

data class Category(val id: Long, val name: String)

data class Photo(val id: Long, val relativePath: String, val createdAt: Long)

data class Pyn(
    val id: Long,
    val name: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double?,
    val longitude: Double?,
    val categories: List<Category>,
    val photos: List<Photo>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class PynDraft(
    val id: Long?,
    val name: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double?,
    val longitude: Double?,
    val categoryIds: List<Long>,
    val photoPaths: List<String>,
)

data class LineItem(val id: Long, val description: String, val quantity: String, val unitPriceCents: Long)

data class LineItemDraft(val description: String, val quantity: String, val unitPriceCents: Long)

data class WorkOrder(
    val id: Long,
    val pynId: Long?,
    val linkedPynName: String?,
    val name: String,
    val customerName: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double?,
    val longitude: Double?,
    val scheduledAt: Long?,
    val status: WorkOrderStatus,
    val termsAcceptedAt: Long?,
    val signaturePath: String?,
    val completedAt: Long?,
    val pdfPath: String?,
    val lineItems: List<LineItem>,
    val photos: List<Photo>,
    val createdAt: Long,
    val updatedAt: Long,
)

data class WorkOrderDraft(
    val id: Long?,
    val pynId: Long?,
    val name: String,
    val customerName: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double?,
    val longitude: Double?,
    val scheduledAt: Long?,
    val lineItems: List<LineItemDraft>,
    val photoPaths: List<String>,
)

data class GeoPoint(val latitude: Double, val longitude: Double)

data class PostalAddress(val fullLine: String, val street: String, val city: String, val state: String, val zip: String)

const val MAX_PHOTOS = 8
