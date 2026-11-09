package com.pushpyns.data.mapper

import com.pushpyns.data.local.CategoryEntity
import com.pushpyns.data.local.LineItemEntity
import com.pushpyns.data.local.PynPhotoEntity
import com.pushpyns.data.local.PynWithDetails
import com.pushpyns.data.local.WorkOrderPhotoEntity
import com.pushpyns.data.local.WorkOrderWithDetails
import com.pushpyns.domain.model.Category
import com.pushpyns.domain.model.LineItem
import com.pushpyns.domain.model.Photo
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.UseType
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderStatus

fun CategoryEntity.toDomain(): Category = Category(id = id, name = name)

fun PynWithDetails.toDomain(): Pyn = Pyn(
    id = pyn.id,
    name = pyn.name,
    description = pyn.description,
    notes = pyn.notes,
    address = pyn.address,
    city = pyn.city,
    state = pyn.state,
    zip = pyn.zip,
    latitude = pyn.latitude,
    longitude = pyn.longitude,
    categories = categories.map { it.toDomain() }.sortedBy { it.name.lowercase() },
    photos = photos.sortedBy { it.sortOrder }.map { it.toDomain() },
    createdAt = pyn.createdAt,
    updatedAt = pyn.updatedAt,
)

fun PynPhotoEntity.toDomain(): Photo = Photo(id = id, relativePath = relativePath, createdAt = createdAt)

fun WorkOrderPhotoEntity.toDomain(): Photo = Photo(id = id, relativePath = relativePath, createdAt = createdAt)

fun LineItemEntity.toDomain(): LineItem = LineItem(
    id = id,
    description = description,
    quantity = quantity,
    unitPriceCents = unitPriceCents,
)

fun WorkOrderWithDetails.toDomain(): WorkOrder = WorkOrder(
    id = workOrder.id,
    pynId = workOrder.pynId,
    linkedPynName = pynName,
    name = workOrder.name,
    customerName = workOrder.customerName,
    description = workOrder.description,
    notes = workOrder.notes,
    address = workOrder.address,
    city = workOrder.city,
    state = workOrder.state,
    zip = workOrder.zip,
    latitude = workOrder.latitude,
    longitude = workOrder.longitude,
    scheduledAt = workOrder.scheduledAt,
    status = runCatching { WorkOrderStatus.valueOf(workOrder.status) }.getOrDefault(WorkOrderStatus.OPEN),
    termsAcceptedAt = workOrder.termsAcceptedAt,
    signaturePath = workOrder.signaturePath,
    completedAt = workOrder.completedAt,
    pdfPath = workOrder.pdfPath,
    lineItems = lineItems.sortedBy { it.sortOrder }.map { it.toDomain() },
    photos = photos.sortedBy { it.sortOrder }.map { it.toDomain() },
    createdAt = workOrder.createdAt,
    updatedAt = workOrder.updatedAt,
)

fun UseType.toStorage(): String = name

fun useTypeFromStorage(value: String): UseType = runCatching { UseType.valueOf(value) }.getOrDefault(UseType.PERSONAL)
