#!/bin/bash
# SupremeAI CLI Installation Script
# Installs the SupremeAI CLI tool system-wide

set -e

echo "🚀 Installing SupremeAI CLI..."

# Check Python version
python_version=$(python3 --version 2>&1 | awk '{print $2}')
echo "✅ Python version: $python_version"

# Get script directory
SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# Create symlink in /usr/local/bin
sudo ln -sf "$SCRIPT_DIR/supcmd.py" /usr/local/bin/supremeai

# Make executable
chmod +x "$SCRIPT_DIR/supcmd.py"
chmod +x /usr/local/bin/supremeai

echo "✅ SupremeAI CLI installed successfully!"
echo ""
echo "📋 Available commands:"
echo "   supremeai login                 - Authenticate with Firebase token"
echo "   supremeai list                  - List all available commands"
echo "   supremeai exec <cmd>            - Execute a command"
echo "   supremeai system learning improve - Improve system learning"
echo "   supremeai system learning status  - View learning status"
echo "   supremeai providers list        - List AI providers"
echo "   supremeai admin mode <mode>     - Set admin mode (AUTO/WAIT/FORCE_STOP)"
echo "   supremeai metrics cache         - View cache statistics"
echo ""
echo "🔗 Configuration:"
echo "   API URL: https://supremeai-lhlwyikwlq-uc.a.run.app/api"
echo "   Token: ~/.supremeai_token"
echo ""
echo "📖 Run 'supremeai list' to see all available commands"
echo "🧠 Run 'supremeai system learning improve' to improve AI learning"
