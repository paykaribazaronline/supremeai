package org.example.generator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TemplateLoader {

    public String loadTemplate(String templateName) throws IOException {
        Path templatePath = Paths.get("src/main/resources/templates", templateName);
        StringBuilder templateContent = new StringBuilder();

        try (BufferedReader reader = Files.newBufferedReader(templatePath)) {
            String line;
            while ((line = reader.readLine()) != null) {
                templateContent.append(line).append("\n");
            }
        }

        return templateContent.toString();
    }
}