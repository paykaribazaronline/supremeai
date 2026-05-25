#!/usr/bin/env node

/**
 * Firebase Knowledge Seeding Script
 * Seeds autonomous_seed_knowledge.json data into Firestore system_learning collection
 * 
 * Usage:
 *   node seed-firebase-knowledge.js              # Dry run (preview only)
 *   node seed-firebase-knowledge.js --execute    # Execute seeding
 *   node seed-firebase-knowledge.js --clear      # Clear existing data first
 * 
 * Environment Variables:
 *   GOOGLE_APPLICATION_CREDENTIALS - Path to Firebase service account key
 *   FIRESTORE_PROJECT_ID - Firebase project ID (default: supremeai-a)
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');

// Load knowledge data
const knowledgeData = JSON.parse(
  fs.readFileSync(path.join(__dirname, 'autonomous_seed_knowledge.json'), 'utf8')
);

// Initialize Firebase Admin SDK
const projectId = process.env.FIRESTORE_PROJECT_ID || 'supremeai-a';

// Check if already initialized
if (admin.apps.length === 0) {
  // Try to use default credentials (for local development with firebase login)
  try {
    admin.initializeApp({
      projectId: projectId
    });
    console.log('✅ Firebase initialized with default credentials');
  } catch (error) {
    console.error('❌ Failed to initialize Firebase:', error.message);
    console.log('\n💡 Make sure you have:');
    console.log('   1. Run: firebase login');
    console.log('   2. Or set GOOGLE_APPLICATION_CREDENTIALS env var');
    process.exit(1);
  }
}

const db = admin.firestore();

// Configuration
const COLLECTION_NAME = 'system_learning';
const BATCH_SIZE = 500; // Firestore batch limit
const DRY_RUN = !process.argv.includes('--execute');
const CLEAR_FIRST = process.argv.includes('--clear');

// Color codes for console output
const colors = {
  reset: '\x1b[0m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  red: '\x1b[31m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  bold: '\x1b[1m'
};

function log(message, color = 'reset') {
  console.log(`${colors[color]}${message}${colors.reset}`);
}

function formatDate(date) {
  return new Date(date).toISOString();
}

/**
 * Transform autonomous_seed_knowledge.json entry to SystemLearning document
 */
function transformKnowledgeEntry(entry, index) {
  const now = new Date();
  const learnedAt = new Date(knowledgeData.metadata.generated_at);
  
  return {
    id: entry.id,
    topic: entry.title,
    category: entry.category.toLowerCase(),
    content: entry.description,
    learningType: 'KNOWLEDGE_BASE',
    confidenceScore: entry.confidence,
    learnedAt: learnedAt,
    permanent: true,
    timesApplied: 0,
    qualityScore: entry.confidence,
    success: true,
    tags: [
      entry.category.toLowerCase(),
      ...(entry.applicable_scope ? [entry.applicable_scope.toLowerCase()] : []),
      'seeded',
      'knowledge-base'
    ],
    metadata: {
      evidence_type: entry.evidence_type,
      applicable_scope: entry.applicable_scope,
      verification_steps: entry.verification_steps,
      anti_patterns: entry.anti_patterns,
      source: 'autonomous_seed_knowledge.json',
      seed_index: index + 1,
      total_seeds: knowledgeData.seed_knowledge.length,
      priority: knowledgeData.priority_list.includes(`${entry.id}: ${entry.title}`) ? 'high' : 'standard',
      conflicts_with_rules: knowledgeData.conflicts_with_rules || [],
      created_at: now,
      updated_at: now
    },
    inputData: {
      verification_steps: entry.verification_steps,
      anti_patterns: entry.anti_patterns
    },
    outputData: {
      expected_outcome: 'Improved system reliability and performance',
      implementation_guidance: 'Follow verification steps, avoid anti-patterns'
    },
    type: 'knowledge',
    resolution: 'Documented best practice',
    context: entry.description,
    errorCount: 0,
    resolved: true,
    timestamp: learnedAt,
    severity: 'info',
    solutions: entry.verification_steps
  };
}

/**
 * Clear existing data from collection
 */
