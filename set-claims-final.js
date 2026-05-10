const admin = require('firebase-admin');
const path = require('path');

// Use the service account key file
const serviceAccount = require('./service-account.json');

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
      console.log(`✅ SUCCESS: Admin claims set for ${email} (UID: ${user.uid})`);
    } catch (err) {
      console.error(`❌ FAILED for ${email}:`, err.code, err.message);
    }
  }
}

setClaims().then(() => {
  console.log('\n✅ All done! Users must sign out and back in to refresh their tokens.');
});
