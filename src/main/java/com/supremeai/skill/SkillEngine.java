package com.supremeai.skill;

import org.springframework.stereotype.Component;
import java.util.*;
import java.nio.file.*;

/**
 * Skill Engine - Plan 24 Phase 1
 * Handles SKILL.md auto-discovery and registration (Pinokio-compatible)
 */
@Component
public class SkillEngine {
    
    private final Map<String, Skill> skillRegistry = new HashMap<>();
    
    /**
     * Register skill from SKILL.md content
     */
    public void registerSkill(String skillMdContent) {
        Skill skill = parseSkillMd(skillMdContent);
        skillRegistry.put(skill.getName(), skill);
    }
    
    /**
     * Parse SKILL.md content (simplified Pinokio format)
     */
    private Skill parseSkillMd(String content) {
        Skill skill = new Skill();
        for (String line : content.split("\n")) {
            if (line.startsWith("name:")) {
                skill.setName(line.replace("name:", "").trim());
            } else if (line.startsWith("description:")) {
                skill.setDescription(line.replace("description:", "").trim());
            } else if (line.startsWith("## Triggers")) {
                // TODO: Parse triggers
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
        public boolean matches(String input) {
            return input.toLowerCase().contains(name.toLowerCase());
        }
    }
}
