package com.supremeai.service;

import com.supremeai.model.UserGuide;
import com.supremeai.repository.UserGuideRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import jakarta.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Map;

/**
 * Data initializer for sample video tutorials.
 * Populates Firestore with Bangla and English guides on first startup.
 */
@Component
public class GuideDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(GuideDataInitializer.class);

    @Autowired
    private UserGuideRepository userGuideRepository;

    @PostConstruct
    public void initialize() {
        // Check if guides already exist
        userGuideRepository.count()
            .flatMapMany(count -> {
                if (count == 0) {
                    log.info("No guides found. Initializing sample video guides...");
                    return createSampleGuides();
                }
                return Flux.empty();
            })
            .subscribe(
                guide -> log.debug("Initialized guide: {}", guide.getId()),
                error -> log.error("Failed to initialize sample guides: {}", error.getMessage()),
                () -> log.info("Sample video guides initialization process completed")
            );
    }

    private Flux<UserGuide> createSampleGuides() {
        UserGuide gettingStartedEn = new UserGuide();
        gettingStartedEn.setId("guide-001");
        gettingStartedEn.setTitle(Map.of(
            "en", "Getting Started with SupremeAI",
            "bn", "সুপ্রিমএআই-এ শুরু করা"
        ));
        gettingStartedEn.setDescription(Map.of(
            "en", "Learn the basics of SupremeAI: how to log in, navigate the dashboard, and create your first AI-powered Android app.",
            "bn", "সুপ্রিমএআই-এর বেরকম: কীভাবে লগইন করবেন, ড্যাশবোর্ডে নেভিগেট করবেন এবং আপনার প্রথম এআই-চালিত অ্যান্ড্রয়েড অ্যাপ তৈরি করবেন।"
        ));
        gettingStartedEn.setVideoUrl(Map.of(
            "en", "https://www.youtube.com/embed/sample-getting-started-en",
            "bn", "https://www.youtube.com/embed/sample-getting-started-bn"
        ));
        gettingStartedEn.setThumbnailUrl("/assets/tutorial-thumbnails/getting-started.jpg");
        gettingStartedEn.setOrder(1);
        gettingStartedEn.setCategory("basics");
        gettingStartedEn.setDurationSeconds(180); // 3 minutes
        gettingStartedEn.setIsPublished(true);
        gettingStartedEn.setTags(Arrays.asList("beginner", "basics", "onboarding"));
        gettingStartedEn.setCreatedAt(LocalDateTime.now());
        gettingStartedEn.setUpdatedAt(LocalDateTime.now());

        UserGuide firebaseAuthEn = new UserGuide();
        firebaseAuthEn.setId("guide-002");
        firebaseAuthEn.setTitle(Map.of(
            "en", "Firebase Authentication Setup",
            "bn", "ফায়ারবেস অথেন্টিকেশন সেটআপ"
        ));
        firebaseAuthEn.setDescription(Map.of(
            "en", "Step-by-step guide to set up Firebase Authentication for your SupremeAI instance. Learn about security rules and user management.",
            "bn", "আপনার সুপ্রিমএআই ইনস্ট্যান্সের জন্য ফায়ারবেস অথেন্টিকেশন সেটআপের ধাপে ধাপে গাইড। সিকিউরিটি রুলস এবং ইউজার ম্যানেজমেন্ট সম্পর্কে শিখুন।"
        ));
        firebaseAuthEn.setVideoUrl(Map.of(
            "en", "https://www.youtube.com/embed/sample-firebase-auth-en",
            "bn", "https://www.youtube.com/embed/sample-firebase-auth-bn"
        ));
        firebaseAuthEn.setThumbnailUrl("/assets/tutorial-thumbnails/firebase-auth.jpg");
        firebaseAuthEn.setOrder(2);
        firebaseAuthEn.setCategory("security");
        firebaseAuthEn.setDurationSeconds(300); // 5 minutes
        firebaseAuthEn.setIsPublished(true);
        firebaseAuthEn.setTags(Arrays.asList("security", "firebase", "authentication"));
        firebaseAuthEn.setCreatedAt(LocalDateTime.now());
        firebaseAuthEn.setUpdatedAt(LocalDateTime.now());

        UserGuide apiKeysEn = new UserGuide();
        apiKeysEn.setId("guide-003");
        apiKeysEn.setTitle(Map.of(
            "en", "Managing API Keys",
            "bn", "API কী ম্যানেজ করা"
        ));
        apiKeysEn.setDescription(Map.of(
            "en", "How to add, update, and secure your API keys for different AI providers (OpenAI, Anthropic, Groq, etc.). Learn about key rotation and monitoring.",
            "bn", "বিভিন্ন এআই প্রোভাইডার (OpenAI, Anthropic, Groq, ইত্যাদি)-এর জন্য আপনার API কী কীভাবে যোগ, আপডেট এবং নিরাপদ রাখবেন। কী রোটেশন এবং মনিটরিং সম্পর্কে শিখুন।"
        ));
        apiKeysEn.setVideoUrl(Map.of(
            "en", "https://www.youtube.com/embed/sample-api-keys-en",
            "bn", "https://www.youtube.com/embed/sample-api-keys-bn"
        ));
        apiKeysEn.setThumbnailUrl("/assets/tutorial-thumbnails/api-keys.jpg");
        apiKeysEn.setOrder(3);
        apiKeysEn.setCategory("basics");
        apiKeysEn.setDurationSeconds(240); // 4 minutes
        apiKeysEn.setIsPublished(true);
        apiKeysEn.setTags(Arrays.asList("api", "keys", "security", "providers"));
        apiKeysEn.setCreatedAt(LocalDateTime.now());
        apiKeysEn.setUpdatedAt(LocalDateTime.now());

        UserGuide projectsEn = new UserGuide();
        projectsEn.setId("guide-004");
        projectsEn.setTitle(Map.of(
            "en", "Creating and Managing Projects",
            "bn", "প্রজেক্ট তৈরি ও ম্যানেজ করা"
        ));
        projectsEn.setDescription(Map.of(
            "en", "Learn how to create new app projects, track their progress, and manage project settings in SupremeAI.",
            "bn", "সুপ্রিমএআই-এ নতুন অ্যাপ প্রজেক্ট কীভাবে তৈরি করবেন, তাদের অগ্রগতি ট্র্যাক করবেন এবং প্রজেক্ট সেটিংস ম্যানেজ করবেন তা শিখুন।"
        ));
        projectsEn.setVideoUrl(Map.of(
            "en", "https://www.youtube.com/embed/sample-projects-en",
            "bn", "https://www.youtube.com/embed/sample-projects-bn"
        ));
        projectsEn.setThumbnailUrl("/assets/tutorial-thumbnails/projects.jpg");
        projectsEn.setOrder(4);
        projectsEn.setCategory("projects");
        projectsEn.setDurationSeconds(200); // 3 min 20 sec
        projectsEn.setIsPublished(true);
        projectsEn.setTags(Arrays.asList("projects", "management", "tracking"));
        projectsEn.setCreatedAt(LocalDateTime.now());
        projectsEn.setUpdatedAt(LocalDateTime.now());

        UserGuide adminGuideEn = new UserGuide();
        adminGuideEn.setId("guide-005");
        adminGuideEn.setTitle(Map.of(
            "en", "Admin Dashboard Overview",
            "bn", "অ্যাডমিন ড্যাশবোর্ড ওভারভিউ"
        ));
        adminGuideEn.setDescription(Map.of(
            "en", "Comprehensive overview of the admin dashboard: monitoring system health, managing users, and configuring system settings.",
            "bn", "অ্যাডমিন ড্যাশবোর্ডের সম্পূর্ণ ওভারভিউ: সিস্টেম হেল्थ মনিটরিং, ব্যবহারকারী ব্যবস্থাপনা এবং সিস্টেম কনফিগারেশন।"
        ));
        adminGuideEn.setVideoUrl(Map.of(
            "en", "https://www.youtube.com/embed/sample-admin-en",
            "bn", "https://www.youtube.com/embed/sample-admin-bn"
        ));
        adminGuideEn.setThumbnailUrl("/assets/tutorial-thumbnails/admin-dashboard.jpg");
        adminGuideEn.setOrder(5);
        adminGuideEn.setCategory("admin");
        adminGuideEn.setDurationSeconds(360); // 6 minutes
        adminGuideEn.setIsPublished(true);
        adminGuideEn.setTags(Arrays.asList("admin", "dashboard", "monitoring", "users"));
        adminGuideEn.setCreatedAt(LocalDateTime.now());
        adminGuideEn.setUpdatedAt(LocalDateTime.now());

        return Flux.merge(
            userGuideRepository.save(gettingStartedEn),
            userGuideRepository.save(firebaseAuthEn),
            userGuideRepository.save(apiKeysEn),
            userGuideRepository.save(projectsEn),
            userGuideRepository.save(adminGuideEn)
        );
    }
}
