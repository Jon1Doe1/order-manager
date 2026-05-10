package com.example.order_manager.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern

data class LabOrderStatusUpdate(
    @field:NotBlank(message = "clientOrderId required")
    val clientOrderId: String,

    @field:Pattern(
        regexp = "COMPLETE|FAILED",
        message = "status must be COMPLETE or FAILED"
    )
    val status: String
)