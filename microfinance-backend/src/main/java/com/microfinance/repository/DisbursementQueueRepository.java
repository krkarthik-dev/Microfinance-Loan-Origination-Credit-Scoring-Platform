package com.microfinance.repository;

import com.microfinance.entity.DisbursementQueue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface DisbursementQueueRepository extends JpaRepository<DisbursementQueue, Long> {
    Optional<DisbursementQueue> findByLoanApplicationId(Long loanApplicationId);
}
