package com.supremeai.service.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class Godmod3OdysseusService {
    private static final Logger logger = LoggerFactory.getLogger(Godmod3OdysseusService.class);
    private static final String GODMOD3_SCRIPT = "g0dm0d3_security_logic.py";
    private static final String ODYSSEUS_SCRIPT = "odysseus_tech_scanner.py";

    public String runGodmod3BoundaryCheck(String prompt) throws IOException, InterruptedException {
        String result = runLocalScript(GODMOD3_SCRIPT, List.of("--prompt", prompt, "--check", "boundary"));
        return result;
    }

    public String runOdysseusHardwareScan() throws IOException, InterruptedException {
        String result = runLocalScript(ODYSSEUS_SCRIPT, List.of("--scan", "hardware", "--format", "json"));
        return result;
    }

    public Map<String, Object> securityJailbreakAudit(String input) throws Exception {
        if (!Files.exists(Paths.get(GODMOD3_SCRIPT))) {
            return Map.of(
                    "status", "skipped",
                    "reason", "G0DM0D3 script not found at: " + GODMOD3_SCRIPT,
                    "input", input);
        }
        String output = runGodmod3BoundaryCheck(input);
        return Map.of("status", "complete", "godmod3_output", output, "input", input);
    }

    public Map<String, Object> techVulnerabilityScan() throws Exception {
        if (!Files.exists(Paths.get(ODYSSEUS_SCRIPT))) {
            return Map.of(
                    "status", "skipped",
                    "reason", "Odysseus script not found at: " + ODYSSEUS_SCRIPT);
        }
        String output = runOdysseusHardwareScan();
        return Map.of("status", "complete", "odysseus_output", output);
    }

    private String runLocalScript(String scriptName, List<String> args) throws IOException, InterruptedException {
        List<String> command = new ArrayList<>();
        command.add("python3");
        command.add(scriptName);
        command.addAll(args);
        ProcessBuilder pb = new ProcessBuilder(command);
        pb.redirectErrorStream(true);
        Process process = pb.start();
        String output;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            output = reader.lines().collect(Collectors.joining("\n"));
        }
        int exitCode = process.waitFor();
        if (exitCode != 0) {
            logger.warn("[G0DM0D3/Odysseus] Script exited with code: {}", exitCode);
        }
        return output;
    }
}
