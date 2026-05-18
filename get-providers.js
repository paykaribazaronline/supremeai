const admin = require('firebase-admin');
const fetch = require('node-fetch');

// Initialize Firebase Admin with service account
const serviceAccount = require('./service-account.json');

if (!admin.apps.length) {
  admin.initializeApp({
    credential: admin.credential.cert(serviceAccount),
    databaseURL: `https://${serviceAccount.project_id}.firebaseio.com`
  });
}

const auth = admin.auth();
const email = 'niloyjoy7@gmail.com';
const password = 'njel.com.bd';

async function checkProviders() {
  try {
    console.log('1. Signing in to Firebase REST API...');
    const apiKey = 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8';
    const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`;

    const response = await fetch(signInUrl, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email,
        password,
        returnSecureToken: true
      })
    });

    const signInData = await response.json();
    if (!response.ok) {
      throw new Error(`Sign-in failed: ${JSON.stringify(signInData)}`);
    }

    const idToken = signInData.idToken;
    console.log('✅ Firebase Sign-in successful!');

    console.log('2. Exchanging Firebase ID token for backend JWT...');
    const backendResponse = await fetch('http://localhost:8080/api/auth/firebase-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken })
    });

    const backendData = await backendResponse.json();
    if (!backendResponse.ok || !backendData.success) {
      throw new Error(`Token exchange failed: ${JSON.stringify(backendData)}`);
    }

    const token = backendData.data.token;
    console.log('✅ Backend JWT token obtained!');

    console.log('3. Fetching configured providers...');
    const providersResponse = await fetch('http://localhost:8080/api/admin/providers/configured', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const providersData = await providersResponse.json();
    console.log('\n--- Configured Providers List (Summary) ---');
    const rawData = providersData.data?.providers || (Array.isArray(providersData.data) ? providersData.data : []);
    console.log(`Total Providers: ${rawData.length}`);
    rawData.forEach(p => {
      console.log(`- ID: ${p.id} | Name: ${p.name} | Source: ${p.deploymentSource} | Status: ${p.status} | Active: ${p.active}`);
    });

    console.log('\n4. Fetching health stats...');
    const statsResponse = await fetch('http://localhost:8080/api/admin/providers/health-stats', {
      headers: {
        'Authorization': `Bearer ${token}`
      }
    });

    const statsData = await statsResponse.json();
    console.log('\n--- Health Stats ---');
    console.log(JSON.stringify(statsData, null, 2));

  } catch (error) {
    console.error('❌ Error:', error.message);
  }
}

checkProviders();
