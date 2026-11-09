package com.pushpyns.ui.preview

import com.pushpyns.domain.model.Category
import com.pushpyns.domain.model.LineItem
import com.pushpyns.domain.model.Pyn
import com.pushpyns.domain.model.WorkOrder
import com.pushpyns.domain.model.WorkOrderStatus

object PreviewData {
    val hiking = Category(1, "Hiking")
    val dining = Category(2, "Dining")
    val personal = Category(3, "Personal")

    val yosemite =
        Pyn(
            id = 1,
            name = "Yosemite - Half Dome River View",
            description = "Pull-off by Sentinel Bridge with a clear view of Half Dome across the Merced River.",
            notes = "Best light late afternoon.",
            address = "Sentinel Bridge",
            city = "Yosemite National Park",
            state = "CA",
            zip = "95389",
            latitude = 37.743415,
            longitude = -119.589713,
            categories = listOf(hiking),
            photos = emptyList(),
            createdAt = 1_700_000_000_000,
            updatedAt = 1_700_000_100_000,
        )

    val ferry =
        Pyn(
            id = 2,
            name = "Ferry Building",
            description = "Marketplace on the Embarcadero.",
            notes = "",
            address = "1 Ferry Building",
            city = "San Francisco",
            state = "CA",
            zip = "94111",
            latitude = 37.795480,
            longitude = -122.393430,
            categories = listOf(dining),
            photos = emptyList(),
            createdAt = 1_700_000_200_000,
            updatedAt = 1_700_000_200_000,
        )

    val park =
        Pyn(
            id = 3,
            name = "Sue Bierman Park",
            description = "Lawn above the Embarcadero.",
            notes = "",
            address = "Sue Bierman Park",
            city = "San Francisco",
            state = "CA",
            zip = "94105",
            latitude = 37.795180,
            longitude = -122.395420,
            categories = listOf(personal),
            photos = emptyList(),
            createdAt = 1_700_000_300_000,
            updatedAt = 1_700_000_300_000,
        )

    val places = listOf(yosemite, ferry, park)

    val openOrder =
        WorkOrder(
            id = 8,
            pynId = 2,
            linkedPynName = ferry.name,
            name = "Garage door maintenance",
            customerName = "Joe D.",
            description = "Replace the worn bottom seal and test the safety reverse.",
            notes = "Customer will be home after 2.",
            address = "1 Ferry Building",
            city = "San Francisco",
            state = "CA",
            zip = "94111",
            latitude = 37.795480,
            longitude = -122.393430,
            scheduledAt = 1_700_100_000_000,
            status = WorkOrderStatus.OPEN,
            termsAcceptedAt = null,
            signaturePath = null,
            completedAt = null,
            pdfPath = null,
            lineItems =
            listOf(
                LineItem(1, "Labor", "1.5", 8500),
                LineItem(2, "Bottom seal", "1", 2400),
            ),
            photos = emptyList(),
            createdAt = 1_700_000_400_000,
            updatedAt = 1_700_000_400_000,
        )
}
