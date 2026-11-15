const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawn} = require('node:child_process');
const assert = require('node:assert/strict');
const directory = process.argv[2] || __dirname;
const manifest = JSON.parse(fs.readFileSync(path.join(directory, 'manifest.json')));
async function verify(run) {
  const file = path.join(directory, run.file);
  const compressed = crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
  assert.equal(compressed, run.archivedSha256, run.file);
  await new Promise((resolve, reject) => {
    const child = spawn('xz', ['-dc', file]);
    const hash = crypto.createHash('sha256'); let bytes = 0;
    child.stdout.on('data', chunk => {hash.update(chunk); bytes += chunk.length;});
    let stderr = ''; child.stderr.on('data', chunk => {stderr += chunk;});
    child.on('error', reject);
    child.on('close', code => {
      try {
        assert.equal(code, 0, stderr);
        assert.equal(bytes, run.jsonlBytes, run.file);
        assert.equal(hash.digest('hex'), run.jsonlSha256, run.file);
        resolve();
      } catch (error) {reject(error);}
    });
  });
}
(async () => {
  for (const run of manifest.runs) await verify(run);
  console.log(JSON.stringify({status: 'ALL_RAW_CONTENT_HASHES_VALID', runs: manifest.runs.length,
    battles: manifest.totalBattles, note: 'Integrity only; this is not balance acceptance'}));
})().catch(error => {console.error(error); process.exitCode = 1;});
