#!/bin/bash
# 🚀 SupremeAI Render Environment Setup Helper
# This script helps you update your .env file with Render service URLs.

echo "----------------------------------------------------"
echo "🛠️ SupremeAI Render Environment Setup"
echo "----------------------------------------------------"

# 1. Ask for URLs (or provide defaults if user just wants to see the structure)
read -p "Enter TinyLlama Render URL (e.g., https://tinyllama.onrender.com): " TINY_URL
read -p "Enter Phi-2 Render URL: " PHI2_URL
read -p "Enter Phi-3 Render URL: " PHI3_URL
read -p "Enter Qwen-0.5B Render URL: " QWEN_URL

# 2. Update .env file
if [ -f .env ]; then
    echo "Updating existing .env file..."
    
    # Remove existing entries if any
    sed -i '/RENDER_TINYLLAMA_URL/d' .env
    sed -i '/RENDER_PHI2_URL/d' .env
    sed -i '/RENDER_PHI3_URL/d' .env
    sed -i '/RENDER_QWEN_URL/d' .env
    
    # Append new ones
    echo "RENDER_TINYLLAMA_URL=$TINY_URL" >> .env
    echo "RENDER_PHI2_URL=$PHI2_URL" >> .env
    echo "RENDER_PHI3_URL=$PHI3_URL" >> .env
    echo "RENDER_QWEN_URL=$QWEN_URL" >> .env
    
    echo "✅ .env updated successfully."
else
    echo "❌ .env file not found in current directory."
    echo "Creating a new .env.render for you to copy from."
    
    cat <<EOF > .env.render
RENDER_TINYLLAMA_URL=$TINY_URL
RENDER_PHI2_URL=$PHI2_URL
RENDER_PHI3_URL=$PHI3_URL
RENDER_QWEN_URL=$QWEN_URL
EOF
    echo "✅ .env.render created."
fi

echo "----------------------------------------------------"
echo "💡 Next Step: Restart your Spring Boot backend to apply changes."
echo "----------------------------------------------------"
