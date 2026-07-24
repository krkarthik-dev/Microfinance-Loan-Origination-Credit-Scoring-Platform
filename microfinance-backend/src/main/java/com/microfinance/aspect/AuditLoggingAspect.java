package com.microfinance.aspect;

import com.microfinance.entity.AuditLog;
import com.microfinance.entity.User;
import com.microfinance.repository.AuditLogRepository;
import com.microfinance.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Aspect to automatically intercept key business transitions and record them
 * in the AuditLog. Removes boilerplate logging from the service layer.
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditLoggingAspect {

    private final AuditLogRepository auditLogRepository;
    private final UserRepository userRepository;

    @AfterReturning(
            pointcut = "execution(* com.microfinance.service.OfficerService.submitDecision(..))",
            returning = "result"
    )
    public void logOfficerDecision(JoinPoint joinPoint, Object result) {
        logAudit("LOAN_DECISION", "OFFICER_DECISION_SUBMITTED", joinPoint.getArgs());
    }

    @AfterReturning(
            pointcut = "execution(* com.microfinance.service.LoanSubmissionService.submitApplication(..))",
            returning = "result"
    )
    public void logLoanSubmission(JoinPoint joinPoint, Object result) {
        logAudit("LOAN_APPLICATION", "SUBMITTED", joinPoint.getArgs());
    }
    
    @AfterReturning(
            pointcut = "execution(* com.microfinance.service.KycDocumentService.uploadDocument(..))",
            returning = "result"
    )
    public void logKycUpload(JoinPoint joinPoint, Object result) {
        logAudit("KYC_DOCUMENT", "UPLOADED", joinPoint.getArgs());
    }

    private void logAudit(String entityType, String action, Object[] args) {
        try {
            User currentUser = getCurrentUser();
            
            // Just capturing the first argument string as a basic summary of what was affected
            String details = "Arguments: ";
            if (args != null && args.length > 0) {
                details += args[0] != null ? args[0].toString() : "null";
            }

            AuditLog auditLog = AuditLog.builder()
                    .entityType(entityType)
                    .entityId(0L) // Since we don't always have the ID easily in AOP without reflection, 0 as placeholder for now, or could parse result
                    .action(action)
                    .performedBy(currentUser)
                    .newValue(details)
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit log saved: {} - {}", entityType, action);
        } catch (Exception e) {
            log.error("Failed to save audit log for {} - {}", entityType, action, e);
        }
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || auth.getPrincipal().equals("anonymousUser")) {
            return null;
        }
        Optional<User> userOpt = userRepository.findByEmail(auth.getName());
        return userOpt.orElse(null);
    }
}
