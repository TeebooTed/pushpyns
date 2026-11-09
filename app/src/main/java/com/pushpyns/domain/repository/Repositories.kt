package com.pushpyns.domain.repository

import com.pushpyns.domain.model.Category
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.PostalAddress
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.model.ThemeMode
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderDraft
import kotlinx.coroutines.flow.Flow

/**
 * Local source of truth for v1. A later cloud sync can wrap these interfaces
 * without changing feature ViewModels.
 */
interface ProfileRepository {
    fun observe(): Flow<Profile?>

    suspend fun get(): Profile?

    suspend fun save(profile: Profile)
}

interface CategoryRepository {
    fun observeAll(): Flow<List<Category>>

    suspend fun add(name: String): CategoryResult

    suspend fun rename(id: Long, name: String): CategoryResult

    suspend fun delete(id: Long)

    suspend fun ensureDefaults()
}

sealed interface CategoryResult {
    data class Saved(val id: Long) : CategoryResult

    data object Blank : CategoryResult

    data object Duplicate : CategoryResult
}

interface PynRepository {
    fun observeAll(): Flow<List<Pyn>>

    fun observe(id: Long): Flow<Pyn?>

    suspend fun get(id: Long): Pyn?

    suspend fun upsert(draft: PynDraft): Long

    suspend fun delete(id: Long)

    /** Inserts the built-in demo places that are not already stored. Returns how many were added. */
    suspend fun insertSamples(): Int
}

interface WorkOrderRepository {
    fun observeAll(): Flow<List<WorkOrder>>

    fun observe(id: Long): Flow<WorkOrder?>

    suspend fun get(id: Long): WorkOrder?

    suspend fun upsert(draft: WorkOrderDraft): Long

    suspend fun delete(id: Long)

    suspend fun recordTermsAccepted(id: Long, atMillis: Long)

    suspend fun markComplete(id: Long, signaturePath: String, pdfPath: String, completedAt: Long)

    suspend fun updatePdfPath(id: Long, pdfPath: String)
}

/**
 * Stand-in for a future Play Billing subscription. v1 stores a device-only
 * preview flag. Replace this binding with purchase-token checks later.
 */
interface ProAccess {
    fun observeIsPro(): Flow<Boolean>

    suspend fun isPro(): Boolean

    suspend fun setPreviewEnabled(enabled: Boolean)
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>

    val dynamicColor: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setDynamicColor(enabled: Boolean)
}

interface LocationSource {
    fun hasPermission(): Boolean

    suspend fun current(): GeoPoint?
}

interface AddressLookup {
    suspend fun reverse(latitude: Double, longitude: Double): PostalAddress?
}

interface PhotoStorage {
    fun prepareCapture(): CaptureTarget

    suspend fun import(uriString: String): String

    fun delete(relativePath: String)

    fun duplicate(relativePath: String): String
}

data class CaptureTarget(val relativePath: String, val uri: String)

interface WorkOrderFiles {
    fun saveSignature(workOrderId: Long, png: ByteArray): String

    fun pdfRelativePath(workOrderId: Long): String

    fun exists(relativePath: String): Boolean
}
