# SupremeAI System Archival & Backup Architecture

## Overview
SupremeAI implements a **Hybrid Storage Strategy (Hot & Cold)** to ensure maximum performance, infinite scalability, and cost-efficiency. This architecture offloads large or older datasets from primary databases (Firestore) to long-term storage (Telegram/Teldrive).

## 1. Hot vs. Cold Storage
| Feature | Hot Storage (Firestore) | Cold Storage (Telegram/Teldrive) |
| :--- | :--- | :--- |
| **Data Type** | Recent chats, active learnings, system config | Archived chats, full learning history, codebase backups |
| **Latency** | Extremely Low (Real-time) | Medium (Download on demand) |
| **Cost** | Limited (Pay per read/write) | Zero (Unlimited storage) |
| **Persistence** | Operational state | Long-term archival |

## 2. Automated Archival Processes

### A. Chat Archival (`ChatArchiveService`)
- **Trigger**: Every 6 hours (Scheduled).
- **Logic**: For any user with more than 50 messages, the oldest messages are moved to Telegram.
- **Retention**: Keeps the last 20 messages in Firestore for instant access.
- **Storage Path**: `/supremeai/archives/chats/{userId}/`

### B. System Learning Archival (`LearningArchiveService`)
- **Trigger**: Every 12 hours (Scheduled).
- **Logic**: Records older than 7 days are archived to JSON files and moved to Telegram.
- **Retention**: Always maintains a minimum of 100 recent learning records in Firestore.
- **Storage Path**: `/supremeai/archives/learning/`

### C. Whole Codebase Backup (`CodebaseBackupService`)
- **Trigger**: Every Sunday at 3 AM (Scheduled) and Manual.
- **Logic**: Zips the entire repository while excluding bulky directories (`node_modules`, `build`, `.git`, etc.).
- **Security**: Backups are encrypted and stored in a secure Telegram channel.
- **Storage Path**: `/supremeai/backups/codebase/`

## 3. Dashboard Integration
The **Telegram Drive** tab in the Admin Dashboard provides:
- **Bot Status**: Real-time health monitoring of the Telegram/Teldrive bot.
- **Manual Triggers**: Buttons to force immediate backup or archival cycles.
- **File Management**: View and download archived files (JSON, ZIP) directly.
- **Storage Metrics**: Track the total amount of cold storage used.

## 4. Security & Integrity
- **AES-256-GCM**: Data is encrypted before being sent to cold storage.
- **Atomic Operations**: Records are only deleted from Firestore AFTER a successful upload confirmation from the Telegram service.
- **Fail-safe**: If the Telegram bot is offline, the system continues to use Firestore without deleting any data.

---
*Created on 2026-05-14 by SupremeAI Architect.*
