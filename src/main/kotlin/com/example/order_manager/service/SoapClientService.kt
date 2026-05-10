package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig.Companion.EXCHANGE_ORDERS
import com.example.order_manager.dto.AnalysisRequest
import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.SampleRequest
import com.example.order_manager.enumeration.LabOrderStatus
import kotlinx.coroutines.*
import org.slf4j.LoggerFactory
import org.springframework.amqp.rabbit.core.RabbitTemplate
import org.springframework.stereotype.Service
import org.w3c.dom.Element
import javax.xml.parsers.DocumentBuilderFactory

/*
 * Expected XML structure from the SOAP API:
 *
 * <orders>
 *   <order>
 *     <clientId>ALDI_DC_01</clientId>
 *     <clientOrderId>LOT-2026-001</clientOrderId>
 *     <samples>
 *       <sample>
 *         <sampleName>Chicken Breast - Batch A22</sampleName>
 *         <analyses>
 *           <analysis>Salmonella Detection</analysis>
 *           <analysis>Total Bacterial Count</analysis>
 *         </analyses>
 *       </sample>
 *     </samples>
 *   </order>
 * </orders>
 */
@Service
class SoapClientService(
    private val rabbitTemplate: RabbitTemplate
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    /**
     * Parses the given XML and processes each order concurrently.
     *
     */
    suspend fun processOrders(xml: String) {
        val orders = parseOrders(xml)
        logger.info("Received ${orders.size} orders from SOAP API")

        supervisorScope {
            orders
                .map { order -> async { processOrder(order) } }
                .awaitAll()
        }

        logger.info("Finished processing ${orders.size} orders")
    }

    /**
     * Transforms a single orderElement into a LabOrderRequest and publishes it to RabbitMQ.
     *
     */
    private suspend fun processOrder(orderElement: Element) {
        val request = transformToLabOrderRequest(orderElement)
        logger.debug("Publishing order {} to RabbitMQ", request.clientOrderId)

        try {
            withContext(Dispatchers.IO) {
                rabbitTemplate.convertAndSend(EXCHANGE_ORDERS, "orders.routing.key.new", request)
            }
            logger.debug("Order {} published successfully", request.clientOrderId)
        } catch (e: Exception) {
            logger.error("Failed to publish order {}: {}", request.clientOrderId, e.message)
        }
    }

    /**
     * Parses the full XML string and returns the list of `<order>` elements.
     *
     */
    private fun parseOrders(xml: String): List<Element> {
        val document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(xml.byteInputStream())
        val nodes = document.getElementsByTagName("order")
        return (0 until nodes.length).map { nodes.item(it) as Element }
    }

    /**
     * Maps an `<order>` XML element to a LabOrderRequest domain object.
     *
     */
    private fun transformToLabOrderRequest(order: Element): LabOrderRequest {
        val clientId = order.getElementsByTagName("clientId").item(0).textContent
        val clientOrderId = order.getElementsByTagName("clientOrderId").item(0).textContent
        val sampleNodes = order.getElementsByTagName("sample")

        val samples = (0 until sampleNodes.length).map { i ->
            val sample = sampleNodes.item(i) as Element
            val sampleName = sample.getElementsByTagName("sampleName").item(0).textContent
            val analysisNodes = sample.getElementsByTagName("analysis")
            val analyses = (0 until analysisNodes.length).map { j ->
                AnalysisRequest(name = analysisNodes.item(j).textContent)
            }
            SampleRequest(sampleName = sampleName, analyses = analyses)
        }

        return LabOrderRequest(
            clientId = clientId,
            clientOrderId = clientOrderId,
            status = LabOrderStatus.NEW,
            samples = samples
        )
    }
}
