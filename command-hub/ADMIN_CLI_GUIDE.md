# SupremeAI CommandHub - Admin Guide & Tips

## 🎯 Quick Start for New CLI Users

Welcome to SupremeAI CommandHub! This guide helps you use the admin commands safely and effectively.

### **What is CommandHub?**

CommandHub is the command-line interface (CLI) for controlling SupremeAI. Think of it like texting instructions to your AI system - you type commands and get results back.

### **Basic Philosophy**

- **Safe First**: Start with read-only commands (viewing data)
- **Test in WAIT Mode**: Make changes carefully, one at a time
- **Verify Everything**: Check results before moving forward
- **Use Audit**: Track what you've done

---

## 📚 Understanding the Three Control Modes

### 1. ⚡ **AUTO Mode** - Instant Execution

```bash
supremeai admin mode:set --mode=auto
```

**What it does:** Commands execute immediately without approval  
**When to use:** After 1+ week of experience in WAIT mode  
**Safety:** ⚠️ Lower (needs experience)

**Example:**

```bash
$ supremeai admin mode:set --mode=auto
$ supremeai admin commit --message="Deploy new feature"
>> Commit created and pushed immediately!
```

---

### 2. ⏸️ **WAIT Mode** - Requires Approval (RECOMMENDED)

```bash
supremeai admin mode:set --mode=wait
```

**What it does:** Commands create pending actions you must approve  
**When to use:** Always while learning (first 1-2 weeks)  
**Safety:** ✅ Highest (recommended for beginners)

**Example:**

```bash
$ supremeai admin mode:set --mode=wait
$ supremeai admin commit --message="Fix bug"
>> ✓ Action pending approval (ID: 12345)
$ supremeai admin approve --id=12345
>> ✓ Approved! Commit created.
```

---

### 3. 🛑 **STOP Mode** - Emergency Halt

```bash
supremeai admin mode:set --mode=stop --reason="Something seems wrong"
```

**What it does:** System stops all operations immediately  
**When to use:** Emergency situations, something unexpected happened  
**Safety:** ✅ Always the right choice when unsure

---

## 🚀 Basic Commands Explained

### **1. Check System Status**

```bash
# See current mode and health
supremeai admin status

# Output:
# Mode: WAIT (orange)
# Health: 95%
# Running: true
# Last sync: 2 min ago
```

### **2. View Pending Actions**

```bash
# See what's waiting for approval
supremeai admin pending

# Output:
# ID: 12345  |  Git Commit  |  "Fix typo"
# ID: 12346  |  Git Push    |  To main
```

### **3. Approve or Reject Actions**

```bash
# Approve an action (after reviewing it)
supremeai admin approve --id=12345

# Reject an action (cancel it)
supremeai admin reject --id=12345
```

### **4. Make a Git Commit**

```bash
# Create a commit (will wait for approval in WAIT mode)
supremeai admin commit \
  --message="Fixed admin dashboard tips" \
  --author="admin@supremeai.com"

# In WAIT mode: you'll see pending action
# In AUTO mode: commits immediately
```

### **5. Push to Remote**

```bash
# Push commits to repository
supremeai admin push --branch=main
```

### **6. View Audit Trail**

```bash
# See all actions that happened
supremeai admin audit

# With filters:
supremeai admin audit --limit=20        # Last 20 actions
supremeai admin audit --user=admin@...  # By specific user
supremeai admin audit --type=commit     # Only commits
```

---

## 💡 Beginner Tips & Best Practices

### **Tip 1: Always Start in WAIT Mode**

```bash
# First thing: Set yourself to WAIT mode
supremeai admin mode:set --mode=wait
supremeai admin status  # Verify it worked
```

### **Tip 2: Make Small Test Changes**

```bash
# Don't make big changes. Test small ones first.
supremeai admin commit --message="Test message"
supremeai admin pending  # See it waiting
supremeai admin approve --id=<ID>  # Approve it

# Now you understand the flow!
```

### **Tip 3: Always Check Status Before Acting**

