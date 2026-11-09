package com.pushpyns.data.repository

import com.pushpyns.data.local.LineItemEntity
import com.pushpyns.data.local.WorkOrderDao
import com.pushpyns.data.local.WorkOrderEntity
import com.pushpyns.data.local.WorkOrderPhotoEntity
import com.pushpyns.data.mapper.toDomain
import com.pushpyns.data.media.AppFiles
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderDraft
import com.pushpyns.domain.model.WorkOrderStatus
import com.pushpyns.domain.repository.WorkOrderRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LocalWorkOrderRepository
@Inject
constructor(
    private val dao: WorkOrderDao,
    private val files: AppFiles,
    @IoDispatcher private val io: CoroutineDispatcher,
) : WorkOrderRepository {
    override fun observeAll(): Flow<List<WorkOrder>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(io)

    override fun observe(id: Long): Flow<WorkOrder?> = dao.observeById(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun get(id: Long): WorkOrder? = withContext(io) {
        dao.getById(id)?.toDomain()
    }

    override suspend fun upsert(draft: WorkOrderDraft): Long = withContext(io) {
        val now = System.currentTimeMillis()
        val id =
            if (draft.id == null) {
                dao.insert(draft.toEntity(id = 0, createdAt = now, updatedAt = now, existing = null))
            } else {
                val existing = dao.getById(draft.id)?.workOrder ?: error("Work order ${draft.id} was not found")
                dao.update(
                    draft.toEntity(
                        id = draft.id,
                        createdAt = existing.createdAt,
                        updatedAt = now,
                        existing = existing,
                    ),
                )
                draft.id
            }
        dao.deleteLineItems(id)
        if (draft.lineItems.isNotEmpty()) {
            dao.insertLineItems(
                draft.lineItems.mapIndexed { index, item ->
                    LineItemEntity(
                        workOrderId = id,
                        description = item.description.trim(),
                        quantity = item.quantity,
                        unitPriceCents = item.unitPriceCents,
                        sortOrder = index,
                    )
                },
            )
        }
        val previous = dao.photosFor(id)
        val keep = draft.photoPaths.toSet()
        previous.filter { it.relativePath !in keep }.forEach { files.delete(it.relativePath) }
        dao.deletePhotos(id)
        if (draft.photoPaths.isNotEmpty()) {
            dao.insertPhotos(
                draft.photoPaths.mapIndexed { index, path ->
                    WorkOrderPhotoEntity(
                        workOrderId = id,
                        relativePath = path,
                        sortOrder = index,
                        createdAt = now,
                    )
                },
            )
        }
        id
    }

    override suspend fun delete(id: Long) = withContext(io) {
        val existing = dao.getById(id)?.workOrder
        dao.photosFor(id).forEach { files.delete(it.relativePath) }
        existing?.signaturePath?.let { files.delete(it) }
        existing?.pdfPath?.let { files.delete(it) }
        dao.delete(id)
    }

    override suspend fun recordTermsAccepted(id: Long, atMillis: Long) = withContext(io) {
        dao.recordTerms(id, atMillis)
    }

    override suspend fun markComplete(id: Long, signaturePath: String, pdfPath: String, completedAt: Long) = withContext(io) {
        dao.markComplete(id, signaturePath, pdfPath, completedAt)
    }

    override suspend fun updatePdfPath(id: Long, pdfPath: String) = withContext(io) {
        dao.updatePdf(id, pdfPath, System.currentTimeMillis())
    }

    private fun WorkOrderDraft.toEntity(id: Long, createdAt: Long, updatedAt: Long, existing: WorkOrderEntity?): WorkOrderEntity =
        WorkOrderEntity(
            id = id,
            pynId = pynId,
            name = name.trim(),
            customerName = customerName.trim(),
            description = description.trim(),
            notes = notes.trim(),
            address = address.trim(),
            city = city.trim(),
            state = state.trim(),
            zip = zip.trim(),
            latitude = latitude,
            longitude = longitude,
            scheduledAt = scheduledAt,
            status = existing?.status ?: WorkOrderStatus.OPEN.name,
            termsAcceptedAt = existing?.termsAcceptedAt,
            signaturePath = existing?.signaturePath,
            completedAt = existing?.completedAt,
            pdfPath = existing?.pdfPath,
            createdAt = createdAt,
            updatedAt = updatedAt,
        )
}
