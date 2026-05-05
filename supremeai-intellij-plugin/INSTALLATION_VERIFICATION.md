# SupremeAI IntelliJ Plugin - Installation Verification Guide

## Overview
This document provides a comprehensive guide to verify the installation and functionality of the SupremeAI IntelliJ Plugin in Android Studio.

**Plugin Version:** 1.2.0  
**Compatible IDE:** Android Studio (2024.3.2.15) / IntelliJ IDEA  
**Last Updated:** 2026-05-03

---

## Pre-Installation Checklist

### System Requirements
- [ ] Android Studio 2024.3.2.15 or later installed
- [ ] Java 21 JDK installed and configured
- [ ] Kotlin plugin enabled in Android Studio
- [ ] Android plugin for Gradle installed
- [ ] Minimum 4GB RAM allocated to IDE
- [ ] Internet connection for AI provider connectivity

### Plugin Dependencies
The plugin requires the following bundled dependencies:
- [ ] `com.intellij.modules.platform` - Core IntelliJ platform
- [ ] `com.intellij.modules.java` - Java support
- [ ] `org.jetbrains.kotlin` - Kotlin language support
- [ ] `org.jetbrains.android` - Android development tools

---

## Installation Verification Steps

### Step 1: Plugin Installation

#### Method A: Install from Source (Development)
1. Open Android Studio
2. Go to `File → Settings → Plugins` (Windows/Linux) or `Android Studio → Settings → Plugins` (macOS)
3. Click the gear icon → `Install Plugin from Disk...`
4. Navigate to the plugin directory: `supremeai-intellij-plugin/`
5. Select the built plugin ZIP file or use Gradle to build: `./gradlew buildPlugin`
6. Restart Android Studio

#### Method B: Install from Marketplace (Production)
1. Open Android Studio
2. Go to `File → Settings → Plugins → Marketplace`
3. Search for "SupremeAI Assistant"
4. Click `Install`
5. Restart Android Studio

**Verification:** After restart, check if "SupremeAI" appears in the installed plugins list.

---

### Step 2: Verify Plugin Components

#### 2.1 Tool Window
- [ ] Open `View → Tool Windows → SupremeAI` or look for SupremeAI icon in the right sidebar
- [ ] Verify the tool window contains three tabs:
  - **Chat**: Interactive chat interface with AI assistant
  - **Orchestration**: Multi-agent collaboration status
  - **Settings**: Plugin configuration panel

#### 2.2 Status Bar Widget
- [ ] Look for the SupremeAI icon (ℹ️) in the bottom-right status bar
- [ ] Click the icon to verify it opens the SupremeAI tool window

#### 2.3 Context Menu Actions
- [ ] Right-click on selected code in the editor
- [ ] Verify "Ask SupremeAI" action appears in the context menu
- [ ] Select code and use the action to send code to chat

#### 2.4 Code Inspection
- [ ] Open a Kotlin file with potential issues
- [ ] Verify SupremeAI inspections are running:
  - Agent pattern detection warnings
  - RecyclerView performance suggestions
  - Abstract agent warnings

#### 2.5 Settings Panel
- [ ] Go to `File → Settings → Tools → SupremeAI`
- [ ] Verify settings fields:
  - API Key input field
  - API Endpoint configuration
  - Model selection (SupremeAI-v1 Stable/Flash, v2 Experimental)
  - Kimo Mode toggle
  - Full Authority mode toggle
  - Share Mode selection (manual/auto/disabled)
  - External Directory access toggle
  - Permission settings (read/edit/bash/task/websearch/external_directory)

---

### Step 3: Learning Functionality Verification

#### 3.1 User Code Learning
- [ ] Open a Kotlin or Java file in the editor
- [ ] Make code changes (add/modify/delete code)
- [ ] Verify document change listeners are active
- [ ] Check IDE logs for learning events (optional)

