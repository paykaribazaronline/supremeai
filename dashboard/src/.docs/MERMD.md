# MERMD - SupremeAI Dashboard

## Overview

The Dashboard is a React-based admin panel for managing SupremeAI features.

## How It Works

### Architecture Flow

```
User → React Components → API Calls → Backend Services → Response
```

## Structure

### Key Directories

| Directory         | Purpose               |
| ----------------- | --------------------- |
| `src/pages/`      | Route-level pages     |
| `src/components/` | Reusable components   |
| `src/services/`   | API service calls     |
| `src/lib/`        | Utilities and helpers |
| `src/hooks/`      | Custom React hooks    |
| `src/contexts/`   | React contexts        |

## Main Pages

| Page                 | Purpose                |
| -------------------- | ---------------------- |
| `AdminDashboard.tsx` | Main dashboard         |
| `AdminQuotas.tsx`    | Quota management       |
| `AdminVPN.tsx`       | VPN configuration      |
| `AdminProviders.tsx` | AI provider management |
| `AdminLearning.tsx`  | Learning system        |
| `AdminSimulator.tsx` | Simulator control      |
| `AdminSecurity.tsx`  | Security settings      |

## Component Libraries

### Admin Components (`components/admin/`)

| Component         | Purpose            |
| ----------------- | ------------------ |
| `ProjectsTab.tsx` | Project management |
| `UsersTab.tsx`    | User management    |

### Dashboard Components (`components/dashboard/`)

| Component              | Purpose           |
| ---------------------- | ----------------- |
| `DashboardHome.tsx`    | Home view         |
| `DashboardHeader.tsx`  | Navigation header |
| `DashboardSidebar.tsx` | Side navigation   |

### Browser Components (`components/browser/`)

| Component              | Purpose          |
| ---------------------- | ---------------- |
| `BrowserViewport.tsx`  | Browser preview  |
| `BrowserToolbar.tsx`   | Browser controls |
| `IntelligenceFeed.tsx` | AI insights      |

### Simulator Components (`components/simulator/`)

| Component                    | Purpose             |
| ---------------------------- | ------------------- |
| `SimulationControlCard.tsx`  | Simulation controls |
| `SimulatorStats.tsx`         | Statistics          |
| `DeploymentHistoryTable.tsx` | Deployment history  |

## Services

| Service               | Purpose         |
| --------------------- | --------------- |
| `supremeAIService.ts` | Main API client |
| `authService.ts`      | Authentication  |
| `prompt-generator.js` | Prompt building |
| `repo-ingestor.js`    | Repo processing |

## State Management

### Contexts

| Context           | Purpose              |
| ----------------- | -------------------- |
| `RoleContext.tsx` | User role management |

### Hooks

| Hook                    | Purpose              |
| ----------------------- | -------------------- |
| `useSystemWebSocket.ts` | WebSocket connection |
| `useModelSearch.ts`     | Model search         |

## Key Features

### Authentication

- Firebase authentication
- JWT token storage
- Protected routes

### Real-time Updates

- WebSocket connections
- Live activity feeds
- Session status updates

### Internationalization

- Bengali language support
- Configurable locales
