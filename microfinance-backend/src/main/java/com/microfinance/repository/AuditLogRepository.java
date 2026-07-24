package com.microfinance.repository;

import com.microfinance.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    java.util.List<AuditLog> findTop10ByOrderByCreatedAtDesc();
}
