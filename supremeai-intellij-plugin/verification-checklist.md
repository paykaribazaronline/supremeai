# SupremeAI IntelliJ Plugin - Verification Checklist

## Quick Installation Verification

Use this checklist to quickly verify that the SupremeAI plugin is properly installed and functioning in Android Studio.

### Pre-Installation Checks

- [ ] Android Studio 2024.3.2.15+ is installed
- [ ] Java 21 JDK is configured
- [ ] Kotlin plugin is enabled
- [ ] Android plugin is installed

### Installation Verification

#### 1. Plugin Installation Status
- [ ] Plugin appears in `Settings → Plugins → Installed`
- [ ] Plugin version is 1.2.0
- [ ] Plugin is enabled (checkbox is checked)
- [ ] No error messages in plugin status

#### 2. IDE Restart
- [ ] Android Studio restarted after installation
- [ ] No startup errors in IDE log
- [ ] Plugin loaded successfully message appears

### UI Component Verification

#### 3. Tool Window
- [ ] SupremeAI tool window appears in right sidebar
- [ ] Tool window icon (ℹ️) is visible
- [ ] Clicking icon opens tool window
- [ ] Tool window has 3 tabs: Chat, Orchestration, Settings

#### 4. Status Bar
- [ ] SupremeAI status bar widget visible in bottom-right
- [ ] Clicking widget opens tool window
- [ ] Status shows connection state

#### 5. Context Menu Actions
- [ ] "Ask SupremeAI" appears in editor context menu
- [ ] Action is enabled when code is selected
- [ ] Action sends selected code to chat

#### 6. Code Inspection
- [ ] SupremeAI inspections enabled in Settings
- [ ] Inspections run on Kotlin files
- [ ] Agent pattern warnings appear
- [ ] RecyclerView suggestions appear

#### 7. Settings Panel
- [ ] Settings accessible via `Settings → Tools → SupremeAI`
- [ ] API Key field is present
- [ ] API Endpoint field is present
- [ ] Model selection dropdown works
- [ ] Kimo Mode toggle works
- [ ] Permission settings are configurable

### Functionality Verification

#### 8. Chat Functionality
- [ ] Chat tab is accessible
- [ ] Messages can be sent
- [ ] Messages appear in chat history
- [ ] Status shows connection state

#### 9. AI Connectivity
- [ ] Status changes from "Connecting..." to "Connected"
- [ ] WebSocket connection established
- [ ] No connection errors in logs

#### 10. Generate App Feature
- [ ] Generate App action is available
- [ ] Action prompts for project name
- [ ] API request is sent (requires API key)
- [ ] Success/failure notification appears

#### 11. Learning Features
- [ ] Document change listeners are active
- [ ] Gradle failure detection is enabled
- [ ] Build failures are captured (test with syntax error)
- [ ] Learning client sends errors to backend

### Security Verification

#### 12. API Key Management
- [ ] API key field masks input
- [ ] Key is not visible in plain text
- [ ] Settings are saved securely

#### 13. Permission System
- [ ] Default permissions are set correctly
- [ ] Permission changes are saved
- [ ] Permission system is enforced

#### 14. Data Transmission
- [ ] All connections use HTTPS/WSS
- [ ] No sensitive data in logs
- [ ] Error reports don't expose secrets

### Performance Verification

#### 15. Resource Usage
- [ ] IDE memory usage is normal
- [ ] CPU usage is reasonable
- [ ] No performance degradation
- [ ] Plugin doesn't slow down IDE

#### 16. Network Usage
- [ ] WebSocket connection is stable
- [ ] No excessive network traffic
- [ ] Bandwidth usage is reasonable

### Testing Checklist

#### 17. Basic Tests
- [ ] Plugin loads without errors
- [ ] All UI components render
- [ ] Settings persist after restart
- [ ] Chat functionality works
- [ ] Code actions execute

#### 18. Integration Tests
- [ ] Works with Android projects
- [ ] Works with Kotlin/Java projects
- [ ] Compatible with K2 compiler
- [ ] Gradle integration works

#### 19. Error Handling
- [ ] Invalid API key handled gracefully
- [ ] Network failures handled
- [ ] Connection timeouts handled
- [ ] Error messages are clear

### Log Verification

#### 20. IDE Logs
- [ ] No plugin loading errors
- [ ] No initialization failures
- [ ] Connection logs appear
- [ ] Learning events logged
- [ ] No security warnings

### Final Verification

#### 21. Complete Workflow Test
- [ ] Install plugin successfully
- [ ] Configure API key
- [ ] Connect to AI service
- [ ] Use chat feature
- [ ] Test code inspection
- [ ] Verify learning features
- [ ] Test Generate App
- [ ] Check all settings
- [ ] Restart IDE
- [ ] Verify persistence

## Quick Test Commands

### Test Plugin Loading
```bash
# Check if plugin is loaded
grep -i "supremeai" idea.log | head -20
```

### Test Gradle Integration
```bash
# Trigger a Gradle sync and check logs
./gradlew clean build 2>&1 | grep -i "supremeai"
```

### Test WebSocket Connection
```bash
# Check connection logs
grep -i "websocket\|connected" idea.log | tail -10
```

## Pass/Fail Criteria

### Must Pass (Critical)
- [ ] Plugin loads without errors
- [ ] UI components render correctly
- [ ] Settings are accessible
- [ ] No startup failures
- [ ] No security warnings

### Should Pass (Important)
- [ ] Chat functionality works
- [ ] AI connectivity is established
- [ ] Code inspection runs
- [ ] Learning features work
- [ ] Settings persist

### Nice to Pass (Optional)
- [ ] Generate App feature works
- [ ] All integrations functional
- [ ] Performance is optimal
- [ ] All tests pass

## Troubleshooting Quick Reference

| Symptom | Likely Cause | Quick Fix |
|---------|-------------|-----------|
| Plugin not loading | Incompatible IDE version | Check IDE version requirements |
| Tool window missing | Plugin disabled | Enable in Settings → Plugins |
| Connection failed | Invalid API key | Configure valid API key |
| Inspections not running | Inspections disabled | Enable in Settings → Inspections |
| Gradle learning not working | External system disabled | Enable Gradle integration |
| Performance issues | Large project | Increase IDE memory |

## Sign-Off

**Verified By:** _________________________  
**Date:** _________________________  
**IDE Version:** _________________________  
**Plugin Version:** 1.2.0  
**Status:** ☐ Passed ☐ Failed  

**Notes:**  
_________________________________________  
_________________________________________

---

*This checklist should be completed after installing or updating the SupremeAI IntelliJ Plugin.*