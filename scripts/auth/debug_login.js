/**
 * Debug script: Tests full login flow
 * 1. Signs in via Firebase REST API (client-side simulation)
 * 2. Sends the ID token to the backend /api/auth/firebase-login
 * 3. Captures and prints all errors
 */

const https = require('https');
const http = require('http');

// Firebase project config
const API_KEY = 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8';
const EMAIL = 'niloyjoy7@gmail.com';
const PASSWORD = '12345678';

function httpsPost(url, data) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(data);
    const urlObj = new URL(url);
    const options = {
      hostname: urlObj.hostname,
      path: urlObj.pathname + urlObj.search,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body)
      }
    };
    const req = https.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, body: JSON.parse(data) });
        } catch(e) {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

function httpPost(url, data, extraHeaders = {}) {
  return new Promise((resolve, reject) => {
    const body = JSON.stringify(data);
    const urlObj = new URL(url);
    const options = {
      hostname: urlObj.hostname,
      port: urlObj.port || 80,
      path: urlObj.pathname,
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'Content-Length': Buffer.byteLength(body),
        ...extraHeaders
      }
    };
    const req = http.request(options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          resolve({ status: res.statusCode, headers: res.headers, body: JSON.parse(data) });
        } catch(e) {
          resolve({ status: res.statusCode, headers: res.headers, body: data });
        }
      });
    });
    req.on('error', reject);
    req.write(body);
    req.end();
  });
}

async function run() {
  console.log('='.repeat(60));
  console.log('SupremeAI Login Debug Tool');
  console.log('='.repeat(60));
  console.log(`Testing login for: ${EMAIL}`);
  console.log('');

  // STEP 1: Sign in via Firebase Identity Toolkit REST API (simulates browser Firebase SDK)
  console.log('STEP 1: Firebase Client Auth (signInWithEmailAndPassword)...');
  const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${API_KEY}`;
  
  let idToken, refreshToken, localId;
  try {
    const firebaseRes = await httpsPost(signInUrl, {
      email: EMAIL,
      password: PASSWORD,
      returnSecureToken: true
    });

    console.log(`  Firebase Auth Status: ${firebaseRes.status}`);
    
    if (firebaseRes.status !== 200) {
      console.log('  ❌ FIREBASE AUTH FAILED!');
      console.log('  Error:', JSON.stringify(firebaseRes.body, null, 2));
      console.log('');
      console.log('  DIAGNOSIS: The user cannot sign in to Firebase itself.');
      if (firebaseRes.body?.error?.message === 'EMAIL_NOT_FOUND') {
        console.log('  → User does not exist in Firebase Authentication.');
        console.log('  → Run: node scripts/recreate_user.js');
      } else if (firebaseRes.body?.error?.message === 'INVALID_PASSWORD') {
        console.log('  → Password is wrong.');
      } else if (firebaseRes.body?.error?.message === 'USER_DISABLED') {
        console.log('  → User account is disabled in Firebase.');
      } else if (firebaseRes.body?.error?.message?.includes('INVALID_LOGIN_CREDENTIALS')) {
        console.log('  → Invalid login credentials (email or password wrong).');
      } else if (firebaseRes.body?.error?.message?.includes('TOO_MANY_ATTEMPTS')) {
        console.log('  → Too many failed attempts. Account temporarily blocked.');
      }
      return;
    }

    idToken = firebaseRes.body.idToken;
    refreshToken = firebaseRes.body.refreshToken;
    localId = firebaseRes.body.localId;
    
    console.log(`  ✅ Firebase Auth SUCCESS!`);
    console.log(`  UID: ${localId}`);
    console.log(`  Email: ${firebaseRes.body.email}`);
    console.log(`  Token length: ${idToken.length} chars`);
    console.log(`  Token preview: ${idToken.substring(0, 50)}...`);
    console.log('');
  } catch(e) {
    console.log(`  ❌ Network error calling Firebase: ${e.message}`);
    return;
  }

  // STEP 2: Send token to backend /api/auth/firebase-login
  console.log('STEP 2: Backend firebase-login endpoint...');
  try {
    // First, get CSRF token (if needed)
    const loginRes = await httpPost('http://localhost:8080/api/auth/firebase-login', {
      idToken: idToken
    });

    console.log(`  Backend Status: ${loginRes.status}`);
    console.log(`  Response Headers: ${JSON.stringify(loginRes.headers, null, 2)}`);
    console.log(`  Response Body: ${JSON.stringify(loginRes.body, null, 2)}`);
    console.log('');

    if (loginRes.status === 200 && loginRes.body?.success) {
      console.log('  ✅ BACKEND LOGIN SUCCESS!');
      console.log('  User role:', loginRes.body?.data?.user?.role);
      console.log('  Token received:', loginRes.body?.data?.token ? 'YES' : 'NO');
    } else if (loginRes.status === 403) {
      console.log('  ❌ 403 FORBIDDEN - Likely a CSRF issue!');
      console.log('  DIAGNOSIS: The backend is rejecting the request due to CSRF protection.');
      console.log('  The /api/auth/firebase-login endpoint should be CSRF-exempt.');
      console.log('  Check SecurityConfig.java csrf().ignoringRequestMatchers()');
    } else if (loginRes.status === 401) {
      console.log('  ❌ 401 UNAUTHORIZED');
      console.log('  DIAGNOSIS: Backend rejected the Firebase token.');
    } else if (loginRes.status === 200 && !loginRes.body?.success) {
      console.log('  ❌ Backend returned error in body:');
      console.log('  Error:', loginRes.body?.error);
      console.log('  Message:', loginRes.body?.message);
    }
  } catch(e) {
    console.log(`  ❌ Network error calling backend: ${e.message}`);
  }
  
  // STEP 3: Check Firestore user record
  console.log('');
  console.log('STEP 3: Checking Firestore for user record...');
  const admin = (() => {
    try { return require('firebase-admin'); } catch(e) { return null; }
  })();
  
  if (admin) {
    try {
      if (admin.apps.length === 0) {
        const serviceAccount = require('../src/main/resources/firebase-service-account.json');
        admin.initializeApp({ credential: admin.credential.cert(serviceAccount) });
      }
      const db = admin.firestore();
      
      // Check users collection
      const userQuery = await db.collection('users').where('email', '==', EMAIL).get();
      if (userQuery.empty) {
        console.log(`  ⚠️  No user document found in Firestore 'users' collection for ${EMAIL}`);
        console.log('  DIAGNOSIS: User exists in Firebase Auth but NOT in Firestore.');
        console.log('  The backend UserRepository.findByFirebaseUid() will return empty,');
        console.log('  causing a new user to be created on first login. This should be OK.');
      } else {
        userQuery.forEach(doc => {
          console.log(`  ✅ Found Firestore user: ${JSON.stringify(doc.data(), null, 4)}`);
        });
      }
    } catch(e) {
      console.log(`  ⚠️  Firestore check failed: ${e.message}`);
    }
  }
  
  console.log('');
  console.log('='.repeat(60));
  console.log('Debug complete.');
  process.exit(0);
}

run().catch(e => {
  console.error('Fatal error:', e);
  process.exit(1);
});
