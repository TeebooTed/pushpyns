package com.pushpyns.domain

import com.google.common.truth.Truth.assertThat
import com.pushpyns.domain.geo.PynSorter
import com.pushpyns.domain.geo.formatAddress
import com.pushpyns.domain.geo.formatDistance
import com.pushpyns.domain.geo.haversineMeters
import com.pushpyns.domain.model.GeoPoint
import com.pushpyns.domain.model.PynSort
import com.pushpyns.domain.money.Money
import com.pushpyns.domain.share.pynShareText
import com.pushpyns.domain.time.relativeTime
import com.pushpyns.domain.validation.LineItemInput
import com.pushpyns.domain.validation.PynFieldError
import com.pushpyns.domain.validation.WorkOrderFieldError
import com.pushpyns.domain.validation.validatePyn
import com.pushpyns.domain.validation.validateWorkOrder
import com.pushpyns.samplePyn
import org.junit.Test

class GeoAndMoneyTest {
    @Test
    fun oneDegreeOfLongitudeAtEquatorIsAbout111Km() {
        val meters = haversineMeters(0.0, 0.0, 0.0, 1.0)
        assertThat(meters).isWithin(200.0).of(111_195.0)
    }

    @Test
    fun distanceSortPutsNearestFirstAndMissingCoordsLast() {
        val near = samplePyn(1, "Near", lat = 37.80, lng = -122.40)
        val far = samplePyn(2, "Far", lat = 37.0, lng = -122.0)
        val none = samplePyn(3, "None")
        val origin = GeoPoint(37.7955, -122.3934)
        val sorted = PynSorter.sort(listOf(far, none, near), PynSort.DISTANCE, origin)
        assertThat(sorted.map { it.name }).containsExactly("Near", "Far", "None").inOrder()
    }

    @Test
    fun categorySortGroupsUncategorizedLast() {
        val hike = samplePyn(1, "Dome", category = "Hiking")
        val dine = samplePyn(2, "Ferry", category = "Dining")
        val bare = samplePyn(3, "Somewhere")
        val sorted = PynSorter.sort(listOf(hike, bare, dine), PynSort.CATEGORY, null)
        assertThat(sorted.map { it.name }).containsExactly("Ferry", "Dome", "Somewhere").inOrder()
    }

    @Test
    fun addressAndDistanceFormatting() {
        assertThat(formatAddress("1 Ferry Building", "San Francisco", "CA", "94111"))
            .isEqualTo("1 Ferry Building, San Francisco, CA 94111")
        assertThat(formatDistance(30.0)).contains("ft")
        assertThat(formatDistance(1609.344)).contains("mi")
    }

    @Test
    fun moneyParsesDollarsAndTotalsLines() {
        assertThat(Money.parseDollars("12.50")).isEqualTo(1250L)
        assertThat(Money.parseDollars("$1,200")).isEqualTo(120_000L)
        assertThat(Money.parseDollars("1.234")).isNull()
        assertThat(Money.lineTotalCents("2", 7500)).isEqualTo(15_000L)
        assertThat(Money.formatCents(15_000)).isEqualTo("$150.00")
        assertThat(Money.displayQuantity("1.50")).isEqualTo("1.5")
    }

    @Test
    fun shareTextIncludesNameAddressAndCoordinates() {
        val pyn = samplePyn(1, "Ferry Building", category = "Dining", lat = 37.79548, lng = -122.39343)
        val text = pynShareText(pyn)
        assertThat(text).contains("Ferry Building")
        assertThat(text).contains("37.795480, -122.393430")
        assertThat(text).contains("Dining")
        assertThat(text).contains("Shared from Pushpyns")
    }

    @Test
    fun relativeTimeBuckets() {
        val now = 3_600_000L
        assertThat(relativeTime(now - 30_000, now)).isEqualTo("Just now")
        assertThat(relativeTime(now - 5 * 60_000, now)).isEqualTo("5 min ago")
        assertThat(relativeTime(now - 2 * 3_600_000, now)).isEqualTo("2 hours ago")
        assertThat(relativeTime(now - 26 * 3_600_000, now)).isEqualTo("Yesterday")
    }

    @Test
    fun validatorsRequireNameAndPairedCoordinates() {
        assertThat(validatePyn("", "", "")).contains(PynFieldError.NAME_REQUIRED)
        assertThat(validatePyn("Trail", "37.1", "")).contains(PynFieldError.LAT_LNG_PAIR)
        assertThat(validatePyn("Trail", "120", "-122")).contains(PynFieldError.LATITUDE)
        assertThat(validatePyn("Trail", "37.1", "-122.4")).isEmpty()
        assertThat(
            validateWorkOrder(
                jobName = "",
                customer = "",
                latitude = "",
                longitude = "",
                lineItems = listOf(LineItemInput("Seal", "0", "4")),
            ),
        ).containsAtLeast(
            WorkOrderFieldError.JOB_REQUIRED,
            WorkOrderFieldError.CUSTOMER_REQUIRED,
            WorkOrderFieldError.LINE_ITEM,
        )
    }
}
