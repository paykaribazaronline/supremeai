const admin = require('firebase-admin');
const path = require('path');
const fs = require('fs');

const _KEY_PATHS = [
  process.env.GOOGLE_APPLICATION_CREDENTIALS,
  path.resolve(__dirname, '../../service-account.json'),
  path.resolve(__dirname, '../service-account.json'),
  path.resolve(__dirname, '../../src/main/resources/firebase-service-account.json'),
].filter(Boolean);

const _KEY_PATH = _KEY_PATHS.find(p => { try { fs.statSync(p); return true; } catch { return false; } });
if (_KEY_PATH && !process.env.GOOGLE_APPLICATION_CREDENTIALS) {
  process.env.GOOGLE_APPLICATION_CREDENTIALS = _KEY_PATH;
}

const projectId = process.env.FIRESTORE_PROJECT_ID || 'supremeai-a';
admin.initializeApp({ projectId });
const db = admin.firestore();

const targets = `- https://stackoverflow.com/search?q=%s (For programming errors and bugs)
- https://pub.dev/packages?q=%s (For Flutter and Dart packages)
- https://gemini.google.com/app?q=%s (For creative writing, songs, translations, and poems)
- https://kimi.moonshot.cn/chat?q=%s (For complex analysis, math, and deep research)
- https://huggingface.co/chat?q=%s (For free open-source AI models and coding help)
- https://github.com/search?q=%s&type=code (For open-source code examples and repositories)
- https://developer.mozilla.org/en-US/search?q=%s (For JavaScript, HTML, and CSS web documentation)
- https://en.wikipedia.org/w/index.php?search=%s (For general factual knowledge and history)
- https://html.duckduckgo.com/html/?q=%s (For latest news, current events, and general info)`;

async function update() {
    try {
        const docRef = db.collection('system_configs').doc('global_settings');
        const doc = await docRef.get();
        if (!doc.exists) {
            console.log("global_settings does not exist, creating new...");
            await docRef.set({ settings: { "agentic.web.targets": targets } }, { merge: true });
        } else {
            console.log("Updating existing global_settings...");
            await docRef.set({
                settings: { "agentic.web.targets": targets }
            }, { merge: true });
        }
        console.log("✅ Web targets updated successfully!");
        process.exit(0);
    } catch (e) {
        console.error(e);
        process.exit(1);
    }
}
update();
