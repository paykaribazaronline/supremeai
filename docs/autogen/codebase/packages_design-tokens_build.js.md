# 📄 ফাইল: packages/design-tokens/build.js

**প্রকার:** .js  
**সাইজ:** 3,569 বাইট  
**আপডেট:** 2026-07-11T09:05:57.833425

---

## কোড

```js
const fs = require('fs');
const path = require('path');

const rawData = fs.readFileSync(path.join(__dirname, 'design-tokens.json'), 'utf-8');
const tokens = JSON.parse(rawData);

// Flatten function
function flattenTokens(obj, prefix = '') {
    let result = {};
    for (const key in obj) {
        if (obj[key] && typeof obj[key] === 'object' && !obj[key].type) {
            const newPrefix = prefix ? `${prefix}-${key}` : key;
            const nested = flattenTokens(obj[key], newPrefix);
            result = { ...result, ...nested };
        } else if (obj[key] && obj[key].type) {
            const newPrefix = prefix ? `${prefix}-${key}` : key;
            result[newPrefix] = obj[key];
        }
    }
    return result;
}

const flatTokens = flattenTokens(tokens);

// Generate tokens.css
let cssOutput = ':root {\n';
for (const key in flatTokens) {
    cssOutput += `  --supremeai-${key}: ${flatTokens[key].value};\n`;
}
cssOutput += '}\n';

// Generate tokens-vscode.css
let vscodeOutput = ':root {\n';
for (const key in flatTokens) {
    vscodeOutput += `  --supremeai-${key}: ${flatTokens[key].value};\n`;
}
vscodeOutput += '}\n';

// Generate tokens.dart
let dartOutput = "import 'package:flutter/material.dart';\n\nclass DesignTokens {\n";
for (const key in flatTokens) {
    const token = flatTokens[key];
    const camelCaseKey = key.replace(/-([a-z])/g, (g) => g[1].toUpperCase());
    
    if (token.type === 'color') {
        let val = token.value;
        if (val.startsWith('#')) {
            val = '0xFF' + val.replace('#', '');
        } else if (val.startsWith('rgba')) {
            const matches = val.match(/rgba\((\d+),\s*(\d+),\s*(\d+),\s*([\d.]+)\)/);
            if (matches) {
                const a = Math.round(parseFloat(matches[4]) * 255).toString(16).padStart(2, '0');
                const r = parseInt(matches[1], 10).toString(16).padStart(2, '0');
                const g = parseInt(matches[2], 10).toString(16).padStart(2, '0');
                const b = parseInt(matches[3], 10).toString(16).padStart(2, '0');
                val = `0x${a}${r}${g}${b}`;
            }
        }
        dartOutput += `  static const Color ${camelCaseKey} = Color(${val});\n`;
    } else if (token.type === 'fontSizes' || token.type === 'spacing' || token.type === 'borderRadius') {
        dartOutput += `  static const double ${camelCaseKey} = ${parseFloat(token.value)};\n`;
    } else if (token.type === 'fontWeights') {
        dartOutput += `  static const FontWeight ${camelCaseKey} = FontWeight.w${token.value};\n`;
    } else if (token.type === 'time') {
        const ms = parseInt(token.value.replace('ms', ''));
        dartOutput += `  static const Duration ${camelCaseKey} = Duration(milliseconds: ${ms});\n`;
    } else if (token.type === 'other' && token.value.startsWith('cubic-bezier')) {
        const matches = token.value.match(/cubic-bezier\(([\d.-]+),\s*([\d.-]+),\s*([\d.-]+),\s*([\d.-]+)\)/);
        if (matches) {
            dartOutput += `  static const Curve ${camelCaseKey} = Cubic(${matches[1]}, ${matches[2]}, ${matches[3]}, ${matches[4]});\n`;
        }
    }
}
dartOutput += '}\n';

// Ensure outputs dir exists
const outDir = path.join(__dirname, 'outputs');
if (!fs.existsSync(outDir)) {
    fs.mkdirSync(outDir);
}

fs.writeFileSync(path.join(outDir, 'tokens.css'), cssOutput);
fs.writeFileSync(path.join(outDir, 'tokens-vscode.css'), vscodeOutput);
fs.writeFileSync(path.join(outDir, 'tokens.dart'), dartOutput);

console.log('✅ Tokens successfully built without Style Dictionary dependencies!');

```