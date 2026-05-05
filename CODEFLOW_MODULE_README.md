# SupremeAI CodeFlow Module

## Overview

SupremeAI CodeFlow is an internal code analysis and visualization engine that integrates with the existing multi-AI architecture (Kimi K2.5 Primary, DeepSeek V3 Fallback, Together AI Backup). It provides code ingestion, analysis, visualization, and error resolution capabilities for both internal development teams and customer-facing dashboards.

## Architecture

### Backend (Spring Boot 3)
- **Firebase/Firestore** for data persistence
- **Multi-AI routing** with fallback logic (Kimi → DeepSeek → Together)
- **Tree-sitter WASM** for code parsing (with Acorn and regex fallbacks)
- **D3.js** for visualization generation

### Frontend (React 18 + TypeScript)
- **Internal Dashboard**: Full-featured analysis with interactive D3 visualizations
- **Customer Widget**: Embeddable, read-only code structure viewer
- **Repo-to-Prompt Engine**: Converts repositories into AI-ready prompts

## Features

### 1. Code Ingestion Layer
- Accept repository URLs (GitHub, GitLab, Bitbucket)
- Direct code uploads via existing file upload system
- Parse 8+ languages: Python, JavaScript/TypeScript, Go, Rust, Java, Ruby, C/C++
- Fallback chain: Tree-sitter → Acorn (JS/TS) → Regex heuristics
- Extract: functions, classes, imports, call references
- Handle embedded scripts in HTML and wiki-links in Markdown

### 2. Analysis Engine (AI-Augmented)
- **Dependency Graph**: Call graph across files with blast radius computation
- **Pattern Detection**: Singleton, Factory, Observer, React hooks, God objects
- **Circular Dependency Detection**
- **Security Scanning**: Hardcoded secrets, eval usage, SQL injection, debug statements
- **Health Scoring**: 100-point scale with A-F grading (90/80/70/60 thresholds)
- **Error Detection**: AI-powered stack trace analysis and fix suggestions
- **Dead Code Detection**: Unused functions, unreachable imports

### 3. Visualization Layer
- **Internal Dashboard**: Interactive D7 graphs (force-directed, treemap, heatmap, arc diagram, circular bundle, dendrogram, sankey)
- **Customer View**: Simplified read-only dependency graph with health score badge and security warnings
- **Error Context View**: Highlight affected files, show blast radius, link to AI-suggested fixes
- **Export**: JSON report, Markdown summary, SVG snapshot

### 4. AI Integration Points
- Route complex analysis through existing AI pipeline
- AI for: pattern recognition, security vulnerability explanation, fix suggestion generation, code ownership inference
- Cache analysis results in Firestore per repository

### 5. Error Resolution Workflow
- Detect error → trigger analysis → identify affected nodes → query AI for fix → present solution
- Track error patterns across repositories for "common fixes" knowledge base
- PR Impact Analysis: analyze changed files, compute risk score, suggest reviewers

### 6. UI/UX
- **Dark mode default**: `#0a0a0c` bg, `#00ff9d` accent, `#f0f0f2` text
- **Draggable panels**: File tree (left), canvas (center), details (right)
- **Topbar**: Repo input, auth selector, analysis trigger
- **Right panel tabs**: Details, Patterns, Security, Code Ownership, Suggestions, Unused Functions, Error Context
- Toast notifications, loading states, modals

### 7. GitHub Action Companion
- **Node.js 24**, zero npm dependencies
- Extracts analyzer logic, walks repo, renders SVG cards (5 styles)
- Commits SVG + JSON state to repo
- Posts sticky PR comment with health score and risk analysis

### 8. Security & Auth
- **GitHub auth tiers**: No token (60/hr), PAT (5000/hr), GitHub App JWT
- All processing in browser or secure VM — code never leaks
- Customer data isolation: sandboxed in Firestore namespace

## Project Structure

