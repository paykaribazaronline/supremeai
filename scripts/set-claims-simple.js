// Simple script to set admin claims - uses Firebase Admin SDK only
const admin = require('firebase-admin');
const path = require('path');

// Use service account from functions folder
const serviceAccount = require('./functions/service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount),
  databaseURL: 'https://supremeai-a.firebaseio.com'
});

const auth = admin.auth();

const emails = ['niloyjoy7@gmail.com', 'admin@supremeai.com'];

async function setClaims() {
  for (const email of emails) {
    try {
      const user = await auth.getUserByEmail(email);
      await auth.setCustomUserClaims(user.uid, { role: 'ADMIN', admin: true });
      console.log(`✅ Set admin claims for: ${email} (UID: ${user.uid})`);
    } catch (err) {
      console.error(`❌ Error for ${email}:`, err.message);
    }
  }
}

setClaims();
