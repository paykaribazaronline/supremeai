const admin = require('firebase-admin');
const fs = require('fs');
const path = require('path');
const crypto = require('crypto');

// Initialize Firebase Admin
const serviceAccountPath = path.join(__dirname, 'service-account.json');
if (!fs.existsSync(serviceAccountPath)) {
    console.error('❌ service-account.json not found!');
    process.exit(1);
}

const serviceAccount = JSON.parse(fs.readFileSync(serviceAccountPath, 'utf8'));

// Realtime Database URL
const databaseURL = "https://supremeai-a-default-rtdb.asia-southeast1.firebasedatabase.app/";

if (admin.apps.length === 0) {
    admin.initializeApp({
        credential: admin.credential.cert(serviceAccount),
        databaseURL: databaseURL
    });
}

const db = admin.firestore();
const rtdb = admin.database();

// Load .env
const envPath = path.join(__dirname, '.env');
if (!fs.existsSync(envPath)) {
    console.error('❌ .env file not found!');
    process.exit(1);
}

const envContent = fs.readFileSync(envPath, 'utf8');
const env = {};
envContent.split('\n').forEach(line => {
    line = line.trim();
    if (!line || line.startsWith('#')) return;
    const parts = line.split('=');
    if (parts.length >= 2) {
        const key = parts[0].trim();
        const value = parts.slice(1).join('=').trim();
        env[key] = value;
    }
});

const encryptionKeyRaw = env['API_ENCRYPTION_KEY'] || 'supreme-ai-default-encryption-key-32-chars!!';
const encryptionKey = crypto.createHash('sha256').update(encryptionKeyRaw).digest();

console.log('🔑 Using encryption key derived from .env');

/**
 * Encrypt using AES-256-GCM
 */
function encrypt(plainText) {
    if (!plainText) return plainText;
    try {
        const iv = crypto.randomBytes(12);
        const cipher = crypto.createCipheriv('aes-256-gcm', encryptionKey, iv);
        let encrypted = cipher.update(plainText, 'utf8');
        encrypted = Buffer.concat([encrypted, cipher.final()]);
        const authTag = cipher.getAuthTag();
        const combined = Buffer.concat([iv, encrypted, authTag]);
        return combined.toString('base64');
    } catch (e) {
        console.error('Encryption failed:', e.message);
        return null;
    }
}

async function deployKeys() {
    const keysToDeploy = Object.keys(env).filter(k =>
        (k.includes('API_KEY') || k.includes('TOKEN')) &&
        !k.startsWith('FIREBASE_ADMIN') &&
        !k.startsWith('API_ENCRYPTION')
    );

    const userId = 'system-admin';
    const firestoreBatch = db.batch();
    const rtdbUpdates = {};
    let count = 0;

    // Helper to determine base provider and sub-key name
    const providers = ['GEMINI', 'GROQ', 'OPENAI', 'DEEPSEEK', 'STEPFUN', 'KIMI', 'ANTHROPIC', 'MISTRAL', 'HUGGINGFACE', 'MAPS', 'GOOGLE_GEMINI', 'CONTINUE_DEV', 'CODEGEEX', 'KILO_CLAW'];

    // First, clear existing keys to prevent duplicates/mess (optional but good for restructuring)
    console.log('🧹 Preparing to restructure Realtime Database keys...');
    await rtdb.ref('config/api_keys').remove();

    for (const envKey of keysToDeploy) {
        const val = env[envKey];
        if (!val || val.includes('your-openai-key') || val.includes('your-ant-your-key') || val === '2oBh1tgJ2f0cC0EuHYlOdQ7pZExi33NGK3sjlJMDIUrweZqpU0IpDhxCume8kdnjJ') {
            continue;
        }

        let baseProvider = 'OTHER';
        let subKeyName = 'key';

        for (const p of providers) {
            if (envKey.startsWith(p)) {
                baseProvider = p;
                // Determine subKeyName from suffix
                let suffix = envKey.replace(p, '').replace('_API_KEY', '').replace('API_KEY_', '').replace('_TOKEN', '').replace(/^_/, '').toLowerCase();
                if (suffix) {
                    subKeyName = `key_${suffix}`;
                } else {
                    subKeyName = 'key';
                }
                break;
            }
        }

        if (baseProvider === 'OTHER') {
            baseProvider = envKey.replace('_API_KEY', '').replace('API_KEY_', '').replace('_TOKEN', '');
        }

        console.log(`📡 Deploying ${envKey} -> config/api_keys/${baseProvider}/${subKeyName}`);

        const encryptedKey = encrypt(val);
        if (!encryptedKey) continue;

        // 1. Deploy to Firestore (Encrypted) - Keep unique doc per envKey for rotation history
        const docRef = db.collection('user_api_keys').doc(`${userId}_${envKey.toLowerCase()}`);
        firestoreBatch.set(docRef, {
            userId: userId,
            provider: baseProvider,
            envKey: envKey,
            subKeyName: subKeyName,
            label: `System Key: ${envKey}`,
            apiKey: encryptedKey,
            status: 'active',
            requestCount: 0,
            estimatedCost: 0.0,
            addedAt: admin.firestore.FieldValue.serverTimestamp(),
            lastTested: null,
            lastUsed: null
        }, { merge: true });

        // 2. Deploy to Realtime Database with the new structure
        rtdbUpdates[`config/api_keys/${baseProvider}/${subKeyName}`] = val;
        rtdbUpdates[`config/api_keys/${baseProvider}/status`] = "active";

        count++;
    }

    if (count > 0) {
        await firestoreBatch.commit();
        await rtdb.ref().update(rtdbUpdates);
        console.log(`\n✅ Successfully restructured and deployed ${count} keys!`);
        console.log(`New structure: config/api_keys/[PROVIDER]/key, key_1, key_2, etc.`);
    } else {
        console.log('\n⚠️ No valid keys found in .env to deploy.');
    }

    process.exit(0);
}

deployKeys().catch(err => {
    console.error('❌ Deployment failed:', err);
    process.exit(1);
});
