# MERMD - Learning & Knowledge Management

## Overview

The Learning feature enables SupremeAI to continuously learn from user code, internet sources, and AI interactions to improve its capabilities.

## How It Works

### Architecture Flow

```
Code/User Input → Learning Router → Knowledge Base → AI Provider → Cached Solutions
```

### Key Components

| Component                     | File                                           | Purpose                      |
| ----------------------------- | ---------------------------------------------- | ---------------------------- |
| `SupremeLearningOrchestrator` | `learning/SupremeLearningOrchestrator.java`    | Main learning orchestrator   |
| `SelfLearningRouter`          | `learning/SelfLearningRouter.java`             | Routes learning requests     |
| `GlobalKnowledgeBase`         | `learning/knowledge/GlobalKnowledgeBase.java`  | Central knowledge repository |
| `SolutionMemory`              | `learning/knowledge/SolutionMemory.java`       | Stores solutions             |
| `EnhancedSelfLearningRouter`  | `learning/EnhancedSelfLearningRouter.java`     | Enhanced learning router     |
| `ActiveLearnerCron`           | `learning/active/ActiveLearnerCron.java`       | Scheduled learning tasks     |
| `KnowledgeSeederService`      | `learning/service/KnowledgeSeederService.java` | Knowledge initialization     |
| `UserCodeLearningService`     | `learning/UserCodeLearningService.java`        | Learns from user code        |

### Learning Sources

#### 1. User Code Learning

- Monitors user repositories
- Extracts patterns and solutions
- Stores in knowledge base

#### 2. Active Internet Learning

- `ActiveInternetScraper` - Web scraping
- `WikipediaExtractor` - Wikipedia content
- `StackOverflowExtractor` - Stack Overflow Q&A
- `SiteAuthority` - Source credibility scoring

#### 3. AI Provider Feedback

- Tracks provider performance
- `AIProviderPerformanceTracker` - Performance metrics
- `TrackingAIProviderDecorator` - Decorator for tracking

### Knowledge Base Structure

#### SolutionMemory

```java
class SolutionMemory {
    String problemHash;
    String solutionCode;
    String provider;
    double confidenceScore;
    List<String> tags;
}
```

#### KnowledgeBaseEntry

```java
class KnowledgeBaseEntry {
    String id;
    String problemStatement;
    String solution;
    String category;
    List<String> relatedTags;
    LocalDateTime createdAt;
    LocalDateTime lastAccessed;
    int accessCount;
}
```

### Learning Modes

| Mode       | Description                        |
| ---------- | ---------------------------------- |
| `ACTIVE`   | Actively scrapes and learns        |
| `PASSIVE`  | Learns only from user interactions |
| `DISABLED` | Learning is paused                 |

### Services

#### EnhancedWebScraperService

- Scrapes web content for learning
- Content sanitization
- Source authority validation

#### AutonomousSkillDiscoveryService

- Discovers new skills from code patterns
- Creates skill definitions automatically

#### LearningQuotaService

- Manages learning API quotas
- Tracks usage per user/provider

### Data Models

#### AIProviderPerformanceTracker

- Tracks latency, success rate, cost
- Updates provider rankings

#### LearningActivityLogService

- Logs all learning activities
- Audit trail for knowledge changes

### Integration Points

- `KnowledgeBaseController` - REST API for knowledge
- `EnhancedLearningController` - Learning management
- `AuditLoggingAspect` - Audit trails
