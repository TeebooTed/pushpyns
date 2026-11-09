package com.pushpyns.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        ProfileEntity::class,
        CategoryEntity::class,
        PynEntity::class,
        PynCategoryCrossRef::class,
        PynPhotoEntity::class,
        WorkOrderEntity::class,
        LineItemEntity::class,
        WorkOrderPhotoEntity::class,
        EntitlementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class PushpynsDatabase : RoomDatabase() {
    abstract fun profileDao(): ProfileDao

    abstract fun categoryDao(): CategoryDao

    abstract fun pynDao(): PynDao

    abstract fun workOrderDao(): WorkOrderDao

    abstract fun entitlementDao(): EntitlementDao
}
