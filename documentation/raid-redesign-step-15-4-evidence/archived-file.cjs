const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawn} = require('node:child_process');
const assert = require('node:assert/strict');
const digest = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
async function readArchivedFile(directory, name) {
  assert.equal(path.basename(name), name, 'Only package-local files are supported');
  const plain = path.join(directory, name);
  if (fs.existsSync(plain)) return fs.readFileSync(plain);
  const index = JSON.parse(fs.readFileSync(path.join(directory, 'compressed-metadata.json')));
  const entry = index.find(value => value.file === name);
  assert(entry, 'Missing archived metadata: ' + name);
  const compressed = path.join(directory, entry.archiveFile);
  assert.equal(digest(fs.readFileSync(compressed)), entry.archiveSha256);
  return new Promise((resolve, reject) => {
    const child = spawn('xz', ['-dc', compressed]);
    const chunks = []; let stderr = '';
    child.stdout.on('data', value => chunks.push(value));
    child.stderr.on('data', value => {stderr += value;});
    child.on('error', reject);
    child.on('close', code => {
      try {
        assert.equal(code, 0, stderr);
        const bytes = Buffer.concat(chunks);
        assert.equal(bytes.length, entry.rawBytes);
        assert.equal(digest(bytes), entry.rawSha256);
        resolve(bytes);
      } catch (error) {reject(error);}
    });
  });
}
module.exports = {digest, readArchivedFile};
