# MERMD Index - SupremeAI Documentation

## Overview

This index provides links to feature documentation (MERMD - Module Explanation with Real-time Markdown Diagrams) for all major source folders in the SupremeAI project.

## Backend (Spring Boot)

| Folder                                     | Documentation    | Description                      |
| ------------------------------------------ | ---------------- | -------------------------------- |
| `src/main/java/com/supremeai`              | `.docs/MERMD.md` | Overall backend architecture     |
| `src/main/java/com/supremeai/simulator`    | `.docs/MERMD.md` | Android simulator management     |
| `src/main/java/com/supremeai/learning`     | `.docs/MERMD.md` | Self-learning and knowledge base |
| `src/main/java/com/supremeai/intelligence` | `.docs/MERMD.md` | AI reasoning and voting          |
| `src/main/java/com/supremeai/security`     | `.docs/MERMD.md` | Authentication and security      |
| `src/main/java/com/supremeai/service`      | `.docs/MERMD.md` | Business logic layer             |
| `src/main/java/com/supremeai/controller`   | `.docs/MERMD.md` | REST API endpoints               |
| `src/main/java/com/supremeai/model`        | `.docs/MERMD.md` | Domain models                    |
| `src/main/java/com/supremeai/repository`   | `.docs/MERMD.md` | Data persistence                 |
| `src/main/java/com/supremeai/dto`          | `.docs/MERMD.md` | Data transfer objects            |

## Frontend

| Folder          | Documentation    | Description             |
| --------------- | ---------------- | ----------------------- |
| `dashboard/src` | `.docs/MERMD.md` | Admin dashboard (React) |
| `supremeai/lib` | `.docs/MERMD.md` | Mobile app (Flutter)    |

## Infrastructure

| Folder          | Documentation    | Description              |
| --------------- | ---------------- | ------------------------ |
| `functions/src` | `.docs/MERMD.md` | Firebase Cloud Functions |
| `scripts`       | `.docs/MERMD.md` | Utility scripts          |
| `config`        | `.docs/MERMD.md` | Configuration files      |

## Quick Links

### Core Features

- [Simulator](./src/main/java/com/supremeai/simulator/.docs/MERMD.md) - Run Android apps in virtual devices
- [Learning](./src/main/java/com/supremeai/learning/.docs/MERMD.md) - Self-learning from code and web
- [Intelligence](./src/main/java/com/supremeai/intelligence/.docs/MERMD.md) - AI voting and reasoning
- [Security](./src/main/java/com/supremeai/security/.docs/MERMD.md) - Auth, JWT, rate limiting

### API Entry Points

- `SimulatorController` - `/api/simulator/**`
- `ChatController` - `/api/chat/**`
- `AuthenticationController` - `/api/auth/**`
- `KnowledgeBaseController` - `/api/knowledge/**`
