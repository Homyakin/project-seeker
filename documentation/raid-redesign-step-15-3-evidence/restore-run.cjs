const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const {spawn} = require('node:child_process');
const {Transform} = require('node:stream');
const {pipeline} = require('node:stream/promises');
const assert = require('node:assert/strict');
const [name, destination] = process.argv.slice(2);
assert(name && destination, 'Specify archived run name and a new output directory');
const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, 'manifest.json')));
const run = manifest.runs.find(r => r.name === name);
assert(run, 'Unknown archived run');
assert(!fs.existsSync(destination), 'Output directory must not exist');
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
(async () => {
  const file = path.join(__dirname, run.file);
  assert.equal(sha(file), run.archivedSha256);
  fs.mkdirSync(destination);
  const child = spawn('xz', ['-dc', file]);
  let stderr = ''; child.stderr.on('data', chunk => {stderr += chunk;});
  const completed = new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('close', code => code === 0 ? resolve() : reject(new Error(stderr || 'xz exited ' + code)));
  });
  const hash = crypto.createHash('sha256'); let bytes = 0;
  const inspect = new Transform({transform(chunk, encoding, callback) {
    hash.update(chunk); bytes += chunk.length; callback(null, chunk);
  }});
  const output = path.join(destination, 'battles.jsonl.gz');
  await Promise.all([completed, pipeline(child.stdout, inspect, zlib.createGzip(),
    fs.createWriteStream(output, {flags: 'wx'}))]);
  assert.equal(bytes, run.jsonlBytes);
  assert.equal(hash.digest('hex'), run.jsonlSha256);
  const summaryPath = path.join(__dirname, name + '-summary.json');
  const summary = JSON.parse(fs.readFileSync(summaryPath));
  const newHash = sha(output);
  const originalSummaryRawHash = summary.rawSha256 ?? null;
  if (originalSummaryRawHash !== null) {
    assert.equal(originalSummaryRawHash, run.originalGzipSha256);
    summary.rawSha256 = newHash;
  }
  // Only the gzip-container digest in this derived validation copy changes.
  fs.writeFileSync(path.join(destination, 'summary.json'), JSON.stringify(summary, null, 2) + '\n', {flag: 'wx'});
  for (const optional of ['manifest.json', 'inputs.json']) {
    const source = path.join(__dirname, name + '-' + optional);
    if (fs.existsSync(source)) fs.copyFileSync(source, path.join(destination, optional), fs.constants.COPYFILE_EXCL);
  }
  const receipt = {status: 'RESTORED_IDENTICAL_JSONL_WITH_NEW_GZIP_CONTAINER', name,
    originalSummarySha256: sha(summaryPath), originalGzipSha256: run.originalGzipSha256,
    archivedSha256: run.archivedSha256, jsonlSha256: run.jsonlSha256,
    jsonlBytes: bytes, restoredGzipSha256: newHash, changedSummaryField: originalSummaryRawHash === null ? null : 'rawSha256',
    note: 'Original archived summary is unchanged. Outcomes, inputs, pair scores and all JSONL bytes are unchanged.'};
  fs.writeFileSync(path.join(destination, 'restoration-receipt.json'), JSON.stringify(receipt, null, 2) + '\n', {flag: 'wx'});
  console.log(JSON.stringify(receipt));
})().catch(error => {console.error(error); process.exitCode = 1;});
