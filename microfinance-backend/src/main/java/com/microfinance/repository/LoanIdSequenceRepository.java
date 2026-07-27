package com.microfinance.repository;

import com.microfinance.entity.LoanIdSequence;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Repository
public interface LoanIdSequenceRepository extends JpaRepository<LoanIdSequence, String> {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    default Long getNextValAtomic(String key) {
        Optional<LoanIdSequence> opt = findById(key);
        if (opt.isPresent()) {
            LoanIdSequence seq = opt.get();
            Long current = seq.getNextVal();
            seq.setNextVal(current + 1);
            saveAndFlush(seq);
            return current;
        } else {
            try {
                LoanIdSequence newSeq = LoanIdSequence.builder()
                        .sequenceKey(key)
                        .nextVal(2L)
                        .build();
                saveAndFlush(newSeq);
                return 1L;
            } catch (Exception e) {
                LoanIdSequence existing = findById(key)
                        .orElseThrow(() -> new IllegalStateException("Sequence key not found: " + key));
                Long current = existing.getNextVal();
                existing.setNextVal(current + 1);
                saveAndFlush(existing);
                return current;
            }
        }
    }
}