```bash
# Before making changes:
supremeai admin status     # Check mode
supremeai admin pending    # See what's waiting
supremeai admin audit --limit=5  # See recent actions
```

### **Tip 4: Use Audit Trail to Debug**

```bash
# Something went wrong? Check the history:
supremeai admin audit

# Search for your recent actions
supremeai admin audit --limit=10

# Look for: What happened? When? By whom? Success or error?
```

### **Tip 5: Review Before Approving**

```bash
# Never approve without reading carefully
supremeai admin pending  # See pending action details

# Read:
# - What will happen?
# - Is this correct?
# - Do I understand it?

# THEN approve:
supremeai admin approve --id=<ID>
```

---

## ⚠️ Safety Rules (Important!)

### **Rule 1: Never Use AUTO Mode First**

```bash
# ❌ DON'T:
supremeai admin mode:set --mode=auto

# ✅ DO:
supremeai admin mode:set --mode=wait
# Use WAIT for first week
```

### **Rule 2: Stop if Unsure**

```bash
# If something seems wrong:
supremeai admin mode:set --mode=stop --reason="Investigating issue"

# Then review:
supremeai admin audit
supremeai admin pending

# Then resume safely:
supremeai admin mode:set --mode=wait
```

### **Rule 3: Read Audit Trail Regularly**

```bash
# Daily practice:
supremeai admin audit --limit=20

# This shows you:
# - What was done
# - When it happened
# - If it succeeded
# - Helps you learn
```

### **Rule 4: One Change at a Time**

```bash
# Make small changes one at a time
supremeai admin commit --message="Change 1"
supremeai admin pending
supremeai admin approve --id=<ID>

# Wait for it to complete, then next change:
supremeai admin commit --message="Change 2"
```

### **Rule 5: Keep Backups of Important Config**

```bash
# Save important settings before changing them
supremeai admin config:backup
supremeai admin config:get --key=important_setting

# If something breaks, you can restore
supremeai admin config:restore --from=<backup_id>
```

---

## 🔍 Common Questions Answered

### **Q: What does "pending approval" mean?**

A: The action is created but not executed yet. You must review and approve it.

```bash
supremeai admin pending  # See it waiting
supremeai admin approve --id=<ID>  # Execute it
```

---

### **Q: How do I know if my command worked?**

A: Check the response message and audit trail.

```bash
supremeai admin commit --message="Test"
# Response will say: ✓ Success or ❌ Error

# Verify in audit:
supremeai admin audit --limit=1
```

---

### **Q: Can I undo a mistake?**

A: You can't undo automatically, but you can fix it.

```bash
# Made a wrong change? Create a fix:
supremeai admin commit --message="Revert: undo previous"

# The system will have both changes logged (audit shows everything)
```

---

### **Q: What if I (forgot which actions need approval?**

A: Use the pending command.

```bash
supremeai admin pending
# Shows all actions waiting for approval
```

---

### **Q: How do I change modes safely?**

A: Use explicit mode setting with verification.

```bash
supremeai admin mode:set --mode=wait
supremeai admin status  # Verify it changed
```

---

### **Q: What does the audit trail show?**

A: Complete record of everything.

```bash
supremeai admin audit

# Shows:
# Time | User | Action | Status | Details
```

---

## 🎯 Your First Week Plan

### **Day 1: Setup & Observation**

```bash
# Just read:
supremeai admin status
supremeai admin audit --limit=10
supremeai admin pending

# Don't change anything yet
```

### **Day 2-3: Make Small Test Changes**

```bash
# In WAIT mode only:
supremeai admin mode:set --mode=wait
supremeai admin commit --message="Test 1"
supremeai admin pending  # See it
supremeai admin approve --id=<ID>  # Approve it
supremeai admin audit  # See it happened
```

### **Day 4-5: Understand Rejection**

```bash
# Let's reject something:
supremeai admin commit --message="Test to reject"
supremeai admin pending

# This time, reject:
supremeai admin reject --id=<ID>

# See? It didn't happen. Check audit:
supremeai admin audit
```

