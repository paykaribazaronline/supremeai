# Technical Debt & Placeholders Report

This document lists all the features or implementations in the code that are currently mocked, hardcoded, or marked as TODO/FIXME, which users or admins might face issues with if they try to use them in production.

## File: `src/main/java/com/supremeai/generation/FullStackCodeGenerator.java`
- Line 169: placeholder="Name"
- Line 174: placeholder="Description"
- Line 179: placeholder="Price"

## File: `src/main/java/com/supremeai/websocket/SimulatorWebSocketHandler.java`
- Line 204: "data", "[screenshot_placeholder_base64]",

## File: `src/main/java/com/supremeai/intelligence/StressTestService.java`
- Line 89: "Create a 3D architectural mockup of a modern eco-friendly office building",

## File: `src/main/java/com/supremeai/learning/ContentSanitizerService.java`
- Line 86: if (containsHardcodedSecretsAfterMasking(code)) {
- Line 87: log.warn("[Sanitizer] REJECTED: Solution contains hardcoded secrets after masking from {}", source);
- Line 116: * Check for hardcoded secrets (post-masking - checks keys that weren't caught by PII mask).
- Line 119: private boolean containsHardcodedSecretsAfterMasking(String code) {

## File: `src/main/java/com/supremeai/learning/service/EnhancedContentSanitizerService.java`
- Line 274: // Check for hardcoded secrets

## File: `src/main/java/com/supremeai/learning/RouterKnowledgeInitializer.java`
- Line 56: logger.info("[ROUTER_INIT] Using runtime learning only — no hardcoded provider preferences");
- Line 110: logger.info("[ROUTER_INIT] No hardcoded benchmarks — router learns from runtime data");

## File: `src/main/java/com/supremeai/learning/SupremeLearningOrchestrator.java`
- Line 413: // TODO Phase 2.5: Push to AdminDashboardFacadeService for dashboard notification

## File: `src/main/java/com/supremeai/codeflow/analyzer/SecurityScanner.java`
- Line 14: * Detects hardcoded secrets, SQL injection, XSS, debug statements, etc.
- Line 23: new SecurityPattern("HARDCODED_SECRET", "CRITICAL", Pattern.compile("(?i)(password|passwd|pwd|secret|api_key|apikey|token|auth)\\s*[=:]\\s*['\"][^'\"]{8,}['\"]"), "Hardcoded secret detected", "Use environment variables or secure vault for secrets", "CWE-798"),

## File: `src/main/java/com/supremeai/model/SystemConfig.java`
- Line 29: // Generic configuration maps to replace hardcoded values

## File: `src/main/java/com/supremeai/model/ProviderTypeConfig.java`
- Line 12: * This replaces all hardcoded switch/case provider mappings in AIProviderFactory.

## File: `src/main/java/com/supremeai/service/SimulatorTestService.java`
- Line 193: default -> "// Auto-generated test placeholder\n// Requirements: " + requirements;

## File: `src/main/java/com/supremeai/service/CacheWarmingService.java`
- Line 64: // Pre-populate with placeholder - actual values will be cached on first use
- Line 65: promptsCache.put(cacheKey, createPlaceholderResponse(prompt));
- Line 88: private String createPlaceholderResponse(String prompt) {

## File: `src/main/java/com/supremeai/service/ProviderTypeRegistry.java`
- Line 20: * Replaces all hardcoded switch/case provider mappings.

## File: `src/main/java/com/supremeai/service/DeviceEmulationService.java`
- Line 22: * - Device API mocking (geolocation, battery, orientation)
- Line 84: // 3. Inject device API polyfills/mocks
- Line 191: * Inject JavaScript to mock device APIs.
- Line 195: <script id="supremeai-device-mock">
- Line 207: // Geolocation mock (configurable)
- Line 229: // Battery API mock

## File: `src/main/java/com/supremeai/service/ChatProcessingService.java`
- Line 273: // TODO: implement provider persistence

## File: `src/main/java/com/supremeai/service/SelfHealingService.java`
- Line 247: return "// Initial code for: " + prompt + "\npublic class Generated {\n    // TODO: Implement\n}";
- Line 251: return code.contains("public") && code.contains("class") && !code.contains("TODO");
- Line 255: return currentCode.replace("TODO", "Implemented in iteration " + (iteration + 1));

## File: `src/main/java/com/supremeai/service/KnowledgeSeederService.java`
- Line 207: "Image understanding: screenshot error reading, visual data extraction, UI mockup parsing. " +
- Line 419: makeBestPractice("bp-001", "Security: No Hardcoded Secrets",
- Line 453: "Unit tests: pure service logic with mocked repositories (Mockito). " +
- Line 458: List.of("testing", "junit5", "mockito", "jacoco", "reactor-test")),

## File: `src/main/java/com/supremeai/service/CyberSecuritySkillService.java`
- Line 19: * This is not hardcoded; the system acquires patterns and generates protections.

## File: `src/main/java/com/supremeai/service/SystemConfigSeeder.java`
- Line 19: * Only seeds global_settings. No hardcoded providers — admin configures via dashboard.

## File: `src/main/java/com/supremeai/service/CodeGenerationService.java`
- Line 882: "import org.springframework.boot.test.mock.mockito.MockBean;\n" +
- Line 883: "import org.springframework.test.web.servlet.MockMvc;\n\n" +
- Line 885: "import static org.mockito.ArgumentMatchers.any;\n" +
- Line 886: "import static org.mockito.Mockito.*;\n" +
- Line 887: "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;\n" +
- Line 888: "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;\n\n" +
- Line 892: "    private MockMvc mockMvc;\n\n" +
- Line 893: "    @MockBean\n" +
- Line 898: "        mockMvc.perform(get(\"/api/" + lowerName + "s\"))\n" +
- Line 904: "        mockMvc.perform(post(\"/api/" + lowerName + "s\")\n" +
- Line 921: "import org.mockito.InjectMocks;\n" +
- Line 922: "import org.mockito.Mock;\n" +
- Line 923: "import org.mockito.junit.jupiter.MockitoExtension;\n\n" +
- Line 926: "import static org.mockito.Mockito.*;\n\n" +
- Line 927: "@ExtendWith(MockitoExtension.class)\n" +
- Line 929: "    @Mock\n" +
- Line 931: "    @InjectMocks\n" +
- Line 975: "    testImplementation 'org.mockito:mockito-core'\n" +
- Line 1074: readme.append("- Automated tests (JUnit 5, Mockito)\n");

## File: `src/main/java/com/supremeai/service/CodeValidationService.java`
- Line 14: * Sprint 2 P0: minimal implementation (placeholder for full validation).

## File: `src/main/java/com/supremeai/service/AuthenticationService.java`
- Line 137: // No hardcoded fallback

## File: `src/main/java/com/supremeai/service/analysis/PatternRepository.java`
- Line 16: * In Phase 1, patterns are hardcoded. Later will be loaded from Firestore config.
- Line 37: .message("Hardcoded secret detected: {match}")
- Line 126: .pattern(Pattern.compile("(TODO|FIXME|XXX|HACK)\\s*:"))

## File: `src/main/java/com/supremeai/service/analysis/VectorSearchService.java`
- Line 31: List<Double> dummyEmbedding = new ArrayList<>();
- Line 32: for (int i = 0; i < 768; i++) dummyEmbedding.add(0.0);
- Line 42: embeddings.add(dummyEmbedding);
- Line 49: List<Double> embedding = i < embeddings.size() ? embeddings.get(i) : dummyEmbedding;

## File: `src/main/java/com/supremeai/service/analysis/DependencyAnalysisAgent.java`
- Line 153: // Check for hardcoded secrets in scripts

## File: `src/main/java/com/supremeai/service/analysis/QualityAnalysisAgent.java`
- Line 177: // TODO/FIXME comments
- Line 178: if (line.toUpperCase().contains("TODO") || line.toUpperCase().contains("FIXME") || line.toUpperCase().contains("HACK")) {

## File: `src/main/java/com/supremeai/service/MultiAIVotingService.java`
- Line 62: // Dynamic model selection (no hardcoded limit)
- Line 101: * Supports 0 to ∞ providers - no hardcoded limits.
- Line 553: .mapToDouble(ProviderVote::getConfidence)
- Line 665: .mapToDouble(ProviderVote::getConfidence)

## File: `src/main/java/com/supremeai/service/VisionService.java`
- Line 20: * - UI mockup parsing (extract component structure)
- Line 123: log.warn("[VISION] Gemini vision failed: {}, using mock", e.getMessage());
- Line 124: return Mono.just(mockAnalysis(analysisType));
- Line 128: .onErrorResume(e -> Mono.just(mockAnalysis(analysisType)));
- Line 130: log.warn("[VISION] No vision API key configured — returning structured mock");
- Line 131: return Mono.just(mockAnalysis(analysisType));
- Line 218: * Parse UI mockup and return component structure description.
- Line 220: public Mono<VisionAnalysisResult> parseMockup(String base64Image) {
- Line 298: case UI_PARSE -> "Parse this UI mockup/screenshot. Describe: " +
- Line 308: private VisionAnalysisResult mockAnalysis(AnalysisType type) {
- Line 310: case ERROR_DEBUG -> "Mock: Screenshot shows a NullPointerException at line 42. " +
- Line 312: case UI_PARSE -> "Mock: UI contains a header (AppBar), body with ListView (3 items), " +
- Line 314: case DATA_EXTRACT -> "Mock: Extracted text — 'Dashboard | Users: 42 | Revenue: $1,234 | Status: Active'";
- Line 315: case GENERAL -> "Mock: Image shows a mobile application interface with standard navigation pattern.";
- Line 317: return VisionAnalysisResult.mock(summary);
- Line 344: public static VisionAnalysisResult mock(String summary) {
- Line 345: return new VisionAnalysisResult(true, summary, null, "mock", null);

## File: `src/main/java/com/supremeai/service/CodeGenerationServiceEnhanced.java`
- Line 381: "import org.springframework.boot.test.mock.mockito.MockBean;\n" +
- Line 382: "import org.springframework.test.web.servlet.MockMvc;\n\n" +
- Line 384: "import static org.mockito.ArgumentMatchers.any;\n" +
- Line 385: "import static org.mockito.Mockito.*;\n" +
- Line 386: "import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;\n" +
- Line 387: "import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;\n\n" +
- Line 391: "    private MockMvc mockMvc;\n\n" +
- Line 392: "    @MockBean\n" +
- Line 397: "        mockMvc.perform(get(\"/api/" + lowerName + "s\"))\n" +
- Line 403: "        mockMvc.perform(post(\"/api/" + lowerName + "s\")\n" +
- Line 420: "import org.mockito.InjectMocks;\n" +
- Line 421: "import org.mockito.Mock;\n" +
- Line 422: "import org.mockito.junit.jupiter.MockitoExtension;\n\n" +
- Line 425: "import static org.mockito.Mockito.*;\n\n" +
- Line 426: "@ExtendWith(MockitoExtension.class)\n" +
- Line 428: "    @Mock\n" +
- Line 430: "    @InjectMocks\n" +
- Line 474: "    testImplementation 'org.mockito:mockito-core'\n" +
- Line 573: readme.append("- Automated tests (JUnit 5, Mockito)\n");

## File: `src/main/java/com/supremeai/service/ConfigService.java`
- Line 49: @org.springframework.beans.factory.annotation.Value("${supremeai.redis.mock-online:false}")
- Line 50: private boolean mockOnline;
- Line 147: if (redisTemplate != null && !mockOnline) {

## File: `src/main/java/com/supremeai/service/ProviderCapabilityAnalyzer.java`
- Line 19: * to discover their capabilities without hardcoded scores.

## File: `src/main/java/com/supremeai/automation/auth/FirebaseAuthAutomator.java`
- Line 30: // Mocking Firebase Admin SDK user creation
- Line 35: String mockUid = "usr_" + System.currentTimeMillis();
- Line 36: log.info("[Firebase Auth] Account created successfully! UID: {}", mockUid);
- Line 37: return new AuthResult(true, "Account created", mockUid);
- Line 55: // Mocking success - email validation only for development
- Line 60: String mockIdToken = "jwt_token_" + System.currentTimeMillis();
- Line 62: return new AuthResult(true, "Login successful", mockIdToken);

## File: `src/main/java/com/supremeai/controller/AdminBackupController.java`
- Line 19: private final List<Map<String, Object>> mockBackups = new ArrayList<>();
- Line 23: // Initial mock data
- Line 24: mockBackups.add(Map.of(
- Line 35: return Mono.just(ResponseEntity.ok(ApiResponse.ok(mockBackups)));

## File: `src/main/java/com/supremeai/controller/ReverseEngineeringController.java`
- Line 79: m.put("startedAt", job.getCreatedAt()); // TODO: add startedAt field

## File: `src/main/java/com/supremeai/controller/HealthController.java`
- Line 26: @org.springframework.beans.factory.annotation.Value("${supremeai.redis.mock-online:false}")
- Line 27: private boolean mockOnline;
- Line 40: if (mockOnline) {
- Line 41: details.put("redis", "UP (MOCKED)");
- Line 73: if (mockOnline) {
- Line 74: health.put("redis", Map.of("status", "UP", "mocked", true));

## File: `src/main/java/com/supremeai/controller/AppGenerationController.java`
- Line 203: *   ios-mock   → interactive device-frame mock-up card
- Line 204: *   desktop    → embedded Electron-style mock shell
- Line 205: *   android    → embedded WebView-styled mock shell
- Line 206: *   fullstack  → React-style dashboard mock-up + API panel
- Line 244: // ── iOS: interactive phone mock card ──────────────────────────
- Line 252: // ── Android: phone mock card ──────────────────────────────────
- Line 261: // ── Desktop: window frame mock-up ────────────────────────────

## File: `dashboard/src/lib/firebase.ts`
- Line 19: apiKey: import.meta.env.VITE_FIREBASE_API_KEY || 'dummy-key-for-development',

## File: `dashboard/src/pages/AdminVPN.tsx`
- Line 250: placeholder="নাম বা হোস্ট দিয়ে খুঁজুন..."
- Line 364: <Input placeholder="e.g. SG-HighSpeed-01" className="dark-input" />
- Line 368: <Input placeholder="1.2.3.4" className="dark-input" style={{ fontFamily: 'monospace' }} />
- Line 375: <Input placeholder="admin" className="dark-input" />
- Line 386: .dark-input-minimal::placeholder {

## File: `dashboard/src/pages/AdminProviders.tsx`
- Line 248: placeholder="Search by Model Name, Provider or API Hints..."
- Line 260: placeholder="Sort by"

## File: `dashboard/src/pages/AdminDashboard.archive.txt`
- Line 8: ❌ 483 lines of hardcoded React JSX
- Line 11: ❌ All config hardcoded in code
- Line 27: ✅ Zero code duplication - no hardcoded menus/components
- Line 34: - 13 hardcoded menu items with nested children
- Line 40: - All hardcoded Ant Design layout

## File: `dashboard/src/pages/AdminPerformance.tsx`
- Line 336: placeholder="ফিল্টার প্রোভাইডার"

## File: `dashboard/src/pages/LoginPage.tsx`
- Line 249: placeholder="ইমেইল অ্যাড্রেস"
- Line 267: placeholder="পাসওয়ার্ড"
- Line 383: placeholder="পূর্ণ নাম"
- Line 402: placeholder="ইমেইল অ্যাড্রেস"
- Line 421: placeholder="পাসওয়ার্ড (অন্তত ৬ অক্ষর)"
- Line 447: placeholder="পাসওয়ার্ড আবার লিখুন"

## File: `dashboard/src/pages/AdminLogs.tsx`
- Line 226: placeholder="ইউজার বা অ্যাকশন দিয়ে খুঁজুন..."
- Line 244: placeholder="সবগুলো"
- Line 320: .dark-input-minimal::placeholder {
- Line 422: .dark-input-minimal::placeholder {

## File: `dashboard/src/pages/AutoBrowser.tsx`
- Line 142: // Backend unavailable — set placeholder so UI still renders
- Line 316: placeholder="লিঙ্ক দিন — e.g. https://supremeai.web.app"
- Line 377: placeholder="কাজের নির্দেশ দিন — e.g. login as a guest and chat with system"
- Line 625: key="placeholder"

## File: `dashboard/src/components/security/SelfHealingPanel.tsx`
- Line 62: placeholder="একটি এরর মেসেজ লিখুন (যেমন: Connection timeout to provider X)..."

## File: `dashboard/src/components/security/CyberLearningPanel.tsx`
- Line 57: placeholder="Vulnerability Topic (e.g., SQL Injection, Zero-day)..."

## File: `dashboard/src/components/reverse-engineer/AutomationLaunchCard.tsx`
- Line 47: placeholder="https://target-website.com"
- Line 74: placeholder="Tell SupremeAI exactly what to do. Example: 'Go to the pricing page, extract all plans into a table, and find the cheapest annual option.'"

## File: `dashboard/src/components/ProjectUploadForm.tsx`
- Line 24: description: 'Hardcoded secrets, SQL injection, XSS, path traversal, vulnerable dependencies',
- Line 138: placeholder="Enter Git repository URL"
- Line 146: placeholder="Branch (default: main)"

## File: `dashboard/src/components/ChatWithAI.tsx`
- Line 390: placeholder="Neural Input Channel [Type your command]..."
- Line 394: className="h-16 bg-white/[0.02] border-white/10 text-white placeholder:text-white/10 rounded-2xl px-6 pr-44 focus:bg-white/[0.05] focus:border-emerald-500/40 transition-all shadow-2xl backdrop-blur-sm"
- Line 473: placeholder="Enter a descriptive name..."

## File: `dashboard/src/components/api-keys/ModelDiscovery.tsx`
- Line 150: placeholder="মডেল সার্চ করুন (উদাঃ Llama 3, GPT-4, Gemini)"
- Line 189: placeholder="Google AI API Key দিন (Gemini মডেল সার্চের জন্য)"

## File: `dashboard/src/components/api-keys/ModelSearchSelect.tsx`
- Line 15: placeholder?: string;
- Line 21: placeholder = "Search AI models... e.g., 'GPT-4', 'Llama', 'Claude'"
- Line 116: placeholder={placeholder}

## File: `dashboard/src/components/api-keys/AddKeyModal.tsx`
- Line 88: <Input placeholder="উদাঃ My Production Gemini" size="large" />
- Line 118: <Input.Password placeholder="sk-..." size="large" />
- Line 127: <Input placeholder="https://api.openai.com/v1" size="large" />
- Line 135: <Select mode="tags" style={{ width: '100%' }} placeholder="উদাঃ gpt-4, claude-3-opus" size="large" />

## File: `dashboard/src/components/VideoTutorials.tsx`
- Line 153: <div className="guide-thumbnail-placeholder">
- Line 226: <div className="guide-thumbnail-placeholder guide-list-thumbnail">

## File: `dashboard/src/components/users/UserModal.tsx`
- Line 40: <Input placeholder="user@example.com" disabled={!!editingUser} style={{ borderRadius: '8px' }} />
- Line 47: <Input placeholder="John Doe" style={{ borderRadius: '8px' }} />
- Line 56: <Input.Password placeholder="Secure password" style={{ borderRadius: '8px' }} />
- Line 65: <Select placeholder="Select tier" style={{ borderRadius: '8px' }}>

## File: `dashboard/src/components/users/UserActionToolbar.tsx`
- Line 47: placeholder="ইমেইল বা নাম দিয়ে খুঁজুন..."
- Line 85: placeholder="বাছাই করুন"

## File: `dashboard/src/components/learning/KnowledgeDomainsTab.tsx`
- Line 134: <Input placeholder="যেমন: Java Spring Boot, Cloud Security ইত্যাদি" />
- Line 141: <Input placeholder="যেমন: spring, security, firewall" />

## File: `dashboard/src/components/learning/LearningSourcesTab.tsx`
- Line 205: placeholder="যেমন: https://growthhackers.com"
- Line 222: <Input placeholder="যেমন: marketing, security, ai_research" />
- Line 226: <Input.TextArea rows={2} placeholder="এই সাইটের ব্যাখ্যা..." />

## File: `dashboard/src/components/projects/AppGenerationCard.tsx`
- Line 64: placeholder="e.g., My Personal Budget Tracker"
- Line 73: placeholder="Example: I need an app for my small grocery shop to track daily sales and stock."
- Line 84: placeholder="Select target platform"
- Line 98: placeholder="Select database engine"

## File: `dashboard/src/components/projects/ProjectModal.tsx`
- Line 51: <Input placeholder="My AI Project" />
- Line 55: <Input.TextArea rows={3} placeholder="Brief description of the project..." />

## File: `dashboard/src/components/projects/ProjectActionToolbar.tsx`
- Line 48: placeholder="প্রজেক্টের নাম বা আইডি দিয়ে খুঁজুন..."
- Line 78: placeholder="বাছাই করুন"

## File: `dashboard/src/components/AdminSystemWorkRules.tsx`
- Line 356: <Input placeholder="RULE_KEY" disabled={!!editing} />
- Line 361: <Select placeholder="Category">
- Line 380: <Input placeholder="What this rule controls…" />
- Line 386: <Input placeholder="e.g. 30min, true, high…" />
- Line 389: <Select placeholder="Type">
- Line 409: <Input placeholder="system_configs/global_settings" />
- Line 413: <Input placeholder="timeouts.auto_learning_interval_min" />
- Line 419: <Input.TextArea rows={2} placeholder="e.g. Admin changed auto-learning from 2h to 30min…" />

## File: `dashboard/src/components/VideoTutorials.css`
- Line 77: .guide-thumbnail-placeholder {
- Line 119: .guide-list-thumbnail.guide-thumbnail-placeholder {
- Line 191: .guide-thumbnail-placeholder {
- Line 206: .guide-list-thumbnail.guide-thumbnail-placeholder {

## File: `dashboard/src/components/SimulatorDashboard.tsx`
- Line 428: placeholder="Enter generated app ID (e.g. abc-123)"

## File: `dashboard/src/components/ApiTestConsole.tsx`
- Line 101: placeholder="Select an API key"
- Line 121: placeholder="https://api.example.com/v1/endpoint"
- Line 137: placeholder="Enter headers as JSON"
- Line 149: placeholder="Enter request body as JSON (for POST/PUT)"

## File: `dashboard/src/components/providers/ProviderActionToolbar.tsx`
- Line 50: placeholder="সার্চ প্রোভাইডার..."

## File: `dashboard/src/components/providers/ProviderModal.tsx`
- Line 115: <Input placeholder="e.g., My Primary GPT-4" />
- Line 119: <Select placeholder="Select architecture">
- Line 132: placeholder="Search model (e.g. gpt-4, claude-3, gemini-pro)"
- Line 155: <Input placeholder="https://api.openai.com/v1" />
- Line 169: placeholder="sk-..."
- Line 185: <Input placeholder="e.g., Personal key from MyAccount1" />
- Line 209: <Select mode="multiple" placeholder="Select roles for this key">

## File: `dashboard/src/components/settings/GeneralSettingsCard.tsx`
- Line 21: placeholder="e.g., gpt-4o"
- Line 31: placeholder="e.g., supremeai/1.5-flash"
- Line 41: <TextArea rows={4} placeholder="You are SupremeAI..." style={{ borderRadius: '8px' }} />

## File: `dashboard/src/components/RepoToPromptEngine.css`
- Line 71: .repo-to-prompt-engine .ant-input::placeholder {

## File: `dashboard/src/components/simulator/SimulationControlCard.tsx`
- Line 38: placeholder="প্রজেক্ট নির্বাচন করুন"

## File: `dashboard/src/components/simulator/QuotaAdminCard.tsx`
- Line 29: placeholder="Quota (1-20)"

## File: `dashboard/src/components/browser/BrowserSafetyDrawer.tsx`
- Line 72: placeholder="domain.com (e.g. facebook.com)"
- Line 155: placeholder="Website Domain (e.g. amazon.com)"
- Line 161: placeholder="Username / Email"
- Line 167: placeholder="Secure Password"
- Line 173: placeholder="Access Token (Optional)"

## File: `dashboard/src/components/browser/MissionProtocol.tsx`
- Line 58: placeholder="Describe what the AI should do (e.g. 'Go to Amazon, search for MacBook Pro M3, and list the prices')..."

## File: `dashboard/src/components/browser/BrowserDirectCommand.tsx`
- Line 28: placeholder="Inject manual command..."

## File: `dashboard/src/components/SystemLearningDashboard.tsx`
- Line 411: placeholder="e.g., React 19 Best Practices"
- Line 421: placeholder="Add relevant keywords"

## File: `dashboard/src/components/RepoToPromptEngine.tsx`
- Line 321: // Hardcoded secrets
- Line 331: type: 'HARDCODED_SECRET',
- Line 333: description: 'Potential hardcoded secret detected',
- Line 503: placeholder="https://github.com/owner/repository"
- Line 539: placeholder="Focus path (optional, e.g., src/api/)"
- Line 551: placeholder="GitHub Token (optional)"

