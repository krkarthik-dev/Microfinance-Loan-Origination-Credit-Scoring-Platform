package com.microfinance.repository;

import com.microfinance.entity.CreditScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CreditScoreRepository extends JpaRepository<CreditScore, Long> {
    Optional<CreditScore> findByApplicationId(Long applicationId);

    @org.springframework.data.jpa.repository.Query("SELECT c.riskTier, COUNT(c) FROM CreditScore c GROUP BY c.riskTier")
    java.util.List<Object[]> countByRiskTier();
}
