package org.example.service;

import com.fasterxml.jackson.core.type.TypeReference;
import jakarta.annotation.PostConstruct;
import org.example.model.ProviderAuditEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ProviderAuditService {
    private static final String STORE_PATH = "provider-audit-log.json";

    @Autowired
    private LocalJsonStoreService localJsonStoreService;

    private final List<ProviderAuditEvent> auditEvents = new ArrayList<>();

    @PostConstruct
    void loadAuditEvents() {
        auditEvents.clear();
        auditEvents.addAll(localJsonStoreService.read(
            STORE_PATH,
            new TypeReference<List<ProviderAuditEvent>>() {},
            new ArrayList<>()
        ));
    }

    public synchronized void log(String action, String providerId, String admin, String status, Map<String, Object> details) {
        ProviderAuditEvent event = new ProviderAuditEvent();
        event.setAction(action);
        event.setProviderId(providerId);
        event.setAdmin(admin == null || admin.isBlank() ? "system" : admin);
        event.setStatus(status == null || status.isBlank() ? "INFO" : status);
        event.setDetails(details == null ? new LinkedHashMap<>() : new LinkedHashMap<>(details));
        auditEvents.add(0, event);

        while (auditEvents.size() > 500) {
            auditEvents.remove(auditEvents.size() - 1);
        }
        localJsonStoreService.write(STORE_PATH, auditEvents);
    }

    public synchronized List<ProviderAuditEvent> getRecentEvents(int limit) {
        int safeLimit = Math.max(1, limit);
        return new ArrayList<>(auditEvents.subList(0, Math.min(safeLimit, auditEvents.size())));
    }
}