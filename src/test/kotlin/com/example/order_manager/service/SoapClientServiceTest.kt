package com.example.order_manager.service

import com.example.order_manager.config.RabbitMQConfig.Companion.EXCHANGE_ORDERS
import com.example.order_manager.dto.AnalysisRequest
import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.SampleRequest
import com.example.order_manager.enumeration.LabOrderStatus
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.amqp.rabbit.core.RabbitTemplate

class SoapClientServiceTest {

    private val rabbitTemplate: RabbitTemplate = mockk(relaxed = true)
    private val soapClientService = SoapClientService(rabbitTemplate)

    private val singleOrderXml = """
        <orders>
          <order>
            <clientId>ALDI_DISTRIBUTION_CENTER_04</clientId>
            <clientOrderId>LOT-2026-XJF88</clientOrderId>
            <samples>
              <sample>
                <sampleName>Chicken Breast - Batch A22</sampleName>
                <analyses>
                  <analysis>Salmonella Detection</analysis>
                  <analysis>Total Bacterial Count</analysis>
                </analyses>
              </sample>
            </samples>
          </order>
        </orders>
    """.trimIndent()

    private val twoOrdersXml = """
        <orders>
          <order>
            <clientId>ALDI_DISTRIBUTION_CENTER_04</clientId>
            <clientOrderId>LOT-2026-XJF88</clientOrderId>
            <samples>
              <sample>
                <sampleName>Chicken Breast - Batch A22</sampleName>
                <analyses>
                  <analysis>Salmonella Detection</analysis>
                </analyses>
              </sample>
            </samples>
          </order>
          <order>
            <clientId>LIDL_WAREHOUSE_BERLIN_02</clientId>
            <clientOrderId>LOT-2026-KPZ14</clientOrderId>
            <samples>
              <sample>
                <sampleName>Ground Beef - Batch B77</sampleName>
                <analyses>
                  <analysis>E. Coli Detection</analysis>
                </analyses>
              </sample>
            </samples>
          </order>
        </orders>
    """.trimIndent()

    @Test
    fun `publishes one message per order`() = runTest {
        soapClientService.processOrders(twoOrdersXml)

        verify(exactly = 2) {
            rabbitTemplate.convertAndSend(EXCHANGE_ORDERS, "orders.routing.key.new", any<LabOrderRequest>())
        }
    }

    @Test
    fun `publishes correct LabOrderRequest content`() = runTest {
        soapClientService.processOrders(singleOrderXml)

        val expectedRequest = LabOrderRequest(
            clientId = "ALDI_DISTRIBUTION_CENTER_04",
            clientOrderId = "LOT-2026-XJF88",
            status = LabOrderStatus.NEW,
            samples = listOf(
                SampleRequest(
                    sampleName = "Chicken Breast - Batch A22",
                    analyses = listOf(
                        AnalysisRequest(name = "Salmonella Detection"),
                        AnalysisRequest(name = "Total Bacterial Count")
                    )
                )
            )
        )

        verify {
            rabbitTemplate.convertAndSend(EXCHANGE_ORDERS, "orders.routing.key.new", expectedRequest)
        }
    }

}
