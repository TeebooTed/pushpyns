package com.pushpyns.ui

import com.google.common.truth.Truth.assertThat
import com.pushpyns.FakeProAccess
import com.pushpyns.FakePynRepository
import com.pushpyns.MainDispatcherRule
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.PynSort
import com.pushpyns.domain.repository.LocationSource
import com.pushpyns.samplePyn
import com.pushpyns.ui.paywall.PaywallViewModel
import com.pushpyns.ui.pyns.PynListViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test
    fun listSearchAndDistanceSort() = runTest {
        val repo =
            FakePynRepository(
                listOf(
                    samplePyn(1, "Yosemite View", lat = 37.74, lng = -119.58),
                    samplePyn(2, "Ferry Building", lat = 37.795, lng = -122.39),
                ),
            )
        val vm = PynListViewModel(repo, FakeLocation(GeoPoint(37.80, -122.40)))
        vm.uiState.first { !it.loading }
        vm.onQuery("ferry")
        val filtered = vm.uiState.first { it.query == "ferry" }
        assertThat(filtered.pyns.map { it.name }).containsExactly("Ferry Building")
        vm.onQuery("")
        vm.onSort(PynSort.DISTANCE)
        val sorted = vm.uiState.first { it.sort == PynSort.DISTANCE && it.pyns.size == 2 }
        assertThat(sorted.pyns.first().name).isEqualTo("Ferry Building")
        assertThat(sorted.locationAvailable).isTrue()
    }

    @Test
    fun paywallPreviewFlipsTheLocalFlag() = runTest {
        val access = FakeProAccess(false)
        val vm = PaywallViewModel(access)
        assertThat(vm.isPro.first()).isFalse()
        vm.setEnabled(true)
        assertThat(vm.isPro.first { it }).isTrue()
        vm.setEnabled(false)
        assertThat(vm.isPro.first { !it }).isFalse()
    }
}

class FakeLocation(private val point: GeoPoint? = null) : LocationSource {
    override fun hasPermission(): Boolean = point != null

    override suspend fun current(): GeoPoint? = point
}
