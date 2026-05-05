# Browser Automation Tool

A TypeScript/Node.js server that provides browser automation capabilities for coding assistants using Playwright.

## Features

- **Browser Navigation**: Navigate to any URL
- **Element Interaction**: Click elements, fill forms
- **Debugging**: Take screenshots, inspect network requests, console logs
- **Performance Analysis**: Run performance traces and get insights
- **Page Content**: Get HTML content of the page

## API Endpoints

### Navigation
- `POST /navigate` - Navigate to URL
  - Body: `{"url": "https://example.com"}`
  - Response: `{"success": true}`

### Element Interaction
- `POST /click` - Click an element
  - Body: `{"selector": "#button"}`
  - Response: `{"success": true}`

- `POST /fill` - Fill a form field
  - Body: `{"selector": "#input", "value": "text"}`
  - Response: `{"success": true}`

### Debugging
- `GET /screenshot` - Take screenshot
  - Response: `{"screenshot": "base64encodedpng"}`

- `GET /console` - Get console logs
  - Response: `{"logs": []}`

- `GET /network` - Get network requests
  - Response: `{"requests": []}`

- `GET /content` - Get page HTML content
  - Response: `{"content": "<html>...</html>"}`

### Performance
- `GET /performance` - Run performance analysis
  - Response: `{"insights": ["DOM Content Loaded: 500ms", "Load Complete: 1000ms"]}`

## Setup

1. Install dependencies:
   ```bash
   npm install
   npx playwright install chromium
   ```

2. Build:
   ```bash
   npm run build
   ```

3. Start server:
   ```bash
   npm start
   ```

Server runs on port 3001 with browser launched in headless mode.

## Integration

The main coding assistant system can make HTTP requests to these endpoints to control the browser and get debugging information.

## Architecture

- **BrowserController**: Manages Playwright browser instance
- **Server**: Express.js API server exposing browser functionality
- **Playwright**: Browser automation library for reliable cross-browser testing