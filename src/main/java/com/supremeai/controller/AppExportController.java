package com.supremeai.controller;

import com.supremeai.model.GeneratedApp;
import com.supremeai.repository.GeneratedAppRepository;
import com.supremeai.service.CodeGenerationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@RestController
@RequestMapping("/api/export")
public class AppExportController {

    @Autowired
    private CodeGenerationService codeGenerationService;

    @GetMapping("/download/{appId}")
    public Mono<ResponseEntity<ByteArrayResource>> downloadApp(@PathVariable String appId) {
        return codeGenerationService.getGeneratedApp(appId)
                .flatMap(app -> {
                    Map<String, String> files = app.getSourceFiles();
                    if (files == null || files.isEmpty()) {
                        return Mono.error(new RuntimeException("No source files found for app: " + appId));
                    }
                    return Mono.fromCallable(() -> createZipFile(app.getAppId(), files));
                })
                .map(zipBytes -> {
                    ByteArrayResource resource = new ByteArrayResource(zipBytes);
                    return ResponseEntity.ok()
                            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"app-" + appId + ".zip\"")
                            .contentType(MediaType.APPLICATION_OCTET_STREAM)
                            .contentLength(zipBytes.length)
                            .body(resource);
                });
    }

    private byte[] createZipFile(String appId, Map<String, String> files) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                ZipEntry ze = new ZipEntry(entry.getKey());
                zos.putNextEntry(ze);
                zos.write(entry.getValue().getBytes());
                zos.closeEntry();
            }
        }
        return baos.toByteArray();
    }
}
