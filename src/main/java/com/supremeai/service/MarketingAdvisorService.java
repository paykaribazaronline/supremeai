package com.supremeai.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Plan 13: Marketing Strategy Advisor.
 *
 * Acts as a business partner by generating contextual marketing strategies
 * based on the project description, target audience, and budget constraints.
 *
 * Provides:
 * - Launch plan recommendations
 * - Social media strategy
 * - Growth hacking tactics for early-stage products
 * - Budget-aware suggestions (free-first approach)
 */
@Service
public class MarketingAdvisorService {

    private static final Logger logger = LoggerFactory.getLogger(MarketingAdvisorService.class);

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generate a marketing strategy for a given project.
     *
     * @param context MarketingContext with project details and constraints
     * @return MarketingStrategy with actionable recommendations
     */
    public MarketingStrategy generateStrategy(MarketingContext context) {
        logger.info("[MARKETING] Generating strategy for project: {}", context.getProjectName());

        List<String> launchSteps = buildLaunchPlan(context);
        List<SocialMediaPlan> socialPlans = buildSocialMediaPlan(context);
        List<String> growthTactics = buildGrowthTactics(context);
        List<String> budgetSuggestions = buildBudgetSuggestions(context);

        MarketingStrategy strategy = new MarketingStrategy(
            context.getProjectName(),
            launchSteps,
            socialPlans,
            growthTactics,
            budgetSuggestions
        );

        logger.info("[MARKETING] Strategy generated: {} launch steps, {} social plans",
            launchSteps.size(), socialPlans.size());
        return strategy;
    }

