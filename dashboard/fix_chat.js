const fs = require('fs');
const content = fs.readFileSync('src/components/ChatWithAI.tsx', 'utf8');
const lines = content.split('\n');
for (let i = 580; i < 665; i++) {
  console.log(`${i+1}: ${lines[i]}`);
}
