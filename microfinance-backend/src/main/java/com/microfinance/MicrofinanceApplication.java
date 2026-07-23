package com.microfinance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Entry point for the Microfinance Loan Origination & Credit Scoring Platform.
 *
 * <p>@EnableAsync enables asynchronous processing for ML credit scoring events
 * triggered via Spring Application Events (LoanSubmittedEvent → async listener).
 */
@SpringBootApplication
@EnableAsync
public class MicrofinanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MicrofinanceApplication.class, args);
    }
}
