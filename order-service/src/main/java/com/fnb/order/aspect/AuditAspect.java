package com.fnb.order.aspect;

import com.fnb.order.annotation.AuditAction;
import com.fnb.order.entity.AuditLog;
import com.fnb.order.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Arrays;
import java.util.UUID;

@Aspect
@Component
@Slf4j
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditLogRepository auditLogRepository;

    @AfterReturning(pointcut = "@annotation(auditAction)", returning = "result")
    public void logAudit(JoinPoint joinPoint, AuditAction auditAction, Object result) {
        try {
            // 1. Thu thập thông tin người dùng từ SecurityContext
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            String userId = auth != null ? auth.getName() : "ANONYMOUS";
            String role = auth != null ? auth.getAuthorities().toString() : "NONE";

            // 2. Thu thập IP
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            String ip = request.getRemoteAddr();

            // 3. Thu thập thông tin phương thức
            String methodName = auditAction.value();
            
            // Try to find a UUID in the arguments (usually the targetId)
            String targetId = Arrays.stream(joinPoint.getArgs())
                    .filter(arg -> arg instanceof UUID || arg instanceof String)
                    .map(Object::toString)
                    .findFirst()
                    .orElse("UNKNOWN");

            // 4. Lưu vào DB
            AuditLog auditLog = AuditLog.builder()
                    .actionName(methodName)
                    .userId(userId)
                    .role(role)
                    .targetId(targetId)
                    .ipAddress(ip)
                    .details("Method: " + joinPoint.getSignature().getName() + " executed successfully.")
                    .build();

            auditLogRepository.save(auditLog);
            log.info("Audit Log saved: {} by user {} on target {}", methodName, userId, targetId);

        } catch (Exception e) {
            log.error("Failed to save audit log", e);
        }
    }
}
