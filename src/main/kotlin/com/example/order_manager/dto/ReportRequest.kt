package com.example.order_manager.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Pattern

data class ReportRequest(
    @field:NotBlank(message = "clientOrderId is required")
    val clientOrderId: String,

    @field:Pattern(
        regexp = "COMPLETE|FAILED",
        message = "orderStatus must be COMPLETE or FAILED"
    )
    val orderStatus: String,

    @field:NotEmpty(message = "samplesResults must not be empty")
    @field:Valid
    val samplesResults: List<SampleResultRequest>,

    val technicianNotes: String?
)

data class SampleResultRequest(
    @field:NotBlank(message = "sampleName is required")
    val sampleName: String,

    @field:NotBlank(message = "result is required")
    val result: String,

    @field:NotEmpty(message = "details must not be empty")
    @field:Valid
    val details: List<AnalysisDetailRequest>
)

data class AnalysisDetailRequest(
    @field:NotBlank(message = "analysisName is required")
    val analysisName: String,

    @field:NotBlank(message = "value is required")
    val value: String,

    @field:NotBlank(message = "limit is required")
    val limit: String,

    @field:NotBlank(message = "status is required")
    val status: String
)
