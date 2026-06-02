package com.supremeai.service;

import com.supremeai.generation.FullStackCodeGenerator;
import com.supremeai.generation.MultiPlatformGenerator;
import com.supremeai.model.EntityDefinition;
import com.supremeai.model.GeneratedApp;
import com.supremeai.repository.GeneratedAppRepository;
import com.supremeai.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.*;

@Service
public class AppGenerationService {

    private static final Logger logger = LoggerFactory.getLogger(AppGenerationService.class);

    private final CodeGenerationService codeGenerationService;
    private final FullStackCodeGenerator fullStackCodeGenerator;
    private final MultiPlatformGenerator multiPlatformGenerator;
    private final GeneratedAppRepository generatedAppRepository;
    private final AppOrchestrationService appOrchestrationService;
    private final ProgressBroadcaster progressBroadcaster;

    public AppGenerationService(CodeGenerationService codeGenerationService,
                                FullStackCodeGenerator fullStackCodeGenerator,
                                MultiPlatformGenerator multiPlatformGenerator,
                                GeneratedAppRepository generatedAppRepository,
                                AppOrchestrationService appOrchestrationService,
                                ProgressBroadcaster progressBroadcaster) {
        this.codeGenerationService = codeGenerationService;
        this.fullStackCodeGenerator = fullStackCodeGenerator;
        this.multiPlatformGenerator = multiPlatformGenerator;
        this.generatedAppRepository = generatedAppRepository;
        this.appOrchestrationService = appOrchestrationService;
        this.progressBroadcaster = progressBroadcaster;
    }

    public Mono<Map<String, Object>> generateApp(String requestId,
                                                  String name,
                                                  String userId,
                                                  String description,
                                                  String platform,
                                                  boolean useAI,
                                                  String type,
                                                  String database,
                                                  List<EntityDefinition> entities) {
        if (useAI && "project".equals(type)) {
            return runFullPipelineAndPersist(requestId, name, userId, platform, description);
        }
        return runLegacyGeneration(requestId, name, userId, description, platform, useAI, database, entities);
    }