```
supremeai/
├── src/main/java/com/supremeai/codeflow/
│   ├── CodeFlowApplication.java
│   ├── config/
│   │   ├── FirestoreConfig.java
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   ├── CodeFlowController.java
│   │   └── ErrorResolutionController.java
│   ├── service/
│   │   ├── CodeFlowService.java
│   │   ├── ErrorResolutionService.java
│   │   ├── CodeAnalyzer.java
│   │   ├── SecurityScanner.java
│   │   ├── PatternDetector.java
│   │   ├── DependencyAnalyzer.java
│   │   └── HealthScorer.java
│   ├── repository/
│   │   ├── CodeRepository.java
│   │   └── CodeFlowRepository.java
│   ├── model/
│   │   ├── CodeRepository.java
│   │   ├── CodeFile.java
│   │   ├── CodeFunction.java
│   │   ├── AnalysisResult.java
│   │   └── HealthScore.java
│   └── dto/
│       ├── AnalysisRequest.java
│       ├── AnalysisResponse.java
│       └── ErrorResolutionRequest.java
├── dashboard/
│   ├── src/
│   │   ├── components/
│   │   │   ├── CodeFlowDashboard.tsx
│   │   │   ├── CodeFlowWidget.tsx
│   │   │   ├── RepoToPromptEngine.tsx
│   │   │   ├── D3ForceGraph.tsx
│   │   │   ├── D3Treemap.tsx
│   │   │   ├── D3Heatmap.tsx
│   │   │   ├── D3ArcDiagram.tsx
│   │   │   ├── D3CircularBundle.tsx
│   │   │   ├── D3Dendrogram.tsx
│   │   │   ├── D3Sankey.tsx
│   │   │   └── ErrorResolutionPanel.tsx
│   │   ├── services/
│   │   │   ├── codeflow-service.ts
│   │   │   ├── ai-service.ts
│   │   │   ├── repo-ingestor.js
│   │   │   ├── code-parser.js
│   │   │   └── prompt-generator.js
│   │   └── utils/
│   │       └── repo-filter.js
│   └── public/
│       └── codeflow-widget.js
├── functions/
│   └── codeflow-analysis/
│       └── index.js
└── .github/workflows/
    └── codeflow-analysis.yml
```

## Installation & Setup

### Backend Setup

1. **Prerequisites**
   ```bash
   # Java 21
   java -version
   
   # Gradle
   ./gradlew --version
   ```

2. **Configure Firestore**
   ```bash
   # Set up Firebase service account
   export GOOGLE_APPLICATION_CREDENTIALS="path/to/service-account.json"
   ```

3. **Configure AI Providers**
   ```bash
   # Kimi API Key
   export KIMI_API_KEY="your-kimi-api-key"
   
   # DeepSeek API Key
   export DEEPSEEK_API_KEY="your-deepseek-api-key"
   
   # Together AI API Key
   export TOGETHER_API_KEY="your-together-api-key"
   ```

4. **Run Backend**
   ```bash
   ./gradlew bootRun
   ```

### Frontend Setup

1. **Install Dependencies**
   ```bash
   cd dashboard
   npm install
   ```

2. **Configure Environment**
   ```bash
   cp .env.example .env
   # Edit .env with your Firebase config
   ```

3. **Run Development Server**
   ```bash
   npm run dev
   ```

## API Endpoints

### CodeFlow API

#### Analyze Repository
```http
POST /api/codeflow/analyze
Content-Type: application/json

{
  "repositoryUrl": "https://github.com/owner/repo",
  "branch": "main",
  "focusPath": "src/",
  "languages": ["javascript", "typescript"]
}
```

#### Get Analysis Result
```http
GET /api/codeflow/analysis/{repositoryId}
```

#### Get Repository Status
```http
GET /api/codeflow/status/{repositoryId}
```

#### List Analyses
```http
GET /api/codeflow/analyses?page=0&size=20
```

#### Delete Analysis
```http
DELETE /api/codeflow/analysis/{repositoryId}
```

#### Trigger Re-analysis
```http
POST /api/codeflow/reanalyze/{repositoryId}
```

#### Get Health Score
```http
GET /api/codeflow/health/{repositoryId}
```

#### Get Security Report
```http
GET /api/codeflow/security/{repositoryId}
```

#### Get Dependency Graph
```http
GET /api/codeflow/dependencies/{repositoryId}
```

#### Get Pattern Analysis
```http
GET /api/codeflow/patterns/{repositoryId}
```

