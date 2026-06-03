#!/bin/bash
# SupremeAI Firebase Migration Tool
# This script helps migrate Firestore data and Auth users to the new Google project.

OLD_PROJECT="supremeai-a"
NEW_PROJECT="supremeai-google-prod"
BUCKET="supremeai-migration-bucket"

echo "🚀 Starting migration from $OLD_PROJECT to $NEW_PROJECT..."

# 1. Export Firestore from Old Project
echo "📦 Exporting Firestore data from $OLD_PROJECT..."
gcloud config set project $OLD_PROJECT
gcloud firestore export gs://$BUCKET/firestore-export

# 2. Import Firestore to New Project
echo "📥 Importing Firestore data to $NEW_PROJECT..."
gcloud config set project $NEW_PROJECT
gcloud firestore import gs://$BUCKET/firestore-export

# 3. Migrate Firebase Auth Users
echo "👤 Migrating Firebase Auth users..."
# Note: Password hashing parameters need to be consistent
firebase auth:export users.json --project $OLD_PROJECT
firebase auth:import users.json --project $NEW_PROJECT

echo "✅ Migration complete! Please update frontend config with new project details."