    private Mono<Map<String, Object>> runFullPipelineAndPersist(String requestId,
                                                                String name,
                                                                String userId,
                                                                String platform,
                                                                String description) {
        return appOrchestrationService.runFullPipeline(description != null ? description : name, null)
                .flatMap(result -> {
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
                .doOnSuccess(res -> logger.info("Orchestration pipeline completed for {}", requestId))
                .doOnError(err -> logger.error("Orchestration pipeline failed for " + requestId, err))
                .then(Mono.just(Map.of(
                        "requestId", requestId,
                        "status", "ACCEPTED",
                        "message", "Full AI orchestration pipeline started"
                )));
    }

    private Mono<Map<String, Object>> runLegacyGeneration(String requestId,
                                                          String name,
                                                          String userId,
                                                          String description,
                                                          String platform,
                                                          boolean useAI,
                                                          String database,
                                                          List<EntityDefinition> entities) {
        return Mono.fromCallable(() -> {
            try {
                progressBroadcaster.broadcastAppGenProgress(requestId, name, "INITIALIZING", 5, "Initializing generation engine...");
                Map<String, String> decisions = buildDecisions(database);
                progressBroadcaster.broadcastAppGenProgress(requestId, name, "ANALYZING", 15, "Analyzing requirements and entities...");

                Map<String, Object> result;
                if (useAI) {
                    List<EntityDefinition> entityList = entities != null ? entities : Collections.emptyList();
                    result = codeGenerationService.generateAppWithAI(name, description, entityList, database, "JWT");
                } else {
                    progressBroadcaster.broadcastAppGenProgress(requestId, name, "GENERATING_CORE", 30, "Generating core application structure...");
                    switch (platform.toLowerCase()) {
                        case "fullstack":
                            result = codeGenerationService.generateFromContext(decisions);
                            break;
                        case "web", "android", "ios", "desktop":
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

                progressBroadcaster.broadcastAppGenProgress(requestId, name, "FINALIZING", 80, "Finalizing files and preparing preview...");
                Map<String, Object> enriched = new HashMap<>(result);
                enriched.put("name", name);
                enriched.put("description", description);
                enriched.put("platform", platform);
                enriched.put("type", type);
                enriched.put("status", "GENERATED");
                enriched.put("requestId", requestId);

                String appId = UUID.randomUUID().toString();
                GeneratedApp generatedApp = new GeneratedApp(appId, userId, platform, "React");
                generatedApp.setHtmlContent(buildPreviewHtml(name, platform, description, enriched));
                generatedApp.setStatus("GENERATED");
                generatedApp.setRequestId(requestId);
                if (enriched.containsKey("files")) {
                    generatedApp.setSourceFiles((Map<String, String>) enriched.get("files"));
                }
                generatedAppRepository.save(generatedApp).subscribe();
                enriched.put("appId", appId);

                progressBroadcaster.broadcastAppGenProgress(requestId, name, "COMPLETED", 100, "Generation completed successfully!");
                return enriched;
            } catch (Exception e) {
                logger.error("Async app generation failed for request " + requestId, e);
                progressBroadcaster.broadcastAppGenProgress(requestId, name, "FAILED", 0, "Error: " + e.getMessage());
                throw new RuntimeException(e);
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Map<String, String> buildDecisions(String database) {
        Map<String, String> decisions = new HashMap<>();
        decisions.put("architecture", "monolith");
        decisions.put("database", database != null ? database : "PostgreSQL");
        decisions.put("apiStyle", "REST");
        decisions.put("authType", "JWT");
        decisions.put("frontend", "React");
        decisions.put("deployment", "GCP");
        return decisions;
    }

    public Mono<ResponseEntity<ApiResponse<Map<String, Object>>>> previewGeneration(Map<String, Object> request) {
        return Mono.fromCallable(() -> {
            String platform = (String) request.getOrDefault("platform", "fullstack");
            Map<String, String> decisions = buildDecisions(null);
            Map<String, Object> result = codeGenerationService.generateFromContext(decisions);
            if (result.containsKey("files")) {
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
            }
            return ResponseEntity.ok(ApiResponse.ok(result));
        }).subscribeOn(Schedulers.boundedElastic())
          .onErrorResume(e -> {
              logger.error("Preview generation failed", e);
              return Mono.just(ResponseEntity.internalServerError().body(ApiResponse.error("Preview generation failed: " + e.getMessage())));
          });
    }

    public Mono<ResponseEntity<ApiResponse<Map<String, String>>>> health() {
        Map<String, String> health = new HashMap<>();
        health.put("status", "UP");
        health.put("service", "AppGenerationService");
        return Mono.just(ResponseEntity.ok(ApiResponse.ok(health)));
    }

    public Mono<ResponseEntity<ApiResponse<String>>> getInfrastructureAdvice(
            String appName, String description, String techStack, String cloudPreference) {
        logger.info("Generating infrastructure advice for app: {}", appName);
        return codeGenerationService.generateInfrastructureAdvice(appName, description, techStack, cloudPreference)
                .map(advice -> ResponseEntity.ok(ApiResponse.ok(advice)))
                .onErrorResume(e -> {
                    logger.error("Failed to generate infrastructure advice", e);
                    return Mono.just(ResponseEntity.internalServerError()
                            .body(ApiResponse.error("Failed to generate infrastructure advice: " + e.getMessage())));
                });
    }

    public EntityDefinition createDefaultProductEntity() {
        EntityDefinition entity = new EntityDefinition();
        entity.setName("Product");
        entity.setDescription("Product entity with basic fields");
        List<FieldDefinition> fields = new ArrayList<>();
        FieldDefinition nameField = createField("name", "string", true, 255);
        fields.add(nameField);
        FieldDefinition descField = createField("description", "text", false, null);
        fields.add(descField);
        FieldDefinition priceField = createField("price", "double", true, null);
        fields.add(priceField);
        FieldDefinition stockField = createField("stock", "integer", false, null);
        fields.add(stockField);
        FieldDefinition categoryField = createField("category", "string", false, 100);
        fields.add(categoryField);
        entity.setFields(fields);
        return entity;
    }

    private FieldDefinition createField(String name, String type, boolean required, Integer maxLength) {
        FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setType(type);
        field.setRequired(required);
        if (maxLength != null) field.setMaxLength(maxLength);
        return field;
    }

    public Mono<String> getAppTemplate(String templateId) {
        return Mono.just("Template " + templateId + " details.");
    }

    // ==== HTML Preview Helpers (unchanged) ====
    private String buildPreviewHtml(String name, String platform, String description, Map<String, Object> result) {
        String safeName = escapeHtml(name != null ? name : "Generated App");
        String safeDesc = escapeHtml(description != null ? description : "");
        String sourceCode = extractSourceCode(result);
        String filesSection = buildFileListSection(result);
        String platformLower = platform.toLowerCase(Locale.ROOT);
        String scriptContent;
        String bodyClass = "preview-generic";
        String headerEmoji = "🚀";
        if (platformLower.contains("web") || platformLower.contains("fullstack") ||
                platformLower.contains("react") || platformLower.contains("angular") || platformLower.contains("vue")) {
            headerEmoji = "🌐"; bodyClass = "preview-web";
            scriptContent = buildWebScript(sourceCode, safeName);
            filesSection = filesSection + buildCodeViewer(sourceCode, "src/App.jsx");
        } else if (platformLower.contains("ios") || platformLower.contains("swift") || platformLower.contains("macos")) {
            headerEmoji = "📱"; bodyClass = "preview-ios";
            scriptContent = buildDeviceScript("iOS App Preview", "SwiftUI", "#007AFF");
            filesSection = filesSection + buildCodeViewer(sourceCode, "ContentView.swift");
        } else if (platformLower.contains("android") || platformLower.contains("kotlin") || platformLower.contains("jetpack")) {
            headerEmoji = "🤖"; bodyClass = "preview-android";
            scriptContent = buildDeviceScript("Android App Preview", "Kotlin + Jetpack", "#3DDC84");
            filesSection = filesSection + buildCodeViewer(sourceCode, "MainActivity.kt");
        } else if (platformLower.contains("desktop") || platformLower.contains("tauri") || platformLower.contains("electron")
                || platformLower.contains("javafx") || platformLower.contains("swing")) {
            headerEmoji = "🖥️"; bodyClass = "preview-desktop";
            scriptContent = buildDeviceScript("Desktop App Preview", platform, "#6B7280");
            filesSection = filesSection + buildCodeViewer(sourceCode, "main.dart / App.java");
        } else {
            headerEmoji = "✨"; scriptContent = buildGenericScript(safeName, safeDesc);
        }
        return assemblePreviewHtml(safeName, safeDesc, platform, "GENERATED", bodyClass, headerEmoji, scriptContent, filesSection);
    }

    private static String escapeHtml(String raw) {
        if (raw == null || raw.isEmpty()) return "";
        return raw.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }

    private static String escapeJs(String raw) {
        if (raw == null) return "null";
        return raw.replace("\\", "\\\\").replace("\"", "\\\"").replace("'", "\\'")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private String assemblePreviewHtml(String name, String desc, String platform, String status,
                                        String bodyClass, String emoji, String bodyScript, String filesSection) {
        return """
            <!DOCTYPE html>
            <html lang="en">
            <head>
              <meta charset="UTF-8"/><meta name="viewport" content="width=device-width, initial-scale=1.0"/>
              <title>%s — Preview</title>
              <style>
                *,*::before,*::after{box-sizing:border-box;margin:0;padding:0}
                body{font-family:'Inter',-apple-system,BlinkMacSystemFont,'Segoe UI',Roboto,sans-serif;
                     background:linear-gradient(135deg,#0f0c29 0%%,#302b63 50%%,#24243e 100%%);
                     color:#e2e8f0;min-height:100vh;display:flex;flex-direction:column;align-items:center;padding:28px}
                h1{font-size:1.5rem;font-weight:700} h2{font-size:1.15rem;font-weight:600;color:#a5b4fc;margin-bottom:12px}
                p{color:#8392ab;line-height:1.6}
                .badge{display:inline-flex;align-items:center;gap:5px;background:rgba(99,102,241,.18);border:1px solid rgba(99,102,241,.4);
                       color:#818cf8;padding:4px 14px;border-radius:999px;font-size:.8rem;font-weight:500}
                .card{background:rgba(255,255,255,.04);border:1px solid rgba(255,255,255,.08);
                      backdrop-filter:blur(24px);border-radius:20px;padding:28px;width:100%%;max-width:760px}
                .header{display:flex;justify-content:space-between;align-items:flex-start;flex-wrap:wrap;gap:12px;margin-bottom:20px}
                .status-tag{font-size:.7rem;padding:3px 10px;border-radius:999px;font-weight:600;text-transform:uppercase;
                            letter-spacing:.05em;background:rgba(52,211,153,.15);border:1px solid rgba(52,211,153,.35);color:#34d399}
                .label{font-size:.75rem;text-transform:uppercase;letter-spacing:.1em;color:#64748b;margin:20px 0 8px}
                .preview-window{background:#1e1b4b;border-radius:12px;overflow:hidden;border:1px solid rgba(255,255,255,.07);margin-bottom:20px}
                .preview-bar{background:rgba(255,255,255,.05);padding:7px 12px;display:flex;align-items:center;gap:6px;border-bottom:1px solid rgba(255,255,255,.07)}
                .dot{width:10px;height:10px;border-radius:50%%} .dot-r{background:#f87171} .dot-y{background:#facc15} .dot-g{background:#4ade80}
                .preview-url{flex:1;text-align:center;font-size:.65rem;color:#4b5563;font-family:monospace}
                .preview-body{padding:22px 24px;min-height:200px}
                .app-canvas{width:100%%;min-height:220px;display:flex;flex-direction:column;align-items:center;justify-content:center;gap:14px}
                .code-wrap{background:#0b0b1e;border-radius:10px;overflow:hidden;border:1px solid rgba(255,255,255,.06)}
                .code-header{background:rgba(255,255,255,.04);padding:6px 14px;font-size:.7rem;color:#6b7280;font-family:monospace;border-bottom:1px solid rgba(255,255,255,.06)}
                .code-block{padding:14px;font-family:'JetBrains Mono','Fira Code',monospace;font-size:.72rem;line-height:1.7;color:#a5b4fc;max-height:200px;overflow-y:auto;white-space:pre-wrap}
                .file-list{list-style:none} .file-list li{display:flex;justify-content:space-between;align-items:center;padding:7px 12px;border-bottom:1px solid rgba(255,255,255,.05);font-size:.82rem}
                .file-list li:last-child{border-bottom:none} .file-ext{font-size:.7rem;color:#4b5563;background:rgba(255,255,255,.05);padding:2px 8px;border-radius:6px}
                @keyframes pulse-glow{0%%{box-shadow:0 0 8px rgba(99,102,241,.2)}50%%{box-shadow:0 0 22px rgba(99,102,241,.45)}100%%{box-shadow:0 0 8px rgba(99,102,241,.2)}}
                .animate-glow{animation:pulse-glow 2.5s ease-in-out infinite}
                .btn{display:inline-flex;align-items:center;gap:6px;padding:8px 18px;border-radius:9px;font-size:.82rem;font-weight:600;text-decoration:none;cursor:pointer;transition:transform .15s,box-shadow .15s}
                .btn-primary{background:#4f46e5;color:#fff;border:none} .btn-primary:hover{transform:translateY(-1px);box-shadow:0 4px 20px rgba(79,70,229,.4)}
                .btn-outline{background:transparent;color:#818cf8;border:1px solid rgba(99,102,241,.4)}
                .btn-row{display:flex;gap:10px;flex-wrap:wrap}
              </style>
            </head>
            <body class="%s">
              <div class="card">
                <div class="header">
                  <div><h1>%s %s</h1><p style="margin-top:5px">%s</p></div>
                  <div style="display:flex;flex-direction:column;align-items:flex-end;gap:8px">
                    <span class="badge">%s</span><span class="status-tag">%s</span>
                  </div>
                </div>
                %s%s%s
              </div>
            </body>
            </html>
            """.formatted(
                name, name, name,                           // title
                bodyClass, name, emoji + " " + name, desc,  // body, h1, badge
                platform, status,                           // badge, status
                buildPreviewToolbar(platform),              // toolbar
                buildPreviewWindow(bodyScript),             // canvas
                filesSection                                // file list
            );
    }

    private String buildPreviewToolbar(String platform) {
        return """
            <div class="btn-row" style="margin-bottom:20px; display:flex; gap:10px;">
              <button class="btn btn-primary" onclick="reloadPreview()"><span>🔄</span> Reload</button>
              <button class="btn btn-outline" onclick="copySource()"><span>📋</span> Copy</button>
              <a class="btn btn-outline" href="/api/generate/preview" target="_blank" style="text-decoration:none;cursor:pointer;"><span>📐</span> Blueprint</a>
            </div>
            <div class="preview-window">
              <div class="preview-bar">
                <span class="dot dot-r"></span><span class="dot dot-y"></span><span class="dot dot-g"></span>
                <span class="preview-url">localhost — %s Preview</span>
              </div>
            """.formatted(escapeHtml(platform));
    }

    private String buildPreviewWindow(String scriptContent) {
        return """
              <div class="preview-body" id="preview-canvas">%s</div>
            </div>
            <script>
              %s
              function reloadPreview(){document.getElementById('preview-canvas').innerHTML=`%s`;eval(`%s`)}
              function copySource(){const ta=document.createElement('textarea');ta.value=document.getElementById('preview-body')?.innerText||'';document.body.appendChild(ta);ta.select();document.execCommand('copy');document.body.removeChild(ta);alert('Source copied.')}
            </script>
            """.formatted(
                scriptContent,
                escapeJs(scriptContent),
                scriptContent.replace("`", "\\`"),
                escapeJs(scriptContent)
            );
    }

    private String buildWebScript(String sourceCode, String appName) {
        return """
            <div class="app-canvas"><span style="font-size:3.5rem">🖥️</span>
              <h2 style="font-size:1.3rem">%s</h2>
              <p style="font-size:.85rem;color:#6b7280;text-align:center;max-width:380px">
                React + Vite project ready for <code>npm run build</code> and deployment.
              </p>
              <div class="btn btn-primary" style="cursor:default">✅ Project Ready</div>
            </div>""".formatted(escapeHtml(appName));
    }

    private String buildDeviceScript(String title, String lang, String accentColor) {
        return """
            <div style="text-align:center;padding:18px 0 8px">
              <div class="device-frame"><div class="device-notch"></div><div class="device-screen">
                <div style="display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:160px;padding:20px;gap:10px;background:linear-gradient(150deg,#0b0b2e,#11113a)">
                  <span style="font-size:2.4rem">%s</span>
                  <span style="font-size:.75rem;color:%s;font-weight:600">%s</span>
                  <span style="font-size:.6rem;color:#4b5563">Generated by SupremeAI</span>
                </div>
              </div></div>
              <p style="font-size:.75rem;color:#4b5563;margin-top:10px">Native %s source in file list below</p>
            </div>""".formatted(emojiFor(title), accentColor, title, lang);
    }

    private String buildGenericScript(String name, String desc) {
        return """
            <div class="app-canvas"><span style="font-size:3.5rem">✨</span>
              <h2 style="font-size:1.3rem">%s — Ready</h2>
              <p style="font-size:.85rem;color:#6b7280;text-align:center;max-width:380px">%s</p>
              <div class="btn-primary" style="padding:10px 24px">✅ App Generated Successfully</div>
            </div>""".formatted(escapeHtml(name), escapeHtml(desc.isEmpty() ? "Your application is ready." : desc));
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
        sb.append("<p class=\"label\">📁 Generated Files (").append(files.size()).append(")</p><ul class=\"file-list\">");
        files.forEach((fname, _) -> {
            String ext = fname.contains(".") ? fname.substring(fname.lastIndexOf('.') + 1).toUpperCase() : "FILE";
            String color = switch (ext) {
                case "JAVA", "KT" -> "#f97316"; case "JSX", "TSX", "JS" -> "#61dafb";
                case "CSS" -> "#a78bfa"; case "SQL" -> "#fb7185"; case "YAML", "YML" -> "#fbbf24";
                default -> "#94a3b8";
            };
            sb.append(String.format("<li><span>%s</span><span class=\"file-ext\" style=\"color:%s\">%s</span></li>",
                    escapeHtml(fname), color, ext));
        });
        sb.append("</ul>");
        return sb.toString();
    }

    private String buildCodeViewer(String sourceCode, String filenameHint) {
        if (sourceCode == null || sourceCode.isBlank()) return "";
        String preview = sourceCode.length() > 1200 ? sourceCode.substring(0, 1200) + "\n\n// ... more" : sourceCode;
        return """
            <p class="label">👁 %s — Preview</p>
            <div class="code-wrap"><div class="code-header">%s</div><div class="code-block">%s</div></div>
            """.formatted(escapeHtml(filenameHint), escapeHtml(filenameHint), escapeHtml(preview));
    }

    private String extractSourceCode(Map<String, Object> result) {
        if (result.get("app") instanceof String) return (String) result.get("app");
        if (result.get("index.html") instanceof String) return (String) result.get("index.html");
        return "";
    }
}
