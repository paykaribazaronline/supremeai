package com.supremeai.skill;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import jakarta.annotation.PostConstruct;
import java.util.*;
import java.nio.file.*;

@Component
public class SkillEngine {
    
    private final Map<String, Skill> skillRegistry = new HashMap<>();
    
    @Value("${app.skills.base-path:#{null}}")
    private String configuredBasePath;
    
    @Value("${app.skills.config-path:config/skills-local.json}")
    private String skillsConfigPath;
    
    @Value("${app.skills.lock-path:config/skills-lock.json}")
    private String skillsLockPath;
    
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private Environment environment;
    
    @PostConstruct
    public void init() {
        List<String> candidates = new ArrayList<>();
        if (configuredBasePath != null && !configuredBasePath.isBlank()) {
            candidates.add(configuredBasePath);
        }
        candidates.add(resolveDefaultBasePath());
        candidates.add(".agents/skills");
        for (String basePath : candidates) {
            scanSkills(basePath);
        }
        loadSkillsFromConfig(skillsConfigPath);
        loadSkillsFromConfig(skillsLockPath);
    }
    
    private String resolveDefaultBasePath() {
        if (environment != null) {
            String explicit = environment.getProperty("user.home") + "/supremeai/.agents/skills";
            return explicit;
        }
        return ".agents/skills";
    }
    
    private void loadSkillsFromConfig(String configPath) {
        try {
            Path path = Paths.get(configPath);
            if (!Files.exists(path)) return;
            String content = Files.readString(path);
            String json = content.substring(content.indexOf('{'));
            // extremely lightweight parsing without external json libs
            String[] skillEntries = json.split("\"");
            for (int i = 0; i < skillEntries.length - 1; i++) {
                if ("skillPath".equals(skillEntries[i])) {
                    String skillPath = skillEntries[i + 1];
                    Path skillFile = path.getParent().resolve(skillPath).normalize();
                    if (Files.exists(skillFile)) {
                        String skillMd = Files.readString(skillFile);
                        registerSkill(skillMd);
                    }
                }
            }
        } catch (Exception e) {
            // ignore
        }
    }
    
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
    
    public void registerSkill(String skillMdContent) {
        Skill skill = parseSkillMd(skillMdContent);
        if (skill.getName() != null && !skill.getName().isEmpty()) {
            skillRegistry.put(skill.getName(), skill);
        }
    }
    
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
    
    public Skill matchSkill(String userInput) {
        if (userInput == null) return null;
        String lowerInput = userInput.toLowerCase();
        return skillRegistry.values().stream()
            .filter(skill -> skill.matches(lowerInput))
            .findFirst()
            .orElse(null);
    }
    
    static class Skill {
        private String name;
        private String description;
        private List<String> triggers = new ArrayList<>();
        
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public List<String> getTriggers() { return triggers; }
        
        public boolean matches(String input) {
            if (input == null) return false;
            if (name != null && input.contains(name.toLowerCase())) return true;
            for (String t : triggers) {
                if (t == null) continue;
                if (input.contains(t.toLowerCase())) return true;
            }
            return false;
        }
    }
}
