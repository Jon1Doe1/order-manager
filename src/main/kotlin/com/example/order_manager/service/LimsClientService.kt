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
import java.util.UUID

@Service
class LimsClientService(
    private val labOrderRepository: LabOrderRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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

    @Transactional
    @RabbitListener(queues = [RabbitMQConfig.QUEUE_LIMS_SEND])
    fun onLimsOrder(orderIdStr: String) {
        val order: LabOrder = labOrderRepository.findByIdWithDetails(UUID.fromString(orderIdStr))
            ?: run {
                logger.warn("Order {} not found, skipping LIMS dispatch", orderIdStr)
                return
            }

        logger.info("Sending order {} to LIMS", order.clientOrderId)
        Thread.sleep(2000)
        logger.info("Order {} sent to LIMS successfully", order.clientOrderId)
    }
}
