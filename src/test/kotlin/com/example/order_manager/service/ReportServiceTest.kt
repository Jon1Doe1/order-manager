package com.example.order_manager.service

import com.example.order_manager.dto.AnalysisDetailRequest
import com.example.order_manager.dto.ReportRequest
import com.example.order_manager.dto.ReportResponse
import com.example.order_manager.dto.SampleResultRequest
import com.example.order_manager.dto.SampleResultResponse
import com.example.order_manager.dto.AnalysisDetailResponse
import com.example.order_manager.entity.LabOrder
import com.example.order_manager.entity.Report
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import com.example.order_manager.repository.ReportRepository
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class ReportServiceTest {

    private val reportRepository: ReportRepository = mockk()
    private val labOrderRepository: LabOrderRepository = mockk()
    private val reportService: ReportService = ReportService(reportRepository, labOrderRepository)

    private val labOrder = LabOrder(
        clientId = "ALDI_DISTRIBUTION_CENTER_04",
        clientOrderId = "LOT-2026-XJF88",
        status = LabOrderStatus.VALIDATED
    )

    private val reportRequest = ReportRequest(
        clientOrderId = "LOT-2026-XJF88",
        orderStatus = "COMPLETE",
        samplesResults = listOf(
            SampleResultRequest(
                sampleName = "Chicken Breast - Batch A22",
                result = "PASS",
                details = listOf(
                    AnalysisDetailRequest(analysisName = "Salmonella Detection", value = "Not detected", limit = "0 CFU/g", status = "PASS"),
                    AnalysisDetailRequest(analysisName = "Total Bacterial Count", value = "1200 CFU/g", limit = "10000 CFU/g", status = "PASS"),
                    AnalysisDetailRequest(analysisName = "Antibiotic Residue Check", value = "Not detected", limit = "0 µg/kg", status = "PASS")
                )
            ),
            SampleResultRequest(
                sampleName = "Bagged Salad - Batch V55",
                result = "FAIL",
                details = listOf(
                    AnalysisDetailRequest(analysisName = "Listeria Detection", value = "Detected", limit = "0 CFU/g", status = "FAIL"),
                    AnalysisDetailRequest(analysisName = "Pesticide Screening", value = "0.002 mg/kg", limit = "0.01 mg/kg", status = "PASS")
                )
            )
        ),
        technicianNotes = "Listeria found in bagged salad batch"
    )

    private val resultAnalysesJson = """
        {
          "samplesResults": [
            {
              "sampleName": "Chicken Breast - Batch A22",
              "result": "PASS",
              "details": [
                {"analysisName": "Salmonella Detection", "value": "Not detected", "limit": "0 CFU/g", "status": "PASS"},
                {"analysisName": "Total Bacterial Count", "value": "1200 CFU/g", "limit": "10000 CFU/g", "status": "PASS"},
                {"analysisName": "Antibiotic Residue Check", "value": "Not detected", "limit": "0 µg/kg", "status": "PASS"}
              ]
            },
            {
              "sampleName": "Bagged Salad - Batch V55",
              "result": "FAIL",
              "details": [
                {"analysisName": "Listeria Detection", "value": "Detected", "limit": "0 CFU/g", "status": "FAIL"},
                {"analysisName": "Pesticide Screening", "value": "0.002 mg/kg", "limit": "0.01 mg/kg", "status": "PASS"}
              ]
            }
          ],
          "technicianNotes": "Listeria found in bagged salad batch"
        }
    """.trimIndent()

    private val report = Report(
        order = labOrder,
        orderStatus = "COMPLETE",
        resultAnalyses = resultAnalysesJson
    )

    private val reportResponse = ReportResponse(
        reportId = report.id,
        clientOrderId = labOrder.clientOrderId,
        orderStatus = "COMPLETE",
        samplesResults = listOf(
            SampleResultResponse(
                sampleName = "Chicken Breast - Batch A22",
                result = "PASS",
                details = listOf(
                    AnalysisDetailResponse(analysisName = "Salmonella Detection", value = "Not detected", limit = "0 CFU/g", status = "PASS"),
                    AnalysisDetailResponse(analysisName = "Total Bacterial Count", value = "1200 CFU/g", limit = "10000 CFU/g", status = "PASS"),
                    AnalysisDetailResponse(analysisName = "Antibiotic Residue Check", value = "Not detected", limit = "0 µg/kg", status = "PASS")
                )
            ),
            SampleResultResponse(
                sampleName = "Bagged Salad - Batch V55",
                result = "FAIL",
                details = listOf(
                    AnalysisDetailResponse(analysisName = "Listeria Detection", value = "Detected", limit = "0 CFU/g", status = "FAIL"),
                    AnalysisDetailResponse(analysisName = "Pesticide Screening", value = "0.002 mg/kg", limit = "0.01 mg/kg", status = "PASS")
                )
            )
        ),
        technicianNotes = "Listeria found in bagged salad batch",
        createdAt = report.createdAt
    )

    @Test
    fun `returns report when found`() {
        every { reportRepository.findByOrderClientOrderId(labOrder.clientOrderId) } returns report

        val result: ReportResponse = reportService.getReportByClientOrderId(labOrder.clientOrderId)

        assertNotNull(reportResponse, "A report must be returned")
        assertEquals(reportResponse, result)
    }


    @Test
    fun `throws 404 when report not found`() {
        every { reportRepository.findByOrderClientOrderId(labOrder.clientOrderId) } returns null

        val exception: ResponseStatusException = assertThrows<ResponseStatusException> {
            reportService.getReportByClientOrderId(labOrder.clientOrderId)
        }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)

    }


    @Test
    fun `creates report and updates order status`() {
        every { reportRepository.findByOrderClientOrderId(reportRequest.clientOrderId) } returns null
        every { labOrderRepository.findByClientOrderId(reportRequest.clientOrderId) } returns labOrder
        every { reportRepository.save(any()) } returns report

        val result = reportService.createReport(reportRequest)

        assertEquals(LabOrderStatus.COMPLETE, labOrder.status)
        assertEquals(reportResponse, result)
        verify { reportRepository.save(any()) }
    }

    @Test
    fun `throws 409 when report for order already exists (idempotency)`() {
        every { reportRepository.findByOrderClientOrderId(reportRequest.clientOrderId) } returns report

        val exception = assertThrows<ResponseStatusException>
            { reportService.createReport(reportRequest) }
        assertEquals(HttpStatus.CONFLICT, exception.statusCode)
    }

    @Test
    fun `throws 404 when order not found`() {
        every { reportRepository.findByOrderClientOrderId(reportRequest.clientOrderId) } returns null
        every { labOrderRepository.findByClientOrderId(reportRequest.clientOrderId) } returns null

        val exception = assertThrows<ResponseStatusException>
        { reportService.createReport(reportRequest) }
        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)
    }

    @Test
    fun `sets order status to COMPLETE when orderStatus is COMPLETE`() {
        every { reportRepository.findByOrderClientOrderId(reportRequest.clientOrderId) } returns null
        every { labOrderRepository.findByClientOrderId(reportRequest.clientOrderId) } returns labOrder
        every { reportRepository.save(any()) } returns report

        reportService.createReport(reportRequest)

        assertEquals(LabOrderStatus.COMPLETE, labOrder.status)
    }

    @Test
    fun `sets order status to FAILED when orderStatus is FAILED`() {
        val failedRequest = reportRequest.copy(orderStatus = "FAILED")
        val failedReport = Report(order = labOrder, orderStatus = "FAILED", resultAnalyses = resultAnalysesJson)
        every { reportRepository.findByOrderClientOrderId(failedRequest.clientOrderId) } returns null
        every { labOrderRepository.findByClientOrderId(failedRequest.clientOrderId) } returns labOrder
        every { reportRepository.save(any()) } returns failedReport

        reportService.createReport(failedRequest)

        assertEquals(LabOrderStatus.FAILED, labOrder.status)
    }

}
