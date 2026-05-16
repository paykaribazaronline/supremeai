#!/usr/bin/env node

/**
 * SupremeAI — Comprehensive Real-Data Seeder
 *
 * Seeds all key Firestore collections with realistic production data:
 *   users, user_tiers, api_providers, ai_agents,
 *   system_learning, knowledge_domains, knowledge_entries,
 *   knowledge_recommendations, consensus_results, provider_task_performance,
 *   workflow_definitions, activity_logs, reasoning_logs
 *
 * AI provider URLs are sourced from src/main/resources/ai-cloud-endpoints.json
 * (the actual deployed GCP Cloud Run endpoints).
 *
 * Usage:
 *   node seed-all-data.js              # Dry run — preview only
 *   node seed-all-data.js --execute    # Execute seeding to Firestore
 *   node seed-all-data.js --clear       # Clear collections before seeding
 *   node seed-all-data.js --json mydata.json    # Use custom seed JSON file
 *
 * Environment:
 *   FIRESTORE_PROJECT_ID — override project (default: supremeai-a)
 *   GOOGLE_APPLICATION_CREDENTIALS — service account key path
 *
 * On success:
 *   - Prints per-collection document counts
 *   - Returns exit code 0
 *
 * On failure:
 *   - Logs error and exits with code 1
 */

const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const { execSync } = require('child_process');

// ── Auto-resolve GCP credentials ───────────────────────────────────────────────
const _KEY_PATHS = [
  process.env.GOOGLE_APPLICATION_CREDENTIALS,
  path.resolve(__dirname, '../../src/main/resources/firebase-service-account.json'),
  path.resolve(__dirname, '../service-account.json'),
].filter(Boolean);

const _KEY_PATH = _KEY_PATHS.find(p => { try { fs.statSync(p); return true; } catch { return false; } });

if (_KEY_PATH && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  process.env.GOOGLE_APPLICATION_CREDENTIALS = _KEY_PATH;
  console.log(`🔑 Credentials: ${_KEY_PATH}`);
}

// ── Firestore Init ────────────────────────────────────────────────────────────
const projectId = process.env.FIRESTORE_PROJECT_ID || 'supremeai-a';

if (admin.apps.length === 0) {
  try {
    admin.initializeApp({ projectId });
    console.log(`✅ Firebase Admin initialized — project: ${projectId}`);
  } catch (err) {
    console.error('❌ Firebase init failed:', err.message);
    console.log('💡 Run: firebase login');
    process.exit(1);
  }
}

const db = admin.firestore();

// ── Config ──────────────────────────────────────────────────────────────────
const BATCH_SIZE = 500;
const EXECUTE    = process.argv.includes('--execute');
const CLEAR_FIRST = process.argv.includes('--clear');
let   customJsonIdx = process.argv.indexOf('--json');
const CUSTOM_JSON  = customJsonIdx !== -1 ? process.argv[customJsonIdx + 1] : null;

const SEED_FILE = path.join(__dirname, CUSTOM_JSON || 'seed-data.json');
const DRY_RUN = !EXECUTE;

// ── Styling ─────────────────────────────────────────────────────────────────
const C = {
  reset:  '\x1b[0m',
  green:  '\x1b[32m',
  yellow: '\x1b[33m',
  red:    '\x1b[31m',
  blue:   '\x1b[34m',
  cyan:   '\x1b[36m',
  bold:   '\x1b[1m',
  dim:    '\x1b[2m'
};

function log(msg, color = 'reset') { console.log(`${C[color]}${msg}${C.reset}`); }

// ── Helpers ─────────────────────────────────────────────────────────────────
async function runBatchOps(batch) {
  const commits = [];
  for (let i = 0; i < batch.length; i += BATCH_SIZE) {
    commits.push(batch.slice(i, i + BATCH_SIZE));
  }

  if (DRY_RUN) {
    log(`    [DRY] Would commit ${batch.length} operations in ${commits.length} batch(es)`, 'dim');
    return;
  }

  let ok = 0;
  for (let i = 0; i < commits.length; i++) {
    const big = db.batch();
    commits[i].forEach(op => op.op(big));
    await big.commit();
    ok += commits[i].length;
    if (commits.length > 1 && i % 5 === 0) {
      log(`    Batch ${i + 1}/${commits.length} committed (${ok} ops)`, 'dim');
    }
  }
  log(`    ✅ ${ok} operations committed`, 'green');
}

