package com.example.order_manager.service

import com.example.order_manager.dto.AnalysisRequest
import com.example.order_manager.dto.LabOrderRequest
import com.example.order_manager.dto.SampleRequest
import com.example.order_manager.enumeration.LabOrderStatus
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RabbitMQServiceTest {

    private val labOrderService: LabOrderService = mockk<LabOrderService>()
    private lateinit var rabbitMQService: RabbitMQService

    @BeforeEach
    fun setup() {
        rabbitMQService = RabbitMQService(labOrderService)
    }

    private val validLabOrderRequest = LabOrderRequest(
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

    @Test
    fun `processes and saves order on valid message`() {
        every { labOrderService.processAndSaveOrder(validLabOrderRequest) } just Runs
        //When
        rabbitMQService.receiveOrder(validLabOrderRequest)
        //Then
        verify (exactly = 1) { labOrderService.processAndSaveOrder(validLabOrderRequest) }
    }

    @Test
    fun `rethrows exception to trigger RabbitMQ retry`() {
        every { labOrderService.processAndSaveOrder(validLabOrderRequest) } throws RuntimeException("Exception")

        assertThrows<RuntimeException> {
            rabbitMQService.receiveOrder(validLabOrderRequest)
        }

        verify(exactly = 1) { labOrderService.processAndSaveOrder(validLabOrderRequest) }
    }


}
