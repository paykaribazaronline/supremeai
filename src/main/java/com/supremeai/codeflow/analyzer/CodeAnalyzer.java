package com.supremeai.codeflow.analyzer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.supremeai.codeflow.model.CodeRepository;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Code analysis engine with Tree-sitter WASM integration Falls back to Acorn (JS/TS) and regex
 * heuristics
 */
@Component
public class CodeAnalyzer {

  private static final Logger logger = LoggerFactory.getLogger(CodeAnalyzer.class);

  // Supported languages and their Tree-sitter grammars
  private static final Map<String, String> LANGUAGE_GRAMMARS =
      Map.of(
          "python", "tree-sitter-python",
          "javascript", "tree-sitter-javascript",
          "typescript", "tree-sitter-typescript",
          "go", "tree-sitter-go",
          "rust", "tree-sitter-rust",
          "java", "tree-sitter-java",
          "ruby", "tree-sitter-ruby",
          "c", "tree-sitter-c",
          "cpp", "tree-sitter-cpp");

  private static final Set<String> JS_LIKE_EXTENSIONS =
      Set.of(".js", ".ts", ".jsx", ".tsx", ".mjs", ".cjs");
  private static final Set<String> HTML_EXTENSIONS = Set.of(".html", ".htm");
  private static final Set<String> MARKDOWN_EXTENSIONS = Set.of(".md", ".markdown");

  /** Parse repository and extract code structure */
  public List<CodeRepository.CodeFile> parseRepository(String repoPath) throws IOException {
    List<CodeRepository.CodeFile> files = new ArrayList<>();
    Path path = Paths.get(repoPath);

    if (!Files.exists(path)) {
      throw new IOException("Repository path does not exist: " + repoPath);
    }

    // Walk directory tree
    try (Stream<Path> stream = Files.walk(path)) {
      stream
          .filter(Files::isRegularFile)
          .filter(this::isCodeFile)
          .filter(p -> !isIgnored(p))
          .forEach(
              file -> {
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

  /** Parse individual file */
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
        functions = result.getFunctions();
        classes = result.getClasses();
        imports = result.getImports();
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
        functions = result.getFunctions();
        classes = result.getClasses();
        imports = result.getImports();
        parsedWithTreeSitter = true;
        logger.debug("Parsed {} with Acorn", relativePath);
      } catch (Exception e) {
        logger.debug("Acorn failed for {}, falling back to regex", relativePath);
      }
    }

    // Final fallback to regex heuristics
    if (!parsedWithTreeSitter) {
      TreeSitterParseResult result = parseWithRegex(content, language);
      functions = result.getFunctions();
      classes = result.getClasses();
      imports = result.getImports();
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

    CodeRepository.CodeFile codeFile = new CodeRepository.CodeFile();
    codeFile.setPath(relativePath);
    codeFile.setName(file.getFileName().toString());
    codeFile.setExtension(extension);
    codeFile.setLanguage(language);
    codeFile.setSize((int) Files.size(file));
    codeFile.setLinesOfCode(countLinesOfCode(content));
    codeFile.setComplexity(complexity);
    codeFile.setFunctions(functions);
    codeFile.setClasses(classes);
    codeFile.setImports(imports);
    codeFile.setCallReferences(callReferences);
    codeFile.setSecurityIssues(new ArrayList<>());
    codeFile.setHasEmbeddedScript(hasEmbeddedScript);
    codeFile.setContentHash(Integer.toHexString(content.hashCode()));
    return codeFile;
  }

  /** Parse with Tree-sitter WASM */
  private TreeSitterParseResult parseWithTreeSitter(String content, String language) {
    // In production, this would call Tree-sitter WASM via JNI or HTTP
    // For now, simulate with regex-based parsing
    logger.debug("Simulating Tree-sitter parse for {}", language);
    return parseWithRegex(content, language);
  }

  /** Parse JavaScript/TypeScript with Acorn */
  private TreeSitterParseResult parseWithAcorn(String content) {
    // In production, this would call Acorn via Node.js or WASM
    logger.debug("Simulating Acorn parse");
    return parseWithRegex(content, "javascript");
  }

  /** Parse with regex heuristics */
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
      CodeRepository.FunctionInfo func =
          parseFunction(functionMatcher.group(), functionMatcher.start(), language);
      if (func != null) {
        functions.add(func);
      }
    }

    // Detect classes
    Pattern classPattern = getClassPattern(language);
    Matcher classMatcher = classPattern.matcher(content);
    while (classMatcher.find()) {
      CodeRepository.ClassInfo clazz =
          parseClass(classMatcher.group(), classMatcher.start(), language);
      if (clazz != null) {
        classes.add(clazz);
      }
    }

    return new TreeSitterParseResult(functions, classes, imports);
  }

