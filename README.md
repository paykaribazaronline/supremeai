# 🚀 SupremeAI - Multi-Agent App Generator System v3.1

> **AI-powered multi-agent system for automated Android app generation using Firebase, Google Cloud, and collaborative AI agents.**

[![Java CI Build & Test](https://github.com/paykaribazaronline/supremeai/actions/workflows/java-ci.yml/badge.svg)](https://github.com/paykaribazaronline/supremeai/actions/workflows/java-ci.yml)

[![Firebase Hosting](https://img.shields.io/badge/Firebase-Active-green?style=flat-square&logo=firebase)](https://github.com/paykaribazaronline/supremeai)
[![Java](https://img.shields.io/badge/Java-17-blue?style=flat-square&logo=oracle)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2.3-brightgreen?style=flat-square&logo=spring-boot)](https://spring.io/projects/spring-boot)
[![License](https://img.shields.io/badge/License-MIT-yellow?style=flat-square)](LICENSE)
[![Contributions Welcome](https://img.shields.io/badge/Contributions-Welcome-brightgreen?style=flat-square)](CONTRIBUTING.md)

---

## 🎯 Vision

> **"AI works, admin watches, approves when needed, and gets the APK."**

SupremeAI is a fully automated app development system where multiple AI agents collaborate to build, test, and deploy Android applications with minimal human intervention.

---

## ✨ Key Features

### 🤖 Multi-Agent Intelligence System

- **X-Builder:** Code generation and implementation

- **Y-Reviewer:** Code quality assurance and testing

- **Z-Architect:** System design and optimization

- **Consensus Engine:** 70% approval requirement for major decisions

- **King Mode:** Admin override capability for critical decisions

### 📊 Advanced Analytics & Monitoring

- Real-time metrics streaming via WebSocket (2-second push intervals)

- Historical metrics persistence with Firestore

- Z-score anomaly detection (3-sigma analysis)

- Failure prediction using linear regression

- Auto-scaling recommendations based on resource utilization

### 🔔 Multi-ChannelNotifications

- Email alerts with SMTP integration

- Slack notifications with color-coded embeds

- Discord alerts with severity indicators

- SMS via Twilio for critical alerts

- Severity-based escalation policies

### 🏗️ Core Architecture

- **Layer 0: AI Brain** - Shared memory & performance scoreboard.

- **Layer 1: Cloud Brain (Firebase)** - Orchestration & Consensus engine.

- **Layer 2: AI Agents** - X-Builder, Y-Reviewer, Z-Architect.

- **Layer 3: App Generator** - Template-based code generation.

---

## 🚀 Quick Start

### Prerequisites

- **Java:** JDK 17+ ([Download](https://www.oracle.com/java/technologies/downloads/))

- **Gradle:** 8.7+ (bundled with project)

- **Firebase Account:** Free tier suffices

- **Git:** Latest version

### 1. Clone & Setup

```bash

# Clone repository

git clone https://github.com/paykaribazaronline/supremeai.git
cd supremeai

# Build project

./gradlew clean build

# Run tests

./gradlew test

```

### 2. Configure Environment

```bash

# Copy example environment file

cp .env.example .env

# Edit with your credentials

export FIREBASE_SERVICE_ACCOUNT=$(cat service-account.json | base64)
export GEMINI_API_KEY=your_gemini_key
export DEEPSEEK_API_KEY=your_deepseek_key

```

⚠️ **Security Warning:** Never commit `.env`, `service-account.json`, or API keys! See [Security Guide](SECURITY_GUIDE.md).

### 3. Start Development Server

```bash

# Run Spring Boot application

./gradlew run

# Application starts at http://localhost:8080

# Admin dashboard: http://localhost:8080/admin

# Monitoring dashboard: http://localhost:8080/monitoring

```

### 4. Deploy

```bash

# Firebase Hosting (automatic via GitHub Actions)

git push origin main

# Google Cloud (manual trigger)

gcloud builds submit

# Docker (local testing)

docker build -t supremeai:latest .
docker run -p 8080:8080 supremeai:latest

```

---

## 📊 Implementation Phases

| Phase | Status | Features |
|-------|--------|----------|
| **1** | ✅ Complete | Foundation, Auth, Admin |

| **2** | ✅ Complete | Intelligence & Ranking |

| **3** | ✅ Complete | App Generator |

| **4** | ✅ Complete | Monitoring |

| **4.1** | ✅ Complete | WebSocket, Real-time |

| **5** | ✅ Complete | Analytics & ML |

| **6-7** | 🏗️ Planned | Viz & Automation |

---

## 📡 API Reference

**Analytics:** 8 endpoints | **Notifications:** 8 endpoints | **ML:** 6 endpoints
**Monitoring:** 42+ endpoints | **Performance:** 16+ endpoints

See [PHASE5_COMPLETE.md](PHASE5_COMPLETE.md) for detailed API specs.

---

## 🔐 Security

⚠️ **Never commit:**

- `.env` files with secrets

- `service-account.json` credentials  

- API keys or encryption keys

**See:** [SECURITY_GUIDE.md](SECURITY_GUIDE.md) | [AUDIT](SECURITY_AUDIT_REPORT.md)

---

## 📚 Documentation

- [CONTRIBUTING.md](CONTRIBUTING.md) - How to contribute

- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) - Community standards

- [PROJECT_ROADMAP.md](PROJECT_ROADMAP.md) - Future plans

- [ADMIN_OPERATIONS_GUIDE.md](ADMIN_OPERATIONS_GUIDE.md) - Admin guide

---

## 🤝 Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

---

## 📄 License

MIT License - [LICENSE](LICENSE)

---

## 🎯 Status (March 29, 2026)

✅ **Phases 1-5:** Complete (9,000+ LOC)  

📊 **Build:** SUCCESS | 🔄 **Deployments:** Active

---
"AI কাজ করে, Admin দেখে, Approve করে, APK পায়" 🎉
