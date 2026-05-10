// Create admin user and get backend JWT token
const admin = require('firebase-admin');
const fetch = require('node-fetch');
const path = require('path');

// Initialize Firebase Admin with service account
const serviceAccount = require('./service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://supremeai-a.firebaseio.com'
});

const auth = admin.auth();

// User credentials
const email = 'admin@supremeai.com';
const password = 'AdminPass123!';
const displayName = 'System Administrator';

async function setupAdmin() {
  try {
    // Check if user exists
    let user;
    try {
      user = await auth.getUserByEmail(email);
      console.log('User exists:', user.uid);
    } catch (e) {
      // Create new user
      user = await auth.createUser({
        email,
        password,
        displayName,
        emailVerified: true
      });
      console.log('Created user:', user.uid);
    }

    // Set admin custom claims
    await auth.setCustomUserClaims(user.uid, { role: 'ADMIN', admin: true });
    console.log('Set admin claims');

    // Sign in using Firebase Auth REST API to get ID token
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
      throw new Error(`Sign in failed: ${JSON.stringify(signInData)}`);
    }

    const idToken = signInData.idToken;
    console.log('Got Firebase ID token, length:', idToken.length);

    // Exchange for backend JWT
    const backendResponse = await fetch('http://localhost:8080/api/auth/firebase-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken })
    });

    const backendData = await backendResponse.json();
    console.log('Backend response:', JSON.stringify(backendData, null, 2));

    if (backendData.token) {
      console.log('\n✅ SUCCESS! Backend JWT token:');
      console.log(backendData.token);
      console.log('\nRefresh token:', backendData.refreshToken);
      console.log('\nUser role:', backendData.user.role);
      // Save token for later use
      require('fs').writeFileSync('./auth-token.txt', backendData.token);
      console.log('\nToken saved to auth-token.txt');
    } else {
      console.error('Failed to get token:', backendData);
    }

  } catch (error) {
    console.error('Error:', error.message);
    process.exit(1);
  }
}

setupAdmin();
