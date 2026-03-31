package org.example.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Agent G: Publishing & Package Distribution Agent
 * Handles packaging, code signing, and distribution to app stores
 */
@Service
public class GPublishAgent {

    /**
     * Prepare and publish application to stores
     */
    public PublishOutput publishApplication(PublishRequest request) {
        PublishOutput output = new PublishOutput();
        output.setTimestamp(LocalDateTime.now());
        output.setApplicationName(request.getApplicationName());
        output.setVersion(request.getVersion());

        // Generate app store metadata
        if (request.isBuildForiOS()) {
            output.setAppStoreMetadata(generateAppStoreMetadata(request));
            output.setIpaPackage(generateIPAPackage(request));
            output.setAppStoreConfig(generateAppStoreConfig(request));
        }

        if (request.isBuildForAndroid()) {
            output.setPlayStoreMetadata(generatePlayStoreMetadata(request));
            output.setAabPackage(generateAABPackage(request));
            output.setPlayStoreConfig(generatePlayStoreConfig(request));
        }

        if (request.isBuildForWeb()) {
            output.setWebPackage(generateWebPackage(request));
            output.setWebDeploymentConfig(generateWebDeploymentConfig(request));
        }

        if (request.isBuildForDesktop()) {
            output.setDesktopPackages(generateDesktopPackages(request));
            output.setInstallerConfigs(generateInstallerConfigs(request));
        }

        // Generate signing certificates
        output.setSigningCertificates(generateSigningCertificates(request));

        // Generate release notes
        output.setReleaseNotes(generateReleaseNotes(request));

        // Generate changelog
        output.setChangelog(generateChangelog(request));

        // Generate submission checklist
        output.setSubmissionChecklist(generateSubmissionChecklist(request));

        // Generate code signing setup
        output.setCodeSigningSetup(generateCodeSigningSetup(request));

        // Generate versioning strategy
        output.setVersioningStrategy(generateVersioningStrategy(request));

        // Generate distribution configuration
        output.setDistributionConfig(generateDistributionConfig(request));

        output.setPublishSuccess(true);
        output.setStatusMessage("Publication package prepared successfully");
        output.setLinesOfCode(estimateLinesOfCode(output));

        return output;
    }

