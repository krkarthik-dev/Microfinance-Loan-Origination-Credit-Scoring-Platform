package com.microfinance;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import com.microfinance.repository.LoanApplicationRepository;
import com.microfinance.entity.LoanApplication;
import java.util.Optional;

@SpringBootTest
public class TestQuery {
    
    @Autowired
    private LoanApplicationRepository repo;

    @Test
    public void test() {
        System.out.println("====== TEST QUERY ======");
        Optional<LoanApplication> app = repo.findByApplicationNumber("MF-2026-00007");
        if (app.isPresent()) {
            System.out.println("APP FOUND!");
            System.out.println("Email: " + app.get().getApplicant().getEmail());
            System.out.println("Status: " + app.get().getStatus());
        } else {
            System.out.println("APP NOT FOUND!");
        }
        System.out.println("====== END ======");
    }
}
