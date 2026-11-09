package com.pushpyns.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "profile")
data class ProfileEntity(@PrimaryKey val id: Long = PROFILE_ID, val name: String, val businessName: String, val useType: String)

@Entity(
    tableName = "categories",
    indices = [Index(value = ["name"], unique = true)],
)
data class CategoryEntity(@PrimaryKey(autoGenerate = true) val id: Long = 0, val name: String, val createdAt: Long)

@Entity(tableName = "pyns")
data class PynEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double?,
    val longitude: Double?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "pyn_categories",
    primaryKeys = ["pynId", "categoryId"],
    foreignKeys = [
        ForeignKey(
            entity = PynEntity::class,
            parentColumns = ["id"],
            childColumns = ["pynId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("categoryId")],
)
data class PynCategoryCrossRef(val pynId: Long, val categoryId: Long)

@Entity(
    tableName = "pyn_photos",
    foreignKeys = [
        ForeignKey(
            entity = PynEntity::class,
            parentColumns = ["id"],
            childColumns = ["pynId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("pynId")],
)
data class PynPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val pynId: Long,
    val relativePath: String,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(
    tableName = "work_orders",
    foreignKeys = [
        ForeignKey(
            entity = PynEntity::class,
            parentColumns = ["id"],
            childColumns = ["pynId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("pynId")],
)
data class WorkOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
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
    val status: String,
    val termsAcceptedAt: Long?,
    val signaturePath: String?,
    val completedAt: Long?,
    val pdfPath: String?,
    val createdAt: Long,
    val updatedAt: Long,
)

@Entity(
    tableName = "work_order_line_items",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["workOrderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workOrderId")],
)
data class LineItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val description: String,
    val quantity: String,
    val unitPriceCents: Long,
    val sortOrder: Int,
)

@Entity(
    tableName = "work_order_photos",
    foreignKeys = [
        ForeignKey(
            entity = WorkOrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["workOrderId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("workOrderId")],
)
data class WorkOrderPhotoEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val workOrderId: Long,
    val relativePath: String,
    val sortOrder: Int,
    val createdAt: Long,
)

@Entity(tableName = "entitlement")
data class EntitlementEntity(@PrimaryKey val id: Long = ENTITLEMENT_ID, val proEnabled: Boolean, val source: String, val updatedAt: Long)

const val PROFILE_ID = 1L
const val ENTITLEMENT_ID = 1L
