package com.fnb.order.repository;

import com.fnb.order.entity.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;

import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByTargetIdOrderByCreatedAtAsc(String targetId);
}