### **Day 6-7: Real Work in WAIT Mode**

```bash
# Do actual work, still reviewing each action:
supremeai admin commit --message="Real change"
supremeai admin pending
supremeai admin approve --id=<ID>

# Repeat for several changes
supremeai admin audit  # See all your work logged
```

---

## 🆘 Troubleshooting

### **Problem: "Command not found"**

```bash
# Make sure SupremeAI is installed:
supremeai --version

# If not:
npm install -g supremeai
# or
pip install supremeai
```

---

### **Problem: "Authentication failed"**

```bash
# Login first:
supremeai login --email=admin@supremeai.com

# Token might have expired (24 hour expiry):
supremeai auth:refresh
```

---

### **Problem: "Mode change didn't work"**

```bash
# Verify the change:
supremeai admin status

# If still wrong, try again:
supremeai admin mode:set --mode=wait
supremeai admin status  # Check again
```

---

### **Problem: "Can't see pending action"**

```bash
# Might take 5-10 seconds to appear:
sleep 5
supremeai admin pending

# Or check status:
supremeai admin status

# Check audit to see what happened:
supremeai admin audit --limit=5
```

---

### **Problem: "Something went wrong - don't know what"**

```bash
# STOP first:
supremeai admin mode:set --mode=stop --reason="Investigating error"

# Then investigate:
supremeai admin status
supremeai admin pending
supremeai admin audit --limit=20

# Read the details carefully
# Then resume:
supremeai admin mode:set --mode=wait
```

---

## 📋 CLI Cheat Sheet

```bash
# Status & Info
supremeai admin status              # Current status
supremeai admin pending             # Pending actions
supremeai admin audit               # Action history
supremeai admin audit --limit=20    # Last 20 actions

# Mode Control
supremeai admin mode:set --mode=auto              # Fast (experienced only)
supremeai admin mode:set --mode=wait              # Safe (recommended)
supremeai admin mode:set --mode=stop              # Emergency

# Actions
supremeai admin commit --message="..."            # Create commit
supremeai admin push --branch=main                # Push to repo
supremeai admin approve --id=<ID>                 # Approve action
supremeai admin reject --id=<ID>                  # Reject action

# Config
supremeai admin config:get --key=<key>            # View setting
supremeai admin config:set --key=<key> --value=<val>  # Change setting
supremeai admin config:backup                     # Backup config
supremeai admin config:restore --from=<backup>    # Restore config

# Help
supremeai admin --help                # Show all commands
supremeai admin <command> --help      # Help for specific command
```

---

## 🎓 Advanced TipsAfter 1-2 Weeks)

### **Use Filters in Audit**

```bash
supremeai admin audit --type=commit --status=success
supremeai admin audit --user=admin@... --since=2h
```

---

### **Batch Multiple Changes Safely**

```bash
# Create multiple pending actions:
supremeai admin commit --message="Change 1"
supremeai admin commit --message="Change 2"
supremeai admin commit --message="Change 3"

# Review all:
supremeai admin pending

# Approve all:
supremeai admin approve --id=<ID1>
supremeai admin approve --id=<ID2>
supremeai admin approve --id=<ID3>
```

---

### **Auto-Approve in AUTO Mode**

```bash
# Only after a week+ in WAIT mode:
supremeai admin mode:set --mode=auto
supremeai admin commit --message="This executes immediately"
supremeai admin push --branch=main
```

---

## 📞 Getting Help

**If you're stuck:**

1. Run `supremeai admin --help`
2. Check Audit Trail: `supremeai admin audit`
3. Read this guide again
4. Contact your SupremeAI administrator

---

## 🌟 Final Thoughts

- **Start in WAIT mode** - It's the safest
- **Review before approving** - Know what you're doing
- **Use Audit Trail** - It's your proof and learning tool
- **Take notes as you learn** - You're becoming an expert!
- **Take your time** - Learning properly takes 1 week, mastering takes 1 month

---

**Version:** 1.0  
**Last Updated:** April 9, 2026  
**For:** SupremeAI CommandHub v1.0+
