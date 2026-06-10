package com.fnb.auth.repository;

import com.fnb.auth.entity.ShiftTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ShiftTemplateRepository extends JpaRepository<ShiftTemplate, UUID> {
    List<ShiftTemplate> findAllByActiveTrue();
}