#### 3.2 Gradle Build Learning
- [ ] Trigger a Gradle build failure (e.g., syntax error)
- [ ] Verify `GradleFailureDetector` captures the failure
- [ ] Check if error is sent to learning backend (requires API key)
- [ ] Look for console output: "🎓 SupremeAI has successfully learned from this Android Studio error!"

#### 3.3 Build System Integration
- [ ] Run a Gradle sync
- [ ] Verify external system task listeners are active
- [ ] Check for Gradle stderr monitoring

---

### Step 4: AI Provider Connectivity

#### 4.1 WebSocket Connection
- [ ] Open SupremeAI tool window
- [ ] Check status label: "● Backend: Connecting..." should change to "● Backend: Connected"
- [ ] Verify WebSocket connection to `wss://supremeai-a.web.app/ws-supreme`

#### 4.2 API Configuration
- [ ] In Settings, configure a valid API key
- [ ] Test API endpoint connectivity
- [ ] Verify HTTPS requests to `https://supremeai-a.web.app/api/project/generate`

#### 4.3 Generate App Feature
- [ ] Right-click in project explorer or use main menu
- [ ] Select `SupremeAI → Generate App` or use action
- [ ] Enter a project name
- [ ] Verify API request is sent with proper authentication
- [ ] Check for success/failure notification

---

### Step 5: Security and Privacy Controls

#### 5.1 Permission System
- [ ] In Settings, verify default permissions:
  - Read: allow
  - Edit: ask
  - Bash: ask
  - Task: allow
  - Websearch: allow
  - External Directory: deny

#### 5.2 API Key Management
- [ ] Verify API key is stored securely (not in plain text in logs)
- [ ] Check that key is masked in UI (password field)
- [ ] Verify key is not committed to version control

#### 5.3 Data Transmission
- [ ] Verify all external communications use HTTPS
- [ ] Check WebSocket uses WSS (secure WebSocket)
- [ ] Verify error reporting doesn't expose sensitive data

---

## Common Issues and Troubleshooting

### Issue 1: Plugin Not Loading
**Symptoms:** SupremeAI not in installed plugins list after installation  
**Solutions:**
- Check IDE logs: `Help → Show Log in Explorer`
- Verify plugin is compatible with your IDE version
- Ensure all dependencies are installed
- Try invalidating caches: `File → Invalidate Caches...`

### Issue 2: Tool Window Not Visible
**Symptoms:** SupremeAI tool window missing  
**Solutions:**
- Check `View → Tool Windows` menu
- Reset tool window layout: `Window → Restore Default Layout`
- Verify plugin is enabled in `Settings → Plugins`

### Issue 3: AI Connectivity Issues
**Symptoms:** Status shows "Backend: Connecting..." indefinitely  
**Solutions:**
- Check internet connection
- Verify API endpoint is accessible
- Check firewall/proxy settings
- Review IDE logs for connection errors
- Ensure valid API key is configured

### Issue 4: Gradle Learning Not Working
**Symptoms:** Build failures not captured  
**Solutions:**
- Verify Gradle external system integration is enabled
- Check `Settings → Build, Execution, Deployment → Build Tools → Gradle`
- Ensure "Use Gradle from" is set to 'gradle-wrapper.properties'
- Review IDE logs for Gradle listener errors

### Issue 5: Code Inspection Not Running
**Symptoms:** SupremeAI inspections not detecting issues  
**Solutions:**
- Enable inspections: `Settings → Editor → Inspections → Kotlin → SupremeAI`
- Ensure inspection severity is not disabled
- Rebuild project: `Build → Rebuild Project`

---

## Performance Monitoring

### Resource Usage
- Monitor IDE memory usage: `Help → Diagnostic Tools → Activity Monitor`
- Check CPU usage during plugin operations
- Verify WebSocket connection doesn't cause excessive network traffic

### Metrics Collection
- Plugin collects anonymous usage metrics
- Metrics sent via WebSocket to dashboard
- Review metrics in `SupremeAIMetricsService` logs

---

## Testing Checklist

