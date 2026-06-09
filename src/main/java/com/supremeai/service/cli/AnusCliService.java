package com.supremeai.service.cli;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class AnusCliService {
  private static final Logger logger = LoggerFactory.getLogger(AnusCliService.class);

  public String runScript(String scriptName, List<String> args)
      throws IOException, InterruptedException {
    String python = resolvePython();
    List<String> command = new ArrayList<>();
    command.add(python);
    command.add(scriptName);
    command.addAll(args != null ? args : List.of());
    return exec(command);
  }

  public boolean isConfigured() {
    try {
      String python = resolvePython();
      ProcessBuilder pb = new ProcessBuilder(python, "--version");
      Process p = pb.start();
      int code = p.waitFor();
      return code == 0;
    } catch (Exception e) {
      return false;
    }
  }

  private String resolvePython() {
    List<String> candidates = List.of("python3", "python", "py");
    for (String c : candidates) {
      try {
        ProcessBuilder pb = new ProcessBuilder(c, "--version");
        pb.redirectErrorStream(true);
        Process p = pb.start();
        int code = p.waitFor();
        if (code == 0) return c;
      } catch (Exception ignored) {
      }
    }
    return "python3";
  }

  private String exec(List<String> command) throws IOException, InterruptedException {
    ProcessBuilder pb = new ProcessBuilder(command);
    pb.redirectErrorStream(true);
    Process process = pb.start();
    String output;
    try (BufferedReader reader =
        new BufferedReader(new InputStreamReader(process.getInputStream()))) {
      output = reader.lines().collect(Collectors.joining("\n"));
    }
    int exitCode = process.waitFor();
    if (exitCode != 0) {
      logger.warn("[AnusCLI] Script exited with code: {} command={}", exitCode, command);
    }
    return output;
  }
}
