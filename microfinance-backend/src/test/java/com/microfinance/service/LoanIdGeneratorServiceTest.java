package com.microfinance.service;

import com.microfinance.entity.LoanIdSequence;
import com.microfinance.repository.LoanIdSequenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
public class LoanIdGeneratorServiceTest {

    @Autowired
    private LoanIdGeneratorService loanIdGeneratorService;

    @Autowired
    private LoanIdSequenceRepository sequenceRepository;

    @BeforeEach
    void setUp() {
        sequenceRepository.deleteAll();
    }

    @Test
    void testFormatAndSequentialIncrement() {
        String id1 = loanIdGeneratorService.generateNextLoanId();
        String id2 = loanIdGeneratorService.generateNextLoanId();

        assertNotNull(id1);
        assertNotNull(id2);

        // Verify format LN-MMYYYYXXXX (e.g. LN-0720260001)
        Pattern pattern = Pattern.compile("^LN-\\d{6}\\d{4}$");
        assertTrue(pattern.matcher(id1).matches(), "ID 1 does not match LN-MMYYYYXXXX format: " + id1);
        assertTrue(pattern.matcher(id2).matches(), "ID 2 does not match LN-MMYYYYXXXX format: " + id2);

        assertTrue(id1.endsWith("0001"), "First ID should end with 0001 but was: " + id1);
        assertTrue(id2.endsWith("0002"), "Second ID should end with 0002 but was: " + id2);
    }

    @Test
    void testConcurrencyNoCollisions() throws InterruptedException {
        int threadCount = 20;
        ExecutorService executor = Executors.newFixedThreadPool(10);
        CountDownLatch latch = new CountDownLatch(threadCount);
        Set<String> generatedIds = Collections.newSetFromMap(new ConcurrentHashMap<>());

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    String id = loanIdGeneratorService.generateNextLoanId();
                    generatedIds.add(id);
                } finally {
                    latch.countDown();
                }
            });
        }

        latch.await();
        executor.shutdown();

        assertEquals(threadCount, generatedIds.size(), "Concurrent generation caused duplicate IDs!");
    }
}
