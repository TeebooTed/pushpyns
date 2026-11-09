package com.pushpyns.data.repository

import com.pushpyns.data.local.CategoryDao
import com.pushpyns.data.local.CategoryEntity
import com.pushpyns.data.local.PynCategoryCrossRef
import com.pushpyns.data.local.PynDao
import com.pushpyns.data.local.PynEntity
import com.pushpyns.data.local.PynPhotoEntity
import com.pushpyns.data.mapper.toDomain
import com.pushpyns.data.media.AppFiles
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.repository.PynRepository
import com.pushpyns.domain.sample.samplePlaces
import com.pushpyns.domain.sample.toDraft
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LocalPynRepository
@Inject
constructor(
    private val pynDao: PynDao,
    private val categoryDao: CategoryDao,
    private val files: AppFiles,
    @IoDispatcher private val io: CoroutineDispatcher,
) : PynRepository {
    override fun observeAll(): Flow<List<Pyn>> = pynDao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(io)

    override fun observe(id: Long): Flow<Pyn?> = pynDao.observeById(id).map { it?.toDomain() }.flowOn(io)

    override suspend fun get(id: Long): Pyn? = withContext(io) {
        pynDao.getById(id)?.toDomain()
    }

    override suspend fun upsert(draft: PynDraft): Long = withContext(io) {
        val now = System.currentTimeMillis()
        val id =
            if (draft.id == null) {
                pynDao.insert(
                    PynEntity(
                        name = draft.name.trim(),
                        description = draft.description.trim(),
                        notes = draft.notes.trim(),
                        address = draft.address.trim(),
                        city = draft.city.trim(),
                        state = draft.state.trim(),
                        zip = draft.zip.trim(),
                        latitude = draft.latitude,
                        longitude = draft.longitude,
                        createdAt = now,
                        updatedAt = now,
                    ),
                )
            } else {
                val existing = pynDao.getById(draft.id)?.pyn ?: error("Pyn ${draft.id} was not found")
                pynDao.update(
                    existing.copy(
                        name = draft.name.trim(),
                        description = draft.description.trim(),
                        notes = draft.notes.trim(),
                        address = draft.address.trim(),
                        city = draft.city.trim(),
                        state = draft.state.trim(),
                        zip = draft.zip.trim(),
                        latitude = draft.latitude,
                        longitude = draft.longitude,
                        updatedAt = now,
                    ),
                )
                draft.id
            }
        pynDao.clearCategories(id)
        if (draft.categoryIds.isNotEmpty()) {
            pynDao.insertCategories(draft.categoryIds.distinct().map { PynCategoryCrossRef(id, it) })
        }
        val previous = pynDao.photosFor(id)
        val keep = draft.photoPaths.toSet()
        previous.filter { it.relativePath !in keep }.forEach { files.delete(it.relativePath) }
        pynDao.deletePhotos(id)
        if (draft.photoPaths.isNotEmpty()) {
            pynDao.insertPhotos(
                draft.photoPaths.mapIndexed { index, path ->
                    PynPhotoEntity(
                        pynId = id,
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
        pynDao.photosFor(id).forEach { files.delete(it.relativePath) }
        pynDao.delete(id)
    }

    override suspend fun insertSamples(): Int = withContext(io) {
        var added = 0
        samplePlaces.forEach { place ->
            if (pynDao.idByName(place.name) != null) return@forEach
            val category =
                categoryDao.findByName(place.category)
                    ?: categoryDao
                        .insert(
                            CategoryEntity(name = place.category, createdAt = System.currentTimeMillis()),
                        ).let { id -> categoryDao.get(id) }
            upsert(place.toDraft(category?.id))
            added += 1
        }
        added
    }
}
