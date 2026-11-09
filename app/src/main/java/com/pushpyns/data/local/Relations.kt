package com.pushpyns.data.local

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class PynWithDetails(
    @Embedded val pyn: PynEntity,
    @Relation(parentColumn = "id", entityColumn = "pynId")
    val photos: List<PynPhotoEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy =
        Junction(
            value = PynCategoryCrossRef::class,
            parentColumn = "pynId",
            entityColumn = "categoryId",
        ),
    )
    val categories: List<CategoryEntity>,
)

data class WorkOrderWithDetails(
    @Embedded val workOrder: WorkOrderEntity,
    val pynName: String?,
    @Relation(parentColumn = "id", entityColumn = "workOrderId")
    val lineItems: List<LineItemEntity>,
    @Relation(parentColumn = "id", entityColumn = "workOrderId")
    val photos: List<WorkOrderPhotoEntity>,
)
