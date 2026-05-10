package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig
import com.example.order_manager.dto.LabOrderRequest
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.annotation.RabbitListener
import org.springframework.stereotype.Service

@Service
class RabbitMQService(
    private val labOrderService: LabOrderService
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * CONSUMER
     */
    @RabbitListener(queues = [RabbitMQConfig.QUEUE_ORDERS])
    fun receiveOrder(labOrderRequest: LabOrderRequest) {
        logger.info("Received order {} from queue", labOrderRequest.clientOrderId)

        try {
            labOrderService.processAndSaveOrder(labOrderRequest)
            logger.info("Order {} processed successfully", labOrderRequest.clientOrderId)
        } catch (e: Exception) {
            logger.error("Failed to process order {}: {}", labOrderRequest.clientOrderId, e.message)
            throw e
        }
    }
}
