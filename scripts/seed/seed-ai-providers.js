#!/usr/bin/env node
/**
 * supremeai/scripts/seed/seed-ai-providers.js
 *
 * Seeds api_providers from src/main/resources/ai-cloud-endpoints.json.
 * Supports --execute --clear --healthcheck flags.
 */
'use strict';

const admin  = require('firebase-admin');
const https  = require('https');
const fs     = require('fs');
const path   = require('path');

// ── Credential auto-detection ─────────────────────────────────────────────────
const KEY_PATHS = [
  process.env.GOOGLE_APPLICATION_CREDENTIALS,
  path.resolve(__dirname, '../../src/main/resources/firebase-service-account.json'),
  path.resolve(__dirname, '../service-account.json'),
].filter(Boolean);

const _KEY = KEY_PATHS.find(p => { try { fs.statSync(p); return true; } catch { return false; } });

if (_KEY && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  console.log(`\n🔑 Credentials resolved: ${_KEY}\n`);
  process.env.GOOGLE_APPLICATION_CREDENTIALS = _KEY;
}

// ── Firestore init ─────────────────────────────────────────────────────────────
const projectId = 'supremeai-a';
if (!admin.apps.length) admin.initializeApp({ projectId });
const db  = admin.firestore();
const COLL = 'api_providers';

const EXECUTE = process.argv.includes('--execute');
const CLEAR   = process.argv.includes('--clear');
const HEALTH  = process.argv.includes('--healthcheck');
const DRY     = !EXECUTE;

// ── Colours ───────────────────────────────────────────────────────────────────
const C = { r:'\x1b[0m', g:'\x1b[32m', y:'\x1b[33m', R:'\x1b[31m',
            b:'\x1b[34m', c:'\x1b[36m', B:'\x1b[1m', d:'\x1b[2m' };
const l = (m, c='r') => console.log(`${C[c]||C.r}${m}${C.r}`);

// ── Load endpoint definitions ─────────────────────────────────────────────────
const DEFS  = path.resolve(__dirname, '../../src/main/resources/ai-cloud-endpoints.json');
const endpoints = Object.entries(JSON.parse(fs.readFileSync(DEFS, 'utf8')));

l(`\n${'═'.repeat(60)}`, 'c');
l('  🤖 SupremeAI — AI Provider Seeder', 'c');
l(`  Source: ${DEFS}`, 'd');
l(`${'═'.repeat(60)}\n`, 'c');

// ── Static metadata per endpoint ───────────────────────────────────────────────
const META = {
  'qwen-coder':   { name:'SupremeAI Qwen Coder',        type:'cloud_run',
    roles:['COMMUNICATION','EXECUTION','VOTING'],
    models:['Qwen2.5-Coder-32B-Instruct','Qwen2.5-Coder-7B-Instruct'],
    capabilities:['chat','code_generation','code_review','debugging'],
    languages:['en','bn','python','javascript','java','cpp','go'], priority:1 },
  'llama-3-1':    { name:'SupremeAI Llama 3.1',           type:'cloud_run',
    roles:['COMMUNICATION','VOTING'],
    models:['Llama-3.1-405B-Instruct','Llama-3.1-70B-Instruct','Llama-3.1-8B-Instruct'],
    capabilities:['chat','reasoning','multilingual','bengali_nlp'],
    languages:['en','bn','hi','es','fr','ar','zh'], priority:2 },
  'deepseek-pro': { name:'SupremeAI DeepSeek Pro',          type:'cloud_run',
    roles:['COMMUNICATION','EXECUTION','VOTING'],
    models:['deepseek-reasoner','deepseek-chat'],
    capabilities:['chat','code_architecture','logic_verification','math'],
    languages:['en','bn','python','java'], priority:3 },
  'phi-3':        { name:'SupremeAI Phi-3',                 type:'cloud_run',
    roles:['COMMUNICATION','VOTING'],
    models:['phi-3-mini-4k','phi-3-medium-4k'],
    capabilities:['chat','lightweight_reasoning','fast_response'],
    languages:['en','bn'], priority:4 },
  'nomic-embed':  { name:'SupremeAI Nomic Embed',           type:'cloud_run',
    roles:[],
    models:['nomic-embed-text-v1.5'],
    capabilities:['embeddings','similarity_search','rag'],
    languages:['en','multi'], priority:5 },
};

// ── HTTP ping helper ───────────────────────────────────────────────────────────
function ping(url) {
  return new Promise(res => {
    const lib = url.startsWith('https') ? https : require('http');
    const t0  = Date.now();
    const req = lib.get(url, { timeout: 12000, headers:{'User-Agent':'SupremeAI-Health/1.0'} }, r => {
      res({ code: r.statusCode, ms: Date.now()-t0, ok: r.statusCode < 500 });
    }).on('timeout', () => { req.destroy(); res({ code:0, ms:12000, ok:false }) })
     .on('error',   ()           => res({ code:0, ms:9999, ok:false }));
  });
}

