package com.supremeai.skill;

import jakarta.annotation.PostConstruct;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
public class SkillEngine {

  private static final Logger log = LoggerFactory.getLogger(SkillEngine.class);

  // স্কিল নাম থেকে স্কিল অবজেক্টের ম্যাপিং (পরিবর্তে HashMap ব্যবহার করা হয়)
  private final Map<String, Skill> skillRegistry = new ConcurrentHashMap<>();
  
  // ট্রিগার কী-ওয়ার্ড থেকে স্কিলের ইনভার্টেড ইনডেক্স - O(1) সার্চের জন্য
  private final Map<String, Set<Skill>> triggerIndex = new ConcurrentHashMap<>();

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
        stream
            .filter(p -> p.getFileName().toString().equalsIgnoreCase("SKILL.md"))
            .forEach(
                p -> {
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

  // স্কিল নিবন্ধন করার সময় ট্রিগার ইনডেক্সও আপডেট করা হয়
  public void registerSkill(String skillMdContent) {
    Skill skill = parseSkillMd(skillMdContent);
    if (skill.getName() != null && !skill.getName().isEmpty()) {
      skillRegistry.put(skill.getName(), skill);
      // ট্রিগার ইনডেক্স আপডেট করা হচ্ছে - O(1) রুটিংএর জন্য
      indexTriggers(skill);
      log.info("[SkillEngine] Registered skill '{}' with {} triggers", skill.getName(), skill.getTriggers().size());
    }
  }

  // একটি স্কিলের ট্রিগারগুলোকে ইনডেক্সে যুক্ত করা হয়
  private void indexTriggers(Skill skill) {
    for (String trigger : skill.getTriggers()) {
      String normalizedTrigger = trigger.toLowerCase();
      // প্রতিটি ট্রিগার কী-ওয়ার্ডের জন্য স্কিল সেট সংযোজন করা হয়
      triggerIndex.computeIfAbsent(normalizedTrigger, k -> ConcurrentHashMap.newKeySet()).add(skill);
      // স্কিলের নামও ট্রিগার হিসেবে যুক্ত করা হয় (স্কিল ডাইরেক্ট ম্যাচিংয়ের জন্য)
      triggerIndex.computeIfAbsent(skill.getName().toLowerCase(), k -> ConcurrentHashMap.newKeySet()).add(skill);
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

  // অপটিমাইজড স্কিল ম্যাচিং - O(1) সময় গতি সহ ইনভার্টেড ইনডেক্স ব্যবহার করে
  public Skill matchSkill(String userInput) {
    if (userInput == null || userInput.trim().isEmpty()) return null;
    
    String lowerInput = userInput.toLowerCase();
    
    // ইনভার্টেড ইনডেক্স থেকে সরাসরি স্কিল খুঁজে বের করা হয়
    for (String word : lowerInput.split("[\\s\\p{Punct}]+")) {
      Set<Skill> matchedSkills = triggerIndex.get(word);
      if (matchedSkills != null && !matchedSkills.isEmpty()) {
        // প্রথমে মেলে যাওয়া স্কিলটি রিটার্ন করা হয়
        return matchedSkills.iterator().next();
      }
    }
    
    // যদি ইনডেক্সে ম্যাচ না পায়, তাহলে ফলব্যাক হিসাবে সম্পূর্ণ স্ক্যান করা হয়
    return skillRegistry.values().stream()
        .filter(skill -> skill.matches(lowerInput))
        .findFirst()
        .orElse(null);
  }

  // স্কিল ম্যাচিংয়ের দ্রুত পারফরম্যান্স মনিটরিংএর জন্য
  public int getTriggerIndexSize() {
    return triggerIndex.size();
  }
  
  public int getSkillRegistrySize() {
    return skillRegistry.size();
  }

  static class Skill {
    private String name;
    private String description;
    private List<String> triggers = new ArrayList<>();

    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getDescription() {
      return description;
    }

    public void setDescription(String description) {
      this.description = description;
    }

    public List<String> getTriggers() {
      return triggers;
    }

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
