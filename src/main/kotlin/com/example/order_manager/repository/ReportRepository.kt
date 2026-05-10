package com.example.order_manager.repository

import com.example.order_manager.entity.Report
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface ReportRepository : JpaRepository<Report, UUID> {

    fun findByOrderClientOrderId(clientOrderId: String): Report?
}