### Functional Tests
- [ ] Plugin loads without errors
- [ ] All UI components render correctly
- [ ] Chat functionality works
- [ ] Code actions execute properly
- [ ] Settings persist after restart
- [ ] Gradle failure detection works
- [ ] Code inspection runs automatically
- [ ] Generate App feature works with valid API key

### Integration Tests
- [ ] Plugin works with Android projects
- [ ] Plugin works with pure Kotlin/Java projects
- [ ] Plugin compatible with K2 compiler
- [ ] WebSocket connection stable
- [ ] API requests authenticated correctly

### Security Tests
- [ ] API key not exposed in logs
- [ ] HTTPS/WSS connections verified
- [ ] Permission system enforced
- [ ] No sensitive data in error reports

---

## Log Analysis

### IDE Logs Location
- **Windows:** `%LOCALAPPDATA%\Google\AndroidStudio<version>\log\idea.log`
- **macOS:** `~/Library/Logs/Google/AndroidStudio<version>/idea.log`
- **Linux:** `~/.cache/Google/AndroidStudio<version>/log/idea.log`

### Key Log Entries to Monitor
```
INFO - SupremeAI - Connected to SupremeAI Dashboard
WARN - GradleFailureDetector - Gradle external system task failed
INFO - SupremeAILearningClient - SupremeAI has successfully learned from this Android Studio error
ERROR - SupremeAIMetricsService - Connection failed
```

---

## Maintenance

### Regular Checks
- [ ] Weekly: Verify plugin is up to date
- [ ] Monthly: Review API usage and costs
- [ ] Quarterly: Audit security settings and permissions
- [ ] Annually: Review and update plugin configuration

### Updates
- Check for updates: `Settings → Plugins → Updates`
- Review changelog before updating
- Test updates in development environment first
- Backup settings before major updates

---

## Support and Documentation

### Resources
- **Plugin Source:** `supremeai-intellij-plugin/`
- **Main Documentation:** `AI_GUIDE.md`
- **API Documentation:** Backend API endpoints
- **Issue Tracker:** GitHub Issues
- **Support Email:** support@supremeai.com

### Known Limitations
1. Gradle build failure detection requires external system integration
2. Learning functionality requires active internet connection
3. Some features require valid API key
4. K2 compiler support is experimental
5. Plugin performance may vary with large projects

---

## Compliance

### Data Privacy
- Plugin complies with GDPR requirements
- User data encrypted in transit
- No persistent storage of sensitive information
- Audit trail for all learning events

### Security Standards
- OWASP Top 10 compliance
- Secure coding practices
- Regular security audits
- Vulnerability scanning

---

## Quick Reference

### Essential URLs
- **Dashboard:** `https://supremeai-a.web.app`
- **API Base:** `https://supremeai-a.web.app/api/`
- **WebSocket:** `wss://supremeai-a.web.app/ws-supreme`
- **Learning API:** `https://supremeai-lhlwyikwlq-uc.a.run.app/api/knowledge/failure`

### Default Settings
```
API Endpoint: https://supremeai-a.web.app
Model: SupremeAI-v1 (Stable)
Small Model: SupremeAI-v1 (Flash)
Kimo Mode: false
Full Authority: false
Share Mode: manual
```

### Keyboard Shortcuts
- **SupremeAI Tool Window:** `Alt+9` (Windows/Linux) or `Cmd+9` (macOS)
- **Ask SupremeAI:** Context menu or `Ctrl+Shift+A` → "Ask SupremeAI"
- **Generate App:** `Ctrl+Shift+A` → "SupremeAI: Generate App"

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.2.0 | 2026-05-03 | Current version - Full feature set |
| 1.1.0 | 2026-04-15 | Added K2 compiler support |
| 1.0.0 | 2026-03-01 | Initial release |

---

## Feedback and Contributions

To report issues or suggest improvements:
1. Check existing issues in GitHub repository
2. Create new issue with detailed description
3. Include IDE logs if applicable
4. Provide steps to reproduce
5. Suggest potential solutions if possible

---

*This verification guide is maintained by the SupremeAI team. Last reviewed: 2026-05-03*