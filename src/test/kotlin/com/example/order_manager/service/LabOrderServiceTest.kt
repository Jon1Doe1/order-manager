package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig
import com.example.order_manager.dto.AnalysisRequest
import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.LabOrderResponse
import com.example.order_manager.dto.SampleRequest
import com.example.order_manager.entity.Analysis
import com.example.order_manager.entity.LabOrder
import com.example.order_manager.entity.Sample
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.api.assertThrows
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.web.server.ResponseStatusException
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class LabOrderServiceTest {

    private val labOrderRepository: LabOrderRepository = mockk<LabOrderRepository>()
    private val rabbitTemplate: RabbitTemplate = mockk<RabbitTemplate>()
    private lateinit var labOrderService: LabOrderService

    private val labOrderRequest = LabOrderRequest(
        clientId = "ALDI_DISTRIBUTION_CENTER_04",
        clientOrderId = "LOT-2026-XJF88",
        status = LabOrderStatus.NEW,
        samples = listOf(
            SampleRequest(
                sampleName = "Chicken Breast - Batch A22",
                analyses = listOf(
                    AnalysisRequest(name = "Salmonella Detection"),
                    AnalysisRequest(name = "Total Bacterial Count"),
                    AnalysisRequest(name = "Antibiotic Residue Check")
                )
            ),
            SampleRequest(
                sampleName = "Bagged Salad - Batch V55",
                analyses = listOf(
                    AnalysisRequest(name = "Listeria Detection"),
                    AnalysisRequest(name = "Pesticide Screening")
                )
            ),
            SampleRequest(
                sampleName = "Organic Eggs - Batch E99",
                analyses = listOf(
                    AnalysisRequest(name = "Dioxin Analysis")
                )
            )
        )
    )
    private val labOrder = LabOrder(
        clientId = "ALDI_DISTRIBUTION_CENTER_04",
        clientOrderId = "LOT-2026-XJF88",
        status = LabOrderStatus.VALIDATED
    ).also { order ->
        val chickenBreast = Sample(sampleName = "Chicken Breast - Batch A22", order = order).also { sample ->
            sample.analyses = mutableListOf(
                Analysis(name = "Salmonella Detection", sample = sample),
                Analysis(name = "Total Bacterial Count", sample = sample),
                Analysis(name = "Antibiotic Residue Check", sample = sample)
            )
        }
        val baggedSalad = Sample(sampleName = "Bagged Salad - Batch V55", order = order).also { sample ->
            sample.analyses = mutableListOf(
                Analysis(name = "Listeria Detection", sample = sample),
                Analysis(name = "Pesticide Screening", sample = sample)
            )
        }
        val organicEggs = Sample(sampleName = "Organic Eggs - Batch E99", order = order).also { sample ->
            sample.analyses = mutableListOf(
                Analysis(name = "Dioxin Analysis", sample = sample)
            )
        }
        order.samples = mutableListOf(chickenBreast, baggedSalad, organicEggs)
    }

    @BeforeEach
    fun setUp(){
        labOrderService = LabOrderService(labOrderRepository, rabbitTemplate)
    }

    @Test
    fun `returns empty list when no orders exist`() {
        every { labOrderRepository.findAll() } returns listOf()

        val labOrders: List<LabOrderResponse> = labOrderService.getLabOrders()

        assertEquals(listOf<LabOrderResponse>(), labOrders,
            "The returned list should be empty")
        verify(exactly = 1) { labOrderRepository.findAll() }
    }

    @Test
    fun `returns all orders mapped to response`() {
        every { labOrderRepository.findAll() } returns listOf(labOrder)

        val labOrders: List<LabOrderResponse> = labOrderService.getLabOrders()

        val expected = LabOrderResponse(
            id = labOrder.id,
            clientId = labOrder.clientId,
            clientOrderId = labOrder.clientOrderId,
            status = labOrder.status,
            createdAt = labOrder.createdAt,
            updatedAt = labOrder.updatedAt
        )

        verify(exactly = 1) { labOrderRepository.findAll() }
        assertNotEquals(listOf(), labOrders,
            "List shoudl not be empty")
        assertEquals(listOf(expected), labOrders)

    }


    @Test
    fun `publishes each order to the queue`() {

        every { rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<LabOrderRequest>()) } just Runs

        labOrderService.enqueueLabOrders(listOf(labOrderRequest))

        verify { rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_ORDERS, "orders.routing.key.new", labOrderRequest) }
    }

    @Test
    fun `does nothing when order with same clientOrderId already exists (idempotency)`() {
        every { labOrderRepository.findByClientOrderId(labOrderRequest.clientOrderId) } returns labOrder

        labOrderService.processAndSaveOrder(labOrderRequest)

        verify(exactly = 0) { labOrderRepository.save(any()) }
    }


    @Test
    fun `returns order when found`() {
        every { labOrderRepository.findByClientOrderId(labOrderRequest.clientOrderId) } returns labOrder

        val labOrderResponse: LabOrderResponse = labOrderService.getOrderByClientOrderId(labOrderRequest.clientOrderId)
        val expected = LabOrderResponse(
            id = labOrder.id,
            clientId = labOrder.clientId,
            clientOrderId = labOrder.clientOrderId,
            status = labOrder.status,
            createdAt = labOrder.createdAt,
            updatedAt = labOrder.updatedAt
        )

        assertEquals(expected, labOrderResponse)
        assertNotNull(labOrderResponse, "If the object exist, it should be returned")

    }

    @Test
    fun `throws 404 when order not found`() {
        every { labOrderRepository.findByClientOrderId(labOrderRequest.clientOrderId) }  returns null

        val exception = assertThrows<ResponseStatusException> {
            labOrderService.getOrderByClientOrderId(labOrderRequest.clientOrderId)
        }

        assertEquals(HttpStatus.NOT_FOUND, exception.statusCode)

    }

}
