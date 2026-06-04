package com.supremeai.service;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import javax.tools.*;
import org.springframework.stereotype.Service;

@Service
public class CodeValidationService {

  private final JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();

  public Map<String, Object> validate(Map<String, String> files) {
    Map<String, Object> result = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    String[] requiredFiles = {
      "build.gradle.kts", "src/main/java/com/example/generated/GeneratedAppApplication.java"
    };

    for (String required : requiredFiles) {
      if (!files.containsKey(required)) {
        errors.add("Missing required file: " + required);
      }
    }

    if (files.containsKey("build.gradle.kts")) {
      String content = files.get("build.gradle.kts");
      if (!content.contains("plugins")) {
        warnings.add("build.gradle.kts missing 'plugins' block");
      }
      if (!content.contains("dependencies")) {
        warnings.add("build.gradle.kts missing 'dependencies' block");
      }
    }

    if (files.containsKey("src/main/java/com/example/generated/GeneratedAppApplication.java")) {
      String content =
          files.get("src/main/java/com/example/generated/GeneratedAppApplication.java");
      if (!content.contains("@SpringBootApplication")) {
        errors.add("Main class missing @SpringBootApplication");
      }
      if (!content.contains("main(")) {
        errors.add("Main class missing main method");
      }
    }

    result.put("valid", errors.isEmpty());
    result.put("errors", errors);
    result.put("warnings", warnings);
    result.put("fileCount", files.size());

    return result;
  }

  public Map<String, Object> validateWithCompilation(Map<String, String> files, String appName) {
    Map<String, Object> result = validate(files);
    List<String> compilationErrors = new ArrayList<>();

    try {
      String tempDir = writeToTempDirectory(files, appName + "-compile");
      Path tempPath = Path.of(tempDir);

      List<JavaFileObject> compilationUnits = new ArrayList<>();
      for (Map.Entry<String, String> entry : files.entrySet()) {
        if (entry.getKey().endsWith(".java")) {
          compilationUnits.add(
              new SimpleJavaFileObject(
                  URI.create("string:///" + entry.getKey().replace(".java", "")),
                  JavaFileObject.Kind.SOURCE) {
                @Override
                public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                  return entry.getValue();
                }
              });
        }
      }

      StringWriter compilerOutput = new StringWriter();
      DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();

      JavaCompiler.CompilationTask task =
          compiler.getTask(
              compilerOutput, null, diagnostics, getCompilationOptions(), null, compilationUnits);

      Boolean compiled = task.call();
      result.put("compilationSuccess", compiled);

      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        compilationErrors.add(
            String.format(
                "%s:%d - %s",
                diagnostic.getKind(),
                diagnostic.getLineNumber(),
                diagnostic.getMessage(Locale.ENGLISH)));
      }

      if (!compiled) {
        result.put("compilationErrors", compilationErrors);
        ((List<String>) result.get("errors")).addAll(compilationErrors);
      }

    } catch (IOException e) {
      compilationErrors.add("Compilation setup failed: " + e.getMessage());
      result.put("compilationErrors", compilationErrors);
    }

    result.put("valid", ((List<String>) result.get("errors")).isEmpty());
    return result;
  }

  private List<String> getCompilationOptions() {
    List<String> options = new ArrayList<>();
    options.add("-classpath");
    options.add(System.getProperty("java.class.path"));
    options.add("-source");
    options.add("21");
    options.add("-target");
    options.add("21");
    options.add("-Xlint:unchecked");
    return options;
  }

  public String writeToTempDirectory(Map<String, String> files, String appName) throws IOException {
    Path baseDir =
        Path.of(
            System.getProperty("java.io.tmpdir"),
            "supremeai-" + appName + "-" + System.currentTimeMillis());
    Files.createDirectories(baseDir);

    for (Map.Entry<String, String> entry : files.entrySet()) {
      Path outFile = baseDir.resolve(entry.getKey());
      Files.createDirectories(outFile.getParent());
      Files.writeString(outFile, entry.getValue(), StandardCharsets.UTF_8);
    }

    return baseDir.toAbsolutePath().toString();
  }

  public Map<String, Object> validateGradleSyntax(String gradleContent) {
    Map<String, Object> result = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (!gradleContent.contains("plugins")) {
      errors.add("Missing 'plugins' block - Gradle syntax error");
    }
    if (!gradleContent.contains("dependencies")) {
      warnings.add("Missing 'dependencies' block - may cause build failures");
    }

    if (gradleContent.contains("dependencies") && !gradleContent.contains(")")) {
      errors.add("Unclosed parentheses in dependencies block");
    }

    result.put("valid", errors.isEmpty());
    result.put("errors", errors);
    result.put("warnings", warnings);
    return result;
  }

  public Map<String, Object> validateJavaSyntax(String javaContent, String className) {
    Map<String, Object> result = new LinkedHashMap<>();
    List<String> errors = new ArrayList<>();
    List<String> warnings = new ArrayList<>();

    if (javaContent == null || javaContent.isBlank()) {
      errors.add("Empty Java file");
      result.put("valid", false);
      result.put("errors", errors);
      result.put("warnings", warnings);
      return result;
    }

    int openBraces = (int) javaContent.chars().filter(ch -> ch == '{').count();
    int closeBraces = (int) javaContent.chars().filter(ch -> ch == '}').count();

    if (openBraces != closeBraces) {
      errors.add("Mismatched braces: " + openBraces + " opening, " + closeBraces + " closing");
    }

    if (!javaContent.contains("public class " + className)
        && !javaContent.contains("class " + className)) {
      warnings.add("Class name '" + className + "' may not match file name");
    }

    result.put("valid", errors.isEmpty());
    result.put("errors", errors);
    result.put("warnings", warnings);
    return result;
  }
}
