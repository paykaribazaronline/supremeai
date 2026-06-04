package com.supremeai.service;

import com.supremeai.model.InstalledSkill;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

@Component
public class SkillsSeedDataProvider {

  private static final Pattern SECTION_HEADER =
      Pattern.compile("^##\\s+[০-৯]+\\.\\s+(.*?)\\s*([🔴🟡🟢🔵])");
  private static final Pattern SKILL_ROW = Pattern.compile("^\\|\\s*\\[ \\]\\s+(.*?)\\s*\\|");

  public Flux<InstalledSkill> provideAllSkillSeeds() {
    try {
      List<InstalledSkill> skills = parseSkillsFromPlan();
      return Flux.fromIterable(skills);
    } catch (Exception e) {
      return Flux.error(new IllegalStateException("Failed to parse skill plan", e));
    }
  }

  List<InstalledSkill> parseSkillsFromPlan() throws IOException {
    List<String> lines =
        Files.readAllLines(
            Path.of("docs", "latest", "AI_SKILL_MARKETPLACE_INSTALL_PLAN.md"),
            StandardCharsets.UTF_8);

    List<InstalledSkill> skills = new ArrayList<>();
    String currentCategory = "Unknown";
    String currentPriority = "MEDIUM";
    int idCounter = 0;

    for (String line : lines) {
      Matcher sectionMatcher = SECTION_HEADER.matcher(line);
      if (sectionMatcher.find()) {
        currentCategory = extractCategory(sectionMatcher.group(1).trim());
        currentPriority = priorityFromEmoji(sectionMatcher.group(2));
        continue;
      }

      Matcher skillMatcher = SKILL_ROW.matcher(line);
      if (skillMatcher.find()) {
        String skillName = skillMatcher.group(1).trim();
        if (!skillName.isEmpty()) {
          idCounter++;
          InstalledSkill skill = new InstalledSkill();
          skill.setId("skill-" + idCounter);
          skill.setName(skillName);
          skill.setCategory(currentCategory);
          skill.setStatus("PENDING");
          skill.setPriority(currentPriority);
          skill.setInstallStatus(false);
          skill.setSecretRef("");
          skill.setCreatedAt(LocalDateTime.now());
          skills.add(skill);
        }
      }
    }

    return skills;
  }

  private String extractCategory(String raw) {
    String s = raw.replaceFirst("^[০-৯]+\\s*\\.\\s*", "");
    s = s.replaceAll("\\s*[🔴🟡🟢🔵]\\s*$", "");
    s = s.replaceAll("\\s*\\([^\\)]*\\)", "");
    return s.trim();
  }

  private String priorityFromEmoji(String emoji) {
    return switch (emoji) {
      case "🔴" -> "HIGH";
      case "🟡" -> "MEDIUM";
      case "🟢", "🔵" -> "LOW";
      default -> "MEDIUM";
    };
  }
}
