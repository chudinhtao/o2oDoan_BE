package com.fnb.kds.repository;

import com.fnb.kds.entity.KdsOrderTicket;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface KdsOrderTicketRepository extends JpaRepository<KdsOrderTicket, UUID> {
    List<KdsOrderTicket> findByStatusInOrderByCreatedAtAsc(List<String> statuses);
}
