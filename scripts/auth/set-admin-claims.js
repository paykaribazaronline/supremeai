// Set admin custom claims for specified users
// Usage: node set-admin-claims.js
// Requires: firebase-admin installed and authenticated

const admin = require('firebase-admin');

// Initialize Firebase Admin
try {
  admin.initializeApp({
    credential: admin.credential.applicationDefault(),
    databaseURL: 'https://supremeai-a.firebaseio.com'
  });
} catch (e) {
  // Already initialized
}

const auth = admin.auth();

// Users to make admin
const usersToMakeAdmin = [
  'niloyjoy7@gmail.com',
  'admin@supremeai.com'
];

async function setAdminClaims() {
  for (const email of usersToMakeAdmin) {
    try {
      console.log(`\nProcessing: ${email}`);
      const user = await auth.getUserByEmail(email);
      console.log(`  User UID: ${user.uid}`);

      // Set admin custom claims
      await auth.setCustomUserClaims(user.uid, { role: 'ADMIN', admin: true });
      console.log(`  ✅ Admin claims set successfully`);

      // Verify
      const updatedUser = await auth.getUser(user.uid);
      console.log(`  Verified claims:`, updatedUser.customClaims);

    } catch (error) {
      console.error(`  ❌ Error for ${email}:`, error.message);
      if (error.code === 'auth/user-not-found') {
        console.log(`  ℹ️  User doesn't exist. Create the user first in Firebase Console.`);
      }
    }
  }
}

setAdminClaims().then(() => {
  console.log('\n✅ Done! Admin claims have been set.');
  console.log('Note: Users may need to sign out and sign back in for claims to take effect.');
}).catch(err => {
  console.error('Fatal error:', err);
  process.exit(1);
});
