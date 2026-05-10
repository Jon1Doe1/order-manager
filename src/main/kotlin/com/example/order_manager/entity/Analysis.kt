package com.example.order_manager.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "analyses")
class Analysis(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "name", nullable = false)
    var name: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sample_id", nullable = false)
    val sample: Sample,

    @OneToOne(mappedBy = "analysis", cascade = [CascadeType.ALL])
    var analysisResult: AnalysisResult? = null
)