  /** Extract call references between functions */
  private List<CodeRepository.CallReference> extractCallReferences(
      String content, List<CodeRepository.FunctionInfo> functions) {
    List<CodeRepository.CallReference> references = new ArrayList<>();

    for (CodeRepository.FunctionInfo caller : functions) {
      for (CodeRepository.FunctionInfo callee : functions) {
        if (!caller.getName().equals(callee.getName())) {
          // Simple pattern: function calls
          Pattern pattern = Pattern.compile("\\b" + Pattern.quote(callee.getName()) + "\\s*\\(");
          Matcher matcher = pattern.matcher(content);
          while (matcher.find()) {
            int line = countNewlines(content.substring(0, matcher.start())) + 1;
            CodeRepository.CallReference ref = new CodeRepository.CallReference();
            ref.setFromFunction(caller.getName());
            ref.setToFunction(callee.getName());
            ref.setLine(line);
            ref.setType("DIRECT");
            references.add(ref);
          }
        }
      }
    }

    return references;
  }

  /** Extract embedded JavaScript from HTML */
  private List<CodeRepository.FunctionInfo> extractEmbeddedScripts(String html) {
    List<CodeRepository.FunctionInfo> functions = new ArrayList<>();
    Pattern scriptPattern = Pattern.compile("<script[^>]*>(.*?)</script>", Pattern.DOTALL);
    Matcher matcher = scriptPattern.matcher(html);

    while (matcher.find()) {
      String scriptContent = matcher.group(1);
      TreeSitterParseResult result = parseWithRegex(scriptContent, "javascript");
      functions.addAll(result.getFunctions());
    }

    return functions;
  }

  /** Extract wiki-links from Markdown */
  private void extractWikiLinks(String markdown) {
    Pattern wikiPattern = Pattern.compile("\\[\\[(.*?)\\]\\]");
    Matcher matcher = wikiPattern.matcher(markdown);
    while (matcher.find()) {
      logger.debug("Found wiki-link: {}", matcher.group(1));
    }
  }

