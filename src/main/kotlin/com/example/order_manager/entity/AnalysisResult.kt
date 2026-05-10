package com.example.order_manager.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "analysis_results")
class AnalysisResult(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "value")
    var value: String,

    @Column(name = "unit")
    var unit: String,

    @Column(name = "reference_range")
    var referenceRange: String,

    @OneToOne
    @JoinColumn(name = "analysis_id")
    val analysis: Analysis
)