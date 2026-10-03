package com.learning.spring.tx;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AuditRepository extends JpaRepository<BankEntities.AuditEntry, Long> {
    List<BankEntities.AuditEntry> findAllByOrderByIdAsc();
}
