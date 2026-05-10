package com.example.order_manager.controller

import com.example.order_manager.entity.LabOrder
import com.example.order_manager.enumeration.LabOrderStatus
import com.example.order_manager.repository.LabOrderRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.server.LocalServerPort
import org.springframework.test.context.ActiveProfiles
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.test.assertEquals

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("integration")
class LabOrderControllerIntegrationTest {

    @LocalServerPort
    var port: Int = 0

    @Autowired lateinit var labOrderRepository: LabOrderRepository

    private val httpClient = HttpClient.newHttpClient()

    @AfterEach
    fun tearDown() {
        labOrderRepository.deleteAll()
    }

    private fun baseUrl() = "http://localhost:$port/api/v1/ordermanager"

    private val validOrderJson = """
        [
          {
            "clientId": "ALDI_DISTRIBUTION_CENTER_04",
            "clientOrderId": "LOT-2026-XJF88",
            "status": "NEW",
            "samples": [
              {
                "sampleName": "Chicken Breast - Batch A22",
                "analyses": [
                  { "name": "Salmonella Detection" },
                  { "name": "Total Bacterial Count" }
                ]
              }
            ]
          }
        ]
    """.trimIndent()

    @Test
    fun `POST orders returns 202 when request is valid`() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(validOrderJson))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(202, response.statusCode())
    }

    @Test
    fun `POST orders returns 400 when request body is invalid`() {
        val invalidJson = """[{"clientId": "", "clientOrderId": "LOT-2026-XJF88", "status": "NEW", "samples": []}]"""
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(invalidJson))
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(400, response.statusCode())
    }

    @Test
    fun `GET orders returns 200 with empty list when no orders exist`() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders"))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assertEquals("[]", response.body())
    }

    @Test
    fun `GET orders returns 200 with list of orders`() {
        labOrderRepository.save(
            LabOrder(
                clientId = "ALDI_DISTRIBUTION_CENTER_04",
                clientOrderId = "LOT-2026-XJF88",
                status = LabOrderStatus.VALIDATED
            )
        )

        val getRequest = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders"))
            .GET()
            .build()

        val response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assert(response.body().contains("LOT-2026-XJF88"))
    }

    @Test
    fun `GET order by clientOrderId returns 200 when found`() {
        labOrderRepository.save(
            LabOrder(
                clientId = "ALDI_DISTRIBUTION_CENTER_04",
                clientOrderId = "LOT-2026-XJF88",
                status = LabOrderStatus.VALIDATED
            )
        )

        val getRequest = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders/LOT-2026-XJF88"))
            .GET()
            .build()

        val response = httpClient.send(getRequest, HttpResponse.BodyHandlers.ofString())

        assertEquals(200, response.statusCode())
        assert(response.body().contains("LOT-2026-XJF88"))
    }

    @Test
    fun `GET order by clientOrderId returns 404 when not found`() {
        val request = HttpRequest.newBuilder()
            .uri(URI.create("${baseUrl()}/orders/NON-EXISTENT-ORDER"))
            .GET()
            .build()

        val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

        assertEquals(404, response.statusCode())
    }
}
