const admin = require('firebase-admin');
const serviceAccount = require('./src/main/resources/firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const email = process.argv[2];
if (!email) {
  console.error('Please provide an email. Usage: node make-admin.js <email>');
  process.exit(1);
}

async function makeAdmin() {
  try {
    const userRecord = await admin.auth().getUserByEmail(email);
    await admin.auth().setCustomUserClaims(userRecord.uid, { role: 'ADMIN', admin: true });
    console.log('Successfully set custom claims for:', email);
    
    const db = admin.firestore();
    await db.collection('users').doc(userRecord.uid).update({
      tier: 'ADMIN',
      role: 'admin',
      isAdmin: true
    });
    console.log('Successfully updated Firestore user document to ADMIN.');
    process.exit(0);
  } catch (error) {
    console.error('Error making user admin:', error.message);
    process.exit(1);
  }
}

makeAdmin();
