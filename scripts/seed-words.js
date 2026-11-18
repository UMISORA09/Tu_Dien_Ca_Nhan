const admin = require('firebase-admin');
const words = require('./seed-words.json');
const path = require('path');
const fs = require('fs');

// Path to service account key - can be provided via SERVICE_ACCOUNT_KEY env var or placed alongside
const serviceAccountPath = process.env.SERVICE_ACCOUNT_KEY || path.join(__dirname, 'serviceAccountKey.json');

if (!fs.existsSync(serviceAccountPath)) {
  console.error(`ERROR: Service account key not found at ${serviceAccountPath}`);
  console.error('Please download your Firebase Admin service account key from:');
  console.error('Firebase Console -> Project settings -> Service accounts -> Generate new private key');
  console.error(`Save it to: ${serviceAccountPath}`);
  process.exit(1);
}

admin.initializeApp({
  credential: admin.credential.cert(require(serviceAccountPath))
});

const db = admin.firestore();

(async () => {
  console.log(`Bắt đầu seed ${words.length} từ vựng vào Firestore...`);
  for (const w of words) {
    const ref = db.collection('words').doc(w.id);
    await ref.set({
      ...w,
      status: 'approved',
      createdBy: 'seed',
      createdAt: admin.firestore.FieldValue.serverTimestamp()
    }, { merge: true });
    console.log(`[OK] Đã nạp từ: ${w.id} (${w.word})`);
  }
  console.log(`Hoàn tất seed thành công: ${words.length} từ vựng vào collection 'words'.`);
  process.exit(0);
})();
