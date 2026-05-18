const admin = require('firebase-admin');
const fetch = require('node-fetch');

// Initialize Firebase Admin with service account
const serviceAccount = require('./service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: `https://${serviceAccount.project_id}.firebaseio.com`
});

const auth = admin.auth();

// User credentials
const email = 'niloyjoy7@gmail.com';
const password = 'njel.com.bd';
const displayName = 'niloyjoy7';

async function setupAdmin() {
  try {
    console.log(`Setting up Admin user for ${email} in project: ${serviceAccount.project_id}`);
    
    // Check if user exists
    let user;
    try {
      user = await auth.getUserByEmail(email);
      console.log('User exists in Firebase Auth. UID:', user.uid);
      
      // Update password to be sure it is 'njel.com.bd'
      await auth.updateUser(user.uid, {
        password: password,
        displayName: displayName
      });
      console.log('Updated user password/displayName');
    } catch (e) {
      console.log('User does not exist. Creating user...');
      // Create new user
      user = await auth.createUser({
        email,
        password,
        displayName,
        emailVerified: true
      });
      console.log('Created user with UID:', user.uid);
    }

    // Set admin custom claims
    await auth.setCustomUserClaims(user.uid, { role: 'ADMIN', admin: true });
    console.log('Set admin custom claims { role: "ADMIN", admin: true } successfully!');

    // Verify claims
    const updatedUser = await auth.getUser(user.uid);
    console.log('Current Custom Claims:', updatedUser.customClaims);

    // Try signing in using Firebase Auth REST API to verify it works
    const apiKey = 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8';
    const signInUrl = `https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`;

    console.log('Testing sign-in using Firebase REST API...');
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
      throw new Error(`REST Sign-in test failed: ${JSON.stringify(signInData)}`);
    }

    console.log('✅ REST Sign-in successful! ID Token obtained.');
    const idToken = signInData.idToken;

    // Exchange for backend JWT
    console.log('Exchanging Firebase ID token for backend JWT at http://localhost:8080/api/auth/firebase-login...');
    const backendResponse = await fetch('http://localhost:8080/api/auth/firebase-login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ idToken })
    });

    const backendData = await backendResponse.json();
    console.log('Backend response status:', backendResponse.status);
    console.log('Backend response:', JSON.stringify(backendData, null, 2));

    if (backendData.success && backendData.data && backendData.data.token) {
      console.log('\n✅ SUCCESS! Backend JWT token obtained successfully:');
      console.log(backendData.data.token);
      console.log('\nRefresh token:', backendData.data.refreshToken);
      console.log('\nUser role:', backendData.data.user.role);
    } else {
      console.error('❌ Failed to exchange token:', backendData);
    }

  } catch (error) {
    console.error('❌ Error setting up admin:', error.message);
    process.exit(1);
  }
}

setupAdmin();
