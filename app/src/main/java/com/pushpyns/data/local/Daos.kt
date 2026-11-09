package com.pushpyns.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ProfileDao {
    @Query("SELECT * FROM profile WHERE id = $PROFILE_ID")
    fun observe(): Flow<ProfileEntity?>

    @Query("SELECT * FROM profile WHERE id = $PROFILE_ID")
    suspend fun get(): ProfileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: ProfileEntity)
}

@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE name = :name COLLATE NOCASE LIMIT 1")
    suspend fun findByName(name: String): CategoryEntity?

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun get(id: Long): CategoryEntity?

    @Insert
    suspend fun insert(entity: CategoryEntity): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    suspend fun rename(id: Long, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun delete(id: Long)
}

@Dao
interface PynDao {
    @Transaction
    @Query("SELECT * FROM pyns ORDER BY updatedAt DESC")
    fun observeAll(): Flow<List<PynWithDetails>>

    @Transaction
    @Query("SELECT * FROM pyns WHERE id = :id")
    fun observeById(id: Long): Flow<PynWithDetails?>

    @Transaction
    @Query("SELECT * FROM pyns WHERE id = :id")
    suspend fun getById(id: Long): PynWithDetails?

    @Query("SELECT id FROM pyns WHERE name = :name LIMIT 1")
    suspend fun idByName(name: String): Long?

    @Insert
    suspend fun insert(entity: PynEntity): Long

    @Update
    suspend fun update(entity: PynEntity)

    @Query("DELETE FROM pyns WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM pyn_categories WHERE pynId = :pynId")
    suspend fun clearCategories(pynId: Long)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCategories(refs: List<PynCategoryCrossRef>)

    @Query("SELECT * FROM pyn_photos WHERE pynId = :pynId ORDER BY sortOrder")
    suspend fun photosFor(pynId: Long): List<PynPhotoEntity>

    @Query("DELETE FROM pyn_photos WHERE pynId = :pynId")
    suspend fun deletePhotos(pynId: Long)

    @Insert
    suspend fun insertPhotos(photos: List<PynPhotoEntity>)
}

@Dao
interface WorkOrderDao {
    @Transaction
    @Query(
        """
        SELECT work_orders.*, pyns.name AS pynName
        FROM work_orders
        LEFT JOIN pyns ON pyns.id = work_orders.pynId
        ORDER BY work_orders.updatedAt DESC
        """,
    )
    fun observeAll(): Flow<List<WorkOrderWithDetails>>

    @Transaction
    @Query(
        """
        SELECT work_orders.*, pyns.name AS pynName
        FROM work_orders
        LEFT JOIN pyns ON pyns.id = work_orders.pynId
        WHERE work_orders.id = :id
        """,
    )
    fun observeById(id: Long): Flow<WorkOrderWithDetails?>

    @Transaction
    @Query(
        """
        SELECT work_orders.*, pyns.name AS pynName
        FROM work_orders
        LEFT JOIN pyns ON pyns.id = work_orders.pynId
        WHERE work_orders.id = :id
        """,
    )
    suspend fun getById(id: Long): WorkOrderWithDetails?

    @Insert
    suspend fun insert(entity: WorkOrderEntity): Long

    @Update
    suspend fun update(entity: WorkOrderEntity)

    @Query("DELETE FROM work_orders WHERE id = :id")
    suspend fun delete(id: Long)

    @Query("DELETE FROM work_order_line_items WHERE workOrderId = :id")
    suspend fun deleteLineItems(id: Long)

    @Insert
    suspend fun insertLineItems(items: List<LineItemEntity>)

    @Query("SELECT * FROM work_order_photos WHERE workOrderId = :id")
    suspend fun photosFor(id: Long): List<WorkOrderPhotoEntity>

    @Query("DELETE FROM work_order_photos WHERE workOrderId = :id")
    suspend fun deletePhotos(id: Long)

    @Insert
    suspend fun insertPhotos(photos: List<WorkOrderPhotoEntity>)

    @Query("UPDATE work_orders SET termsAcceptedAt = :at, updatedAt = :at WHERE id = :id")
    suspend fun recordTerms(id: Long, at: Long)

    @Query(
        """
        UPDATE work_orders
        SET status = 'COMPLETE', signaturePath = :signature, pdfPath = :pdf,
            completedAt = :at, updatedAt = :at
        WHERE id = :id
        """,
    )
    suspend fun markComplete(id: Long, signature: String, pdf: String, at: Long)

    @Query("UPDATE work_orders SET pdfPath = :pdf, updatedAt = :at WHERE id = :id")
    suspend fun updatePdf(id: Long, pdf: String, at: Long)
}

@Dao
interface EntitlementDao {
    @Query("SELECT * FROM entitlement WHERE id = $ENTITLEMENT_ID")
    fun observe(): Flow<EntitlementEntity?>

    @Query("SELECT * FROM entitlement WHERE id = $ENTITLEMENT_ID")
    suspend fun get(): EntitlementEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: EntitlementEntity)
}