async function clearCollection() {
  log('\n🗑️  Clearing existing system_learning collection...', 'yellow');
  
  const snapshot = await db.collection(COLLECTION_NAME).get();
  
  if (snapshot.empty) {
    log('   Collection is already empty', 'green');
    return;
  }
  
  log(`   Found ${snapshot.size} documents to delete...`, 'yellow');
  
  const batch = db.batch();
  let count = 0;
  
  snapshot.docs.forEach(doc => {
    batch.delete(doc.ref);
    count++;
    
    if (count % BATCH_SIZE === 0) {
      // Commit batch and start new one
      batch.commit();
    }
  });
  
  await batch.commit();
  log(`   ✅ Deleted ${snapshot.size} documents`, 'green');
}

/**
 * Seed knowledge data into Firestore
 */
async function seedKnowledge() {
  const entries = knowledgeData.seed_knowledge;
  const total = entries.length;
  
  log('\n' + '='.repeat(70), 'cyan');
  log('  🌱 Firebase Knowledge Seeding Tool', 'cyan');
  log('='.repeat(70), 'cyan');
  
  log(`\n📊 Metadata:`, 'bold');
  log(`   Version: ${knowledgeData.metadata.version}`);
  log(`   Generated: ${formatDate(knowledgeData.metadata.generated_at)}`);
  log(`   Total Items: ${knowledgeData.metadata.total_items}`);
  log(`   Categories: ${knowledgeData.metadata.category_coverage.join(', ')}`);
  
  log(`\n📝 Seeding ${total} knowledge entries...`, 'bold');
  log(`   Mode: ${DRY_RUN ? '🟡 DRY RUN (preview only)' : '🟢 EXECUTE'}\n`);
  
  if (CLEAR_FIRST && !DRY_RUN) {
    await clearCollection();
  }
  
  // Group by category for summary
  const categoryStats = {};
  
  // Process in batches
  const batches = [];
  let currentBatch = db.batch();
  let batchCount = 0;
  let totalBatchOps = 0;
  
  entries.forEach((entry, index) => {
    const docData = transformKnowledgeEntry(entry, index);
    const docRef = db.collection(COLLECTION_NAME).doc(entry.id);
    
    currentBatch.set(docRef, docData, { merge: true });
    batchCount++;
    totalBatchOps++;
    
    // Track category stats
    const cat = entry.category;
    categoryStats[cat] = (categoryStats[cat] || 0) + 1;
    
    // Commit batch when it reaches BATCH_SIZE
    if (batchCount >= BATCH_SIZE) {
      batches.push(currentBatch);
      currentBatch = db.batch();
      batchCount = 0;
    }
  });
  
  // Add remaining operations
  if (batchCount > 0) {
    batches.push(currentBatch);
  }
  
  // Display summary
  log('📂 Category Distribution:', 'bold');
  Object.entries(categoryStats).forEach(([cat, count]) => {
    log(`   ${cat}: ${count}`);
  });
  
  log(`\n🔢 Total Documents: ${total}`);
  log(`   Firestore Batches: ${batches.length}`);
  log(`   Operations per Batch: ≤${BATCH_SIZE}`);
  
  // Priority items
  const priorityItems = entries.filter(e => 
    knowledgeData.priority_list.some(p => p.includes(e.id))
  );
  
  if (priorityItems.length > 0) {
    log(`\n⭐ Priority Items (${priorityItems.length}):`, 'bold');
    priorityItems.forEach(item => {
      log(`   • ${item.id}: ${item.title}`);
    });
  }
  
  if (DRY_RUN) {
    log('\n🟡 DRY RUN COMPLETE - No data written', 'yellow');
    log('   Run with --execute to seed data\n');
    return;
  }
  
  // Execute seeding
  log('\n🚀 Executing seed...', 'bold');
  
  try {
    for (let i = 0; i < batches.length; i++) {
      await batches[i].commit();
      log(`   Batch ${i + 1}/${batches.length} committed ✅`);
    }
    
    log(`\n✅ SUCCESS! Seeded ${total} knowledge entries`, 'green');
    log(`   Collection: ${COLLECTION_NAME}`);
    log(`   Project: ${projectId}`);
    
    // Verify
    const verifySnapshot = await db.collection(COLLECTION_NAME).get();
    log(`   Verified: ${verifySnapshot.size} documents in collection\n`);
    
  } catch (error) {
    log(`\n❌ ERROR: ${error.message}`, 'red');
    process.exit(1);
  }
}

// Run
seedKnowledge().catch(error => {
  console.error('Fatal error:', error);
  process.exit(1);
});
