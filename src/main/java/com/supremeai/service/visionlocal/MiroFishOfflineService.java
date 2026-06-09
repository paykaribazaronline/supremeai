package com.supremeai.service.visionlocal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class MiroFishOfflineService {
    private static final Logger logger = LoggerFactory.getLogger(MiroFishOfflineService.class);

    @Value("${mirofish.model.path:./mirofish-offline}")
    private String modelPath;

    public String detect(String imagePath) {
        if (!Files.exists(Paths.get(modelPath))) {
            logger.warn("[MiroFish] Model not found at: {}", modelPath);
            return "skipped_model_not_found";
        }
        Path full = Paths.get(modelPath, "detect.py");
        if (!Files.exists(full)) {
            logger.warn("[MiroFish] detect.py not found");
            return "skipped_script_not_found";
        }
        try {
            java.util.List<String> cmd = java.util.List.of("python3", full.toString(), imagePath);
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectErrorStream(true);
            Process p = pb.start();
            String out;
            try (var reader = new java.io.BufferedReader(new java.io.InputStreamReader(p.getInputStream()))) {
                out = reader.lines().collect(java.util.stream.Collectors.joining("\n"));
            }
            int code = p.waitFor();
            if (code != 0) logger.warn("[MiroFish] detect.py exit code: {}", code);
            return out;
        } catch (Exception e) {
            logger.error("[MiroFish] detection failed", e);
            return "error:" + e.getMessage();
        }
    }

    public boolean isConfigured() {
        return Files.exists(Paths.get(modelPath));
    }
}
