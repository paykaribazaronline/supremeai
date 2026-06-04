package com.supremeai.service.analysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class CodeChunkerService {
  private static final Logger log = LoggerFactory.getLogger(CodeChunkerService.class);

  private static final int DEFAULT_CHUNK_SIZE = 100;
  private static final int DEFAULT_OVERLAP = 10;

  public List<CodeChunkData> chunkFile(File file, String relativePath) throws IOException {
    return chunkFile(file, relativePath, DEFAULT_CHUNK_SIZE, DEFAULT_OVERLAP);
  }

  public List<CodeChunkData> chunkFile(File file, String relativePath, int chunkSize, int overlap)
      throws IOException {
    List<String> lines = readFileLines(file);
    String language = detectLanguage(file.getName());
    List<CodeChunkData> chunks = new ArrayList<>();

    int totalLines = lines.size();
    int step = chunkSize - overlap;
    if (step <= 0) step = chunkSize;

    int chunkIndex = 0;
    for (int start = 0; start < totalLines; start += step) {
      int end = Math.min(start + chunkSize, totalLines);
      List<String> chunkLines = lines.subList(start, end);
      String content = String.join("\n", chunkLines);
      String hash = sha256(content);

      CodeChunkData chunk =
          CodeChunkData.builder()
              .id(relativePath + "_chunk_" + chunkIndex)
              .file(relativePath)
              .startLine(start + 1)
              .endLine(end)
              .content(content)
              .hash(hash)
              .language(language)
              .build();

      chunks.add(chunk);
      chunkIndex++;

      if (end >= totalLines) break;
    }

    log.debug(
        "Chunked file {} into {} chunks (size={}, overlap={})",
        relativePath,
        chunks.size(),
        chunkSize,
        overlap);
    return chunks;
  }

  public List<CodeChunkData> chunkFileSemantic(File file, String relativePath) throws IOException {
    List<String> lines = readFileLines(file);
    String language = detectLanguage(file.getName());
    List<CodeChunkData> chunks = new ArrayList<>();

    List<int[]> boundaries = detectSemanticBoundaries(lines, language);

    int chunkIndex = 0;
    int currentStart = 0;

    for (int[] boundary : boundaries) {
      int boundaryLine = boundary[0];
      if (boundaryLine - currentStart >= DEFAULT_CHUNK_SIZE / 2) {
        int endLine = Math.min(boundaryLine, lines.size());
        List<String> chunkLines = lines.subList(currentStart, endLine);
        String content = String.join("\n", chunkLines);

        CodeChunkData chunk =
            CodeChunkData.builder()
                .id(relativePath + "_chunk_" + chunkIndex)
                .file(relativePath)
                .startLine(currentStart + 1)
                .endLine(endLine)
                .content(content)
                .hash(sha256(content))
                .language(language)
                .build();

        chunks.add(chunk);
        chunkIndex++;
        currentStart = Math.max(endLine - DEFAULT_OVERLAP, currentStart + 1);
      }
    }

    if (currentStart < lines.size()) {
      List<String> chunkLines = lines.subList(currentStart, lines.size());
      String content = String.join("\n", chunkLines);
      chunks.add(
          CodeChunkData.builder()
              .id(relativePath + "_chunk_" + chunkIndex)
              .file(relativePath)
              .startLine(currentStart + 1)
              .endLine(lines.size())
              .content(content)
              .hash(sha256(content))
              .language(language)
              .build());
    }

    log.debug("Semantically chunked file {} into {} chunks", relativePath, chunks.size());
    return chunks;
  }

  private List<int[]> detectSemanticBoundaries(List<String> lines, String language) {
    List<int[]> boundaries = new ArrayList<>();
    for (int i = 0; i < lines.size(); i++) {
      String trimmed = lines.get(i).trim();
      if (isMethodBoundary(trimmed, language) || isClassBoundary(trimmed, language)) {
        boundaries.add(new int[] {i, 1});
      }
    }
    return boundaries;
  }

  private boolean isMethodBoundary(String line, String language) {
    return line.matches(
            "^(public|private|protected|static|\\s)*(void|int|String|boolean|long|double|float|char|byte|short|var|def|fn|func)\\s+\\w+\\s*\\(.*\\).*\\{?\\s*$")
        || line.matches("^(function|def|fn|func)\\s+\\w+\\s*\\(.*\\).*\\{?\\s*$");
  }

  private boolean isClassBoundary(String line, String language) {
    return line.matches(
        "^(public|private|protected|\\s)*(class|interface|enum|struct|trait|impl)\\s+\\w+.*\\{?\\s*$");
  }

  private String detectLanguage(String filename) {
    String lower = filename.toLowerCase();
    if (lower.endsWith(".java")) return "java";
    if (lower.endsWith(".js")) return "javascript";
    if (lower.endsWith(".ts") || lower.endsWith(".tsx")) return "typescript";
    if (lower.endsWith(".py")) return "python";
    if (lower.endsWith(".go")) return "go";
    if (lower.endsWith(".rb")) return "ruby";
    if (lower.endsWith(".php")) return "php";
    if (lower.endsWith(".cs")) return "csharp";
    if (lower.endsWith(".c") || lower.endsWith(".h")) return "c";
    if (lower.endsWith(".cpp") || lower.endsWith(".hpp")) return "cpp";
    if (lower.endsWith(".kt")) return "kotlin";
    if (lower.endsWith(".swift")) return "swift";
    if (lower.endsWith(".scala")) return "scala";
    return "unknown";
  }

  private List<String> readFileLines(File file) throws IOException {
    try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
      return reader.lines().collect(Collectors.toList());
    }
  }

  private String sha256(String input) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hash = digest.digest(input.getBytes());
      StringBuilder hex = new StringBuilder();
      for (byte b : hash) {
        hex.append(String.format("%02x", b));
      }
      return hex.toString();
    } catch (Exception e) {
      return String.valueOf(input.hashCode());
    }
  }
}
