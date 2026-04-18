package com.supremeai.service;

import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.Map;

@Service
public class QuotaTracker {

    public static class QuotaStatus {
        public int used;
        public int limit;
    }

    public Map<String, QuotaStatus> getAllStatus() {
        return Collections.emptyMap();
    }
}