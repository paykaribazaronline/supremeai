package com.supremeai.audit;

import com.supremeai.model.ActivityLog;
import com.supremeai.repository.ActivityLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.AfterThrowing;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
public class AuditLoggingAspect {

  private static final Logger log = LoggerFactory.getLogger(AuditLoggingAspect.class);

  @Autowired private ActivityLogRepository activityLogRepository;

  @Pointcut("@annotation(com.supremeai.audit.Audited)")
  public void auditedMethod() {}

  @Pointcut("execution(* com.supremeai.controller..*(..))")
  public void controllerMethod() {}

  @AfterReturning("auditedMethod()")
  public void logAdminAction(JoinPoint jp) {
    logAudit(jp, "success", null);
  }

  @AfterThrowing(pointcut = "auditedMethod()", throwing = "ex")
  public void logAdminActionFailure(JoinPoint jp, Exception ex) {
    logAudit(jp, "failure", ex.getMessage());
  }

  private void logAudit(JoinPoint jp, String outcome, String error) {
    try {
      MethodSignature signature = (MethodSignature) jp.getSignature();
      Audited audited = signature.getMethod().getAnnotation(Audited.class);

      String username = getUsername();
      String methodName = signature.getMethod().getName();
      String declaringType = signature.getDeclaringType().getSimpleName();
      String resource = audited.resource().isEmpty() ? declaringType : audited.resource();
      String action = audited.action().isEmpty() ? methodName : audited.action();

      String ip = getClientIp();
      String args = truncateArgs(jp.getArgs());

      ActivityLog activityLog =
          new ActivityLog(
              action,
              username,
              resource,
              error != null ? "warning" : "info",
              args + (error != null ? " | error: " + error : ""),
              outcome,
              ip);
      activityLog.setTimestamp(LocalDateTime.now());

      activityLogRepository.save(activityLog);
    } catch (Exception e) {
      log.error("Failed to write audit log: {}", e.getMessage());
    }
  }

  private String getUsername() {
    Authentication auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.isAuthenticated()) {
      return auth.getName();
    }
    return "anonymous";
  }

  private String getClientIp() {
    try {
      ServletRequestAttributes attrs =
          (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
      if (attrs != null) {
        HttpServletRequest request = attrs.getRequest();
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isEmpty()) {
          return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
      }
    } catch (Exception e) {
      log.debug("Could not determine client IP");
    }
    return "unknown";
  }

  private String truncateArgs(Object[] args) {
    if (args == null || args.length == 0) return "[]";
    StringBuilder sb = new StringBuilder("[");
    for (int i = 0; i < args.length; i++) {
      if (i > 0) sb.append(", ");
      String str = args[i] != null ? args[i].toString() : "null";
      if (str.length() > 200) str = str.substring(0, 200) + "...";
      sb.append(str);
    }
    sb.append("]");
    return sb.toString();
  }
}
