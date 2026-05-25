const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const serviceAccountPath = path.join(__dirname, '..', 'service-account.json');
if (!fs.existsSync(serviceAccountPath)) {
  console.error('service-account.json not found in root directory');
  process.exit(1);
}

const serviceAccount = require(serviceAccountPath);

admin.initializeApp({
  credential: admin.credential.cert(serviceAccount)
});

const db = admin.firestore();

async function fixProviders() {
  console.log('Starting Firestore data cleanup for api_providers...');
  const providersRef = db.collection('api_providers');
  const snapshot = await providersRef.get();
  
  let count = 0;
  const dateFields = ['addedAt', 'lastTested', 'lastValidated', 'lastCheck', 'lastBenchmarkedAt', 'lastErrorDate', 'deadAt'];

  for (const doc of snapshot.docs) {
    const data = doc.data();
    const updates = {};
    let changed = false;

    dateFields.forEach(field => {
      // If it's a map (like chronology, dayOfMonth, etc.) and not a Timestamp
      if (data[field] && typeof data[field] === 'object' && data[field].chronology) {
        console.log(`Found corrupted field [${field}] in doc [${doc.id}]. Converting to null (will be fixed by @ServerTimestamp on next save).`);
        updates[field] = null; 
        changed = true;
      }
    });

    if (changed) {
      await doc.ref.update(updates);
      count++;
    }
  }

  console.log(`Cleanup complete. Updated ${count} documents.`);
}

fixProviders().catch(err => {
  console.error('Error during cleanup:', err);
  process.exit(1);
});
