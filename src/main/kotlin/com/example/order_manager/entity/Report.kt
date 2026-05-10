package com.example.order_manager.entity

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.type.SqlTypes
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.Instant
import java.util.*

@Entity
@Table(name = "reports")
@EntityListeners(AuditingEntityListener::class) // for @CreatedDate
class Report(
    @Id
    val id: UUID = UUID.randomUUID(),

    @OneToOne
    @JoinColumn(name = "order_id", nullable = false)
    val order: LabOrder,

    @Column(name = "order_status", nullable = false)
    var orderStatus: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "result_analyses", columnDefinition = "jsonb", nullable = false)
    var resultAnalyses: String
) {
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    var createdAt: Instant? = null
}