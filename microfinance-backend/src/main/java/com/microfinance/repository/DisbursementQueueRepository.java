package com.microfinance.repository;

import com.microfinance.entity.DisbursementQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DisbursementQueueRepository extends JpaRepository<DisbursementQueue, Long> {
}
