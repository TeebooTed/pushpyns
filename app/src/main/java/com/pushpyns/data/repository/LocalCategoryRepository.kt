package com.pushpyns.data.repository

import com.pushpyns.data.local.CategoryDao
import com.pushpyns.data.local.CategoryEntity
import com.pushpyns.data.mapper.toDomain
import com.pushpyns.di.IoDispatcher
import com.pushpyns.domain.model.Category
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.CategoryResult
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

@Singleton
class LocalCategoryRepository
@Inject
constructor(private val dao: CategoryDao, @IoDispatcher private val io: CoroutineDispatcher) :
    CategoryRepository {
    override fun observeAll(): Flow<List<Category>> = dao.observeAll().map { rows -> rows.map { it.toDomain() } }.flowOn(io)

    override suspend fun add(name: String): CategoryResult = withContext(io) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext CategoryResult.Blank
        if (dao.findByName(trimmed) != null) return@withContext CategoryResult.Duplicate
        val id = dao.insert(CategoryEntity(name = trimmed, createdAt = System.currentTimeMillis()))
        CategoryResult.Saved(id)
    }

    override suspend fun rename(id: Long, name: String): CategoryResult = withContext(io) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@withContext CategoryResult.Blank
        val existing = dao.findByName(trimmed)
        if (existing != null && existing.id != id) return@withContext CategoryResult.Duplicate
        dao.rename(id, trimmed)
        CategoryResult.Saved(id)
    }

    override suspend fun delete(id: Long) = withContext(io) {
        dao.delete(id)
    }

    override suspend fun ensureDefaults() = withContext(io) {
        DEFAULT_CATEGORIES.forEach { name ->
            if (dao.findByName(name) == null) {
                dao.insert(CategoryEntity(name = name, createdAt = System.currentTimeMillis()))
            }
        }
    }

    companion object {
        val DEFAULT_CATEGORIES =
            listOf(
                "Business",
                "Personal",
                "Dining",
                "Camping",
                "Hiking",
                "Work site",
            )
    }
}
