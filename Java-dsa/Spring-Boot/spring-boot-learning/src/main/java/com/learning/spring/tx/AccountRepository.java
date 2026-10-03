package com.learning.spring.tx;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<BankEntities.Account, Long> {
    Optional<BankEntities.Account> findByOwner(String owner);
}
