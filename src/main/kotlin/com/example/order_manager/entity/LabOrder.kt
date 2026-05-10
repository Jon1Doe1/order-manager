package com.example.order_manager.entity

import com.example.order_manager.enumeration.LabOrderStatus
import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "orders")
@EntityListeners(AuditingEntityListener::class) // for @CreatedDate and @LastModifiedDate
class LabOrder(
    @Id
    val id: UUID = UUID.randomUUID(),

    @Column(name = "client_id", nullable = false)
    var clientId: String,

    @Column(name = "client_order_id", nullable = false, unique = true)
    var clientOrderId: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: LabOrderStatus = LabOrderStatus.NEW,

    @Column(name = "sent", nullable = false)
    var sent: Boolean = false,

    @OneToMany(mappedBy = "order", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    var samples: MutableList<Sample> = mutableListOf()
) {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant? = null
}