async function clearCollection(name) {
  log(`  Clearing ${name}...`, 'yellow');
  const snaps = await db.collection(name).get();
  if (snaps.empty) { log(`    Already empty`, 'green'); return; }
  const batch = db.batch();
  snaps.docs.forEach(d => batch.delete(d.ref));
  await batch.commit();
  log(`    🗑️  Deleted ${snaps.size} docs`, 'green');
}

async function verifyCollection(name) {
  const snaps = await db.collection(name).limit(10).get();
  return snaps.size;
}

// ── Main ─────────────────────────────────────────────────────────────────────
async function main() {
  log('\n' + '═'.repeat(68), 'cyan');
  log('  🌱 SupremeAI — Real Data Seeder', 'cyan');
  log('═'.repeat(68), 'cyan');

  // Load seed data
  if (!fs.existsSync(SEED_FILE)) {
    console.error(`❌ Seed file not found: ${SEED_FILE}`);
    process.exit(1);
  }
  const raw = fs.readFileSync(SEED_FILE, 'utf8');
  const data = JSON.parse(raw);

  log(`\n📄 Seed file: ${CUSTOM_JSON ? CUSTOM_JSON : SEED_FILE}`, 'bold');
  log(`   Version: ${data.metadata.version}`, 'dim');
  log(`   Generated: ${data.metadata.generated_at}`, 'dim');
  log(`   Collections: ${data.metadata.collections.join(', ')}`, 'dim');

  if (DRY_RUN) log('\n🟡 DRY RUN — no data will be written', 'yellow');
  else         log('\n🟢 EXECUTE MODE — writing to Firestore', 'green');

  const results = {};
  const t0 = Date.now();

  // ── 1. user_tiers ──────────────────────────────────────────────────────────
  {
    const name = 'user_tiers';
    log(`\n📦 ${name}`, 'blue');
    const entries = data.userTiers || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    const batchOps = entries.map((e, i) => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true }),
      idx: i
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} tiers (FREE, PRO, ADMIN)`, 'dim');
  }

  // ── 2. users ────────────────────────────────────────────────────────────────
  {
    const name = 'users';
    log(`\n👤 ${name}`, 'blue');
    const entries = data.users || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    log(`    Users: ${entries.map(u => `${u.displayName} (${u.role})`).join(', ')}`, 'dim');
    const batchOps = entries.map((e) => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true }),
      idx: 0
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} users`, 'dim');
  }

  // ── 3. api_providers ────────────────────────────────────────────────────────
  {
    const name = 'api_providers';
    log(`\n🤖 ${name}`, 'blue');
    const entries = data.apiProviders || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => {
      log(`    ${e.id}: ${e.name} [${e.type}] priority=${e.priority} roles=[${e.assignedRoles.join(',')}] latency=${e.lastLatency}ms`, 'dim');
    });
    const batchOps = entries.map((e) => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} providers`, 'dim');
  }

  // ── 4. ai_agents ────────────────────────────────────────────────────────────
  {
    const name = 'ai_agents';
    log(`\n🧠 ${name}`, 'blue');
    const entries = data.aiAgents || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.name} | ${e.type} | tasks=${e.tasks} | load=${e.load}%`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} agents`, 'dim');
  }

  // ── 5. system_learning ──────────────────────────────────────────────────────
  {
    const name = 'system_learning';
    log(`\n📚 ${name}`, 'blue');
    const entries = data.systemLearning || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    SL-${e.id.split('-')[1]}: ${e.topic} [${e.category}] conf=${e.confidenceScore}`, 'dim'));
    const batchOps = entries.map((e, i) => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} system_learning entries`, 'dim');
  }

  // ── 6. knowledge_domains ────────────────────────────────────────────────────
  {
    const name = 'knowledge_domains';
    log(`\n🌐 ${name}`, 'blue');
    const entries = data.knowledgeDomains || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.name}: status=${e.status} depth=${e.depth} confidence=${e.averageConfidence}`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} knowledge domains`, 'dim');
  }

  // ── 7. knowledge_entries ─────────────────────────────────────────────────────
  {
    const name = 'knowledge_entries';
    log(`\n📖 ${name}`, 'blue');
    const entries = data.knowledgeEntries || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: ${e.topic} [${e.sourceProvider}]`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} knowledge entries`, 'dim');
  }

  // ── 8. knowledge_recommendations ────────────────────────────────────────────
  {
    const name = 'knowledge_recommendations';
    log(`\n💡 ${name}`, 'blue');
    const entries = data.knowledgeRecommendations || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: ${e.topic} [${e.status}]`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} recommendations`, 'dim');
  }

  // ── 9. consensus_results ─────────────────────────────────────────────────────
  {
    const name = 'consensus_results';
    log(`\n🗳️  ${name}`, 'blue');
    const entries = data.consensusResults || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: STRENGTH=${e.strength} conf=${e.averageConfidence} votes=${(e.votes || []).length}`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} consensus results`, 'dim');
  }

  // ── 10. provider_task_performance ───────────────────────────────────────────
  {
    const name = 'provider_task_performance';
    log(`\n📊 ${name}`, 'blue');
    const entries = data.providerTaskPerformance || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: rate=${e.successRate} avgMs=${e.averageResponseTimeMs} tasks=${e.totalTasks}`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} performance records`, 'dim');
  }

  // ── 11. workflow_definitions ─────────────────────────────────────────────────
  {
    const name = 'workflow_definitions';
    log(`\n⚡ ${name}`, 'blue');
    const entries = data.workflowDefinitions || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: ${e.name} [${e.trigger}] steps=${(e.steps || []).length}`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e, { merge: true })
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} workflows`, 'dim');
  }

  // ── 12. activity_logs ────────────────────────────────────────────────────────
  {
    const name = 'activity_logs';
    log(`\n📋 ${name}`, 'blue');
    const entries = data.activityLogs || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: ${e.action} (${e.severity}) → ${e.outcome}`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e)
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} activity logs`, 'dim');
  }

  // ── 13. reasoning_logs ───────────────────────────────────────────────────────
  {
    const name = 'reasoning_logs';
    log(`\n🤔 ${name}`, 'blue');
    const entries = data.reasoningLogs || [];
    if (CLEAR_FIRST && !DRY_RUN) await clearCollection(name);
    entries.forEach(e => log(`    ${e.id}: ${e.taskId} decision="${e.decision?.slice(0, 60)}..."`, 'dim'));
    const batchOps = entries.map(e => ({
      op: (b) => b.set(db.collection(name).doc(e.id), e)
    }));
    results[name] = { total: entries.length, verified: DRY_RUN ? 0 : await verifyCollection(name) };
    if (!DRY_RUN) await runBatchOps(batchOps);
    else log(`    Would upsert ${entries.length} reasoning logs`, 'dim');
  }

  // ── Summary ─────────────────────────────────────────────────────────────────
  const elapsed = ((Date.now() - t0) / 1000).toFixed(2);
  const totalDocs = Object.values(results).reduce((s, r) => s + (r.total || 0), 0);

  log('\n' + '═'.repeat(68), 'cyan');
  log('  📊 Seeding Summary', 'cyan');
  log('═'.repeat(68), 'cyan');
  log(`\n  Mode        : ${DRY_RUN ? '🟡 DRY RUN (no writes)' : '🟢 LIVE (Firestore written)'}`, DRY_RUN ? 'yellow' : 'green');
  log(`  Collections : 13 seeded`);
  log(`  Total docs  : ${totalDocs}`);
  log(`  Elapsed     : ${elapsed}s`);

  if (!DRY_RUN) {
    log('\n── Verified Counts ──────────────────────────', 'blue');
    let allOk = true;
    for (const [col, r] of Object.entries(results)) {
      const verified = r.verified ?? 0;
      const diff = Math.abs(verified - r.total);
      const ok = verified >= r.total; // allow more if --additive without --clear
      const status = ok ? C.green : C.red;
      const mark = ok ? '✅' : '❌';
      log(`  ${mark} ${col.padEnd(37)} seeded=${String(r.total).padStart(4)}  firestore=${verified}`, status);
      if (!ok) allOk = false;
    }
    log('──────────────────────────────────────────────', 'blue');
    if (allOk) {
      log('\n✅ All collections verified — data is live in Firestore!', 'green');
    } else {
      log('\n⚠️  Some collections show lower count — check for import errors above', 'yellow');
    }
  }

  const elapsed2 = ((Date.now() - t0) / 1000).toFixed(1);
  log(`\n${DRY_RUN ? '💡 Add --execute to write to Firestore' : '💡 Run again with --json <file> to customise'}`, 'cyan');
  log(`   Time: ${elapsed2}s`, 'dim');
  log('');

  process.exit(0);
}

main().catch(err => {
  console.error('\n❌ Fatal error:', err);
  process.exit(1);
});
