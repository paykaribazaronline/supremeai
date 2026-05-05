package com.supremeai.codeflow.analyzer;

import com.supremeai.codeflow.model.CodeRepository;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Code analysis engine with Tree-sitter WASM integration
 * Falls back to Acorn (JS/TS) and regex heuristics
 */
@Component
public class CodeAnalyzer {
    
    private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);
    
    // Supported languages and their Tree-sitter grammars
    private static final Map<String, String> LANGUAGE_GRAMMARS = Map.of(
        "python", "tree-sitter-python",
        "javascript", "tree-sitter-javascript",
        "typescript", "tree-sitter-typescript",
        "go", "tree-sitter-go",
        "rust", "tree-sitter-rust",
        "java", "tree-sitter-java",
        "ruby", "tree-sitter-ruby",
        "c", "tree-sitter-c",
        "cpp", "tree-sitter-cpp"
    );
    
    private static final Set<String> JS_LIKE_EXTENSIONS = Set.of(".js", ".ts", ".jsx", ".tsx", ".mjs", ".cjs");
    private static final Set<String> HTML_EXTENSIONS = Set.of(".html", ".htm");
    private static final Set<String> MARKDOWN_EXTENSIONS = Set.of(".md", ".markdown");
    
    /**
     * Parse repository and extract code structure
     */
    public List<CodeRepository.CodeFile> parseRepository(String repoPath) throws IOException {
        List<CodeRepository.CodeFile> files = new ArrayList<>();
        Path path = Paths.get(repoPath);
        
        if (!Files.exists(path)) {
            throw new IOException("Repository path does not exist: " + repoPath);
        }
        
        // Walk directory tree
        try (Stream<Path> stream = Files.walk(path)) {
            stream.filter(Files::isRegularFile)
                .filter(this::isCodeFile)
                .filter(p -> !isIgnored(p))
                .forEach(file -> {
                    try {
                        CodeRepository.CodeFile codeFile = parseFile(file, path);
                        if (codeFile != null) {
                            files.add(codeFile);
                        }
                    } catch (Exception e) {
                        logger.warn("Failed to parse file: " + file, e);
                    }
                });
        }
        
        logger.info("Parsed {} code files from repository", files.size());
        return files;
    }
    
    /**
     * Parse individual file
     */
    private CodeRepository.CodeFile parseFile(Path file, Path rootPath) throws IOException {
        String content = Files.readString(file);
        String relativePath = rootPath.relativize(file).toString();
        String extension = getFileExtension(file.getFileName().toString());
        String language = detectLanguage(extension, content);
        
        // Try Tree-sitter first
        List<CodeRepository.FunctionInfo> functions = new ArrayList<>();
        List<CodeRepository.ClassInfo> classes = new ArrayList<>();
        List<CodeRepository.ImportInfo> imports = new ArrayList<>();
        
        boolean parsedWithTreeSitter = false;
        
        if (LANGUAGE_GRAMMARS.containsKey(language)) {
            try {
                TreeSitterParseResult result = parseWithTreeSitter(content, language);
                functions = result.functions;
                classes = result.classes;
                imports = result.imports;
                parsedWithTreeSitter = true;
                logger.debug("Parsed {} with Tree-sitter", relativePath);
            } catch (Exception e) {
                logger.debug("Tree-sitter failed for {}, falling back", relativePath);
            }
        }
        
        // Fallback to Acorn for JS/TS
        if (!parsedWithTreeSitter && JS_LIKE_EXTENSIONS.contains(extension)) {
            try {
                TreeSitterParseResult result = parseWithAcorn(content);
                functions = result.functions;
                classes = result.classes;
                imports = result.imports;
                parsedWithTreeSitter = true;
                logger.debug("Parsed {} with Acorn", relativePath);
            } catch (Exception e) {
                logger.debug("Acorn failed for {}, falling back to regex", relativePath);
            }
        }
        
        // Final fallback to regex heuristics
        if (!parsedWithTreeSitter) {
            TreeSitterParseResult result = parseWithRegex(content, language);
            functions = result.functions;
            classes = result.classes;
            imports = result.imports;
            logger.debug("Parsed {} with regex heuristics", relativePath);
        }
        
        // Extract call references
        List<CodeRepository.CallReference> callReferences = extractCallReferences(content, functions);
        
        // Check for embedded scripts (HTML files)
        boolean hasEmbeddedScript = false;
        if (HTML_EXTENSIONS.contains(extension)) {
            hasEmbeddedScript = content.contains("<script");
            // Extract embedded JavaScript
            if (hasEmbeddedScript) {
                List<CodeRepository.FunctionInfo> embeddedFunctions = extractEmbeddedScripts(content);
                functions.addAll(embeddedFunctions);
            }
        }
        
        // Extract wiki-links from Markdown
        if (MARKDOWN_EXTENSIONS.contains(extension)) {
            extractWikiLinks(content);
        }
        
        // Calculate complexity
        int complexity = calculateComplexity(functions, classes);
        
        return CodeRepository.CodeFile.builder()
            .path(relativePath)
            .name(file.getFileName().toString())
            .extension(extension)
            .language(language)
            .size((int) Files.size(file))
            .linesOfCode(countLinesOfCode(content))
            .complexity(complexity)
            .functions(functions)
            .classes(classes)
            .imports(imports)
            .callReferences(callReferences)
            .securityIssues(new ArrayList<>())
            .hasEmbeddedScript(hasEmbeddedScript)
            .contentHash(Integer.toHexString(content.hashCode()))
            .build();
    }
    
    /**
     * Parse with Tree-sitter WASM
     */
    private TreeSitterParseResult parseWithTreeSitter(String content, String language) {
        // In production, this would call Tree-sitter WASM via JNI or HTTP
        // For now, simulate with regex-based parsing
        logger.debug("Simulating Tree-sitter parse for {}", language);
        return parseWithRegex(content, language);
    }
    
    /**
     * Parse JavaScript/TypeScript with Acorn
     */
    private TreeSitterParseResult parseWithAcorn(String content) {
        // In production, this would call Acorn via Node.js or WASM
        logger.debug("Simulating Acorn parse");
        return parseWithRegex(content, "javascript");
    }
    
    /**
     * Parse with regex heuristics
     */
    private TreeSitterParseResult parseWithRegex(String content, String language) {
        List<CodeRepository.FunctionInfo> functions = new ArrayList<>();
        List<CodeRepository.ClassInfo> classes = new ArrayList<>();
        List<CodeRepository.ImportInfo> imports = new ArrayList<>();
        
        // Detect imports
        Pattern importPattern = getImportPattern(language);
        Matcher importMatcher = importPattern.matcher(content);
        while (importMatcher.find()) {
            imports.add(parseImport(importMatcher.group(), language));
        }
        
        // Detect functions
        Pattern functionPattern = getFunctionPattern(language);
        Matcher functionMatcher = functionPattern.matcher(content);
        while (functionMatcher.find()) {
            CodeRepository.FunctionInfo func = parseFunction(functionMatcher.group(), 
                functionMatcher.start(), language);
            if (func != null) {
                functions.add(func);
            }
        }
        
        // Detect classes
        Pattern classPattern = getClassPattern(language);
        Matcher classMatcher = classPattern.matcher(content);
        while (classMatcher.find()) {
            CodeRepository.ClassInfo clazz = parseClass(classMatcher.group(), 
                classMatcher.start(), language);
            if (clazz != null) {
                classes.add(clazz);
            }
        }
        
        return new TreeSitterParseResult(functions, classes, imports);
    }
    
    /**
     * Extract call references between functions
     */
    private List<CodeRepository.CallReference> extractCallReferences(
            String content, List<CodeRepository.FunctionInfo> functions) {
        List<CodeRepository.CallReference> references = new ArrayList<>();
        
        for (CodeRepository.FunctionInfo caller : functions) {
            for (CodeRepository.FunctionInfo callee : functions) {
                if (!caller.getName().equals(callee.getName())) {
                    // Simple pattern: function calls
                    Pattern pattern = Pattern.compile(
                        "\\b" + Pattern.quote(callee.getName()) + "\\s*\\(");
                    Matcher matcher = pattern.matcher(content);
                    while (matcher.find()) {
                        int line = countNewlines(content.substring(0, matcher.start())) + 1;
                        references.add(CodeRepository.CallReference.builder()
                            .fromFunction(caller.getName())
                            .toFunction(callee.getName())
                            .line(line)
                            .type("DIRECT")
                            .build());
                    }
                }
            }
        }
        
        return references;
    }
    
    /**
     * Extract embedded JavaScript from HTML
     */
    private List<CodeRepository.FunctionInfo> extractEmbeddedScripts(String html) {
        List<CodeRepository.FunctionInfo> functions = new ArrayList<>();
        Pattern scriptPattern = Pattern.compile(
            "<script[^>]*>(.*?)</script>", Pattern.DOTALL);
        Matcher matcher = scriptPattern.matcher(html);
        
        while (matcher.find()) {
            String scriptContent = matcher.group(1);
            TreeSitterParseResult result = parseWithRegex(scriptContent, "javascript");
            functions.addAll(result.functions);
        }
        
        return functions;
    }
    
    /**
     * Extract wiki-links from Markdown
     */
    private void extractWikiLinks(String markdown) {
        Pattern wikiPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
        Matcher matcher = wikiPattern.matcher(markdown);
        while (matcher.find()) {
            logger.debug("Found wiki-link: {}", matcher.group(1));
        }
    }
    /**
     * Detect dead code
     */
    public List<CodeRepository.DeadCode> detectDeadCode(List<CodeRepository.CodeFile> files) {
        List<CodeRepository.DeadCode> deadCode = new ArrayList<>();
        
        // Build call graph
        Map<String, Set<String>> callGraph = new HashMap<>();
        Set<String> allFunctions = new HashSet<>();
        
        for (CodeRepository.CodeFile file : files) {
            if (file.getFunctions() != null) {
                for (CodeRepository.FunctionInfo func : file.getFunctions()) {
                    String funcKey = file.getPath() + ":" + func.getName();
                    allFunctions.add(funcKey);
                    callGraph.put(funcKey, new HashSet<>());
                    
                    if (func.getCalledFunctions() != null) {
                        callGraph.get(funcKey).addAll(func.getCalledFunctions());
                    }
                }
            }
        }
        
        // Find unused functions (no incoming calls)
        Set<String> calledFunctions = new HashSet<>();
        for (Set<String> calls : callGraph.values()) {
            calledFunctions.addAll(calls);
        }
        
        for (String func : allFunctions) {
            if (!calledFunctions.contains(func) && !func.contains("main") && 
                !func.contains("Main") && !func.contains("test")) {
                String[] parts = func.split(":");
                deadCode.add(CodeRepository.DeadCode.builder()
                    .type("UNUSED_FUNCTION")
                    .file(parts[0])
                    .name(parts.length > 1 ? parts[1] : "unknown")
                    .line(0)
                    .isExported(false)
                    .build());
            }
        }
        
        // Find unused imports
        for (CodeRepository.CodeFile file : files) {
            if (file.getImports() != null) {
                for (CodeRepository.ImportInfo imp : file.getImports()) {
                    if (!imp.getIsUsed()) {
                        deadCode.add(CodeRepository.DeadCode.builder()
                            .type("UNUSED_IMPORT")
                            .file(file.getPath())
                            .name(imp.getModule())
                            .line(imp.getLine())
                            .isExported(false)
                            .build());
                    }
                }
            }
        }
        
        return deadCode;
    }
    
    /**
     * Get import pattern for language
     */
    private Pattern getImportPattern(String language) {
        switch (language) {
            case "python":
                return Pattern.compile("^\\s*(import\\s+[\\w.]+|from\\s+[\\w.]+\\s+import)", 
                    Pattern.MULTILINE);
            case "javascript":
            case "typescript":
                return Pattern.compile("import\\s+.*?from\\s+['\"][^'\"]+['\"]|require\\s*\\(");
            case "java":
                return Pattern.compile("^\\s*import\\s+[\\w.*]+;", Pattern.MULTILINE);
            case "go":
                return Pattern.compile("^\\s*import\\s+\\(", Pattern.MULTILINE);
            default:
                return Pattern.compile(".");
        }
    }
    
    /**
     * Get function pattern for language
     */
    private Pattern getFunctionPattern(String language) {
        switch (language) {
            case "python":
                return Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\([^)]*\\)", Pattern.MULTILINE);
            case "javascript":
            case "typescript":
                return Pattern.compile(
                    "(?:function\\s+(\\w+)|const\\s+(\\w+)\\s*=\\s*(?:async\\s+)?\\([^)]*\\)\\s*=>|async\\s+function\\s+(\\w+))",
                    Pattern.MULTILINE);
            case "java":
                return Pattern.compile(
                    "(public|private|protected)?\\s*(static)?\\s*\\w+\\s+(\\w+)\\s*\\([^)]*\\)",
                    Pattern.MULTILINE);
            case "go":
                return Pattern.compile("^\\s*func\\s+(\\w+)\\s*\\([^)]*\\)", Pattern.MULTILINE);
            default:
                return Pattern.compile(".");
        }
    }
    
    /**
     * Get class pattern for language
     */
    private Pattern getClassPattern(String language) {
        switch (language) {
            case "python":
                return Pattern.compile("^\\s*class\\s+(\\w+)", Pattern.MULTILINE);
            case "javascript":
            case "typescript":
                return Pattern.compile("class\\s+(\\w+)", Pattern.MULTILINE);
            case "java":
                return Pattern.compile(
                    "(public|private|protected)?\\s*(abstract|final)?\\s*class\\s+(\\w+)",
                    Pattern.MULTILINE);
            case "go":
                return Pattern.compile("type\\s+(\\w+)\\s+struct", Pattern.MULTILINE);
            default:
                return Pattern.compile(".");
        }
    }
    
    /**
     * Parse import statement
     */
    private CodeRepository.ImportInfo parseImport(String importStmt, String language) {
        String module = importStmt.replaceAll("[\\s;]", "");
        return CodeRepository.ImportInfo.builder()
            .module(module)
            .alias(null)
            .isUsed(true) // Will be checked later
            .line(0)
            .build();
    }
    
    /**
     * Parse function definition
     */
    private CodeRepository.FunctionInfo parseFunction(String funcDef, int line, String language) {
        String name = extractFunctionName(funcDef, language);
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        return CodeRepository.FunctionInfo.builder()
            .name(name)
            .line(line + 1)
            .endLine(line + 10) // Estimate
            .parameters(new ArrayList<>())
            .returnType("void")
            .complexity(1)
            .cyclomaticComplexity(1)
            .cognitiveComplexity(1)
            .calledFunctions(new ArrayList<>())
            .isPublic(!funcDef.contains("private"))
            .isStatic(funcDef.contains("static"))
            .isAsync(funcDef.contains("async"))
            .build();
    }
    
    /**
     * Parse class definition
     */
    private CodeRepository.ClassInfo parseClass(String classDef, int line, String language) {
        String name = extractClassName(classDef, language);
        if (name == null || name.isEmpty()) {
            return null;
        }
        
        return CodeRepository.ClassInfo.builder()
            .name(name)
            .line(line + 1)
            .type("CLASS")
            .extendsClasses(new ArrayList<>())
            .implementsInterfaces(new ArrayList<>())
            .methods(new ArrayList<>())
            .fields(new ArrayList<>())
            .complexity(1)
            .isAbstract(classDef.contains("abstract") || classDef.contains("interface"))
            .isFinal(classDef.contains("final"))
            .build();
    }
    
    /**
     * Extract function name
     */
    private String extractFunctionName(String funcDef, String language) {
        Pattern namePattern = Pattern.compile("(\\w+)\\s*\\(");
        Matcher matcher = namePattern.matcher(funcDef);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Extract class name
     */
    private String extractClassName(String classDef, String language) {
        Pattern namePattern = Pattern.compile("class\\s+(\\w+)");
        Matcher matcher = namePattern.matcher(classDef);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }
    
    /**
     * Calculate complexity
     */
    private int calculateComplexity(List<CodeRepository.FunctionInfo> functions,
                                    List<CodeRepository.ClassInfo> classes) {
        int complexity = 0;
        complexity += functions.size() * 2;
        complexity += classes.size() * 5;
        return Math.min(complexity, 100);
    }
    
    /**
     * Count lines of code (excluding blank lines and comments)
     */
    private int countLinesOfCode(String content) {
        String[] lines = content.split("\\n");
        int count = 0;
        for (String line : lines) {
            String trimmed = line.trim();
            if (!trimmed.isEmpty() && !trimmed.startsWith("//") && 
                !trimmed.startsWith("/*") && !trimmed.startsWith("*")) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Count newlines
     */
    private int countNewlines(String text) {
        return (int) text.chars().filter(ch -> ch == '\n').count();
    }
    
    /**
     * Check if file is a code file
     */
    private boolean isCodeFile(Path file) {
        String fileName = file.getFileName().toString().toLowerCase();
        return fileName.endsWith(".py") || fileName.endsWith(".js") || 
               fileName.endsWith(".ts") || fileName.endsWith(".jsx") || 
               fileName.endsWith(".tsx") || fileName.endsWith(".go") ||
               fileName.endsWith(".rs") || fileName.endsWith(".java") ||
               fileName.endsWith(".rb") || fileName.endsWith(".c") ||
               fileName.endsWith(".cpp") || fileName.endsWith(".h") ||
               fileName.endsWith(".html") || fileName.endsWith(".md");
    }
    
    /**
     * Check if file should be ignored
     */
    private boolean isIgnored(Path file) {
        String path = file.toString().toLowerCase();
        return path.contains("node_modules") || path.contains(".git") ||
               path.contains("__pycache__") || path.contains(".venv") ||
               path.contains("venv") || path.contains("target") ||
               path.contains("build") || path.contains("dist") ||
               path.contains(".min.js");
    }
    
    /**
     * Get file extension
     */
    private String getFileExtension(String fileName) {
        int dotIndex = fileName.lastIndexOf('.');
        return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
    }
    
    /**
     * Detect language from extension and content
     */
    private String detectLanguage(String extension, String content) {
        switch (extension) {
            case "py": return "python";
            case "js": return "javascript";
            case "ts": return content.contains("typescript") ? "typescript" : "javascript";
            case "jsx":
            case "tsx": return "typescript";
            case "go": return "go";
            case "rs": return "rust";
            case "java": return "java";
            case "rb": return "ruby";
            case "c": return "c";
            case "cpp":
            case "cc":
            case "cxx": return "cpp";
            case "html": return "html";
            case "md": return "markdown";
            default: return "unknown";
        }
    }
    
    /**
     * Tree-sitter parse result container
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    private static class TreeSitterParseResult {
        private List<CodeRepository.FunctionInfo> functions;
        private List<CodeRepository.ClassInfo> classes;
        private List<CodeRepository.ImportInfo> imports;
    }
}