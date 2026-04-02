package org.example.controller;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class AdminDashboardContractTest {

    @Test
    void canonicalAdminDashboardContainsProviderLifecycleEndpoints() throws Exception {
        String html = Files.readString(Path.of("src", "main", "resources", "static", "admin.html"));

        assertTrue(html.contains("/api/providers/add"));
        assertTrue(html.contains("/api/providers/probe/"));
        assertTrue(html.contains("/api/providers/rotate/"));
        assertTrue(html.contains("/api/providers/remove"));
        assertTrue(html.contains("/api/providers/audit"));
    }

    @Test
    void legacyAdminEntryRedirectsToCanonicalDashboard() throws Exception {
        String html = Files.readString(Path.of("admin", "index.html"));

        assertTrue(html.contains("/admin.html"));
        assertTrue(html.contains("window.location.replace('/admin.html')"));
    }
}