package com.example.order_manager.dto

import com.example.order_manager.enumeration.LabOrderStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.AssertTrue
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull

data class LabOrderRequest(

    @field:NotBlank(message = "clientId is required")
    val clientId: String,

    @field:NotBlank(message = "clientId is required")
    val clientOrderId: String,

    @field:NotNull(message = "status is required")
    val status: LabOrderStatus,

    @field:NotEmpty(message = "The order should contain at least one sample")
    @field:Valid // to validate objects inside the list
    val samples: List<SampleRequest>
){
    @AssertTrue(message = "The status of any new order should be NEW")
    fun isStatusValid(): Boolean {
        return status == LabOrderStatus.NEW
    }
}

data class SampleRequest(
    @field:NotBlank(message = "Name of the sample is required")
    val sampleName: String,

    @field:NotEmpty(message = "Every sample should have at least one analysis")
    @field:Valid
    val analyses: List<AnalysisRequest>
)

data class AnalysisRequest(
    @field:NotBlank(message = "Name of the analysis is required")
    val name: String
)