package com.example.order_manager.entity

import jakarta.persistence.*
import java.util.*

@Entity
@Table(name = "samples")
class Sample(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "sample_name", nullable = false)
    var sampleName: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    val order: LabOrder,

    @OneToMany(mappedBy = "sample", cascade = [CascadeType.ALL], orphanRemoval = true)
    var analyses: MutableList<Analysis> = mutableListOf()
)