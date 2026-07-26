package com.microfinance.repository;

import com.microfinance.entity.CorrectionRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CorrectionRequestRepository extends JpaRepository<CorrectionRequest, Long> {
    List<CorrectionRequest> findByLoanApplicationIdAndResolvedFalse(Long loanApplicationId);
    List<CorrectionRequest> findByLoanApplicationId(Long loanApplicationId);
}
