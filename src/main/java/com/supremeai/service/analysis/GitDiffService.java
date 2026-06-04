package com.supremeai.service.analysis;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class GitDiffService {

  private static final Logger log = LoggerFactory.getLogger(GitDiffService.class);

  public List<FileDiff> computeDiff(String repoPath, String baselineCommit, String currentCommit)
      throws IOException, InterruptedException {
    List<String> cmd = new ArrayList<>();
    cmd.add("git");
    cmd.add("diff");
    cmd.add("--unified=3");

    if (baselineCommit != null && !baselineCommit.isEmpty()) {
      cmd.add(baselineCommit);
    }
    if (currentCommit != null && !currentCommit.isEmpty()) {
      cmd.add(currentCommit);
    }
    cmd.add("--");

    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(new File(repoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();
    String output;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      output = reader.lines().collect(Collectors.joining("\n"));
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException("git diff failed with exit code " + exitCode);
    }

    return parseDiffOutput(output);
  }

  public List<FileDiff> computeWorkingTreeDiff(String repoPath)
      throws IOException, InterruptedException {
    List<String> cmd = List.of("git", "diff", "--unified=3", "--");
    ProcessBuilder pb = new ProcessBuilder(cmd);
    pb.directory(new File(repoPath));
    pb.redirectErrorStream(true);

    Process process = pb.start();
    String output;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      output = reader.lines().collect(Collectors.joining("\n"));
    }

    int exitCode = process.waitFor();
    if (exitCode != 0) {
      throw new IOException("git diff failed with exit code " + exitCode);
    }

    return parseDiffOutput(output);
  }

  public String getCurrentCommitHash(String repoPath) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder("git", "rev-parse", "HEAD");
    pb.directory(new File(repoPath));
    Process process = pb.start();

    String hash;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      hash = reader.readLine();
    }

    int exitCode = process.waitFor();
    if (exitCode != 0 || hash == null) {
      throw new IOException("Failed to get current commit hash");
    }

    return hash.trim();
  }

  public List<FileDiff> parseDiffOutput(String diffOutput) {
    List<FileDiff> diffs = new ArrayList<>();
    if (diffOutput == null || diffOutput.isEmpty()) {
      return diffs;
    }

    Pattern fileHeaderPattern = Pattern.compile("^diff --git a/(.+) b/(.+)$", Pattern.MULTILINE);
    Matcher fileMatcher = fileHeaderPattern.matcher(diffOutput);

    int lastEnd = 0;
    String lastFileName = null;
    ChangeType lastChangeType = ChangeType.MODIFY;

    while (fileMatcher.find()) {
      if (lastFileName != null) {
        String hunk = diffOutput.substring(lastEnd, fileMatcher.start());
        FileDiff diff = parseFileHunks(lastFileName, hunk, lastChangeType);
        if (diff != null) {
          diffs.add(diff);
        }
      }

      String fileA = fileMatcher.group(1);
      String fileB = fileMatcher.group(2);

      if (fileA.equals("/dev/null") || fileA.equals("dev/null")) {
        lastChangeType = ChangeType.ADD;
        lastFileName = fileB;
      } else if (fileB.equals("/dev/null") || fileB.equals("dev/null")) {
        lastChangeType = ChangeType.DELETE;
        lastFileName = fileA;
      } else if (!fileA.equals(fileB)) {
        lastChangeType = ChangeType.RENAME;
        lastFileName = fileB;
      } else {
        lastChangeType = ChangeType.MODIFY;
        lastFileName = fileA;
      }

      lastEnd = fileMatcher.end();
    }

    if (lastFileName != null) {
      String hunk = diffOutput.substring(lastEnd);
      FileDiff diff = parseFileHunks(lastFileName, hunk, lastChangeType);
      if (diff != null) {
        diffs.add(diff);
      }
    }

    log.debug("Parsed {} file diffs", diffs.size());
    return diffs;
  }

  private FileDiff parseFileHunks(String fileName, String hunk, ChangeType changeType) {
    List<LineRange> changedRanges = new ArrayList<>();

    Pattern rangePattern =
        Pattern.compile("^@@ -(\\d+)(?:,(\\d+))? \\+(\\d+)(?:,(\\d+))? @@", Pattern.MULTILINE);
    Matcher rangeMatcher = rangePattern.matcher(hunk);

    while (rangeMatcher.find()) {
      int newStart = Integer.parseInt(rangeMatcher.group(3));
      int newCount = rangeMatcher.group(4) != null ? Integer.parseInt(rangeMatcher.group(4)) : 1;
      changedRanges.add(
          LineRange.builder().startLine(newStart).endLine(newStart + newCount - 1).build());
    }

    return FileDiff.builder()
        .fileName(fileName)
        .changeType(changeType)
        .changedLineRanges(changedRanges)
        .rawDiff(hunk)
        .build();
  }

  public List<String> getChangedFiles(String repoPath, String baselineCommit, String currentCommit)
      throws IOException, InterruptedException {
    List<FileDiff> diffs = computeDiff(repoPath, baselineCommit, currentCommit);
    return diffs.stream().map(FileDiff::getFileName).distinct().collect(Collectors.toList());
  }

  public static class FileDiff {
    private String fileName;
    private ChangeType changeType;
    private List<LineRange> changedLineRanges;
    private String rawDiff;

    public FileDiff() {}

    public FileDiff(
        String fileName, ChangeType changeType, List<LineRange> changedLineRanges, String rawDiff) {
      this.fileName = fileName;
      this.changeType = changeType;
      this.changedLineRanges = changedLineRanges;
      this.rawDiff = rawDiff;
    }

    public String getFileName() {
      return fileName;
    }

    public void setFileName(String fileName) {
      this.fileName = fileName;
    }

    public ChangeType getChangeType() {
      return changeType;
    }

    public void setChangeType(ChangeType changeType) {
      this.changeType = changeType;
    }

    public List<LineRange> getChangedLineRanges() {
      return changedLineRanges;
    }

    public void setChangedLineRanges(List<LineRange> changedLineRanges) {
      this.changedLineRanges = changedLineRanges;
    }

    public String getRawDiff() {
      return rawDiff;
    }

    public void setRawDiff(String rawDiff) {
      this.rawDiff = rawDiff;
    }

    public static FileDiffBuilder builder() {
      return new FileDiffBuilder();
    }

    public static class FileDiffBuilder {
      private String fileName;
      private ChangeType changeType;
      private List<LineRange> changedLineRanges;
      private String rawDiff;

      public FileDiffBuilder fileName(String f) {
        this.fileName = f;
        return this;
      }

      public FileDiffBuilder changeType(ChangeType c) {
        this.changeType = c;
        return this;
      }

      public FileDiffBuilder changedLineRanges(List<LineRange> r) {
        this.changedLineRanges = r;
        return this;
      }

      public FileDiffBuilder rawDiff(String d) {
        this.rawDiff = d;
        return this;
      }

      public FileDiff build() {
        return new FileDiff(fileName, changeType, changedLineRanges, rawDiff);
      }
    }
  }

  public static class LineRange {
    private int startLine;
    private int endLine;

    public LineRange() {}

    public LineRange(int startLine, int endLine) {
      this.startLine = startLine;
      this.endLine = endLine;
    }

    public int getStartLine() {
      return startLine;
    }

    public void setStartLine(int startLine) {
      this.startLine = startLine;
    }

    public int getEndLine() {
      return endLine;
    }

    public void setEndLine(int endLine) {
      this.endLine = endLine;
    }

    public static LineRangeBuilder builder() {
      return new LineRangeBuilder();
    }

    public static class LineRangeBuilder {
      private int startLine;
      private int endLine;

      public LineRangeBuilder startLine(int s) {
        this.startLine = s;
        return this;
      }

      public LineRangeBuilder endLine(int e) {
        this.endLine = e;
        return this;
      }

      public LineRange build() {
        return new LineRange(startLine, endLine);
      }
    }
  }

  public enum ChangeType {
    ADD,
    MODIFY,
    DELETE,
    RENAME
  }
}
