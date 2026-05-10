package com.example.order_manager.dto

import java.time.Instant
import java.util.UUID

data class ReportResponse(
    val reportId: UUID,
    val clientOrderId: String,
    val orderStatus: String,
    val samplesResults: List<SampleResultResponse>,
    val technicianNotes: String?,
    val createdAt: Instant?
)

data class SampleResultResponse(
    val sampleName: String,
    val result: String,
    val details: List<AnalysisDetailResponse>
)

data class AnalysisDetailResponse(
    val analysisName: String,
    val value: String,
    val limit: String,
    val status: String
)
