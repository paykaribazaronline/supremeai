#!/bin/bash

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$SCRIPT_DIR"

# SupremeAI Auto Deploy Script
# This script automates the deployment process for Firebase Functions and Local Server

echo "🚀 Starting SupremeAI Deployment..."

# Check if Firebase CLI is installed
if ! command -v firebase &> /dev/null
then
    echo "❌ Firebase CLI is not installed. Please install it first:"
    echo "   npm install -g firebase-tools"
    exit 1
fi

# Check if Python is installed
if ! command -v python &> /dev/null
then
    echo "❌ Python is not installed. Please install Python first."
    exit 1
fi

# Deploy Firebase Functions
echo ""
echo "📦 Deploying Firebase Functions..."
cd "$PROJECT_ROOT/functions"
echo "📥 Installing dependencies..."
npm install
echo "🚀 Deploying functions..."
firebase deploy --only functions
cd "$PROJECT_ROOT"

# Install Python dependencies for local server
echo ""
echo "🐍 Setting up Local Server..."
cd "$PROJECT_ROOT/smart_chat_system"
echo "📥 Installing Python dependencies..."
pip install -r requirements.txt
echo "✅ Local server setup complete!"
cd "$PROJECT_ROOT"

echo ""
echo "✅ Deployment complete!"
echo ""
echo "📋 Next steps:"
echo "   1. Start local server: cd smart_chat_system && python app.py"
echo "   2. Access admin dashboard at: https://your-project.firebaseapp.com/admin-dashboard.html"
echo ""