    /**
     * Quick advice for a specific marketing question.
     */
    public String quickAdvice(String question, String projectType) {
        String q = question.toLowerCase();
        if (q.contains("launch")) {
            return "For " + projectType + " launch: Start with Product Hunt + Twitter/X announcement. "
                + "Create a short demo video (60s). Offer early-adopter discount or free tier.";
        }
        if (q.contains("social media") || q.contains("social")) {
            return "Focus on platforms where developers hang out: Twitter/X, LinkedIn, Reddit (r/programming, r/SideProject). "
                + "Post consistently — 3-4 times per week minimum.";
        }
        if (q.contains("price") || q.contains("pricing")) {
            return "For AI tools: freemium works best. Free tier builds trust, paid tier for power users. "
                + "Consider $9-29/month for individual, $49-99/month for teams.";
        }
        if (q.contains("user") || q.contains("acquisition")) {
            return "Early user acquisition: cold outreach to relevant communities, "
                + "content marketing (tutorials/blog posts), and referral programs (give $5 credit for each referral).";
        }
        return "Focus on solving one problem really well. Tell your story authentically. "
            + "Engage with your first 100 users personally — their feedback shapes everything.";
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Private builders
    // ─────────────────────────────────────────────────────────────────────────

    private List<String> buildLaunchPlan(MarketingContext ctx) {
        List<String> steps = new ArrayList<>();
        steps.add("Week 1: Build landing page with email capture (free: Carrd/Notion)");
        steps.add("Week 2: Create 60-second demo video showing core value proposition");
        steps.add("Week 3: Post on Product Hunt, Hacker News (Show HN), and Reddit r/SideProject");
        steps.add("Week 4: Reach out to 50 potential users directly via LinkedIn/Twitter");

        if ("AI_TOOL".equalsIgnoreCase(ctx.getProductType())) {
            steps.add("Submit to AI tool directories: There's An AI For That, Futurepedia, AI Tools Directory");
        }
        if ("MOBILE_APP".equalsIgnoreCase(ctx.getProductType())) {
            steps.add("Submit to app review sites: AppAdvice, 148Apps (iOS) or APKPure (Android)");
        }
        if (ctx.getBudgetUsd() > 0 && ctx.getBudgetUsd() < 500) {
            steps.add("Budget tip: Allocate $100 for Twitter/X promoted posts targeting developers");
        }
        return steps;
    }

    private List<SocialMediaPlan> buildSocialMediaPlan(MarketingContext ctx) {
        List<SocialMediaPlan> plans = new ArrayList<>();

        plans.add(new SocialMediaPlan(
            "Twitter/X",
            "Primary channel for developer audience",
            List.of(
                "Daily build-in-public updates (#buildinpublic)",
                "Share metrics transparently (users, revenue)",
                "Engage with other indie hackers and developers",
                "Tweet about problems your product solves"
            ),
            "3-5 posts/day"
        ));

        plans.add(new SocialMediaPlan(
            "LinkedIn",
            "Professional network for B2B/enterprise users",
            List.of(
                "Weekly long-form posts about lessons learned",
                "Share case studies and customer wins",
                "Connect with potential enterprise clients"
            ),
            "1 post/day"
        ));

        plans.add(new SocialMediaPlan(
            "Reddit",
            "Community engagement and organic discovery",
            List.of(
                "r/SideProject — weekly progress updates",
                "r/programming — technical content",
                "r/entrepreneur — business insights",
                "Provide value first, promote second"
            ),
            "3-4 posts/week"
        ));

        if ("MOBILE_APP".equalsIgnoreCase(ctx.getProductType())) {
            plans.add(new SocialMediaPlan(
                "TikTok/YouTube Shorts",
                "Visual demo content for mobile apps",
                List.of(
                    "Screen-record app demos (30-60 seconds)",
                    "Before/after comparison videos",
                    "Tutorial walkthroughs"
                ),
                "2-3 videos/week"
            ));
        }

        return plans;
    }

    private List<String> buildGrowthTactics(MarketingContext ctx) {
        List<String> tactics = new ArrayList<>();
        tactics.add("Referral program: Give existing users 1 month free for each referral that converts");
        tactics.add("Content marketing: Write tutorials solving problems your target users search for");
        tactics.add("Open-source a small utility related to your product for GitHub traction");
        tactics.add("Partnership: Reach out to 5 complementary non-competing tools for integration/cross-promotion");
        tactics.add("Community: Start a Discord server early — even 50 engaged members is powerful social proof");
        if (ctx.getTargetAudience() != null && ctx.getTargetAudience().toLowerCase().contains("developer")) {
            tactics.add("Developer-specific: Provide a free public API, write developer docs, sponsor a podcast");
        }
        return tactics;
    }

    private List<String> buildBudgetSuggestions(MarketingContext ctx) {
        List<String> suggestions = new ArrayList<>();
        int budget = ctx.getBudgetUsd();

        if (budget == 0) {
            suggestions.add("$0 budget: Focus entirely on organic channels — Twitter, Reddit, Product Hunt");
            suggestions.add("Your time is your biggest asset: 2 hours/day on community engagement");
            suggestions.add("Free tools: Buffer (social scheduling), Canva (design), Google Analytics");
        } else if (budget < 200) {
            suggestions.add("$50: Sponsored tweet targeting #buildinpublic audience");
            suggestions.add("$50: Buy a proper domain + Carrd landing page ($19/year)");
            suggestions.add("$100: Micro-influencer shoutout in your niche (look for creators with <10k followers)");
        } else {
            suggestions.add("$200: Google Ads targeting competitor brand keywords");
            suggestions.add("$100: Content writing for 2 SEO-optimized blog posts");
            suggestions.add("$100: Product Hunt upvote day promotion tools");
            suggestions.add("Rest: Reserve for retargeting ads once you have initial traffic");
        }
        return suggestions;
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Inner Models
    // ─────────────────────────────────────────────────────────────────────────

    public static class MarketingContext {
        private String projectName;
        private String productType; // AI_TOOL, MOBILE_APP, WEB_APP, SAAS, etc.
        private String targetAudience;
        private int budgetUsd;
        private String stage; // IDEA, MVP, GROWTH, SCALE

        public MarketingContext() {}

        public MarketingContext(String projectName, String productType, String targetAudience,
                                int budgetUsd, String stage) {
            this.projectName = projectName;
            this.productType = productType;
            this.targetAudience = targetAudience;
            this.budgetUsd = budgetUsd;
            this.stage = stage;
        }

        public String getProjectName() { return projectName; }
        public void setProjectName(String projectName) { this.projectName = projectName; }
        public String getProductType() { return productType; }
        public void setProductType(String productType) { this.productType = productType; }
        public String getTargetAudience() { return targetAudience; }
        public void setTargetAudience(String targetAudience) { this.targetAudience = targetAudience; }
        public int getBudgetUsd() { return budgetUsd; }
        public void setBudgetUsd(int budgetUsd) { this.budgetUsd = budgetUsd; }
        public String getStage() { return stage; }
        public void setStage(String stage) { this.stage = stage; }
    }

    public static class SocialMediaPlan {
        private final String platform;
        private final String rationale;
        private final List<String> tactics;
        private final String frequency;

        public SocialMediaPlan(String platform, String rationale, List<String> tactics, String frequency) {
            this.platform = platform;
            this.rationale = rationale;
            this.tactics = tactics;
            this.frequency = frequency;
        }

        public String getPlatform() { return platform; }
        public String getRationale() { return rationale; }
        public List<String> getTactics() { return tactics; }
        public String getFrequency() { return frequency; }
    }

    public static class MarketingStrategy {
        private final String projectName;
        private final List<String> launchPlan;
        private final List<SocialMediaPlan> socialMediaPlans;
        private final List<String> growthTactics;
        private final List<String> budgetSuggestions;

        public MarketingStrategy(String projectName, List<String> launchPlan,
                                 List<SocialMediaPlan> socialMediaPlans,
                                 List<String> growthTactics, List<String> budgetSuggestions) {
            this.projectName = projectName;
            this.launchPlan = launchPlan;
            this.socialMediaPlans = socialMediaPlans;
            this.growthTactics = growthTactics;
            this.budgetSuggestions = budgetSuggestions;
        }

        public String getProjectName() { return projectName; }
        public List<String> getLaunchPlan() { return launchPlan; }
        public List<SocialMediaPlan> getSocialMediaPlans() { return socialMediaPlans; }
        public List<String> getGrowthTactics() { return growthTactics; }
        public List<String> getBudgetSuggestions() { return budgetSuggestions; }

        public Map<String, Object> toMap() {
            return Map.of(
                "projectName", projectName,
                "launchPlan", launchPlan,
                "socialMediaPlatforms", socialMediaPlans.stream().map(SocialMediaPlan::getPlatform).toList(),
                "growthTactics", growthTactics,
                "budgetSuggestions", budgetSuggestions
            );
        }
    }
}
