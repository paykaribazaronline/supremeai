
# SupremeAI VS Code Extension

AI-powered development assistant for SupremeAI platform.

## Features

- **Interactive Chat Interface**: Chat with SupremeAI to get help with development tasks
- **Multi-Language Support**: English and Bengali (বাংলা) language support
- **Agent Orchestration**: View and manage AI agents (X-Builder, Z-Architect)
- **Project Management**: Generate and manage Android projects
- **Code Review**: Get AI-powered code reviews and suggestions
- **Deployment**: Deploy your apps directly from VS Code
- **Quick Actions**: Fast access to common tasks via command palette

## Installation

1. Clone this repository
2. Open VS Code
3. Press `F5` to open a new Extension Development Host window
4. The extension will be activated automatically

## Usage

### Commands

- `SupremeAI: New Android App` - Generate a new Android application
- `SupremeAI: Add Feature` - Add a new feature to your project
- `SupremeAI: Review Code` - Review current file for bugs and performance issues
- `SupremeAI: Deploy` - Deploy your application
- `SupremeAI: Start Chat` - Open full chat interface
- `SupremeAI: Ask About Code` - Explain selected code
- `SupremeAI: Explain File` - Explain the current file

### Views

- **SupremeAI Chat**: Interactive chat interface with AI
- **Agents**: View and manage AI agents
- **Projects**: Manage your generated projects
- **Orchestration**: View agent orchestration details

### Configuration

Configure the extension in VS Code settings:

```json
{
  "supremeai.apiEndpoint": "https://supremeai-lhlwyikwlq-uc.a.run.app",
  "supremeai.apiKey": "your-api-key",
  "supremeai.model": "google/gemini-1.5-pro",
  "supremeai.smallModel": "google/gemini-1.5-flash",
  "supremeai.fullAuthority": false,
  "supremeai.permissions": {
    "read": "allow",
    "edit": "ask",
    "bash": "ask",
    "task": "allow",
    "websearch": "allow",
    "external_directory": "deny"
  }
}
```

## Development

### Project Structure

```
vscode-extension/
├── src/
│   ├── providers/          # Tree data providers
│   ├── services/           # API services
│   ├── views/             # Webview views
│   └── extension.ts       # Main extension file
├── package.json           # Extension manifest
└── tsconfig.json         # TypeScript configuration
```

### Building

```bash
npm install
npm run compile
```

### Testing

```bash
npm run test
```

### Packaging

```bash
vsce package
```

## Contributing

Contributions are welcome! Please read our contributing guidelines before submitting PRs.

## License

MIT License - see LICENSE file for details
