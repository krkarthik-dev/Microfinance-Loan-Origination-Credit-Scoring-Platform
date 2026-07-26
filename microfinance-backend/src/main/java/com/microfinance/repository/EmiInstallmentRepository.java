package com.microfinance.repository;

import com.microfinance.entity.EmiInstallment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface EmiInstallmentRepository extends JpaRepository<EmiInstallment, Long> {

    List<EmiInstallment> findByLoanApplicationIdOrderByInstallmentNumberAsc(Long loanApplicationId);

    long countByLoanApplicationIdAndStatus(Long loanApplicationId, String status);
}