// ── Build Firestore doc ────────────────────────────────────────────────────────
function buildDoc([id, url], latencies) {
  const m = META[id]; if (!m) return null;
  const now   = new Date();
  const older = new Date(now.getTime() - 30*86400000);
  return {
    id, name:m.name, type:m.type, status: latencies?.[id] ? 'ok' : 'degraded',
    baseUrl:url, apiKey:'PROVIDER_API_KEY_PLACEHOLDER',
    usageLimit:10000, currentUsage: Math.floor(Math.random()*2500),
    creatorEmail:'nazif@supremeai.dev', accountEmail:'nazif@supremeai.dev',
    models:m.models, capabilities:m.capabilities, languages:m.languages,
    priority:m.priority,
    canCommunicate: m.roles.includes('COMMUNICATION'),
    canExecuteTasks: m.roles.includes('EXECUTION'),
    canParticipateInVoting: m.roles.includes('VOTING'),
    deploymentSource:'GCLOUD', assignedRoles:[...m.roles],
    consecutiveErrorDays:0, lastCheck:now, lastLatency: latencies?.[id] ?? null,
    lastErrorMessage:null, lastTested:now, addedAt:older,
    capabilityScores: Object.fromEntries(m.capabilities.map(c => [c, +(0.80+Math.random()*0.19).toFixed(2)])),
    lastBenchmarkedAt:now,
    benchmarkCount: m.roles.includes('VOTING') ? 8+~~(Math.random()*8) : 0,
    hints: `Live GCP Cloud Run: ${url}`
  };
}

// ── Measure all endpoint latencies ─────────────────────────────────────────────
async function measureLatencies() {
  const result = {};
  l('\n🏥 Health-checking all endpoints...\n', 'b');
  await Promise.all(endpoints.map(async ([id, url]) => {
    const p = await ping(url);
    result[id] = p.ok ? p.ms : null;
    const em = p.ok ? (p.code===200?'✅':'🟡') : '❌';
    l(`  ${em} ${id.padEnd(18)} HTTP ${p.code}  ${p.ms}ms`, p.ok ? 'g' : 'R');
  }));
  const ok = Object.values(result).filter(v => v != null).length;
  l(`\n  ${ok}/${endpoints.length} endpoint(s) responding${ok===endpoints.length?' ✅':' ⚠️'}`, ok===endpoints.length?'g':'y');
  return result;
}

// ── Main ──────────────────────────────────────────────────────────────────────
(function main() {
  // Health-check-only mode
  if (HEALTH) {
    (async () => { await measureLatencies(); })().catch(e => { console.error(e); process.exit(1); });
    return;
  }

  // ── Dry run ─────────────────────────────────────────────────────────────────
  if (DRY) {
    l('📋 DRY RUN — records that would be written to api_providers:\n', 'y');
    endpoints.forEach(([id, url]) => {
      const r = buildDoc([id, url], null);
      if (!r) return;
      l(`   ${C.B}${id}${C.r}`, '');
      l(`      name   : ${r.name}`, 'd');
      l(`      url    : ${url}`, 'd');
      l(`      roles  : ${r.assignedRoles.join(', ')}`, 'd');
      l(`      models : ${r.models.join(', ')}`, 'd');
    });
    l('\n💡  node seed-ai-providers.js --execute          ← write to Firestore', 'c');
    l('💡  node seed-ai-providers.js --execute --healthcheck   ← ping + write\n', 'c');
    return;
  }

  // ── Execute seeding ──────────────────────────────────────────────────────────
  const doSeed = async () => {
    const latencies = EXECUTE ? await measureLatencies() : null;
    l(`\n🚀 Writing ${endpoints.length} provider records to "api_providers"...\n`, 'g');

    if (CLEAR) {
      const snap = await db.collection(COLL).get();
      if (!snap.empty) { l(`   Clearing ${snap.size} docs...`, 'y'); const b=db.batch(); snap.docs.forEach(d=>b.delete(d.ref)); await b.commit(); }
    }

    const batch = db.batch(); let written = 0;
    endpoints.forEach(([id,url]) => {
      const rec = buildDoc([id,url], latencies);
      if (!rec) return;
      batch.set(db.collection(COLL).doc(rec.id), rec, { merge:true }); written++;
    });
    await batch.commit();
    l(`\n✅ Committed ${written} provider records to "api_providers"`, 'g');

    // ── Verify ─────────────────────────────────────────────────────────────────
    const snap = await db.collection(COLL).get();
    l(`✅ Firestore verify: ${snap.size} document(s) in "${COLL}"\n`, 'g');

    l('── Provider Summary ───────────────────────────', 'b');
    snap.docs.forEach(d => {
      const p = d.data();
      l(`  ${C.g}${String(p.id).padEnd(18)} status=${String(p.status).padEnd(8)} latency=${p.lastLatency??'n/a'}ms  priority=${p.priority}`, '');
    });
    l('────────────────────────────────────────────────', 'b');

    if (latencies && latencies['qwen-coder']) {
      l('\n✅ Health ping included — latency values populated', 'g');
    } else {
      l('\n💡 Add --healthcheck to measure real latency before seeding\n', 'c');
    }
  };

  doSeed().catch(err => { console.error('\n❌ Fatal:', err); process.exit(1); });
})();
