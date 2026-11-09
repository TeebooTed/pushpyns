package com.pushpyns.di

import android.content.Context
import androidx.room.Room
import com.pushpyns.data.local.PushpynsDatabase
import com.pushpyns.data.location.AndroidAddressLookup
import com.pushpyns.data.location.DeviceLocationSource
import com.pushpyns.data.media.AppWorkOrderFiles
import com.pushpyns.data.media.LocalPhotoStorage
import com.pushpyns.data.pdf.AndroidWorkOrderPdfWriter
import com.pushpyns.data.repository.DataStoreSettingsRepository
import com.pushpyns.data.repository.LocalCategoryRepository
import com.pushpyns.data.repository.LocalProAccess
import com.pushpyns.data.repository.LocalProfileRepository
import com.pushpyns.data.repository.LocalPynRepository
import com.pushpyns.data.repository.LocalWorkOrderRepository
import com.pushpyns.domain.pdf.WorkOrderPdfComposer
import com.pushpyns.domain.pdf.WorkOrderPdfWriter
import com.pushpyns.domain.repository.AddressLookup
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.LocationSource
import com.pushpyns.domain.repository.PhotoStorage
import com.pushpyns.domain.repository.ProAccess
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.domain.repository.PynRepository
import com.pushpyns.domain.repository.SettingsRepository
import com.pushpyns.domain.repository.WorkOrderFiles
import com.pushpyns.domain.repository.WorkOrderRepository
import com.pushpyns.domain.work.CompleteWorkOrder
import com.pushpyns.domain.work.TimeSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Qualifier
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

class SystemTimeSource
@javax.inject.Inject
constructor() : TimeSource {
    override fun nowMillis(): Long = System.currentTimeMillis()
}

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun database(@ApplicationContext context: Context): PushpynsDatabase =
        Room.databaseBuilder(context, PushpynsDatabase::class.java, "pushpyns.db").build()

    @Provides
    fun profileDao(db: PushpynsDatabase) = db.profileDao()

    @Provides
    fun categoryDao(db: PushpynsDatabase) = db.categoryDao()

    @Provides
    fun pynDao(db: PushpynsDatabase) = db.pynDao()

    @Provides
    fun workOrderDao(db: PushpynsDatabase) = db.workOrderDao()

    @Provides
    fun entitlementDao(db: PushpynsDatabase) = db.entitlementDao()

    @Provides
    @Singleton
    fun composer(): WorkOrderPdfComposer = WorkOrderPdfComposer()

    @Provides
    @Singleton
    fun completeWorkOrder(
        workOrders: WorkOrderRepository,
        profiles: ProfileRepository,
        writer: WorkOrderPdfWriter,
        files: WorkOrderFiles,
        time: TimeSource,
    ): CompleteWorkOrder = CompleteWorkOrder(workOrders, profiles, writer, files, time)

    @Provides
    @IoDispatcher
    fun ioDispatcher(): CoroutineDispatcher = Dispatchers.IO
}

@Module
@InstallIn(SingletonComponent::class)
abstract class BindingModule {
    @Binds
    @Singleton
    abstract fun pyns(impl: LocalPynRepository): PynRepository

    @Binds
    @Singleton
    abstract fun categories(impl: LocalCategoryRepository): CategoryRepository

    @Binds
    @Singleton
    abstract fun workOrders(impl: LocalWorkOrderRepository): WorkOrderRepository

    @Binds
    @Singleton
    abstract fun profiles(impl: LocalProfileRepository): ProfileRepository

    @Binds
    @Singleton
    abstract fun pro(impl: LocalProAccess): ProAccess

    @Binds
    @Singleton
    abstract fun settings(impl: DataStoreSettingsRepository): SettingsRepository

    @Binds
    @Singleton
    abstract fun location(impl: DeviceLocationSource): LocationSource

    @Binds
    @Singleton
    abstract fun addresses(impl: AndroidAddressLookup): AddressLookup

    @Binds
    @Singleton
    abstract fun photos(impl: LocalPhotoStorage): PhotoStorage

    @Binds
    @Singleton
    abstract fun workOrderFiles(impl: AppWorkOrderFiles): WorkOrderFiles

    @Binds
    @Singleton
    abstract fun pdfWriter(impl: AndroidWorkOrderPdfWriter): WorkOrderPdfWriter

    @Binds
    @Singleton
    abstract fun time(impl: SystemTimeSource): TimeSource
}
