const admin = require('firebase-admin');
const serviceAccount = require('../src/main/resources/firebase-service-account.json');

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

async function recreateUser(email, password) {
  try {
    // 1. Try to get the user
    let user;
    try {
      user = await admin.auth().getUserByEmail(email);
      console.log(`Found existing user with UID: ${user.uid}. Deleting...`);
      await admin.auth().deleteUser(user.uid);
      console.log(`User ${email} deleted successfully.`);
    } catch (e) {
      if (e.code === 'auth/user-not-found') {
        console.log(`User ${email} not found. Proceeding to create.`);
      } else {
        throw e;
      }
    }

    // 2. Create the user
    const newUser = await admin.auth().createUser({
      email: email,
      password: password,
      displayName: email.split('@')[0],
      emailVerified: true
    });

    console.log(`Successfully created new user with UID: ${newUser.uid}`);

    // 3. Ensure the user is in adminEmails in Firestore
    const db = admin.firestore();
    const configRef = db.collection('system_configs').doc('global_settings');
    const doc = await configRef.get();
    let adminEmails = [];
    if (doc.exists) {
      adminEmails = doc.data().adminEmails || [];
    }
    if (!adminEmails.includes(email)) {
      adminEmails.push(email);
      await configRef.set({ adminEmails: adminEmails }, { merge: true });
      console.log(`Added ${email} to adminEmails list.`);
    }

  } catch (error) {
    console.error('Operation failed:', error);
  } finally {
    process.exit();
  }
}

const email = 'niloyjoy7@gmail.com';
const password = '12345678';
recreateUser(email, password);
