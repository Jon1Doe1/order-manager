package com.example.order_manager.controller

import com.example.order_manager.dto.ReportRequest
import com.example.order_manager.dto.ReportResponse
import com.example.order_manager.service.ReportService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ordermanager")
class ReportController(
    private val reportService: ReportService
) {

    @GetMapping("/reports/{clientOrderId}")
    fun getReport(@PathVariable clientOrderId: String): ResponseEntity<ReportResponse> {
        val reportResponse: ReportResponse =  reportService.getReportByClientOrderId(clientOrderId)
        return ResponseEntity.ok(reportResponse)
    }

    @PostMapping ("/reports")
    fun createReport(@Valid @RequestBody request: ReportRequest): ResponseEntity<ReportResponse> {
        val reportResponse: ReportResponse = reportService.createReport(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(reportResponse)
    }
}
