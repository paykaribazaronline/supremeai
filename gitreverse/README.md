# GitReverse

Generate natural-language prompts from GitHub repositories to recreate projects from scratch using AI.

## Overview

GitReverse is a web application that takes a public GitHub repository URL or owner/repo slug and generates a single natural-language prompt that someone could paste into an AI coding assistant to recreate that project from scratch. It captures the intent and architecture, not implementation details.

## Features

- **AI-Powered Prompts**: Generates 120-200 word natural-language prompts using OpenRouter (Gemini 2.5 Pro) or Google AI Studio
- **GitHub Integration**: Fetches repo metadata, README (up to 8000 chars), and depth-1 file tree
- **Multiple Pages**: Home, Library, History, and Repo Detail pages
- **Prompt Library**: Browse and search previously generated prompts (requires Supabase)
- **Local History**: Last 20 repos saved in localStorage (no server sync needed)
- **Custom Focus Mode**: Manual control with custom focus string and backend service proxy
- **In-Flight Deduplication**: Prevents duplicate LLM calls for the same repo
- **Smart Fallbacks**: Retries with 'master' if 'main' branch not found
- **Rate Limit Handling**: Clean error messages directing users to the library
- **Beautiful UI**: Professional design with Next.js 14, React 19, and Tailwind CSS 4

## Tech Stack

- **Framework**: Next.js 14 with App Router
- **UI**: React 19, Tailwind CSS 4
- **Fonts**: Geist Sans and Geist Mono via next/font
- **LLM**: OpenRouter API (default: google/gemini-2.5-pro) or Google AI Studio
- **Database**: Supabase (optional, for library and caching)
- **Visualization**: Recharts for graphical views
- **Styling**: Custom color palette with red accent (#d31611)

## Installation

```bash
cd gitreverse
npm install
```

## Usage

### Development

```bash
npm run dev
# Open http://localhost:3000
```

### Build for Production

```bash
npm run build
npm start
```

### Using the App

1. **Home Page**: Paste a GitHub URL or owner/repo (e.g., `fastapi/fastapi`)
2. **Generate Prompt**: Click "Reverse" to generate the prompt
3. **Copy Prompt**: Use the "Copy to Clipboard" button
4. **Paste into AI**: Use the prompt in ChatGPT, Claude, Cursor, etc.

### Pages

- **Home (/) **: Main input page with prompt generation
- **Library (/library)**: Browse all generated prompts (requires Supabase)
- **History (/history)**: Your recently reversed repos (localStorage)
- **Repo Detail (/[owner]/[repo])**: Auto-generates or shows cached prompt

## Environment Variables

Create a `.env.local` file:

```bash
# LLM Provider (at least one required)
OPENROUTER_API_KEY=sk-or-v1-...
OPENROUTER_MODEL=google/gemini-2.5-pro
GOOGLE_GENERATIVE_AI_API_KEY=...
GOOGLE_AI_STUDIO_MODEL=gemini-2.5-pro

# GitHub (optional, for higher rate limits)
GITHUB_TOKEN=ghp_...

# Supabase (all optional)
SUPABASE_URL=https://xxx.supabase.co
SUPABASE_PUBLISHABLE_KEY=eyJ...
SUPABASE_SERVICE_ROLE_KEY=eyJ...  # Server-side only

# Custom Reverse Service
CUSTOM_REVERSE_SERVICE_URL=http://localhost:3001

# View Counter
VIEWS_IP_SALT=random-salt-string

# Next.js
NEXT_PUBLIC_APP_URL=http://localhost:3000
```

## API Routes

### POST /api/reverse

Generate a prompt from a GitHub repository.

**Request:**
```json
{
  "url": "https://github.com/fastapi/fastapi",
  "focus": "Optional: focus on specific aspects"
}
```

**Response:**
```json
{
  "prompt": "Create a modern Python web framework..."
}
```

### POST /api/custom

Proxy to custom reverse service with SSE streaming support.

**Request:**
```json
{
  "url": "https://github.com/user/repo",
  "focus": "Add authentication system"
}
```

**Response:** JSON or SSE stream

## Supabase Schema

```sql
-- Quick reverse cache (prompts generated from repos)
CREATE TABLE quick_reverse_cache (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  repo_full_name TEXT NOT NULL,
  prompt TEXT NOT NULL,
  metadata JSONB,
  view_count INTEGER DEFAULT 0,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- Custom reverse cache (focus-based prompts)
CREATE TABLE custom_reverse_cache (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  focus_hash TEXT NOT NULL UNIQUE,
  focus_text TEXT,
  result TEXT NOT NULL,
  created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);

-- View counter
CREATE TABLE view_counter (
  id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
  ip_hash TEXT NOT NULL UNIQUE,
  count INTEGER DEFAULT 1,
  updated_at TIMESTAMP WITH TIME ZONE DEFAULT NOW()
);
```

## Features in Detail

### In-Flight Request Deduplication

Prevents multiple simultaneous LLM calls for the same repository using a request map.

### Smart Branch Handling

Automatically retries with 'master' if 'main' branch is not found (and vice versa).

### Custom Focus Mode

When enabled, sends the repo URL and focus string to a backend service (configurable URL) that supports both regular POST and SSE streaming.

### Rate Limit Handling

If the LLM returns a rate limit error, shows an alert directing users to the library instead.

### Graphical Views

The app includes professional visualizations:
- **Stats Bar**: Word count, character count, estimated tokens
- **Detail View**: Source URL, line count, paragraph count, averages
- **Library Cards**: Clean grid layout with metadata

## Color Palette

- **Background**: #FFFDF8 (warm off-white)
- **Accent**: #d31611 (red for "Reverse" logo and buttons)
- **Input Background**: #fff4da (cream)
- **Button Hover**: #ffc480 (yellow-orange)
- **Border**: zinc-900, 3px solid
- **Shadow Effect**: translate-x-2 translate-y-2

## Development

```bash
# Install dependencies
npm install

# Run development server
npm run dev

# Build for production
npm run build

# Run linting
npm run lint
```

## License

MIT License
