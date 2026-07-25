const fs = require('fs');
const path = require('path');

function log(...args) { console.log(...args); }
function err(...args) { console.error(...args); }

const repoRoot = path.resolve(__dirname, '..', '..');
const targetDir = path.join(repoRoot, 'target');
const destDir = path.resolve(__dirname, '..', 'spring-boot-app');

if (!fs.existsSync(targetDir)) {
  err('Target directory not found:', targetDir);
  process.exit(1);
}

const files = fs.readdirSync(targetDir).filter(f => f.endsWith('.jar') || f.endsWith('.jar.original'));
if (files.length === 0) {
  err('No JAR files found in', targetDir);
  process.exit(1);
}

// Prefer a normal .jar over .jar.original
let chosen = files.find(f => f.endsWith('.jar') && !f.endsWith('.jar.original')) || files[0];
let srcPath = path.join(targetDir, chosen);
let destName = chosen.endsWith('.original') ? chosen.replace(/\.original$/, '') : chosen;
// Ensure destName ends with .jar
if (!destName.endsWith('.jar')) destName = destName + '.jar';

if (!fs.existsSync(destDir)) fs.mkdirSync(destDir, { recursive: true });
const destPath = path.join(destDir, destName);

try {
  fs.copyFileSync(srcPath, destPath);
  log('Copied', srcPath, '->', destPath);
  process.exit(0);
} catch (e) {
  err('Failed to copy JAR:', e.message);
  process.exit(1);
}
