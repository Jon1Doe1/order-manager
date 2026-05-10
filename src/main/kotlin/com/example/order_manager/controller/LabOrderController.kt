package com.example.order_manager.controller

import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.LabOrderResponse
import com.example.order_manager.service.LabOrderService
import com.example.order_manager.service.SoapClientService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/v1/ordermanager")
class LabOrderController(
    private val labOrderService: LabOrderService,
    private val soapClientService: SoapClientService
) {

    @GetMapping("/orders")
    fun getLabOrders(): ResponseEntity<List<LabOrderResponse>>{
        val listLabOrders: List<LabOrderResponse> = labOrderService.getLabOrders()
        return ResponseEntity.ok(listLabOrders)
    }

    @PostMapping("/orders")
    fun createOrders(@Valid @RequestBody labOrdersRequest: List<LabOrderRequest>): ResponseEntity<String>{
        labOrderService.enqueueLabOrders(labOrdersRequest)
        return ResponseEntity.accepted().body("Orders accepted")
    }

    @GetMapping("/orders/{clientOrderId}")
    fun getLabOrderByClientOrderId(@PathVariable clientOrderId: String): ResponseEntity<LabOrderResponse>{
        val labOrderResponse: LabOrderResponse = labOrderService.getOrderByClientOrderId(clientOrderId)
        return ResponseEntity.ok(labOrderResponse)
    }

    @PostMapping("/orders/soap-client")
    suspend fun receiveSoapOrders(@RequestBody xml: String): ResponseEntity<String> {
        soapClientService.processOrders(xml)
        return ResponseEntity.accepted().body("Orders accepted")
    }

}