package com.pushpyns.domain.pdf

import com.pushpyns.domain.model.Profile
import com.pushpyns.domain.model.WorkOrder

interface WorkOrderPdfWriter {
    suspend fun write(order: WorkOrder, profile: Profile?, relativePath: String)
}
