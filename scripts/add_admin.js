const admin = require('firebase-admin');
const serviceAccount = require('../src/main/resources/firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function addAdmin(email) {
  const configRef = db.collection('system_configs').doc('global_settings');
  
  try {
    const doc = await configRef.get();
    let adminEmails = [];
    
    if (doc.exists) {
      const data = doc.data();
      adminEmails = data.adminEmails || [];
    }
    
    if (!adminEmails.includes(email)) {
      adminEmails.push(email);
      await configRef.set({ adminEmails: adminEmails }, { merge: true });
      console.log(`Successfully added ${email} to adminEmails`);
    } else {
      console.log(`${email} is already in adminEmails`);
    }
  } catch (error) {
    console.error('Error updating document:', error);
  } finally {
    process.exit();
  }
}

const emailToAdd = 'niloyjoy7@gmail.com';
addAdmin(emailToAdd);
