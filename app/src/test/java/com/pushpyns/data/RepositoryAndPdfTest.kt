package com.pushpyns.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import com.pushpyns.data.local.PushpynsDatabase
import com.pushpyns.data.media.AppFiles
import com.pushpyns.data.repository.LocalCategoryRepository
import com.pushpyns.data.repository.LocalPynRepository
import com.pushpyns.data.repository.LocalWorkOrderRepository
import com.pushpyns.domain.model.LineItemDraft
import com.pushpyns.domain.model.PynDraft
import com.pushpyns.domain.model.WorkOrderDraft
import com.pushpyns.domain.model.WorkOrderStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33], application = android.app.Application::class)
class RepositoryAndPdfTest {
    private lateinit var db: PushpynsDatabase
    private lateinit var files: AppFiles
    private lateinit var pyns: LocalPynRepository
    private lateinit var categories: LocalCategoryRepository
    private lateinit var orders: LocalWorkOrderRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder(context, PushpynsDatabase::class.java)
                .allowMainThreadQueries()
                .build()
        files = AppFiles(context)
        categories = LocalCategoryRepository(db.categoryDao(), Dispatchers.Unconfined)
        pyns = LocalPynRepository(db.pynDao(), db.categoryDao(), files, Dispatchers.Unconfined)
        orders = LocalWorkOrderRepository(db.workOrderDao(), files, Dispatchers.Unconfined)
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun pynRoundTripKeepsCategoriesAndSurvivesDelete() = runTest {
        categories.ensureDefaults()
        val hiking = categories.observeAll().first().first { it.name == "Hiking" }
        val id =
            pyns.upsert(
                PynDraft(
                    id = null,
                    name = "Sentinel Bridge",
                    description = "River view",
                    notes = "Afternoon",
                    address = "Sentinel Bridge",
                    city = "Yosemite National Park",
                    state = "CA",
                    zip = "95389",
                    latitude = 37.743415,
                    longitude = -119.589713,
                    categoryIds = listOf(hiking.id),
                    photoPaths = emptyList(),
                ),
            )
        val stored = pyns.get(id)
        assertThat(stored?.name).isEqualTo("Sentinel Bridge")
        assertThat(stored?.categories?.map { it.name }).containsExactly("Hiking")
        val added = pyns.insertSamples()
        assertThat(added).isEqualTo(4)
        assertThat(pyns.observeAll().first()).hasSize(5)
        pyns.delete(id)
        assertThat(pyns.get(id)).isNull()
    }

    @Test
    fun workOrderLinksToPynAndClearsLinkWhenPynIsDeleted() = runTest {
        val pynId =
            pyns.upsert(
                PynDraft(
                    null,
                    "Job site",
                    "",
                    "",
                    "1 Main",
                    "Town",
                    "CA",
                    "94111",
                    1.0,
                    2.0,
                    emptyList(),
                    emptyList(),
                ),
            )
        val orderId =
            orders.upsert(
                WorkOrderDraft(
                    id = null,
                    pynId = pynId,
                    name = "Seal",
                    customerName = "Joe",
                    description = "Door",
                    notes = "",
                    address = "1 Main",
                    city = "Town",
                    state = "CA",
                    zip = "94111",
                    latitude = 1.0,
                    longitude = 2.0,
                    scheduledAt = null,
                    lineItems = listOf(LineItemDraft("Labor", "1", 5000)),
                    photoPaths = emptyList(),
                ),
            )
        assertThat(orders.get(orderId)?.linkedPynName).isEqualTo("Job site")
        pyns.delete(pynId)
        val after = orders.get(orderId)
        assertThat(after?.pynId).isNull()
        assertThat(after?.lineItems).hasSize(1)
        orders.recordTermsAccepted(orderId, 20L)
        orders.markComplete(orderId, "signatures/wo_$orderId.png", "pdfs/work-order-$orderId.pdf", 30L)
        assertThat(orders.get(orderId)?.status).isEqualTo(WorkOrderStatus.COMPLETE)
    }
}
