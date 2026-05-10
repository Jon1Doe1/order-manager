package com.example.order_manager.repository

import com.example.order_manager.entity.LabOrder
import com.example.order_manager.enumeration.LabOrderStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface LabOrderRepository : JpaRepository<LabOrder, UUID> {

    fun findByClientOrderId(clientOrderId: String): LabOrder?

    fun findByStatusAndSent(status: LabOrderStatus, sent: Boolean): List<LabOrder>

    @Query("SELECT o FROM LabOrder o LEFT JOIN FETCH o.samples s LEFT JOIN FETCH s.analyses WHERE o.id = :id")
    fun findByIdWithDetails(id: UUID): LabOrder?
}