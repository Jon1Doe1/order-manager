package com.example.order_manager.service

import com.example.order_manager.dto.AnalysisDetailResponse
import com.example.order_manager.dto.ReportRequest
import com.example.order_manager.dto.ReportResponse
import com.example.order_manager.dto.SampleResultResponse
import com.example.order_manager.entity.Report
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import com.example.order_manager.repository.ReportRepository
import jakarta.transaction.Transactional
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import tools.jackson.module.kotlin.jacksonObjectMapper
import tools.jackson.module.kotlin.readValue

@Service
class ReportService(
    private val reportRepository: ReportRepository,
    private val labOrderRepository: LabOrderRepository
) {

    private val objectMapper = jacksonObjectMapper()

    /**
     * Get the report from order with specific client order id
     */
    fun getReportByClientOrderId(clientOrderId: String): ReportResponse {
        val report = reportRepository.findByOrderClientOrderId(clientOrderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Report for order $clientOrderId not found")

        return toResponse(report)
    }

    /**
     * Create the report
     */
    @Transactional
    fun createReport(request: ReportRequest): ReportResponse {
        if (reportRepository.findByOrderClientOrderId(request.clientOrderId) != null) //idempotency
            throw ResponseStatusException(HttpStatus.CONFLICT, "Report for order ${request.clientOrderId} already exists")

        val order = labOrderRepository.findByClientOrderId(request.clientOrderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order ${request.clientOrderId} not found")

        val resultAnalysesJson = objectMapper.writeValueAsString(
            ResultAnalysesPayload(
                samplesResults = request.samplesResults.map { sample ->
                    SampleResultPayload(
                        sampleName = sample.sampleName,
                        result = sample.result,
                        details = sample.details.map { detail ->
                            AnalysisDetailPayload(
                                analysisName = detail.analysisName,
                                value = detail.value,
                                limit = detail.limit,
                                status = detail.status
                            )
                        }
                    )
                },
                technicianNotes = request.technicianNotes
            )
        )

        order.status = LabOrderStatus.valueOf(request.orderStatus)

        val report = reportRepository.save(
            Report(
                order = order,
                orderStatus = request.orderStatus,
                resultAnalyses = resultAnalysesJson
            )
        )

        return toResponse(report)
    }

    private fun toResponse(report: Report): ReportResponse {
        val payload = objectMapper.readValue<ResultAnalysesPayload>(report.resultAnalyses)
        return ReportResponse(
            reportId = report.id,
            clientOrderId = report.order.clientOrderId,
            orderStatus = report.orderStatus,
            samplesResults = payload.samplesResults.map { sample ->
                SampleResultResponse(
                    sampleName = sample.sampleName,
                    result = sample.result,
                    details = sample.details.map { detail ->
                        AnalysisDetailResponse(
                            analysisName = detail.analysisName,
                            value = detail.value,
                            limit = detail.limit,
                            status = detail.status
                        )
                    }
                )
            },
            technicianNotes = payload.technicianNotes,
            createdAt = report.createdAt
        )
    }

    private data class ResultAnalysesPayload(
        val samplesResults: List<SampleResultPayload>,
        val technicianNotes: String?
    )

    private data class SampleResultPayload(
        val sampleName: String,
        val result: String,
        val details: List<AnalysisDetailPayload>
    )

    private data class AnalysisDetailPayload(
        val analysisName: String,
        val value: String,
        val limit: String,
        val status: String
    )
}
