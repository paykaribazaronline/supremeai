# SupremeAI Glossary

**Version:** 1.0  
**Last Updated:** April 5, 2026  
**Purpose:** Centralized terminology reference for SupremeAI

---

## A

### Agent

An AI-powered component that performs specific tasks in the system. See also: X-Builder, Y-Reviewer, Z-Architect.

### Agent Orchestration

The process of coordinating multiple AI agents to work together on a project, managing their execution order and data flow.

### API Key Manager

Admin dashboard feature for securely storing and managing LLM provider API keys (Gemini, OpenAI, DeepSeek, etc.).

### APK (Android Package Kit)

The package file format used by Android operating system for distribution and installation of mobile apps.

---

## B

### Bootstrap Token

A special authentication token used for initial system setup and creating the first admin user.

### Builder (X-Builder)

AI agent responsible for code generation and implementation. Takes designs from Architect and produces source code.

---

## C

### Circuit Breaker

A resilience pattern that prevents cascade failures by temporarily blocking requests to failing services.

### Cloud Brain

Layer 1 of the architecture - Firebase-based orchestration and consensus engine that coordinates AI agents.

### Consensus Engine

System component that validates AI agent outputs by requiring 70% agreement before proceeding.

### Consensus Percentage

The threshold (70%) of AI agents that must agree on a decision before it's approved.

---

## D

### DeepSeek

Third-party LLM provider used as a tertiary AI provider in the failover chain.

### Distributed Tracing

System for tracking requests as they flow through multiple services, using trace IDs and span IDs.

---

## E

### Enterprise Resilience Layer

Advanced system features including distributed tracing, circuit breakers, and automatic failover mechanisms.

### Error Fixing Controller

API component that provides auto-healing capabilities to detect and fix errors without human intervention.

---

## F

### Failover

Automatic switching to a backup system or provider when the primary fails.

### Firebase

Google's mobile platform used for authentication, database, and hosting services.

### Firestore

Firebase's NoSQL document database used for persistent storage of project metadata.

### Flutter

Google's UI toolkit used for building the admin dashboard and mobile applications.

---

## G

### Gemini

Google's LLM (primary AI provider) used for code generation and design tasks.

### Groq

High-performance inference provider used as a quaternary fallback option.

---

## K

### King Mode

Admin override capability that allows bypassing consensus for critical decisions.

### Knowledge Base

Centralized repository of learned patterns, solutions, and best practices accumulated by the system.

---

## L

### Layer 0 (AI Brain)

The shared memory and performance tracking layer of the architecture.

### Layer 1 (Cloud Brain)

Firebase-based orchestration layer managing agent coordination.

### Layer 2 (AI Agents)

The AI agent layer containing Architect, Builder, and Reviewer agents.

### Layer 3 (App Generator)

The code generation layer producing actual application code.

### Learning System

Component that analyzes execution results and improves future performance based on patterns.

### LLM (Large Language Model)

AI models like Gemini, GPT-4, or DeepSeek used for code generation and reasoning.

---

## M

### MDC (Mapped Diagnostic Context)

Logging context used for distributed tracing, storing trace IDs and span IDs.

### Monitoring Dashboard

Real-time web interface showing system metrics, project status, and agent activity.

### MuonClip

Agent memory system that stores context and learnings for individual AI agents.

### Multi-Agent System

Architecture where multiple specialized AI agents collaborate on tasks.

---

## O

### OpenAI

Provider of GPT models, used as a secondary AI provider in the failover chain.

### Orchestration

The coordination and management of multiple AI agents working on a project.

---

## P

### Phase

Major development milestone in the SupremeAI roadmap (Phases 1-10).

### Phoenix

Self-healing system component that can recover from failures automatically.

### Project

A complete app generation task from requirements to deployed APK.

---

## Q

### Quota Management

System for tracking and limiting API usage across different LLM providers.

---

## R

### Realtime Database

Firebase feature used for live data synchronization and agent communication.

### Resilience

The system's ability to recover from failures and continue operating.

### Reviewer (Y-Reviewer)

AI agent responsible for quality assurance, testing, and validation of generated code.

---

## S

### Safe Zone Protection

Feature that prevents critical agents from being modified during active operations.

### Self-Healing

System capability to automatically detect and fix errors without human intervention.

### Shared Memory

Central data store accessible by all AI agents for coordination.

### Spring Boot

Java framework used for building the SupremeAI backend application.

### SupremeAI

The complete multi-agent AI system for automated app generation.

---

## T

### Teaching System

Component that allows training SupremeAI on new patterns and error solutions.

### Template Engine

System for generating code based on predefined patterns and frameworks.

### Trace ID

Unique identifier assigned to a request for distributed tracing purposes.

### Troubleshooting

The process of diagnosing and resolving system issues.

---

## W

### WebSocket

Protocol used for real-time communication between server and monitoring dashboard.

---

## Z

### Z-Architect

AI agent responsible for system design, architecture planning, and optimization.

### Z-Score

Statistical measure used in anomaly detection for monitoring system health.

---

## Acronyms

| Acronym | Full Form |
|---------|-----------|
| AI | Artificial Intelligence |
| API | Application Programming Interface |
| APK | Android Package Kit |
| CI/CD | Continuous Integration/Continuous Deployment |
| CORS | Cross-Origin Resource Sharing |
| GCP | Google Cloud Platform |
| GPT | Generative Pre-trained Transformer |
| HMAC | Hash-based Message Authentication Code |
| HTTP | Hypertext Transfer Protocol |
| IAM | Identity and Access Management |
| JWT | JSON Web Token |
| LLM | Large Language Model |
| MDC | Mapped Diagnostic Context |
| REST | Representational State Transfer |
| SMS | Short Message Service |
| SMTP | Simple Mail Transfer Protocol |
| SQL | Structured Query Language |
| SSL | Secure Sockets Layer |
| TLS | Transport Layer Security |
| UI | User Interface |
| URL | Uniform Resource Locator |
| UUID | Universally Unique Identifier |
| YAML | YAML Ain't Markup Language |

---

## Related Documentation

- [Architecture Overview](../02-ARCHITECTURE/ARCHITECTURE_AND_IMPLEMENTATION.md)
- [Quick Start Guide](../00-START-HERE/QUICK_START_5MIN.md)
- [API Reference](../13-REPORTS/API_ENDPOINT_INVENTORY.md)

---

**Last Updated:** April 5, 2026  
**Maintained by:** SupremeAI Documentation Team  
**Status:** ✅ Complete
