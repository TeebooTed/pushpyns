package com.pushpyns

import com.pushpyns.domain.model.Category
import com.pushpyns.domain.model.LineItem
import com.pushpyns.domain.model.Photo
import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.model.UseType
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderDraft
import com.pushpyns.domain.model.WorkOrderStatus
import com.pushpyns.domain.pdf.WorkOrderPdfWriter
import com.pushpyns.domain.repository.CategoryRepository
import com.pushpyns.domain.repository.CategoryResult
import com.pushpyns.domain.repository.ProAccess
import com.pushpyns.domain.repository.ProfileRepository
import com.pushpyns.domain.repository.PynRepository
import com.pushpyns.domain.repository.WorkOrderFiles
import com.pushpyns.domain.repository.WorkOrderRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

fun samplePyn(
    id: Long,
    name: String,
    category: String? = null,
    lat: Double? = null,
    lng: Double? = null,
    createdAt: Long = id * 1_000,
): Pyn = Pyn(
    id = id,
    name = name,
    description = "About $name",
    notes = "",
    address = "1 Main",
    city = "Town",
    state = "CA",
    zip = "94111",
    latitude = lat,
    longitude = lng,
    categories = listOfNotNull(category?.let { Category(id, it) }),
    photos = emptyList(),
    createdAt = createdAt,
    updatedAt = createdAt,
)

class FakePynRepository(initial: List<Pyn> = emptyList()) : PynRepository {
    val items = MutableStateFlow(initial)
    var samplesInserted = 0

    override fun observeAll(): Flow<List<Pyn>> = items

    override fun observe(id: Long): Flow<Pyn?> = items.map { list -> list.find { it.id == id } }

    override suspend fun get(id: Long): Pyn? = items.value.find { it.id == id }

    override suspend fun upsert(draft: PynDraft): Long {
        val id = draft.id ?: (items.value.maxOfOrNull { it.id } ?: 0L) + 1L
        val pyn =
            Pyn(
                id = id,
                name = draft.name,
                description = draft.description,
                notes = draft.notes,
                address = draft.address,
                city = draft.city,
                state = draft.state,
                zip = draft.zip,
                latitude = draft.latitude,
                longitude = draft.longitude,
                categories = draft.categoryIds.map { Category(it, "Category $it") },
                photos = draft.photoPaths.mapIndexed { index, path -> Photo(index.toLong(), path, 0L) },
                createdAt = items.value.find { it.id == id }?.createdAt ?: 1L,
                updatedAt = 2L,
            )
        items.update { current -> current.filterNot { it.id == id } + pyn }
        return id
    }

    override suspend fun delete(id: Long) {
        items.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun insertSamples(): Int {
        samplesInserted += 1
        return 4
    }
}

class FakeWorkOrderRepository(initial: List<WorkOrder> = emptyList()) : WorkOrderRepository {
    val items = MutableStateFlow(initial)

    override fun observeAll(): Flow<List<WorkOrder>> = items

    override fun observe(id: Long): Flow<WorkOrder?> = items.map { list -> list.find { it.id == id } }

    override suspend fun get(id: Long): WorkOrder? = items.value.find { it.id == id }

    override suspend fun upsert(draft: WorkOrderDraft): Long = draft.id ?: 1L

    override suspend fun delete(id: Long) {
        items.update { current -> current.filterNot { it.id == id } }
    }

    override suspend fun recordTermsAccepted(id: Long, atMillis: Long) {
        items.update { current ->
            current.map { if (it.id == id) it.copy(termsAcceptedAt = atMillis) else it }
        }
    }

    override suspend fun markComplete(id: Long, signaturePath: String, pdfPath: String, completedAt: Long) {
        items.update { current ->
            current.map {
                if (it.id == id) {
                    it.copy(
                        status = WorkOrderStatus.COMPLETE,
                        signaturePath = signaturePath,
                        pdfPath = pdfPath,
                        completedAt = completedAt,
                    )
                } else {
                    it
                }
            }
        }
    }

    override suspend fun updatePdfPath(id: Long, pdfPath: String) {
        items.update { current -> current.map { if (it.id == id) it.copy(pdfPath = pdfPath) else it } }
    }
}

class FakeProfileRepository(var profile: Profile? = Profile("Ada", "Ada Field Co", UseType.BUSINESS)) : ProfileRepository {
    override fun observe(): Flow<Profile?> = MutableStateFlow(profile)

    override suspend fun get(): Profile? = profile

    override suspend fun save(profile: Profile) {
        this.profile = profile
    }
}

class FakeProAccess(initial: Boolean = false) : ProAccess {
    private val flag = MutableStateFlow(initial)

    override fun observeIsPro(): Flow<Boolean> = flag

    override suspend fun isPro(): Boolean = flag.value

    override suspend fun setPreviewEnabled(enabled: Boolean) {
        flag.value = enabled
    }
}

class FakeCategoryRepository : CategoryRepository {
    override fun observeAll() = MutableStateFlow(emptyList<Category>())

    override suspend fun add(name: String) = CategoryResult.Saved(1)

    override suspend fun rename(id: Long, name: String) = CategoryResult.Saved(id)

    override suspend fun delete(id: Long) = Unit

    override suspend fun ensureDefaults() = Unit
}

class MemoryWorkOrderFiles : WorkOrderFiles {
    val signatures = mutableMapOf<Long, ByteArray>()
    val existing = mutableSetOf<String>()

    override fun saveSignature(workOrderId: Long, png: ByteArray): String {
        signatures[workOrderId] = png
        return "signatures/wo_$workOrderId.png"
    }

    override fun pdfRelativePath(workOrderId: Long): String = "pdfs/work-order-$workOrderId.pdf"

    override fun exists(relativePath: String): Boolean = relativePath in existing
}

class FakePdfWriter : WorkOrderPdfWriter {
    var writes = 0
    var lastBusiness: String? = null

    override suspend fun write(order: WorkOrder, profile: Profile?, relativePath: String) {
        writes += 1
        lastBusiness = profile?.businessName
    }
}

fun sampleOrder(id: Long = 7L, termsAt: Long? = 50L): WorkOrder = WorkOrder(
    id = id,
    pynId = null,
    linkedPynName = null,
    name = "Garage door maintenance",
    customerName = "Joe D.",
    description = "Replace the bottom seal.",
    notes = "After 2.",
    address = "1 Ferry Building",
    city = "San Francisco",
    state = "CA",
    zip = "94111",
    latitude = 37.79548,
    longitude = -122.39343,
    scheduledAt = 80L,
    status = WorkOrderStatus.OPEN,
    termsAcceptedAt = termsAt,
    signaturePath = null,
    completedAt = null,
    pdfPath = null,
    lineItems = listOf(LineItem(1, "Labor", "2", 7500)),
    photos = emptyList(),
    createdAt = 10L,
    updatedAt = 10L,
)
