package com.supremeai.service;

import org.springframework.stereotype.Service;
import java.util.Base64;

@Service
public class VisionService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(VisionService.class);

    /**
     * Analyze a screenshot and extract UI elements or debug errors.
     * @param base64Image The image data in base64 format.
     * @return Analysis result string.
     */
    public String analyzeScreenshot(String base64Image) {
        // In a real implementation, this would call GPT-4V or Gemini Pro Vision
        log.info("Analyzing screenshot (length: {})", base64Image.length());
        
        // Mock analysis result
        return "UI Analysis: Found 3 buttons and 1 text field. Detected overflow error in the footer.";
    }

    public String convertImageToBase64(byte[] imageBytes) {
        return Base64.getEncoder().encodeToString(imageBytes);
    }
}
