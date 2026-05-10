package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig
import com.example.order_manager.entity.LabOrder
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import io.mockk.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import java.util.*

class LimsClientServiceTest {

    private val labOrderRepository = mockk<LabOrderRepository>()
    private val rabbitTemplate = mockk<RabbitTemplate>()
    private val webClient = mockk<WebClient>()
    private val orderId = UUID.randomUUID()

    private lateinit var service: LimsClientService

    @BeforeEach
    fun setUp() {
        service = LimsClientService(labOrderRepository, rabbitTemplate, webClient)
    }


    @Test
    fun `does nothing when no validated orders exist`() {
        every { labOrderRepository.findByStatusAndSent(LabOrderStatus.VALIDATED, false) } returns emptyList()

        service.dispatchOrders()

        verify(exactly = 0) { rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<String>()) }
    }

    @Test
    fun `publishes each order to the queue and marks it as sent`() {
        val id1 = UUID.randomUUID()
        val id2 = UUID.randomUUID()
        val order1 = mockk<LabOrder>(relaxed = true).also { every { it.id } returns id1 }
        val order2 = mockk<LabOrder>(relaxed = true).also { every { it.id } returns id2 }
        every { labOrderRepository.findByStatusAndSent(LabOrderStatus.VALIDATED, false) } returns listOf(order1, order2)
        every { rabbitTemplate.convertAndSend(any<String>(), any<String>(), any<String>()) } just Runs

        service.dispatchOrders()

        verify { rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_LIMS_SEND, "lims.send.order", id1.toString()) }
        verify { rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_LIMS_SEND, "lims.send.order", id2.toString()) }
        verify { order1.sent = true }
        verify { order2.sent = true }
    }


    @Test
    fun `does nothing when order is not found`() {
        every { labOrderRepository.findByIdWithDetails(orderId) } returns null

        service.onLimsOrder(orderId.toString())

        verify(exactly = 0) { webClient.post() }
    }

    @Test
    fun `sends HTTP request to LIMS`() {
        val order = buildOrder()
        mockWebClientSuccess()

        service.onLimsOrder(orderId.toString())

        verify { webClient.post() }
    }

    @Test
    fun `swallows LIMS error without throwing`() {
        buildOrder()
        mockWebClientError()

        service.onLimsOrder(orderId.toString())  // must not throw
    }

    private fun buildOrder(): LabOrder {
        val order = mockk<LabOrder>(relaxed = true)
        every { order.samples } returns mutableListOf()
        every { labOrderRepository.findByIdWithDetails(orderId) } returns order
        return order
    }

    private fun mockWebClientSuccess() {
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()
        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.toBodilessEntity() } returns Mono.empty()
    }

    private fun mockWebClientError() {
        val requestBodyUriSpec = mockk<WebClient.RequestBodyUriSpec>()
        val requestBodySpec = mockk<WebClient.RequestBodySpec>()
        val responseSpec = mockk<WebClient.ResponseSpec>()
        every { webClient.post() } returns requestBodyUriSpec
        every { requestBodyUriSpec.uri(any<String>()) } returns requestBodySpec
        every { requestBodySpec.bodyValue(any()) } returns requestBodySpec
        every { requestBodySpec.retrieve() } returns responseSpec
        every { responseSpec.toBodilessEntity() } returns Mono.error(
            WebClientResponseException(500, "Internal Server Error", null, null, null)
        )
    }

}
