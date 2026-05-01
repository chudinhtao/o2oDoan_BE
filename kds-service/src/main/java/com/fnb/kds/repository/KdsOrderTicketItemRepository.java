package com.fnb.kds.repository;

import com.fnb.kds.entity.KdsOrderTicketItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface KdsOrderTicketItemRepository extends JpaRepository<KdsOrderTicketItem, UUID> {
}