#### Export Analysis
```http
GET /api/codeflow/export/{repositoryId}?format=json
```

### Error Resolution API

#### Analyze Error
```http
POST /api/codeflow/error/analyze
Content-Type: application/json

{
  "stackTrace": "...",
  "repositoryId": "owner/repo",
  "context": {
    "file": "src/index.js",
    "line": 42
  }
}
```

#### Get Fix Suggestions
```http
GET /api/codeflow/error/fixes/{errorId}
```

#### Apply Fix
```http
POST /api/codeflow/error/apply-fix
Content-Type: application/json

{
  "errorId": "error-123",
  "fixId": "fix-456",
  "repositoryId": "owner/repo"
}
```

## Usage Examples

### Analyze a Repository

```javascript
// Using the dashboard
const response = await fetch('/api/codeflow/analyze', {
  method: 'POST',
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify({
    repositoryUrl: 'https://github.com/owner/repo',
    branch: 'main'
  })
});

const analysis = await response.json();
console.log('Health Score:', analysis.healthScore);
console.log('Security Issues:', analysis.securityIssues);
```

### Use the Customer Widget

```html
<div id="codeflow-widget"></div>
<script src="https://cdn.supremeai.com/codeflow-widget.js"></script>
<script>
  CodeFlowWidget.init({
    container: '#codeflow-widget',
    repositoryId: 'owner/repo',
    compact: true,
    theme: 'dark'
  });
</script>
```

### GitHub Action

```yaml
name: CodeFlow Analysis

on: [push, pull_request]

jobs:
  codeflow:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v3
      
      - name: Run CodeFlow Analysis
        uses: supremeai/codeflow-action@v1
        with:
          repository: ${{ github.repository }}
          token: ${{ secrets.GITHUB_TOKEN }}
          output: 'svg'
```

## Configuration

### Firestore Schema

```javascript
// repositories collection
{
  id: "owner/repo",
  name: "repo",
  owner: "owner",
  platform: "github",
  language: "javascript",
  createdAt: timestamp,
  updatedAt: timestamp,
  status: "active"
}

// analyses collection (subcollection)
{
  id: "analysis-123",
  repositoryId: "owner/repo",
  branch: "main",
  commit: "abc123",
  status: "completed",
  healthScore: 85,
  healthGrade: "B",
  totalFiles: 50,
  totalFunctions: 200,
  securityIssues: [...],
  patterns: [...],
  dependencies: [...],
  createdAt: timestamp,
  completedAt: timestamp
}
```

### Environment Variables

```bash
# Firebase
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CLIENT_EMAIL=your-client-email
FIREBASE_PRIVATE_KEY=your-private-key

# AI Providers
KIMI_API_KEY=your-kimi-api-key
DEEPSEEK_API_KEY=your-deepseek-api-key
TOGETHER_API_KEY=your-together-api-key

# GitHub
GITHUB_APP_ID=your-app-id
GITHUB_PRIVATE_KEY=your-private-key

# Application
SERVER_PORT=8080
NODE_ENV=production
```

## Performance

- **Analysis Speed**: < 30 seconds for 50-file repository
- **Security Detection**: > 90% accuracy
- **AI Fix Acceptance**: > 70% developer acceptance
- **Widget Load Time**: < 2 seconds (cached)
- **Memory Usage**: < 512MB per analysis

## Security

- All code analysis runs in isolated environments
- No code is stored outside authorized Firestore namespaces
- GitHub tokens are encrypted at rest
- API keys are never exposed to client-side
- Rate limiting: 60 requests/hour (no token), 5000/hour (PAT)

## Testing

```bash
# Run backend tests
./gradlew test

# Run with coverage
./gradlew jacocoTestReport

# Run frontend tests
cd dashboard && npm test
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests
5. Ensure all tests pass
6. Submit a pull request

## License

MIT License - see LICENSE file for details

## Support

- Documentation: https://docs.supremeai.com/codeflow
- Discord: https://discord.gg/supremeai
- Email: support@supremeai.com

## Changelog

### v1.0.0 (2026-05-05)
- Initial release
- Multi-language code parsing
- AI-powered analysis
- Interactive visualizations
- GitHub Action integration
- Customer widget
- Error resolution workflow