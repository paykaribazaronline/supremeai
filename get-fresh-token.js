// Get a Firebase ID token for the admin user and exchange for backend JWT
const admin = require('firebase-admin');
const fetch = require('node-fetch');

const serviceAccount = require('./service-account.json');
admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://supremeai-a.firebaseio.com'
});

const email = 'niloyjoy7@gmail.com'; // Using existing user from dashboard
const password = 'AdminPass123!';
const apiKey = 'AIzaSyCib1UPogwLoAshIWm9YQJB_RR0UxC07i8';

async function testFlow() {
  try {
    // Sign in with Firebase to get ID token
    console.log('Attempting Firebase sign-in...');
    const signInResp = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ email, password, returnSecureToken: true })
    });
    
    const signInData = await signInResp.json();
    if (!signInResp.ok) {
      console.log('Sign-in failed, attempting to create user...');
      // Create the user if it doesn't exist
      const createResp = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=${apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, returnSecureToken: true })
      });
      const createData = await createResp.json();
      if (!createResp.ok) throw new Error(JSON.stringify(createData));
      console.log('Created user, now setting admin claims...');
      
      // Set admin custom claims
      await admin.auth().setCustomUserClaims(createData.localId, { role: 'ADMIN', admin: true });
      console.log('Admin claims set');
      
      // Sign in with the new credentials
      const signInResp2 = await fetch(`https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=${apiKey}`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, password, returnSecureToken: true })
      });
      const signInData2 = await signInResp2.json();
      if (!signInResp2.ok) throw new Error(JSON.stringify(signInData2));
      
      // Exchange for backend JWT
      console.log('Exchanging Firebase ID token for backend JWT...');
      const backendResp = await fetch('http://localhost:8080/api/auth/firebase-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken: signInData2.idToken })
      });
      const backendData = await backendResp.json();
      console.log('Backend response:', JSON.stringify(backendData, null, 2));
      
      if (backendData.token) {
        console.log('\n✅ Backend JWT token:', backendData.token);
        console.log('Role:', backendData.user.role);
        // Save to file
        require('fs').writeFileSync('./auth-token.txt', backendData.token);
      }
    } else {
      console.log('User exists, checking claims...');
      // Set admin claims just in case
      const user = await admin.auth().getUserByEmail(email);
      await admin.auth().setCustomUserClaims(user.uid, { role: 'ADMIN', admin: true });
      console.log('Admin claims ensured');
      
      // Exchange for backend JWT
      const backendResp = await fetch('http://localhost:8080/api/auth/firebase-login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ idToken: signInData.idToken })
      });
      const backendData = await backendResp.json();
      console.log('Backend response:', JSON.stringify(backendData, null, 2));
      
      if (backendData.token) {
        console.log('\n✅ Backend JWT token:', backendData.token);
        require('fs').writeFileSync('./auth-token.txt', backendData.token);
      }
    }
  } catch (err) {
    console.error('Error:', err.message);
  }
}

testFlow();
