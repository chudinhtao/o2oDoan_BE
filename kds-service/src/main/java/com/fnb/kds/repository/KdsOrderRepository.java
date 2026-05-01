package com.fnb.kds.repository;

import com.fnb.kds.entity.KdsOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KdsOrderRepository extends JpaRepository<KdsOrder, UUID> {
}