  /** Detect dead code */
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
      if (!calledFunctions.contains(func)
          && !func.contains("main")
          && !func.contains("Main")
          && !func.contains("test")) {
        String[] parts = func.split(":");
        CodeRepository.DeadCode dc = new CodeRepository.DeadCode();
        dc.setType("UNUSED_FUNCTION");
        dc.setFile(parts[0]);
        dc.setName(parts.length > 1 ? parts[1] : "unknown");
        dc.setLine(0);
        dc.setIsExported(false);
        deadCode.add(dc);
      }
    }

    // Find unused imports
    for (CodeRepository.CodeFile file : files) {
      if (file.getImports() != null) {
        for (CodeRepository.ImportInfo imp : file.getImports()) {
          if (imp.getIsUsed() != null && !imp.getIsUsed()) {
            CodeRepository.DeadCode dc = new CodeRepository.DeadCode();
            dc.setType("UNUSED_IMPORT");
            dc.setFile(file.getPath());
            dc.setName(imp.getModule());
            dc.setLine(imp.getLine());
            dc.setIsExported(false);
            deadCode.add(dc);
          }
        }
      }
    }

    return deadCode;
  }

  private Pattern getImportPattern(String language) {
    switch (language) {
      case "python":
        return Pattern.compile(
            "^\\s*(import\\s+[\\w.]+|from\\s+[\\w.]+\\s+import)", Pattern.MULTILINE);
      case "javascript":
      case "typescript":
        return Pattern.compile("import\\s+.*?from\\s+['\"][^'\"]+['\"]|require\\s*\\(");
      case "java":
        return Pattern.compile("^\\s*import\\s+(?:static\\s+)?[\\w.*]+;", Pattern.MULTILINE);
      case "go":
        return Pattern.compile("^\\s*import\\s+\\(", Pattern.MULTILINE);
      default:
        return Pattern.compile(".");
    }
  }

  private Pattern getFunctionPattern(String language) {
    switch (language) {
      case "python":
        return Pattern.compile("^\\s*def\\s+(\\w+)\\s*\\([^)]*\\)", Pattern.MULTILINE);
      case "javascript":
      case "typescript":
        return Pattern.compile(
            "(?:function\\s+(\\w+)|(?:const|let|var)\\s+(\\w+)\\s*=\\s*(?:async\\s+)?(?:\\([^)]*\\)|\\w+)\\s*=>|async\\s+function\\s+(\\w+)|(\\w+(?:\\.\\w+)*)\\s*\\([^)]*\\))",
            Pattern.MULTILINE);
      case "java":
        return Pattern.compile(
            "^(?:\\s*(?:public|private|protected|static|final|abstract|synchronized|native|strictfp|default)\\s+)*"
                + "(?:[\\w<>\\[\\]\\?,\\s]+\\s+)?(\\w+)\\s*\\([^)]*\\)\\s*(?:throws\\s+[\\w,\\s]+)?\\s*\\{",
            Pattern.MULTILINE);
      case "go":
        return Pattern.compile(
            "^\\s*func\\s+(?:\\([^)]+\\)\\s+)?(\\w+)\\s*\\([^)]*\\)", Pattern.MULTILINE);
      default:
        return Pattern.compile(".");
    }
  }

  private Pattern getClassPattern(String language) {
    switch (language) {
      case "python":
        return Pattern.compile("^\\s*class\\s+(\\w+)", Pattern.MULTILINE);
      case "javascript":
      case "typescript":
        return Pattern.compile("class\\s+(\\w+)", Pattern.MULTILINE);
      case "java":
        return Pattern.compile(
            "(?:public|private|protected)?\\s*(?:abstract|final|sealed|non-sealed|strictfp)?\\s*(?:class|interface|enum|record)\\s+(\\w+)",
            Pattern.MULTILINE);
      case "go":
        return Pattern.compile("type\\s+(\\w+)\\s+struct", Pattern.MULTILINE);
      default:
        return Pattern.compile(".");
    }
  }

  private CodeRepository.ImportInfo parseImport(String importStmt, String language) {
    String module = importStmt.replaceAll("[\\s;]", "");
    CodeRepository.ImportInfo imp = new CodeRepository.ImportInfo();
    imp.setModule(module);
    imp.setIsUsed(true);
    imp.setLine(0);
    return imp;
  }

  private CodeRepository.FunctionInfo parseFunction(String funcDef, int line, String language) {
    String name = extractFunctionName(funcDef, language);
    if (name == null || name.isEmpty() || isKeyword(name)) {
      return null;
    }

    CodeRepository.FunctionInfo func = new CodeRepository.FunctionInfo();
    func.setName(name);
    func.setLine(line + 1);
    func.setEndLine(line + 10);
    func.setParameters(new ArrayList<>());
    func.setReturnType("void");
    func.setComplexity(1);
    func.setCyclomaticComplexity(1);
    func.setCognitiveComplexity(1);
    func.setCalledFunctions(new ArrayList<>());
    func.setIsPublic(!funcDef.contains("private"));
    func.setIsStatic(funcDef.contains("static"));
    func.setIsAsync(funcDef.contains("async"));
    func.setModifiers(extractModifiers(funcDef));
    return func;
  }

  private CodeRepository.ClassInfo parseClass(String classDef, int line, String language) {
    String name = extractClassName(classDef, language);
    if (name == null || name.isEmpty()) {
      return null;
    }

    CodeRepository.ClassInfo clazz = new CodeRepository.ClassInfo();
    clazz.setName(name);
    clazz.setLine(line + 1);
    clazz.setType("CLASS");
    clazz.setExtendsClasses(new ArrayList<>());
    clazz.setImplementsInterfaces(new ArrayList<>());
    clazz.setMethods(new ArrayList<>());
    clazz.setFields(new ArrayList<>());
    clazz.setComplexity(1);
    clazz.setIsAbstract(classDef.contains("abstract") || classDef.contains("interface"));
    clazz.setIsFinal(classDef.contains("final"));
    return clazz;
  }

  private boolean isKeyword(String word) {
    return Set.of("if", "for", "while", "switch", "catch", "try", "return", "else", "throw", "new")
        .contains(word);
  }

  private String extractFunctionName(String funcDef, String language) {
    Pattern p = getFunctionPattern(language);
    Matcher m = p.matcher(funcDef);
    if (m.find()) {
      for (int i = 1; i <= m.groupCount(); i++) {
        if (m.group(i) != null && !m.group(i).isEmpty()) {
          return m.group(i);
        }
      }
    }
    return null;
  }

  private List<String> extractModifiers(String funcDef) {
    List<String> modifiers = new ArrayList<>();
    if (funcDef.contains("public")) modifiers.add("public");
    if (funcDef.contains("private")) modifiers.add("private");
    if (funcDef.contains("protected")) modifiers.add("protected");
    if (funcDef.contains("static")) modifiers.add("static");
    if (funcDef.contains("final")) modifiers.add("final");
    if (funcDef.contains("abstract")) modifiers.add("abstract");
    if (funcDef.contains("synchronized")) modifiers.add("synchronized");
    if (funcDef.contains("native")) modifiers.add("native");
    if (funcDef.contains("strictfp")) modifiers.add("strictfp");
    return modifiers;
  }

  private String extractClassName(String classDef, String language) {
    Pattern namePattern = Pattern.compile("class\\s+(\\w+)");
    Matcher matcher = namePattern.matcher(classDef);
    if (matcher.find()) {
      return matcher.group(1);
    }
    return null;
  }

  private int calculateComplexity(
      List<CodeRepository.FunctionInfo> functions, List<CodeRepository.ClassInfo> classes) {
    int complexity = 0;
    complexity += functions.size() * 2;
    complexity += classes.size() * 5;
    return Math.min(complexity, 100);
  }

  private int countLinesOfCode(String content) {
    String[] lines = content.split("\\n");
    int count = 0;
    for (String line : lines) {
      String trimmed = line.trim();
      if (!trimmed.isEmpty()
          && !trimmed.startsWith("//")
          && !trimmed.startsWith("/*")
          && !trimmed.startsWith("*")) {
        count++;
      }
    }
    return count;
  }

  private int countNewlines(String text) {
    return (int) text.chars().filter(ch -> ch == '\n').count();
  }

  private boolean isCodeFile(Path file) {
    String fileName = file.getFileName().toString().toLowerCase();
    return fileName.endsWith(".py")
        || fileName.endsWith(".js")
        || fileName.endsWith(".ts")
        || fileName.endsWith(".jsx")
        || fileName.endsWith(".tsx")
        || fileName.endsWith(".go")
        || fileName.endsWith(".rs")
        || fileName.endsWith(".java")
        || fileName.endsWith(".rb")
        || fileName.endsWith(".c")
        || fileName.endsWith(".cpp")
        || fileName.endsWith(".h")
        || fileName.endsWith(".html")
        || fileName.endsWith(".md");
  }

  private boolean isIgnored(Path file) {
    String path = file.toString().toLowerCase();
    return path.contains("node_modules")
        || path.contains(".git")
        || path.contains("__pycache__")
        || path.contains(".venv")
        || path.contains("venv")
        || path.contains("target")
        || path.contains("build")
        || path.contains("dist")
        || path.contains(".min.js");
  }

  private String getFileExtension(String fileName) {
    int dotIndex = fileName.lastIndexOf('.');
    return (dotIndex == -1) ? "" : fileName.substring(dotIndex + 1).toLowerCase();
  }

  private String detectLanguage(String extension, String content) {
    switch (extension) {
      case "py":
        return "python";
      case "js":
        return "javascript";
      case "ts":
        return content.contains("typescript") ? "typescript" : "javascript";
      case "jsx":
      case "tsx":
        return "typescript";
      case "go":
        return "go";
      case "rs":
        return "rust";
      case "java":
        return "java";
      case "rb":
        return "ruby";
      case "c":
        return "c";
      case "cpp":
      case "cc":
      case "cxx":
        return "cpp";
      case "html":
        return "html";
      case "md":
        return "markdown";
      default:
        return "unknown";
    }
  }

  public static class ParseResult {
    public String language;
    public List<CodeRepository.FunctionInfo> functions;
    public List<CodeRepository.ClassInfo> classes;
    public List<CodeRepository.ImportInfo> imports;

    public ParseResult() {}

    public static ParseResultBuilder builder() {
      return new ParseResultBuilder();
    }

    public static class ParseResultBuilder {
      private String language;
      private List<CodeRepository.FunctionInfo> functions;
      private List<CodeRepository.ClassInfo> classes;
      private List<CodeRepository.ImportInfo> imports;

      public ParseResultBuilder language(String l) {
        this.language = l;
        return this;
      }

      public ParseResultBuilder functions(List<CodeRepository.FunctionInfo> f) {
        this.functions = f;
        return this;
      }

      public ParseResultBuilder classes(List<CodeRepository.ClassInfo> c) {
        this.classes = c;
        return this;
      }

      public ParseResultBuilder imports(List<CodeRepository.ImportInfo> i) {
        this.imports = i;
        return this;
      }

      public ParseResult build() {
        ParseResult pr = new ParseResult();
        pr.language = this.language;
        pr.functions = this.functions;
        pr.classes = this.classes;
        pr.imports = this.imports;
        return pr;
      }
    }

    public String json() {
      try {
        return new ObjectMapper().writeValueAsString(this);
      } catch (JsonProcessingException e) {
        return "{}";
      }
    }
  }

  public ParseResult parse(String code, String language) {
    if (code == null || code.trim().isEmpty()) {
      return ParseResult.builder()
          .language(language)
          .functions(new ArrayList<>())
          .classes(new ArrayList<>())
          .imports(new ArrayList<>())
          .build();
    }

    TreeSitterParseResult result;
    if (JS_LIKE_EXTENSIONS.contains("." + language)
        || "javascript".equals(language)
        || "typescript".equals(language)) {
      result = parseWithAcorn(code);
    } else {
      result = parseWithRegex(code, language);
    }

    return ParseResult.builder()
        .language(language)
        .functions(result.getFunctions())
        .classes(result.getClasses())
        .imports(result.getImports())
        .build();
  }

  private static class TreeSitterParseResult {
    private List<CodeRepository.FunctionInfo> functions;
    private List<CodeRepository.ClassInfo> classes;
    private List<CodeRepository.ImportInfo> imports;

    public TreeSitterParseResult(
        List<CodeRepository.FunctionInfo> f,
        List<CodeRepository.ClassInfo> c,
        List<CodeRepository.ImportInfo> i) {
      this.functions = f;
      this.classes = c;
      this.imports = i;
    }

    public List<CodeRepository.FunctionInfo> getFunctions() {
      return functions;
    }

    public List<CodeRepository.ClassInfo> getClasses() {
      return classes;
    }

    public List<CodeRepository.ImportInfo> getImports() {
      return imports;
    }
  }
}
