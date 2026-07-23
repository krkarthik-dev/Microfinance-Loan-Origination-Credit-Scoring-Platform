package com.microfinance.repository;

import com.microfinance.entity.LoanProduct;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link LoanProduct} entity.
 */
@Repository
public interface LoanProductRepository extends JpaRepository<LoanProduct, Long> {

    /** Returns only currently active products — shown to borrowers during application. */
    List<LoanProduct> findByActiveTrueOrderByProductNameAsc();

    /** Lookup by name for admin duplicate check. */
    Optional<LoanProduct> findByProductName(String productName);

    /** Check name uniqueness before creating new product. */
    boolean existsByProductName(String productName);
}
