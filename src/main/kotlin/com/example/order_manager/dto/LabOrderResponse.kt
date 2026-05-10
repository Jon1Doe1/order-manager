package com.example.order_manager.dto

import com.example.order_manager.enumeration.LabOrderStatus
import java.time.Instant
import java.util.*

data class LabOrderResponse(
    val id: UUID,
    val clientId: String,
    val clientOrderId: String,
    val status: LabOrderStatus,
    val createdAt: Instant?,
    val updatedAt: Instant?,
)