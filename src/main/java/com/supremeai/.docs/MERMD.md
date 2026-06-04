# MERMD - SupremeAI Backend Documentation

## Overview
This document provides a comprehensive view of how major features in the SupremeAI backend system work.

## Package Structure

### Core Packages

| Package | Purpose | Key Classes |
|---------|---------|-------------|
| `com.supremeai` | Application entry point | `Application.java` |
| `com.supremeai.config` | Configuration classes | `SecurityConfig.java`, `WebConfig.java` |
| `com.supremeai.controller` | REST API endpoints | `SimulatorController.java`, `ChatController.java`, `AuthenticationController.java` |
| `com.supremeai.service` | Business logic layer | `SimulatorService.java`, `AuthenticationService.java`, `AIProviderService.java` |
| `com.supremeai.dto` | Data transfer objects | Various request/response DTOs |
| `com.supremeai.model` | Domain models | `UserSimulatorProfile.java`, `SimulatorDeploymentRecord.java` |
| `com.supremeai.repository` | Data persistence | Repository interfaces for Firestore |
| `com.supremeai.filter` | Security filters | `AuthenticationFilter.java`, `JwtAuthFilter.java` |
| `com.supremeai.exception` | Exception handling | `GlobalExceptionHandler.java`, simulator-specific exceptions |
| `com.supremeai.security` | Security utilities | JWT utilities, encryption, rate limiting |

### Feature Packages

| Package | Purpose |
|---------|---------|
| `com.supremeai.simulator` | Android simulator orchestration |
| `com.supremeai.swarm` | AI agent coordination |
| `com.supremeai.learning` | Self-learning and knowledge management |
| `com.supremeai.intelligence` | AI reasoning and voting systems |
| `com.supremeai.generation` | Code generation capabilities |
| `com.supremeai.codeflow` | Code analysis and flow management |
| `com.supremeai.ml` | Machine learning models |
| `com.supremeai.selfhealing` | Automatic error recovery |

## Key Features Flow

### 1. Authentication Flow
```
Client Request → AuthenticationFilter → Firebase Token Validation → JwtAuthFilter → SecurityContext
```
- `AuthenticationController`: Handles login/register endpoints
- `AuthenticationService`: Validates credentials and generates tokens
- `JwtUtil`: Creates and validates JWT tokens

### 2. Simulator Management Flow
```
Client API → SimulatorController → SimulatorService → Deployment Orchestrator → Android Emulator
```
- `SimulatorController`: REST endpoints for install/start/stop
- `SimulatorService`: Business logic for simulator operations
- `SimulatorDeploymentService`: Manages app deployment
- `DeviceEmulationService`: Handles device profiles

### 3. AI Chat Flow
```
User Query → ChatController → NeuralChatService → MultiAIConsensusService → Provider API
```
- `ChatController`: Entry point for chat requests
- `NeuralChatService`: Routes to appropriate AI provider
- `MultiAIConsensusService`: Aggregates responses from multiple providers
- `VotingController`: Handles voting-based decision making

### 4. Code Generation Flow
```
Request → AppGenerationController → CodeGenerationService → File System → Response
```
- `AppGenerationController`: Handles app generation requests
- `CodeGenerationService`: Generates Flutter/Android code
- `FullStackCodeGenerator`: Multi-platform code generation

## Security Architecture
- Firebase Authentication for user management
- JWT tokens for API authentication
- Role-based access control (ADMIN, USER roles)
- Rate limiting via Redis/In-memory limiters
- CSRF protection for state-changing operations

## Data Persistence
- Firestore for primary data storage
- Reactive repositories for non-blocking operations
- Audit logging via `@Audited` annotation