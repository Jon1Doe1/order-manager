package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig
import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.LabOrderResponse
import com.example.order_manager.entity.Analysis
import com.example.order_manager.entity.LabOrder
import com.example.order_manager.entity.Sample
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import jakarta.transaction.Transactional
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class LabOrderService(
    private val labOrderRepository: LabOrderRepository,
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * The method return the list of the orders
     */
    fun getLabOrders(): List<LabOrderResponse> {
        val orders = labOrderRepository.findAll().map { toResponse(it) }
        logger.debug("Returning {} orders", orders.size)
        return orders
    }

    /**
     * The method sends orders to RabbitMQ
     */
    fun enqueueLabOrders(listLabOrders: List<LabOrderRequest>) {
        logger.info("Enqueueing {} orders to RabbitMQ", listLabOrders.size)
        listLabOrders.forEach { labOrderRequest ->
            rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_ORDERS,
                "orders.routing.key.new",
                labOrderRequest
            )
            logger.debug("Order {} sent to queue", labOrderRequest.clientOrderId)
        }
    }

    /**
     * The method saves the lab order into the db
     */
    @Transactional
    fun processAndSaveOrder(labOrderRequest: LabOrderRequest) {
        if (labOrderRepository.findByClientOrderId(labOrderRequest.clientOrderId) != null) {
            logger.warn("Order {} already exists, skipping (idempotency)", labOrderRequest.clientOrderId)
            return
        }

        val labOrder: LabOrder = LabOrder(
            clientId = labOrderRequest.clientId,
            clientOrderId = labOrderRequest.clientOrderId,
            status = LabOrderStatus.VALIDATED
        )
        val sampleEntities: MutableList<Sample> = labOrderRequest.samples.map { sampleDto ->
            val sample = Sample(
                sampleName = sampleDto.sampleName,
                order = labOrder
            )
            val analysisEntities: MutableList<Analysis> = sampleDto.analyses.map { analysisDto ->
                Analysis(
                    name = analysisDto.name,
                    sample = sample
                )
            }.toMutableList()

            sample.analyses = analysisEntities
            sample
        }.toMutableList()

        labOrder.samples = sampleEntities
        labOrderRepository.save(labOrder)
        logger.info("Order {} saved with status {}", labOrder.clientOrderId, labOrder.status)
    }

    /**
     * The method return the lab order with specific customer order id
     */
    fun getOrderByClientOrderId(clientOrderId: String): LabOrderResponse {
        val order = labOrderRepository.findByClientOrderId(clientOrderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found").also {
                logger.warn("Order {} not found", clientOrderId)
            }
        return toResponse(order)
    }

    /**
     * The method changes the status of the order
     */
    @Transactional
    fun changeStatusByClientId(clientOrderId: String, newStatus: String): LabOrderResponse {
        val order = labOrderRepository.findByClientOrderId(clientOrderId)
            ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "Order $clientOrderId not found").also {
                logger.warn("Order {} not found", clientOrderId)
            }

        val targetStatus = LabOrderStatus.valueOf(newStatus.uppercase())

        if (order.status == targetStatus) {
            logger.warn("Order {} already has status {}, skipping (idempotency)", clientOrderId, targetStatus)
            return toResponse(order)
        }

        order.status = targetStatus
        labOrderRepository.save(order)
        logger.info("Order {} status changed to {}", clientOrderId, targetStatus)

        return toResponse(order)
    }

    private fun toResponse(order: LabOrder) = LabOrderResponse(
        id = order.id,
        clientId = order.clientId,
        clientOrderId = order.clientOrderId,
        status = order.status,
        createdAt = order.createdAt,
        updatedAt = order.updatedAt
    )
}
