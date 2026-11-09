package com.pushpyns.domain.sample

import com.pushpyns.domain.model.PynDraft

data class SamplePlace(
    val name: String,
    val description: String,
    val notes: String,
    val address: String,
    val city: String,
    val state: String,
    val zip: String,
    val latitude: Double,
    val longitude: Double,
    val category: String,
)

/** Public demo places, not customer records. */
val samplePlaces: List<SamplePlace> =
    listOf(
        SamplePlace(
            name = "Yosemite - Half Dome River View",
            description = "Pull-off by Sentinel Bridge with a clear view of Half Dome across the Merced River.",
            notes = "Best light late afternoon. Parking fills on summer weekends.",
            address = "Sentinel Bridge",
            city = "Yosemite National Park",
            state = "CA",
            zip = "95389",
            latitude = 37.743415,
            longitude = -119.589713,
            category = "Hiking",
        ),
        SamplePlace(
            name = "Ferry Building",
            description = "Marketplace on the Embarcadero. Easy meeting spot before a walk up Market.",
            notes = "Indoor stalls if the wind is up.",
            address = "1 Ferry Building",
            city = "San Francisco",
            state = "CA",
            zip = "94111",
            latitude = 37.795480,
            longitude = -122.393430,
            category = "Dining",
        ),
        SamplePlace(
            name = "Sue Bierman Park",
            description = "Lawn above the Embarcadero, a short break between waterfront stops.",
            notes = "Shaded benches on the west edge.",
            address = "Sue Bierman Park",
            city = "San Francisco",
            state = "CA",
            zip = "94105",
            latitude = 37.795180,
            longitude = -122.395420,
            category = "Personal",
        ),
        SamplePlace(
            name = "Lost Dutchman Trailhead",
            description = "Superstition Mountains trailhead. Check the gate hours before driving out.",
            notes = "Carry water. Summer heat comes on early.",
            address = "2323 N Tomahawk Rd",
            city = "Apache Junction",
            state = "AZ",
            zip = "85119",
            latitude = 33.459200,
            longitude = -111.480100,
            category = "Camping",
        ),
    )

fun SamplePlace.toDraft(categoryId: Long?, createdPhotoPaths: List<String> = emptyList()): PynDraft = PynDraft(
    id = null,
    name = name,
    description = description,
    notes = notes,
    address = address,
    city = city,
    state = state,
    zip = zip,
    latitude = latitude,
    longitude = longitude,
    categoryIds = listOfNotNull(categoryId),
    photoPaths = createdPhotoPaths,
)
