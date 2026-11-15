const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const {spawn} = require('node:child_process');
const {Transform} = require('node:stream');
const {pipeline} = require('node:stream/promises');
const assert = require('node:assert/strict');
const [name, source] = process.argv.slice(2);
assert(name && /^[a-z0-9-]+$/.test(name) && source, 'Specify run name and completed study directory');
const fileName = name + '-battles.jsonl.xz';
const target = path.join(__dirname, fileName);
assert(!fs.existsSync(target), 'Archive already exists');
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
(async () => {
  const original = path.join(source, 'battles.jsonl.gz');
  const child = spawn('xz', ['-T1', '-3', '-c']);
  let stderr = ''; child.stderr.on('data', chunk => {stderr += chunk;});
  const completed = new Promise((resolve, reject) => {
    child.on('error', reject);
    child.on('close', code => code === 0 ? resolve() : reject(new Error(stderr || 'xz exited ' + code)));
  });
  const hash = crypto.createHash('sha256'); let bytes = 0, rows = 0;
  const inspect = new Transform({transform(chunk, encoding, callback) {
    hash.update(chunk); bytes += chunk.length;
    for (const value of chunk) if (value === 10) ++rows;
    callback(null, chunk);
  }});
  await Promise.all([completed,
    pipeline(fs.createReadStream(original), zlib.createGunzip(), inspect, child.stdin),
    pipeline(child.stdout, fs.createWriteStream(target, {flags: 'wx'}))]);
  const artifacts = [];
  const copy = (originalFile, label) => {
    if (!fs.existsSync(originalFile)) return;
    const destination = path.join(__dirname, name + '-' + label);
    fs.copyFileSync(originalFile, destination, fs.constants.COPYFILE_EXCL);
    artifacts.push({file: path.basename(destination), sha256: sha(destination)});
  };
  for (const file of ['manifest.json', 'summary.json', 'inputs.json', 'seeds.json', 'analysis.json', 'validation.json', 'Step154FormulaProbe.java'])
    copy(path.join(source, file), file);
  for (const file of ['manifest.json', 'inputs.json', 'seeds.json'])
    copy(path.join(source, '../preparation', file), 'preparation-' + file);
  const receipt = {name, file: fileName, battles: rows, originalGzipSha256: sha(original),
    archivedSha256: sha(target), archivedBytes: fs.statSync(target).size,
    jsonlSha256: hash.digest('hex'), jsonlBytes: bytes, artifacts,
    rawPolicy: 'Every original JSONL byte retained; only gzip container replaced with xz'};
  fs.writeFileSync(path.join(__dirname, name + '-pack-receipt.json'), JSON.stringify(receipt, null, 2) + '\n', {flag: 'wx'});
  console.log(JSON.stringify(receipt));
})().catch(error => {console.error(error); process.exitCode = 1;});