    /**
     * Generate App Store metadata for iOS
     */
    private String generateAppStoreMetadata(PublishRequest request) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("{\n");
        metadata.append("  \"appName\": \"").append(request.getApplicationName()).append("\",\n");
        metadata.append("  \"bundleId\": \"com.example.").append(request.getApplicationName().toLowerCase()).append("\",\n");
        metadata.append("  \"version\": \"").append(request.getVersion()).append("\",\n");
        metadata.append("  \"buildNumber\": \"").append(request.getBuildNumber()).append("\",\n");
        metadata.append("  \"minimumOSVersion\": \"14.0\",\n");
        metadata.append("  \"description\": \"").append(request.getDescription()).append("\",\n");
        metadata.append("  \"keywords\": ").append(request.getKeywords()).append(",\n");
        metadata.append("  \"supportURL\": \"https://example.com/support\",\n");
        metadata.append("  \"privacyPolicyURL\": \"https://example.com/privacy\",\n");
        metadata.append("  \"releaseNotes\": \"").append(request.getReleaseNotes()).append("\",\n");
        metadata.append("  \"screenshots\": [\n");
        metadata.append("    { \"language\": \"en-US\", \"devices\": [\"iPhone6.7\", \"iPad12.9\"] }\n");
        metadata.append("  ],\n");
        metadata.append("  \"categories\": [\"Productivity\"],\n");
        metadata.append("  \"contentRating\": \"4+\",\n");
        metadata.append("  \"ageRestriction\": false\n");
        metadata.append("}\n");
        return metadata.toString();
    }

    /**
     * Generate IPA package configuration
     */
    private String generateIPAPackage(PublishRequest request) {
        StringBuilder ipa = new StringBuilder();
        ipa.append("#!/bin/bash\n\n");
        ipa.append("# Build and package iOS app\n");
        ipa.append("xcodebuild -scheme ").append(request.getApplicationName()).append(" \\\n");
        ipa.append("  -configuration Release \\\n");
        ipa.append("  -archivePath ./build/").append(request.getApplicationName()).append(".xcarchive \\\n");
        ipa.append("  -derivedDataPath ./build/derived \\\n");
        ipa.append("  archive\n\n");
        ipa.append("# Export IPA\n");
        ipa.append("xcodebuild -exportArchive \\\n");
        ipa.append("  -archivePath ./build/").append(request.getApplicationName()).append(".xcarchive \\\n");
        ipa.append("  -exportOptionsPlist ./ExportOptions.plist \\\n");
        ipa.append("  -exportPath ./build/ipa\n");
        return ipa.toString();
    }

    /**
     * Generate App Store Connect configuration
     */
    private String generateAppStoreConfig(PublishRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"teamId\": \"YOUR_TEAM_ID\",\n");
        config.append("  \"appId\": \"YOUR_APP_ID\",\n");
        config.append("  \"bundleId\": \"com.example.").append(request.getApplicationName().toLowerCase()).append("\",\n");
        config.append("  \"contractType\": \"Free\",\n");
        config.append("  \"availability\": {\n");
        config.append("    \"regions\": [\"US\", \"CA\", \"GB\", \"AU\"],\n");
        config.append("    \"releaseDate\": \"immediate\"\n");
        config.append("  },\n");
        config.append("  \"pricing\": {\n");
        config.append("    \"tier\": \"free\",\n");
        config.append("    \"inAppPurchases\": []\n");
        config.append("  },\n");
        config.append("  \"features\": {\n");
        config.append("    \"gamecenter\": false,\n");
        config.append("    \"iCloud\": false,\n");
        config.append("    \"pushNotifications\": true\n");
        config.append("  }\n");
        config.append("}\n");
        return config.toString();
    }

    /**
     * Generate Google Play Store metadata
     */
    private String generatePlayStoreMetadata(PublishRequest request) {
        StringBuilder metadata = new StringBuilder();
        metadata.append("{\n");
        metadata.append("  \"appName\": \"").append(request.getApplicationName()).append("\",\n");
        metadata.append("  \"packageName\": \"com.example.").append(request.getApplicationName().toLowerCase()).append("\",\n");
        metadata.append("  \"versionName\": \"").append(request.getVersion()).append("\",\n");
        metadata.append("  \"versionCode\": ").append(parseVersionCode(request.getVersion())).append(",\n");
        metadata.append("  \"minSdkVersion\": 26,\n");
        metadata.append("  \"targetSdkVersion\": 34,\n");
        metadata.append("  \"description\": \"").append(request.getDescription()).append("\",\n");
        metadata.append("  \"shortDescription\": \"").append(truncate(request.getDescription(), 80)).append("\",\n");
        metadata.append("  \"fullDescription\": \"").append(request.getDescription()).append("\",\n");
        metadata.append("  \"keywords\": ").append(request.getKeywords()).append(",\n");
        metadata.append("  \"categoryId\": \"PRODUCTIVITY\",\n");
        metadata.append("  \"contentRating\": \"Everyone\",\n");
        metadata.append("  \"releaseNotes\": \"").append(request.getReleaseNotes()).append("\"\n");
        metadata.append("}\n");
        return metadata.toString();
    }

    /**
     * Generate AAB (Android App Bundle) package configuration
     */
    private String generateAABPackage(PublishRequest request) {
        StringBuilder aab = new StringBuilder();
        aab.append("#!/bin/bash\n\n");
        aab.append("# Build Android App Bundle\n");
        aab.append("./gradlew bundleRelease \\\n");
        aab.append("  -PversionName=").append(request.getVersion()).append(" \\\n");
        aab.append("  -PversionCode=").append(parseVersionCode(request.getVersion())).append("\n\n");
        aab.append("# Sign the bundle\n");
        aab.append("jarsigner -verbose -sigalg MDA1withRSA \\\n");
        aab.append("  -digestalg SHA256 \\\n");
        aab.append("  -keystore android-release-key.jks \\\n");
        aab.append("  app/build/outputs/bundle/release/app-release.aab \\\n");
        aab.append("  release-key-alias\n");
        return aab.toString();
    }

    /**
     * Generate Google Play Store configuration
     */
    private String generatePlayStoreConfig(PublishRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"googleProjectId\": \"YOUR_PROJECT_ID\",\n");
        config.append("  \"appId\": \"YOUR_APP_ID\",\n");
        config.append("  \"packageName\": \"com.example.").append(request.getApplicationName().toLowerCase()).append("\",\n");
        config.append("  \"tracks\": [\"production\", \"beta\", \"alpha\", \"internal\"],\n");
        config.append("  \"releaseTrack\": \"production\",\n");
        config.append("  \"rolloutPercentage\": 100,\n");
        config.append("  \"countries\": [\"US\", \"CA\", \"GB\", \"AU\", \"DE\", \"FR\"],\n");
        config.append("  \"storePresence\": {\n");
        config.append("    \"icon\": \"512x512.png\",\n");
        config.append("    \"hero\": \"1024x500.png\",\n");
        config.append("    \"screenshots\": [\"portrait\", \"landscape\"]\n");
        config.append("  }\n");
        config.append("}\n");
        return config.toString();
    }

    /**
     * Generate web deployment package
     */
    private String generateWebPackage(PublishRequest request) {
        StringBuilder web = new StringBuilder();
        web.append("#!/bin/bash\n\n");
        web.append("# Build web application for production\n");
        web.append("npm run build\n\n");
        web.append("# Create distribution archive\n");
        web.append("tar -czf ").append(request.getApplicationName()).append("-web-").append(request.getVersion()).append(".tar.gz dist/\n\n");
        web.append("# Generate SHA256 checksum\n");
        web.append("sha256sum ").append(request.getApplicationName()).append("-web-").append(request.getVersion()).append(".tar.gz > CHECKSUM.txt\n");
        return web.toString();
    }

    /**
     * Generate web deployment configuration
     */
    private String generateWebDeploymentConfig(PublishRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"deploymentTargets\": [\"Vercel\", \"Netlify\", \"AWS\", \"Azure\"],\n");
        config.append("  \"webConfig\": {\n");
        config.append("    \"domain\": \"app.example.com\",\n");
        config.append("    \"ssl\": true,\n");
        config.append("    \"cdn\": \"CloudFront\",\n");
        config.append("    \"analytics\": \"Google Analytics\",\n");
        config.append("    \"monitoring\": \"New Relic\"\n");
        config.append("  },\n");
        config.append("  \"environmentVariables\": {\n");
        config.append("    \"REACT_APP_VERSION\": \"").append(request.getVersion()).append("\",\n");
        config.append("    \"REACT_APP_ENV\": \"production\"\n");
        config.append("  }\n");
        config.append("}\n");
        return config.toString();
    }

    /**
     * Generate desktop packages (Windows, macOS, Linux)
     */
    private String generateDesktopPackages(PublishRequest request) {
        StringBuilder packages = new StringBuilder();
        packages.append("# Desktop Package Configurations\n\n");
        packages.append("## Windows (MSI Installer)\n");
        packages.append("```\n");
        packages.append("candle.exe -o obj/Release/ -dProductVersion=").append(request.getVersion()).append(" product.wxs\n");
        packages.append("light.exe -out ").append(request.getApplicationName()).append("-").append(request.getVersion()).append(".msi obj/Release/product.wixobj\n");
        packages.append("```\n\n");
        packages.append("## macOS (DMG Installer)\n");
        packages.append("```\n");
        packages.append("create-dmg ").append(request.getApplicationName()).append(".dmg ").append(request.getApplicationName()).append(".app\n");
        packages.append("```\n\n");
        packages.append("## Linux (AppImage + Deb)\n");
        packages.append("```\n");
        packages.append("linuxdeploy-x86_64.AppImage --appdir AppDir --output appimage\n");
        packages.append("dpkg-deb --build AppDir ").append(request.getApplicationName()).append("_").append(request.getVersion()).append(".deb\n");
        packages.append("```\n");
        return packages.toString();
    }

    /**
     * Generate installer configurations
     */
    private String generateInstallerConfigs(PublishRequest request) {
        StringBuilder configs = new StringBuilder();
        configs.append("{\n");
        configs.append("  \"windows\": {\n");
        configs.append("    \"certificateInfo\": {\n");
        configs.append("      \"thumbprint\": \"YOUR_THUMBPRINT\",\n");
        configs.append("      \"issuer\": \"Sectigo\"\n");
        configs.append("    },\n");
        configs.append("    \"installer\": {\n");
        configs.append("      \"type\": \"MSI\",\n");
        configs.append("      \"version\": \"").append(request.getVersion()).append("\"\n");
        configs.append("    }\n");
        configs.append("  },\n");
        configs.append("  \"macos\": {\n");
        configs.append("    \"certificateInfo\": {\n");
        configs.append("      \"teamId\": \"YOUR_TEAM_ID\",\n");
        configs.append("      \"developerId\": \"YOUR_DEV_ID\"\n");
        configs.append("    },\n");
        configs.append("    \"notarization\": {\n");
        configs.append("      \"enabled\": true,\n");
        configs.append("      \"stapling\": true\n");
        configs.append("    }\n");
        configs.append("  },\n");
        configs.append("  \"linux\": {\n");
        configs.append("    \"packages\": [\"appimage\", \"deb\", \"rpm\"],\n");
        configs.append("    \"signing\": {\n");
        configs.append("      \"enabled\": false\n");
        configs.append("    }\n");
        configs.append("  }\n");
        configs.append("}\n");
        return configs.toString();
    }

    /**
     * Generate code signing certificates and setup
     */
    private String generateSigningCertificates(PublishRequest request) {
        StringBuilder certs = new StringBuilder();
        certs.append("# Code Signing Setup\n\n");
        certs.append("## iOS Code Signing\n");
        certs.append("```\n");
        certs.append("# Development Certificate\n");
        certs.append("openssl req -new -key private.key -out dev.csr\n\n");
        certs.append("# Provisioning Profile Setup\n");
        certs.append("security import WWDR.cer -k ~/Library/Keychains/login.keychain\n");
        certs.append("```\n\n");
        certs.append("## Android Code Signing\n");
        certs.append("```\n");
        certs.append("# Generate keystore\n");
        certs.append("keytool -genkey -v -keystore release-key.jks \\\n");
        certs.append("  -keyalg RSA -keysize 2048 -validity 10000 \\\n");
        certs.append("  -alias release-key-alias\n");
        certs.append("```\n\n");
        certs.append("## Desktop Code Signing\n");
        certs.append("```\n");
        certs.append("# Windows signing\n");
        certs.append("signtool sign /f certificate.pfx /p password /t http://timestamp.server /v app.exe\n");
        certs.append("```\n");
        return certs.toString();
    }

    /**
     * Generate release notes
     */
    private String generateReleaseNotes(PublishRequest request) {
        StringBuilder notes = new StringBuilder();
        notes.append("# Release Notes - Version ").append(request.getVersion()).append("\n\n");
        notes.append("**Release Date:** ").append(LocalDateTime.now().toLocalDate()).append("\n\n");
        notes.append("## What's New\n");
        notes.append(request.getReleaseNotes()).append("\n\n");
        notes.append("## Bug Fixes\n");
        notes.append("- Fixed stability issues\n");
        notes.append("- Improved performance\n");
        notes.append("- Enhanced security\n\n");
        notes.append("## Known Issues\n");
        notes.append("- None reported\n\n");
        notes.append("## Supported Platforms\n");
        notes.append("- iOS 14.0+\n");
        notes.append("- Android 8.0+ (API 26)\n");
        notes.append("- Web (Modern browsers)\n");
        notes.append("- macOS 10.15+\n");
        notes.append("- Windows 10+\n");
        notes.append("- Linux (Ubuntu 20.04+)\n");
        return notes.toString();
    }

    /**
     * Generate changelog
     */
    private String generateChangelog(PublishRequest request) {
        StringBuilder changelog = new StringBuilder();
        changelog.append("# Changelog\n\n");
        changelog.append("## [").append(request.getVersion()).append("] - ").append(LocalDateTime.now().toLocalDate()).append("\n");
        changelog.append("### Added\n");
        changelog.append("- New features and improvements\n");
        changelog.append("- Enhanced UI/UX\n");
        changelog.append("- Performance optimizations\n\n");
        changelog.append("### Fixed\n");
        changelog.append("- Critical bug fixes\n");
        changelog.append("- Security improvements\n");
        changelog.append("- Stability enhancements\n\n");
        changelog.append("### Deprecated\n");
        changelog.append("- Legacy APIs (use new ones)\n\n");
        changelog.append("### Removed\n");
        changelog.append("- Old features\n\n");
        changelog.append("### Security\n");
        changelog.append("- Security fixes and updates\n");
        return changelog.toString();
    }

    /**
     * Generate submission checklist
     */
    private String generateSubmissionChecklist(PublishRequest request) {
        StringBuilder checklist = new StringBuilder();
        checklist.append("# App Store Submission Checklist\n\n");
        checklist.append("## Pre-Submission\n");
        checklist.append("- [ ] Update version number: ").append(request.getVersion()).append("\n");
        checklist.append("- [ ] Update build number\n");
        checklist.append("- [ ] Test on all target devices\n");
        checklist.append("- [ ] Run security scan\n");
        checklist.append("- [ ] Update release notes\n");
        checklist.append("- [ ] Test all app permissions\n\n");
        checklist.append("## App Store Requirements\n");
        checklist.append("- [ ] Privacy Policy URL is valid\n");
        checklist.append("- [ ] Support URL is provided\n");
        checklist.append("- [ ] Screenshots are optimized\n");
        checklist.append("- [ ] App artwork (icon, hero image) is present\n");
        checklist.append("- [ ] Keywords are relevant\n");
        checklist.append("- [ ] Category is appropriate\n");
        checklist.append("- [ ] Content rating is accurate\n\n");
        checklist.append("## Technical Requirements\n");
        checklist.append("- [ ] Code is signed with valid certificate\n");
        checklist.append("- [ ] Provisioning profiles are configured\n");
        checklist.append("- [ ] All entitlements are correct\n");
        checklist.append("- [ ] Push notification certificate is valid\n");
        checklist.append("- [ ] No hardcoded secrets or credentials\n\n");
        checklist.append("## Post-Submission\n");
        checklist.append("- [ ] Monitor for approval/rejection\n");
        checklist.append("- [ ] Address any feedback\n");
        checklist.append("- [ ] Prepare for release\n");
        checklist.append("- [ ] Set release date/time\n");
        checklist.append("- [ ] Monitor app reviews\n");
        return checklist.toString();
    }

    /**
     * Generate code signing setup guide
     */
    private String generateCodeSigningSetup(PublishRequest request) {
        StringBuilder setup = new StringBuilder();
        setup.append("# Code Signing Setup Guide\n\n");
        setup.append("## For iOS\n");
        setup.append("1. Create certificates in Apple Developer Portal\n");
        setup.append("2. Download and install certificates\n");
        setup.append("3. Create provisioning profiles\n");
        setup.append("4. Configure Xcode signing\n");
        setup.append("5. Test on physical device\n\n");
        setup.append("## For Android\n");
        setup.append("1. Create keystore file\n");
        setup.append("2. Store signing key securely\n");
        setup.append("3. Configure gradle signing config\n");
        setup.append("4. Build signed APK/AAB\n\n");
        setup.append("## For Desktop\n");
        setup.append("1. Obtain code signing certificate\n");
        setup.append("2. Import certificate to signing tool\n");
        setup.append("3. Sign executable/installer\n");
        setup.append("4. Verify signature\n");
        return setup.toString();
    }

    /**
     * Generate versioning strategy
     */
    private String generateVersioningStrategy(PublishRequest request) {
        StringBuilder strategy = new StringBuilder();
        strategy.append("# Versioning Strategy (Semantic Versioning)\n\n");
        strategy.append("## Format: MAJOR.MINOR.PATCH\n");
        strategy.append("Current Version: ").append(request.getVersion()).append("\n\n");
        strategy.append("## MAJOR (").append(request.getVersion().split("\\.")[0]).append(")\n");
        strategy.append("- Increment for incompatible API changes\n");
        strategy.append("- May require user action\n\n");
        strategy.append("## MINOR (").append(request.getVersion().split("\\.")[1]).append(")\n");
        strategy.append("- Increment for new features (backward compatible)\n");
        strategy.append("- Transparent to users\n\n");
        strategy.append("## PATCH (").append(request.getVersion().split("\\.")[2]).append(")\n");
        strategy.append("- Increment for bug fixes\n");
        strategy.append("- Recommended for all users\n\n");
        strategy.append("## Release Cadence\n");
        strategy.append("- Major: Quarterly\n");
        strategy.append("- Minor: Monthly\n");
        strategy.append("- Patch: As needed\n");
        return strategy.toString();
    }

    /**
     * Generate distribution configuration
     */
    private String generateDistributionConfig(PublishRequest request) {
        StringBuilder config = new StringBuilder();
        config.append("{\n");
        config.append("  \"distributionChannels\": {\n");
        config.append("    \"iOS\": {\n");
        config.append("      \"appStore\": true,\n");
        config.append("      \"testFlight\": true,\n");
        config.append("      \"enterprise\": false\n");
        config.append("    },\n");
        config.append("    \"Android\": {\n");
        config.append("      \"playStore\": true,\n");
        config.append("      \"amazonAppstore\": true,\n");
        config.append("      \"sideload\": false\n");
        config.append("    },\n");
        config.append("    \"Web\": {\n");
        config.append("      \"production\": true,\n");
        config.append("      \"staging\": true,\n");
        config.append("      \"development\": true\n");
        config.append("    },\n");
        config.append("    \"Desktop\": {\n");
        config.append("      \"windows\": true,\n");
        config.append("      \"macos\": true,\n");
        config.append("      \"linux\": true\n");
        config.append("    }\n");
        config.append("  },\n");
        config.append("  \"rolloutStrategy\": \"phased\",\n");
        config.append("  \"canaryPercentage\": 10,\n");
        config.append("  \"productionPercentage\": 100\n");
        config.append("}\n");
        return config.toString();
    }

    /**
     * Estimate total lines of code
     */
    private int estimateLinesOfCode(PublishOutput output) {
        int total = 0;
        if (output.appStoreMetadata != null) total += output.appStoreMetadata.split("\n").length;
        if (output.ipaPackage != null) total += output.ipaPackage.split("\n").length;
        if (output.appStoreConfig != null) total += output.appStoreConfig.split("\n").length;
        if (output.playStoreMetadata != null) total += output.playStoreMetadata.split("\n").length;
        if (output.aabPackage != null) total += output.aabPackage.split("\n").length;
        if (output.playStoreConfig != null) total += output.playStoreConfig.split("\n").length;
        if (output.webPackage != null) total += output.webPackage.split("\n").length;
        if (output.webDeploymentConfig != null) total += output.webDeploymentConfig.split("\n").length;
        if (output.desktopPackages != null) total += output.desktopPackages.split("\n").length;
        if (output.installerConfigs != null) total += output.installerConfigs.split("\n").length;
        total += output.signingCertificates.split("\n").length;
        total += output.releaseNotes.split("\n").length;
        total += output.changelog.split("\n").length;
        total += output.submissionChecklist.split("\n").length;
        total += output.codeSigningSetup.split("\n").length;
        total += output.versioningStrategy.split("\n").length;
        total += output.distributionConfig.split("\n").length;
        return total;
    }

    private String truncate(String text, int length) {
        if (text.length() <= length) return text;
        return text.substring(0, length) + "...";
    }

    private int parseVersionCode(String version) {
        String[] parts = version.split("\\.");
        if (parts.length >= 3) {
            int major = Integer.parseInt(parts[0]) * 10000;
            int minor = Integer.parseInt(parts[1]) * 100;
            int patch = Integer.parseInt(parts[2]);
            return major + minor + patch;
        }
        return 1;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishRequest {
        private String applicationName;
        private String version;
        private int buildNumber;
        private String description;
        private List<String> keywords;
        private String releaseNotes;
        private boolean buildForiOS;
        private boolean buildForAndroid;
        private boolean buildForWeb;
        private boolean buildForDesktop;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PublishOutput {
        private LocalDateTime timestamp;
        private String applicationName;
        private String version;
        
        // iOS
        private String appStoreMetadata;
        private String ipaPackage;
        private String appStoreConfig;
        
        // Android
        private String playStoreMetadata;
        private String aabPackage;
        private String playStoreConfig;
        
        // Web
        private String webPackage;
        private String webDeploymentConfig;
        
        // Desktop
        private String desktopPackages;
        private String installerConfigs;
        
        // Common
        private String signingCertificates;
        private String releaseNotes;
        private String changelog;
        private String submissionChecklist;
        private String codeSigningSetup;
        private String versioningStrategy;
        private String distributionConfig;
        
        private boolean publishSuccess;
        private String statusMessage;
        private int linesOfCode;
    }
}
