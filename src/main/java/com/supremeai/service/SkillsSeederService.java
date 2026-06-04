package com.supremeai.service;

import com.supremeai.model.InstalledSkill;
import com.supremeai.repository.InstalledSkillRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

@Component
public class SkillsSeederService {

    private static final Logger log = LoggerFactory.getLogger(SkillsSeederService.class);

    @Autowired
    private InstalledSkillRepository installedSkillRepository;

    @Autowired
    private SkillsSeedDataProvider skillsSeedDataProvider;

    @EventListener(ApplicationReadyEvent.class)
    public void seedSkills() {
        log.info("[SKILL-SEED] Checking installed_skills collection...");
        installedSkillRepository.count()
            .subscribeOn(Schedulers.boundedElastic())
            .flatMapMany(count -> {
                if (count == 0) {
                    log.info("[SKILL-SEED] installed_skills is empty — seeding skill marketplace...");
                    return seedAll();
                } else {
                    log.info("[SKILL-SEED] installed_skills already has {} entries — skipping seed", count);
                    return Flux.empty();
                }
            })
            .subscribe(
                entry -> log.debug("[SKILL-SEED] Saved: {}", entry.getId()),
                error -> log.error("[SKILL-SEED] Failed to seed skills: {}", error.getMessage()),
                () -> log.info("[SKILL-SEED] Skill marketplace seed complete")
            );
    }

    private Flux<InstalledSkill> seedAll() {
        return skillsSeedDataProvider.provideAllSkillSeeds()
            .flatMap(skill -> installedSkillRepository
                    .save(skill)
                    .doOnSuccess(saved -> log.debug("[SKILL-SEED] Saved: {}", saved.getId()))
            );
    }
}
