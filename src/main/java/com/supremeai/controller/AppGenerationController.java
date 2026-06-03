package com.supremeai.controller;

import com.supremeai.service.CodeGenerationService;
import com.supremeai.service.AppOrchestrationService;
import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.model.GeneratedApp;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.FieldDefinition;
import com.supremeai.repository.GeneratedAppRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import com.supremeai.dto.AppGenerationRequest;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import com.supremeai.response.ApiResponse;
import java.util.UUID;

/**
 * Controller for app generation endpoints.
 * Handles requests to generate applications based on user requirements.
 */
@RestController
@RequestMapping({"/api/generate", "/api/teaching/create-app"})
public class AppGenerationController {
    public AppGenerationController(CodeGenerationService codeGenerationService, FullStackCodeGenerator fullStackCodeGenerator, MultiPlatformGenerator multiPlatformGenerator, GeneratedAppRepository generatedAppRepository, AppOrchestrationService appOrchestrationService, WebSocketController webSocketController) {
        this.codeGenerationService = codeGenerationService;
        this.fullStackCodeGenerator = fullStackCodeGenerator;
        this.multiPlatformGenerator = multiPlatformGenerator;
        this.generatedAppRepository = generatedAppRepository;
        this.appOrchestrationService = appOrchestrationService;
        this.webSocketController = webSocketController;
    }

    
    private static final Logger logger = LoggerFactory.getLogger(AppGenerationController.class);
    
    
    




    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> generateApp(
            @Valid @RequestBody AppGenerationRequest request,
            Authentication auth) {
        
        String requestId = UUID.randomUUID().toString();
        String name = request.getName();
        String userId = auth != null ? auth.getName() : "anonymous";
        String description = request.getDescription();
        String platform = request.getPlatform();
        boolean useAI = request.isUseAI();

        logger.info("App generation request received [{}]: {} (useAI: {}) by user {}", 
            requestId, name, useAI, userId);

        if (useAI && "project".equals(request.getType())) {
            // Trigger the Full Orchestration Pipeline (AI + Code + GitHub)
            appOrchestrationService.runFullPipeline(description != null ? description : name, null)
                .flatMap(result -> {
                    // Save to repository for persistence/preview
                    String appId = UUID.randomUUID().toString();
                    GeneratedApp generatedApp = new GeneratedApp(appId, userId, platform, "React");
                    generatedApp.setHtmlContent(buildPreviewHtml(name, platform, description, result));
                    generatedApp.setStatus("GENERATED");
                    generatedApp.setRequestId(requestId);
                    
                    if (result.get("generatedApp") instanceof Map) {
                        Map<String, Object> genApp = (Map<String, Object>) result.get("generatedApp");
                        if (genApp.containsKey("files")) {
                            generatedApp.setSourceFiles((Map<String, String>) genApp.get("files"));
                        }
                    }
                    
                    return generatedAppRepository.save(generatedApp).thenReturn(result);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .subscribe(
                    res -> logger.info("Orchestration pipeline completed for {}", requestId),
                    err -> logger.error("Orchestration pipeline failed for " + requestId, err)
                );

            Map<String, Object> acceptedResponse = new HashMap<>();
            acceptedResponse.put("requestId", requestId);
            acceptedResponse.put("status", "ACCEPTED");
            acceptedResponse.put("message", "Full AI orchestration pipeline started");
            
            return Mono.just(ResponseEntity.accepted().body(ApiResponse.ok(acceptedResponse)));
        }
        
        // Fallback to legacy/direct generation logic
        Mono.fromCallable(() -> {
            try {
                String database = request.getDatabase();
                String type = request.getType();

                webSocketController.broadcastAppGenProgress(requestId, name, "INITIALIZING", 5, "Initializing generation engine...");

                Map<String, String> decisions = new HashMap<>();
                decisions.put("architecture", "monolith");
                decisions.put("database", database);
                decisions.put("apiStyle", "REST");
                decisions.put("authType", "JWT");
                decisions.put("frontend", "React");
                decisions.put("deployment", "GCP");
                
                webSocketController.broadcastAppGenProgress(requestId, name, "ANALYZING", 15, "Analyzing requirements and entities...");
                
                Map<String, Object> result;
                
                // Use enhanced AI-powered generation if requested (legacy path)
                if (useAI) {
                    List<EntityDefinition> entities = request.getEntities();
                    if (entities == null) entities = new ArrayList<>();
                    result = codeGenerationService.generateAppWithAI(name, description, entities, database, "JWT");
                } else {
                    webSocketController.broadcastAppGenProgress(requestId, name, "GENERATING_CORE", 30, "Generating core application structure...");
                    
                    switch (platform.toLowerCase()) {
                        case "fullstack":
                            result = codeGenerationService.generateFromContext(decisions);
                            break;
                        case "web":
                        case "android":
                        case "ios":
                        case "desktop":
                            Map<String, String> platformResult = multiPlatformGenerator.generateForPlatform(
                                description != null && !description.isEmpty() ? description : name, platform);
                            result = new HashMap<>(platformResult);
                            result.put("decisions", decisions);
                            break;
                        default:
                            result = codeGenerationService.generateFromContext(decisions);
                            break;
                    }
                }
                
                webSocketController.broadcastAppGenProgress(requestId, name, "FINALIZING", 80, "Finalizing files and preparing preview...");

                result.put("name", name);
                result.put("description", description);
                result.put("platform", platform);
                result.put("type", type);
                result.put("status", "GENERATED");
                result.put("requestId", requestId);

                String appId = UUID.randomUUID().toString();
                GeneratedApp generatedApp = new GeneratedApp(appId, userId, platform, "React");
                generatedApp.setHtmlContent(buildPreviewHtml(name, platform, description, result));
                generatedApp.setStatus("GENERATED");
                generatedApp.setRequestId(requestId);
                
                if (result.containsKey("files")) {
                    generatedApp.setSourceFiles((Map<String, String>) result.get("files"));
                }
                
                generatedAppRepository.save(generatedApp).subscribe();
                result.put("appId", appId);
                
                webSocketController.broadcastAppGenProgress(requestId, name, "COMPLETED", 100, "Generation completed successfully!");
                return result;
            } catch (Exception e) {
                logger.error("Async app generation failed for request " + requestId, e);
                webSocketController.broadcastAppGenProgress(requestId, name, "FAILED", 0, "Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .subscribe(); 

        Map<String, Object> acceptedResponse = new HashMap<>();
        acceptedResponse.put("requestId", requestId);
        acceptedResponse.put("status", "ACCEPTED");
        acceptedResponse.put("message", "App generation started in background");
        
        return Mono.just(ResponseEntity.accepted().body(ApiResponse.ok(acceptedResponse)));
    }


    /**
     * Builds a fully self-contained, executable HTML single-page preview of the
     * generated application.  The HTML is safe to load directly into an iframe
     * via GET /api/simulator/preview/{appId}.
     *
     * Strategy per platform:
     *   web        → embedded vanilla JS from the "app" field (no TSX needed)
     *   ios-mock   → interactive device-frame mock-up card
     *   desktop    → embedded Electron-style mock shell
     *   android    → embedded WebView-styled mock shell
     *   fullstack  → React-style dashboard mock-up + API panel
     *   unknown    → beautiful generic "App Ready" card
     */
    private String buildPreviewHtml(String name, String platform, String description,
                                    Map<String, Object> result) {

        String safeName  = escapeHtml(name != null ? name : "Generated App");
        String safeDesc  = escapeHtml(description != null ? description : "");
        String status    = "GENERATED";

        // Extract any source-code text from the result map
        String sourceCode = "";
        if (result.get("app") instanceof String) {
            sourceCode = (String) result.get("app");
        } else if (result.get("index.html") instanceof String) {
            sourceCode = (String) result.get("index.html");
        }

        String filesSection = buildFileListSection(result);

        // ── Decide preview style ──────────────────────────────────────────
        String platformLower = platform.toLowerCase(Locale.ROOT);
        String scriptContent;
        String bodyClass = "preview-generic";
        String headerEmoji = "🚀";

        if (platformLower.contains("web") || platformLower.contains("fullstack")
                || platformLower.contains("react") || platformLower.contains("angular")
                || platformLower.contains("vue")) {

            // ── Web: render actual interactive HTML from source code ──────
            headerEmoji = "🌐";
            bodyClass = "preview-web";
            scriptContent = buildWebScript(sourceCode, safeName);
            filesSection = buildFileListSection(result) + buildCodeViewer(sourceCode, "src/App.jsx");

        } else if (platformLower.contains("ios") || platformLower.contains("swift") || platformLower.contains("macos")) {

            // ── iOS: interactive phone mock card ──────────────────────────
            headerEmoji = "📱";
            bodyClass = "preview-ios";
            scriptContent = buildDeviceScript("iOS App Preview", "SwiftUI", "#007AFF");
            filesSection = buildFileListSection(result) + buildCodeViewer(sourceCode, "ContentView.swift");

        } else if (platformLower.contains("android") || platformLower.contains("kotlin") || platformLower.contains("jetpack")) {

            // ── Android: phone mock card ──────────────────────────────────
            headerEmoji = "🤖";
            bodyClass = "preview-android";
            scriptContent = buildDeviceScript("Android App Preview", "Kotlin + Jetpack", "#3DDC84");
            filesSection = buildFileListSection(result) + buildCodeViewer(sourceCode, "MainActivity.kt");

        } else if (platformLower.contains("desktop") || platformLower.contains("tauri") || platformLower.contains("electron")
                   || platformLower.contains("javafx") || platformLower.contains("swing")) {

            // ── Desktop: window frame mock-up ────────────────────────────
            headerEmoji = "🖥️";
            bodyClass = "preview-desktop";
            scriptContent = buildDeviceScript("Desktop App Preview", platform, "#6B7280");
            filesSection = buildFileListSection(result) + buildCodeViewer(sourceCode, "main.dart / App.java");

        } else {

            // ── Generic fallback ─────────────────────────────────────────
            headerEmoji = "✨";
            scriptContent = buildGenericScript(safeName, safeDesc);
        }

        return assemblePreviewHtml(safeName, safeDesc, platform, status,
                                   bodyClass, headerEmoji, scriptContent,
                                   filesSection);
    }

    // ── XSS-safe helpers ───────────────────────────────────────────────────────
    private static String escapeHtml(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.replace("&", "&amp;")
                  .replace("<", "&lt;")
                  .replace(">", "&gt;")
                  .replace("\"", "&quot;")
                  .replace("'", "&#39;");
    }

    private static String escapeJs(String raw) {
        if (raw == null) return "null";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("'", "\\'")
                  .replace("\n", "\\n")
                  .replace("\r", "")
                  .replace("\t", "\\t");
    }

    // ── HTML Assembly Helpers ────────────────────────────────────────────────

    private String assemblePreviewHtml(String name, String desc, String platform,
                                       String status, String bodyClass,
                                       String emoji, String bodyScript,
                                       String filesSection) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/>
              <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>%s — Preview</title>
              <style>
                *, *::before, *::after { box-sizing: border-box; margin: 0; padding: 0; }
                body { font-family: 'Inter', -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                       background: linear-gradient(135deg, #0f0c29 0%%, #302b63 50%%, #24243e 100%%);
                       color: #e2e8f0; min-height: 100vh; display: flex; flex-direction: column; align-items: center; padding: 28px; }
                h1 { font-size: 1.5rem; font-weight: 700; }
                h2 { font-size: 1.15rem; font-weight: 600; color: #a5b4fc; margin-bottom: 12px; }
                p  { color: #8392ab; line-height: 1.6; }
                .badge { display: inline-flex; align-items: center; gap: 5px;
                         background: rgba(99,102,241,.18); border: 1px solid rgba(99,102,241,.4);
                         color: #818cf8; padding: 4px 14px; border-radius: 999px; font-size: .8rem; font-weight: 500; }
                .card { background: rgba(255,255,255,.04); border: 1px solid rgba(255,255,255,.08);
                        backdrop-filter: blur(24px); border-radius: 20px; padding: 28px;
                        width: 100%%; max-width: 760px; }
                .header { display: flex; justify-content: space-between; align-items: flex-start;
                          flex-wrap: wrap; gap: 12px; margin-bottom: 20px; }
                .hero-emoji { font-size: 2.8rem; }
                .status-tag { font-size: .7rem; padding: 3px 10px; border-radius: 999px; font-weight: 600;
                              text-transform: uppercase; letter-spacing: .05em; background: rgba(52,211,153,.15);
                              border: 1px solid rgba(52,211,153,.35); color: #34d399; }
                .label     { font-size: .75rem; text-transform: uppercase; letter-spacing: .1em;
                             color: #64748b; margin: 20px 0 8px; }
                /* preview-window — shades the active canvas area */
                .preview-window { background: #1e1b4b; border-radius: 12px; overflow: hidden;
                                  border: 1px solid rgba(255,255,255,.07); margin-bottom: 20px; }
                .preview-bar   { background: rgba(255,255,255,.05); padding: 7px 12px; display: flex;
                                  align-items: center; gap: 6px; border-bottom: 1px solid rgba(255,255,255,.07); }
                .dot { width: 10px; height: 10px; border-radius: 50%%; }
                .dot-r { background: #f87171; } .dot-y { background: #facc15; } .dot-g { background: #4ade80; }
                .preview-url { flex: 1; text-align: center; font-size: .65rem; color: #4b5563; font-family: monospace; }
                /* in-preview content */
                .preview-body  { padding: 22px 24px; min-height: 200px; position: relative; }
                /* device frame overlays */
                .device-frame { border: 2px solid rgba(255,255,255,.12); border-radius: 28px;
                                padding: 14px; margin: 0 auto; max-width: 300px; }
                .device-screen { background: #0a0a1a; border-radius: 18px; overflow: hidden; min-height: 160px; }
                .device-notch { width: 80px; height: 22px; background: #0a0a1a; border-radius: 0 0 14px 14px; margin: 0 auto -1px; }
                /* generic iframe canvas */
                .app-canvas { width: 100%%; min-height: 220px; display: flex; flex-direction: column;
                               align-items: center; justify-content: center; gap: 14px; }
                /* code block */
                .code-wrap { background: #0b0b1e; border-radius: 10px; overflow: hidden;
                              border: 1px solid rgba(255,255,255,.06); }
                .code-header { background: rgba(255,255,255,.04); padding: 6px 14px;
                               font-size: .7rem; color: #6b7280; font-family: monospace;
                               border-bottom: 1px solid rgba(255,255,255,.06); }
                .code-block  { padding: 14px; font-family: 'JetBrains Mono', 'Fira Code', monospace;
                               font-size: .72rem; line-height: 1.7; color: #a5b4fc;
                               max-height: 200px; overflow-y: auto; white-space: pre-wrap; }
                .file-list   { list-style: none; }
                .file-list li { display: flex; justify-content: space-between; align-items: center;
                                padding: 7px 12px; border-bottom: 1px solid rgba(255,255,255,.05);
                                font-size: .82rem; }
                .file-list li:last-child { border-bottom: none; }
                .file-ext { font-size: .7rem; color: #4b5563; background: rgba(255,255,255,.05);
                            padding: 2px 8px; border-radius: 6px; }
                /* tip banner */
                .tip { margin-top: 20px; padding: 14px 16px; background: rgba(251,191,36,.08);
                       border-left: 3px solid #f59e0b; border-radius: 8px; font-size: .82rem;
                       color: #fde68a; }
                /* animation */
                @keyframes pulse-glow {
                  0%%   { box-shadow: 0 0 8px rgba(99,102,241,.2); }
                  50%%  { box-shadow: 0 0 22px rgba(99,102,241,.45); }
                  100%% { box-shadow: 0 0 8px rgba(99,102,241,.2); }
                }
                .animate-glow { animation: pulse-glow 2.5s ease-in-out infinite; }
                .btn { display: inline-flex; align-items: center; gap: 6px; padding: 8px 18px;
                       border-radius: 9px; font-size: .82rem; font-weight: 600; text-decoration: none;
                       cursor: pointer; transition: transform .15s, box-shadow .15s; }
                .btn-primary { background: #4f46e5; color: #fff; border: none; }
                .btn-primary:hover { transform: translateY(-1px); box-shadow: 0 4px 20px rgba(79,70,229,.4); }
                .btn-outline { background: transparent; color: #818cf8; border: 1px solid rgba(99,102,241,.4); }
                .btn-row { display: flex; gap: 10px; flex-wrap: wrap; }
              </style>
            </head>
            <body class="%s">
              <div class="card">
                <div class="header">
                  <div>
                    <h1>%s %s</h1>
                    <p style="margin-top:5px">%s</p>
                  </div>
                  <div style="display:flex;flex-direction:column;align-items:flex-end;gap:8px">
                    <span class="badge">%s</span>
                    <span class="status-tag">%s</span>
                  </div>
                </div>
                %s
                %s
                %s
              </div>
            </body>
            </html>
            """.formatted(
                name,  name,  name,                                            // <title>
                bodyClass, name, emoji + " " + name, desc,                     // <body>, <h1>, badge
                platform, status,                                             // badge, status
                buildPreviewToolbar(name, platform),                          // preview toolbar
                buildPreviewWindow(platform, bodyScript),                     // preview canvas + script
                filesSection                                                  // generated files
            );
    }

    private String buildPreviewToolbar(String name, String platform) {
        return """
            <div class="btn-row" style="margin-bottom:20px; display:flex; gap:10px;">
              <button class="btn btn-primary" onclick="reloadPreview()">
                <span>🔄</span> Reload Preview
              </button>
              <button class="btn btn-outline" onclick="copySource()">
                <span>📋</span> Copy Source
              </button>
              <a class="btn btn-outline" href="/api/generate/preview" target="_blank"
                 style="text-decoration:none; cursor:pointer;">
                <span>📐</span> Updated Blueprint
              </a>
            </div>
            <div class="preview-window">
              <div class="preview-bar">
                <span class="dot dot-r"></span>
                <span class="dot dot-y"></span>
                <span class="dot dot-g"></span>
                <span class="preview-url">localhost — %s Preview</span>
              </div>
            """.formatted(escapeHtml(platform));
    }

    private String buildPreviewWindow(String platform, String scriptContent) {
        return """
              <div class="preview-body" id="preview-canvas">
                 %s
              </div>
            </div>
            <script>
              %s
              function reloadPreview() {
                document.getElementById('preview-canvas').innerHTML = `%s`;
                eval(`%s`);
              }
              function copySource() {
                const ta = document.createElement('textarea');
                ta.value = document.getElementById('preview-body')?.innerText || '';
                document.body.appendChild(ta);
                ta.select();
                document.execCommand('copy');
                document.body.removeChild(ta);
                alert('Source code copied to clipboard.');
              }
            </script>
            """.formatted(
                scriptContent,          // initial body HTML
                escapeJs(scriptContent),// script to run
                scriptContent.replace("`", "\\`"), // backtick-escape for reload
                escapeJs(scriptContent)
            );
    }

    // ── Platform-Specific Script Blocks ─────────────────────────────────────

    private String buildWebScript(String sourceCode, String appName) {
        String snippet = sourceCode.length() > 600 ? sourceCode.substring(0, 600) + "\n// ... more" : sourceCode;
        return """
            <div class="app-canvas">
              <span style="font-size:3.5rem">🖥️</span>
              <h2 style="font-size:1.3rem">%s</h2>
              <p style="font-size:.85rem;color:#6b7280;text-align:center;max-width:380px">
                Web app generated successfully. The React/Vite source code is in the file list below — ready for <code>npm run build</code> and deployment.
              </p>
              <div class="btn btn btn-primary" style="cursor:default">
                ✅ React + Vite Project Ready
              </div>
            </div>
            """.formatted(escapeHtml(appName));
    }

    private String buildDeviceScript(String title, String lang, String accentColor) {
        return """
            <div style="text-align:center; padding:18px 0 8px">
              <div class="device-frame">
                <div class="device-notch"></div>
                <div class="device-screen">
                  <div style="display:flex;flex-direction:column;align-items:center;
                       justify-content:center;min-height:160px;padding:20px;gap:10px;
                       background:linear-gradient(150deg,#0b0b2e 0%%,#11113a 100%%)">
                    <span style="font-size:2.4rem">%s</span>
                    <span style="font-size:.75rem;color:%s;font-weight:600">%s</span>
                    <span style="font-size:.6rem;color:#4b5563">Generated by SupremeAI</span>
                  </div>
                </div>
              </div>
              <p style="font-size:.75rem;color:#4b5563;margin-top:10px">Native %s source in file list below → open in Xcode / Android Studio</p>
            </div>
            """.formatted(emojiFor(title), accentColor, title, lang);
    }

    private String buildGenericScript(String name, String desc) {
        return """
            <div class="app-canvas">
              <span style="font-size:3.5rem">✨</span>
              <h2 style="font-size:1.3rem">%s — Ready</h2>
              <p style="font-size:.85rem;color:#6b7280;text-align:center;max-width:380px">%s</p>
              <div class="btn btn-primary btn-animate-glow" style="padding:10px 24px">
                ✅ App Generated Successfully
              </div>
            </div>
            """.formatted(escapeHtml(name), escapeHtml(desc.isEmpty() ? "Your application is ready for review and deployment." : desc));
    }

    private String emojiFor(String title) {
        if (title.toLowerCase().contains("ios"))    return "🍎";
        if (title.toLowerCase().contains("android"))return "🤖";
        if (title.toLowerCase().contains("desktop"))return "🖥️";
        return "✨";
    }

    private String buildFileListSection(Map<String, Object> result) {
        if (!result.containsKey("files")) return "";
        @SuppressWarnings("unchecked")
        Map<String, String> files = (Map<String, String>) result.get("files");
        if (files == null || files.isEmpty()) return "";

        StringBuilder sb = new StringBuilder();
        sb.append("<p class=\"label\">📁 Generated Files (").append(files.size()).append(")</p>");
        sb.append("<ul class=\"file-list\">");
        files.forEach((fname, fcontent) -> {
            String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf('.') + 1).toUpperCase() : "FILE";
            String langColor = switch (ext) {
                case "JAVA", "KT" -> "#f97316";
                case "JSX", "TSX", "JS" -> "#61dafb";
                case "CSS" -> "#a78bfa";
                case "SQL" -> "#fb7185";
                case "YAML", "YML" -> "#fbbf24";
                default -> "#94a3b8";
            };
            sb.append(String.format(
                "<li><span>%s</span><span class=\"file-ext\" style=\"color:%s\">%s</span></li>",
                escapeHtml(fname), langColor, ext
            ));
        });
        sb.append("</ul>");
        return sb.toString();
    }

    private String buildCodeViewer(String sourceCode, String filenameHint) {
        if (sourceCode == null || sourceCode.isBlank()) return "";
        // Show first 1200 chars of source for a quick peek
        String preview = sourceCode.length() > 1200 ? sourceCode.substring(0, 1200) + "\n\n// ── remaining content in source file ──" : sourceCode;
        return """
            <p class="label">👁 %s — Live Preview</p>
            <div class="code-wrap">
              <div class="code-header">%s</div>
              <div class="code-block">%s</div>
            </div>
            """.formatted(
                escapeHtml(filenameHint),
                escapeHtml(filenameHint),
                escapeHtml(preview)
            );
    }

    private List<EntityDefinition> parseEntitiesFromRequest(Map<String, Object> request) {
        List<EntityDefinition> entities = new ArrayList<>();
        
        // Check if custom entities are provided
        if (request.containsKey("entities")) {
            List<Map<String, Object>> entityMaps = (List<Map<String, Object>>) request.get("entities");
            for (Map<String, Object> entityMap : entityMaps) {
                EntityDefinition entity = new EntityDefinition();
                entity.setName((String) entityMap.get("name"));
                entity.setDescription((String) entityMap.get("description"));
                
                List<FieldDefinition> fields = new ArrayList<>();
                if (entityMap.containsKey("fields")) {
                    List<Map<String, Object>> fieldMaps = (List<Map<String, Object>>) entityMap.get("fields");
                    for (Map<String, Object> fieldMap : fieldMaps) {
                        FieldDefinition field = new FieldDefinition();
                        field.setName((String) fieldMap.get("name"));
                        field.setType((String) fieldMap.get("type"));
                        field.setRequired((Boolean) fieldMap.getOrDefault("required", false));
                        field.setUnique((Boolean) fieldMap.getOrDefault("unique", false));
                        if (fieldMap.containsKey("maxLength")) {
                            field.setMaxLength(((Number) fieldMap.get("maxLength")).intValue());
                        }
                        fields.add(field);
                    }
                }
                entity.setFields(fields);
                entities.add(entity);
            }
        } else {
            // Default to Product entity
            entities.add(createDefaultProductEntity());
        }
        
        return entities;
    }
    
    /**
     * Create default Product entity
     */
    private EntityDefinition createDefaultProductEntity() {
        EntityDefinition entity = new EntityDefinition();
        entity.setName("Product");
        entity.setDescription("Product entity with basic fields");
        
        List<FieldDefinition> fields = new ArrayList<>();
        
        FieldDefinition nameField = new FieldDefinition();
        nameField.setName("name");
        nameField.setType("string");
        nameField.setRequired(true);
        nameField.setMaxLength(255);
        fields.add(nameField);
        
        FieldDefinition descField = new FieldDefinition();
        descField.setName("description");
        descField.setType("text");
        descField.setRequired(false);
        fields.add(descField);
        
        FieldDefinition priceField = new FieldDefinition();
        priceField.setName("price");
        priceField.setType("double");
        priceField.setRequired(true);
        fields.add(priceField);
        
        FieldDefinition stockField = new FieldDefinition();
        stockField.setName("stock");
        stockField.setType("integer");
        stockField.setRequired(false);
        fields.add(stockField);
        
        FieldDefinition categoryField = new FieldDefinition();
        categoryField.setName("category");
        categoryField.setType("string");
        categoryField.setRequired(false);
        categoryField.setMaxLength(100);
        fields.add(categoryField);
        
        entity.setFields(fields);
        return entity;
    }
    
    /**
     * Health check endpoint.
     */
    @GetMapping("/health")
    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AppGenerationService");
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(health)));
    }
    
    /**
     * Infrastructure advice endpoint - provides AI-powered deployment guidance.
     */
    @PostMapping("/infrastructure-advice")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN', 'GUEST')")
    public Mono<ResponseEntity<ApiResponse<String>>> getInfrastructureAdvice(
            @RequestBody Map<String, String> request) {
        
        String appName = request.getOrDefault("appName", "My App");
        String description = request.getOrDefault("description", "");
        String techStack = request.getOrDefault("techStack", "Full Stack Spring Boot/React");
        String cloudPreference = request.getOrDefault("cloudPreference", "GCP");

        logger.info("Generating infrastructure advice for app: {}", appName);

        return codeGenerationService.generateInfrastructureAdvice(appName, description, techStack, cloudPreference)
                .map(advice -> ResponseEntity.ok(ApiResponse.ok(advice)))
                .onErrorResume(e -> {
                    logger.error("Failed to generate infrastructure advice", e);
                    return Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error("Failed to generate infrastructure advice: " + e.getMessage())));
                });
    }

    /**
     * Preview generation - returns sample output without creating files.
     */
    @PostMapping("/preview")
    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> previewGeneration(@RequestBody Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String platform = (String) request.getOrDefault("platform", "fullstack");
            
            Map<String, String> decisions = new HashMap<>();
            decisions.put("architecture", "monolith");
            decisions.put("database", "PostgreSQL");
            decisions.put("apiStyle", "REST");
            decisions.put("authType", "JWT");
            decisions.put("frontend", "React");
            decisions.put("deployment", "GCP");
            
            Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
            
            // Limit preview to first few files
            @SuppressWarnings("unchecked")
            Map<String, String> files = (Map<String, String>) result.get("files");
            if (files != null && files.size() > 3) {
                Map<String, String> previewFiles = new HashMap<>();
                int count = 0;
                for (Map.Entry<String, String> entry : files.entrySet()) {
                    if (count++ >= 3) break;
                    previewFiles.put(entry.getKey(), entry.getValue());
                }
                result.put("files", previewFiles);
                result.put("preview", true);
                result.put("totalFiles", files.size());
            }
            
            return ResponseEntity.ok(ApiResponse.ok(result));
            
        }).subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(e -> {
            logger.error("Preview generation failed", e);
            return Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error("Preview generation failed: " + e.getMessage())));
        });
    }
}
