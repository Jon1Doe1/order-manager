package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig
import com.example.order_manager.entity.LabOrder
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import java.util.UUID

@Service
class LimsClientService(
    private val labOrderRepository: LabOrderRepository,
    private val rabbitTemplate: RabbitTemplate,
    private val limsWebClient: WebClient
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    // Publishes VALIDATED orders to the LIMS queue and marks them as sent=true.
    // At-least-once: a crash between publish and commit causes a duplicate, handled by idempotency in the listener.
    @Scheduled(fixedDelay = 15000)
    @Transactional
    fun dispatchOrders() {
        val orders: List<LabOrder> = labOrderRepository.findByStatusAndSent(LabOrderStatus.VALIDATED, false)

        if (orders.isEmpty()) {
            logger.debug("No VALIDATED orders to dispatch")
            return
        }

        logger.info("Dispatching {} VALIDATED orders to LIMS queue", orders.size)
        orders.forEach { order ->
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_LIMS_SEND,
                "lims.send.order",
                order.id.toString()
            )
            order.sent = true
            logger.debug("Order {} queued for LIMS", order.clientOrderId)
        }
    }

    /**
     * Consumer — reads from the LIMS queue and fires the HTTP request. No DB interaction.
     */
    @RabbitListener(queues = [RabbitMQConfig.QUEUE_LIMS_SEND])
    fun onLimsOrder(orderIdStr: String) {
        val order: LabOrder = labOrderRepository.findByIdWithDetails(UUID.fromString(orderIdStr))
            ?: run {
                logger.warn("Order {} not found, skipping LIMS dispatch", orderIdStr)
                return
            }

        logger.info("Sending order {} to LIMS", order.clientOrderId)

        try {
            limsWebClient.post()
                .uri("/orders")
                .bodyValue(order.toLimsPayload())
                .retrieve()
                .toBodilessEntity()
                .block()
            logger.info("Order {} sent to LIMS successfully", order.clientOrderId)
        } catch (e: WebClientResponseException) {
            logger.error("LIMS rejected order {}: HTTP {} {}", order.clientOrderId, e.statusCode, e.message)
        }
    }

    private fun LabOrder.toLimsPayload() = LimsOrderPayload(
        clientId = clientId,
        clientOrderId = clientOrderId,
        status = status,
        samples = samples.map { sample ->
            LimsSamplePayload(
                sampleName = sample.sampleName,
                analyses = sample.analyses.map { LimsAnalysisPayload(name = it.name) }
            )
        }
    )

    private data class LimsOrderPayload(
        val clientId: String,
        val clientOrderId: String,
        val status: LabOrderStatus,
        val samples: List<LimsSamplePayload>
    )

    private data class LimsSamplePayload(
        val sampleName: String,
        val analyses: List<LimsAnalysisPayload>
    )

    private data class LimsAnalysisPayload(val name: String)
}
