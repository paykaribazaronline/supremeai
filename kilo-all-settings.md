# Kilo Code Complete Settings Reference

All configuration settings, tabs, commands, and options for Kilo CLI.

---

## 📁 Configuration File Locations

### Config Files (kilo.json / kilo.jsonc)

| Scope | Path |
|---|---|
| Project | `./kilo.json`, `./kilo.jsonc`, `./opencode.json` (legacy), `./opencode.jsonc` (legacy) |
| Global | `~/.config/kilo/kilo.json`, `~/.config/kilo/kilo.jsonc`, `~/.config/kilo/opencode.json` (legacy), `~/.config/kilo/opencode.jsonc` (legacy), `~/.config/kilo/config.json` (legacy) |
| Managed | Linux: `/etc/kilo/`, macOS: `/Library/Application Support/kilo/`, Windows: `%ProgramData%\kilo\` |

### Config Directories (Search Order)

1. `~/.config/kilo/`
2. `~/.kilo/`
3. `~/.kilocode/`
4. `~/.opencode/`
5. `KILO_CONFIG_DIR` (environment variable)
6. Project `.kilo/`, `.kilocode/`, `.opencode/` (from CWD up to git root)

---

## ⚙️ kilo.json Top Level Settings

| Setting | Type | Description |
|---|---|---|
| `$schema` | string | `https://app.kilo.ai/config.json` |
| `model` | string | Default model `provider/model` |
| `small_model` | string | Model for titles/summaries |
| `default_agent` | string | Default primary agent (default: `code`) |
| `instructions` | string[] | Glob patterns for additional instruction files |
| `plugin` | string[] | Plugin specifiers (npm packages or file:// paths) |
| `snapshot` | boolean | Enable git snapshots |
| `share` | string | Session sharing: `manual`, `auto`, `disabled` |
| `autoupdate` | boolean\|string | Auto-update: `true`, `false`, `notify` |
| `username` | string | Display name override |
| `compaction.auto` | boolean | Auto-compact when context full (default: true) |
| `compaction.prune` | boolean | Prune old tool outputs (default: true) |
| `enabled_providers` | string[] | Only enable these providers |
| `disabled_providers` | string[] | Disable specific providers |

---

## 🔌 Providers Configuration

```jsonc
{
  "provider": {
    "<provider-id>": {
      "options": {
        "apiKey": "string",
        "baseURL": "string",
        "timeout": "number"
      },
      "models": {
        "<model-id>": { "name": "string" }
      },
      "whitelist": ["string"],
      "blacklist": ["string"]
    }
  }
}
```

Available provider IDs: `kilo`, `openai`, `anthropic`, `google`, `groq`

---

## 🔒 Permissions Settings

Permission keys:
`read`, `edit`, `glob`, `grep`, `list`, `bash`, `task`, `webfetch`, `websearch`, `codesearch`, `lsp`, `skill`, `external_directory`, `todowrite`, `todoread`, `question`, `doom_loop`, plus `{server}_{tool}` for MCP tools

Permission values: `allow`, `ask`, `deny`

```jsonc
{
  "permission": {
    "<tool>": "allow",
    "<tool>": {
      "<glob-pattern>": "allow",
      "*": "ask"
    }
  }
}
```

---

## 📡 MCP Servers Configuration

```jsonc
{
  "mcp": {
    "<server-name>": {
      "type": "local|remote",
      "enabled": true,
      "command": ["string"],
      "url": "string",
      "environment": {},
      "headers": {},
      "timeout": 10000
    }
  }
}
```

---

## 🛠️ Skills Configuration

```jsonc
{
  "skills": {
    "paths": ["./my-skills", "~/shared-skills"],
    "urls": ["https://example.com/.well-known/skills/"]
  }
}
```

---

## 📋 Commands (/.kilo/command/*.md)

Filename (minus .md) becomes command name invoked via `/name`.

Frontmatter fields:

| Field | Type | Description |
|---|---|---|
| `description` | string | Shown in command list |
| `agent` | string | Route to specific agent |
| `model` | string | Override model |
| `subtask` | boolean | Run as subtask |

Template variables: `$1`-$N, `$ARGUMENTS`,`@file`, \`!cmd\`

---

## 🤖 Agents (/.kilo/agent/*.md)

Frontmatter fields:

| Field | Type | Description |
|---|---|---|
| `description` | string | Agent description |
| `mode` | string | `primary`, `subagent`, `all` |
| `model` | string | Override model |
| `steps` | number | Max agentic iterations |
| `hidden` | boolean | Hide from @ menu |
| `color` | string | Hex color code |
| `permission` | object | Agent-level permissions |

---

## 🖥️ VS Code Extension Settings Tabs

These are the tabs shown in the Kilo Code VS Code extension settings panel:

| Tab Name | Features & Settings |
|---|---|
| **Models** | Default Model, Small Model, Model per Mode (Code, Ask, Debug, Orchestrator, Plan) |
| **Providers** | LLM provider configurations, API keys, endpoints, model whitelists/blacklists |
| **Agent Behaviour** | Agent personality, response style, reasoning depth preferences |
| **Auto-Approve** | Tool permission auto-approve rules, glob patterns for file operations |
| **Browser** | Browser automation settings, viewport size, timeout configuration |
| **Checkpoints** | Session checkpointing, autosave frequency, history retention |
| **Display** | UI theme, font preferences, output formatting options |
| **Autocomplete** | Code completion behavior, trigger settings, suggestion limits |
| **Notifications** | Alert preferences, sound settings, notification visibility |
| **Context** | Context window management, file inclusion rules, token limits |
| **Commit Message** | Git commit message generation templates and style |
| **Experimental** | Beta features, preview functionality, developer options |
| **Language** | Interface language, localization preferences |
| **About Kilo Code** | Version information, licensing, about details |

---

## 🖥️ CLI TUI Interface Tabs & Settings

Access via Ctrl+P (command palette)

### 🎨 Theme & Appearance Tab

| Action | Keybind | Slash Command |
|---|---|---|
| Switch theme | `<leader>t` | `/themes` |
| Toggle dark/light | Ctrl+P | - |
| Custom themes | - | `~/.config/kilo/themes/` |

### 📑 Sessions Tab

| Action | Keybind | Slash Command |
|---|---|---|
| List sessions | `<leader>l` | `/sessions` |
| New session | `<leader>n` | `/new`, `/clear` |
| Share session | - | `/share` |
| Rename session | `ctrl+r` | `/rename` |
| Jump to message | `<leader>g` | `/timeline` |
| Fork session | - | `/fork` |
| Compact session | `<leader>c` | `/compact`, `/summarize` |
| Undo message | `<leader>u` | `/undo` |
| Redo message | `<leader>r` | `/redo` |
| Copy last response | `<leader>y` | - |
| Copy transcript | - | `/copy` |

### 🧠 Agent & Model Tab

| Action | Keybind | Slash Command |
|---|---|---|
| Switch model | `<leader>m` | `/models` |
| Switch agent | `<leader>a` | `/agents` |
| Toggle MCP servers | - | `/mcps` |
| Cycle agent | `tab` / `shift+tab` | - |

### 👁️ Display Toggles Tab

Toggle options:

- Notifications
- Animations
- Diff wrapping
- Sidebar (`<leader>b`)
- Thinking output (`/thinking`)
- Tool details
- Timestamps (`/timestamps`)
- Scrollbar
- Header
- Code concealment (`<leader>h`)

### ⚙️ System Tab

| Action | Slash Command |
|---|---|
| View status | `/status` |
| Help | `/help` |
| Exit | `/exit`, `/quit`, `/q` |
| Open editor | `/editor` |

---

## 🔑 Environment Variables

| Variable | Description |
|---|---|
| `KILO_CONFIG` | Path to additional config file |
| `KILO_CONFIG_DIR` | Additional config directory |
| `KILO_CONFIG_CONTENT` | Inline JSON config string |
| `KILO_DISABLE_PROJECT_CONFIG` | Skip all project-level config |

---

## ⌨️ Keybinds Reference

| Keybind | Action |
|---|---|
| `Ctrl+P` | Open command palette |
| `Ctrl+X` | Leader key prefix |
| `Ctrl+X t` | Switch theme |
| `Ctrl+X l` | List sessions |
| `Ctrl+X n` | New session |
| `Ctrl+X g` | Jump to message |
| `Ctrl+X c` | Compact session |
| `Ctrl+X u` | Undo message |
| `Ctrl+X r` | Redo message |
| `Ctrl+X y` | Copy last response |
| `Ctrl+X m` | Switch model |
| `Ctrl+X a` | Switch agent |
| `Ctrl+X b` | Toggle sidebar |
| `Ctrl+X h` | Toggle code concealment |
| `Ctrl+R` | Rename session |
| `Tab` / `Shift+Tab` | Cycle agents |
