package com.microfinance.service;

import com.microfinance.dto.LoanProductDTO;
import com.microfinance.entity.LoanProduct;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.repository.LoanProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoanProductServiceTest {

    @Mock
    private LoanProductRepository loanProductRepository;

    @Mock
    private LoanApplicationRepository loanApplicationRepository;

    @InjectMocks
    private LoanProductService loanProductService;

    private LoanProduct sampleProduct;
    private LoanProductDTO sampleDto;

    @BeforeEach
    void setUp() {
        sampleProduct = LoanProduct.builder()
                .id(1L)
                .productName("Personal Loan")
                .description("Standard personal loan")
                .minAmount(new BigDecimal("10000.00"))
                .maxAmount(new BigDecimal("500000.00"))
                .interestRatePa(new BigDecimal("14.50"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .active(true)
                .build();

        sampleDto = LoanProductDTO.builder()
                .productName("Personal Loan")
                .description("Standard personal loan")
                .minAmount(new BigDecimal("10000.00"))
                .maxAmount(new BigDecimal("500000.00"))
                .interestRatePa(new BigDecimal("14.50"))
                .minTenureMonths(6)
                .maxTenureMonths(36)
                .active(true)
                .build();
    }

    @Test
    void testCreateProductSuccess() {
        when(loanProductRepository.existsByProductName("Personal Loan")).thenReturn(false);
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(sampleProduct);

        LoanProductDTO created = loanProductService.createProduct(sampleDto);

        assertNotNull(created);
        assertEquals("Personal Loan", created.getProductName());
        assertEquals(new BigDecimal("14.50"), created.getInterestRatePa());
        verify(loanProductRepository, times(1)).save(any(LoanProduct.class));
    }

    @Test
    void testCreateProductDuplicateNameThrowsException() {
        when(loanProductRepository.existsByProductName("Personal Loan")).thenReturn(true);

        assertThrows(IllegalArgumentException.class, () -> loanProductService.createProduct(sampleDto));
        verify(loanProductRepository, never()).save(any());
    }

    @Test
    void testCreateProductInvalidAmountLimitsThrowsException() {
        sampleDto.setMaxAmount(new BigDecimal("5000.00")); // max < min

        assertThrows(IllegalArgumentException.class, () -> loanProductService.createProduct(sampleDto));
        verify(loanProductRepository, never()).save(any());
    }

    @Test
    void testUpdateProductSuccess() {
        when(loanProductRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(loanProductRepository.save(any(LoanProduct.class))).thenReturn(sampleProduct);

        sampleDto.setInterestRatePa(new BigDecimal("13.00"));
        LoanProductDTO updated = loanProductService.updateProduct(1L, sampleDto);

        assertNotNull(updated);
        verify(loanProductRepository, times(1)).save(any(LoanProduct.class));
    }

    @Test
    void testToggleStatusSuccess() {
        when(loanProductRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(loanProductRepository.save(any(LoanProduct.class))).thenAnswer(invocation -> invocation.getArgument(0));

        LoanProductDTO toggled = loanProductService.toggleStatus(1L, false);

        assertFalse(toggled.isActive());
        verify(loanProductRepository, times(1)).save(any(LoanProduct.class));
    }

    @Test
    void testDeleteProductBlockedByHistoricalReference() {
        when(loanProductRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(loanApplicationRepository.existsByLoanProductId(1L)).thenReturn(true);

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> loanProductService.deleteProduct(1L));
        assertTrue(exception.getMessage().contains("Cannot permanently delete a loan product linked to historical applications"));
        verify(loanProductRepository, never()).delete(any());
    }

    @Test
    void testDeleteProductSuccessWhenNoHistoricalReference() {
        when(loanProductRepository.findById(1L)).thenReturn(Optional.of(sampleProduct));
        when(loanApplicationRepository.existsByLoanProductId(1L)).thenReturn(false);

        assertDoesNotThrow(() -> loanProductService.deleteProduct(1L));
        verify(loanProductRepository, times(1)).delete(sampleProduct);
    }
}
