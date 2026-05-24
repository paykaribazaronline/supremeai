package com.supremeai.skill;

import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.nio.file.*;

/**
 * Skill Engine - Plan 24 Phase 1
 * Handles SKILL.md auto-discovery and registration (Pinokio-compatible)
 */
@Component
public class SkillEngine {
    
    private final Map<String, Skill> skillRegistry = new HashMap<>();
    
    @PostConstruct
    public void init() {
        scanSkills("/home/nazifarabbu/supremeai/.agents/skills");
    }
    
    /**
     * Scan path recursively for SKILL.md files and register them
     */
    public void scanSkills(String basePath) {
        try {
            Path path = Paths.get(basePath);
            if (!Files.exists(path)) return;
            
            try (java.util.stream.Stream<Path> stream = Files.walk(path)) {
                stream.filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                      .forEach(p -> {
                          try {
                              String content = Files.readString(p);
                              registerSkill(content);
                          } catch (Exception e) {
                              // ignore
                          }
                      });
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
    public Map<String, Skill> getSkillRegistry() {
        return skillRegistry;
    }
    
    /**
     * Register skill from SKILL.md content
     */
    public void registerSkill(String skillMdContent) {
        Skill skill = parseSkillMd(skillMdContent);
        if (skill.getName() != null && !skill.getName().isEmpty()) {
            skillRegistry.put(skill.getName(), skill);
        }
    }
    
    /**
     * Parse SKILL.md content (simplified Pinokio format)
     */
    private Skill parseSkillMd(String content) {
        Skill skill = new Skill();
        boolean parsingTriggers = false;
        for (String line : content.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.startsWith("name:")) {
                skill.setName(trimmed.replace("name:", "").trim());
                parsingTriggers = false;
            } else if (trimmed.startsWith("description:")) {
                skill.setDescription(trimmed.replace("description:", "").trim());
                parsingTriggers = false;
            } else if (trimmed.startsWith("## Triggers")) {
                parsingTriggers = true;
            } else if (parsingTriggers && trimmed.startsWith("-")) {
                skill.getTriggers().add(trimmed.substring(1).trim());
            } else if (parsingTriggers && trimmed.startsWith("##")) {
                parsingTriggers = false;
            }
        }
        return skill;
    }
    
    /**
     * Match user input to registered skills
     */
    public Skill matchSkill(String userInput) {
        return skillRegistry.values().stream()
            .filter(skill -> skill.matches(userInput))
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Skill POJO
     */
    static class Skill {
        private String name;
        private String description;
        private List<String> triggers = new ArrayList<>();
        
        // Getters/setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getTriggers() { return triggers; }
        
        public boolean matches(String input) {
            String lowerInput = input.toLowerCase();
            if (name != null && lowerInput.contains(name.toLowerCase())) return true;
            for (String t : triggers) {
                if (lowerInput.contains(t.toLowerCase())) return true;
            }
            return false;
        }
    }
}
