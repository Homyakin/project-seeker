const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const {spawn} = require('node:child_process');
const {Transform} = require('node:stream');
const {pipeline} = require('node:stream/promises');
const assert = require('node:assert/strict');
const {digest, readArchivedFile} = require('./archived-file.cjs');
const [name, destination] = process.argv.slice(2);
assert(name && destination, 'Specify archived run name and a new output root');
const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, 'manifest.json')));
const run = manifest.runs.find(r => r.name === name);
assert(run, 'Unknown archived run');
assert(!fs.existsSync(destination), 'Output directory must not exist');
(async () => {
  const file = path.join(__dirname, run.file);
  assert.equal(digest(fs.readFileSync(file)), run.archivedSha256);
  fs.mkdirSync(destination);
  const screen = path.join(destination, 'screen');
  fs.mkdirSync(screen);
  fs.mkdirSync(path.join(destination, 'preparation'));
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
  const output = path.join(screen, 'battles.jsonl.gz');
  await Promise.all([completed, pipeline(child.stdout, inspect, zlib.createGzip(),
    fs.createWriteStream(output, {flags: 'wx'}))]);
  assert.equal(bytes, run.jsonlBytes);
  assert.equal(hash.digest('hex'), run.jsonlSha256);
  const newHash = digest(fs.readFileSync(output));
  const restored = [];
  for (const artifact of run.artifacts) {
    const content = await readArchivedFile(__dirname, artifact.file);
    assert.equal(digest(content), artifact.sha256, artifact.file);
    const label = artifact.file.slice(name.length + 1);
    const target = label.startsWith('preparation-')
      ? path.join(destination, 'preparation', label.slice('preparation-'.length))
      : path.join(screen, label);
    // None of these summaries contains a top-level gzip digest. Preserve
    // metadata byte for byte; record the new container digest separately.
    if (label === 'summary.json') assert.equal(JSON.parse(content).rawSha256, undefined);
    fs.writeFileSync(target, content, {flag: 'wx'});
    restored.push({file: path.relative(destination, target), sha256: artifact.sha256});
  }
  const prefix = name + '-parent-';
  const metadataIndex = JSON.parse(fs.readFileSync(path.join(__dirname, 'compressed-metadata.json')));
  const available = [...new Set([...fs.readdirSync(__dirname).filter(value => !value.endsWith('.xz')),
    ...metadataIndex.map(value => value.file)])];
  const parents = available.filter(value => value.startsWith(prefix)).map(file => ({file, label: file.slice(prefix.length)}));
  if (name === 'b-confirmation') {
    for (const label of ['pilot-reference-inputs.json', 'wide-reference-inputs.json']) {
      parents.push({file: 'bnorm-confirmation-parent-' + label, label});
    }
  }
  for (const {file, label} of parents) {
    const content = await readArchivedFile(__dirname, file);
    const target = path.join(destination, label);
    fs.writeFileSync(target, content, {flag: 'wx'});
    restored.push({file: path.basename(target), sha256: digest(content)});
  }
  const receipt = {status: 'RESTORED_IDENTICAL_JSONL_AND_METADATA_WITH_NEW_GZIP_CONTAINER', name,
    originalGzipSha256: run.originalGzipSha256, restoredGzipSha256: newHash,
    archivedSha256: run.archivedSha256, jsonlSha256: run.jsonlSha256, jsonlBytes: bytes,
    restored, note: 'No outcome, input, summary or JSONL byte changed; only gzip container changed.'};
  fs.writeFileSync(path.join(destination, 'restoration-receipt.json'), JSON.stringify(receipt, null, 2) + '\n', {flag: 'wx'});
  console.log(JSON.stringify(receipt));
})().catch(error => {console.error(error); process.exitCode = 1;});
