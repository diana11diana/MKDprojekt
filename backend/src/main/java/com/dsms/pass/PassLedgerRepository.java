package com.dsms.pass;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PassLedgerRepository extends JpaRepository<PassLedgerEntry, Long> {
}

