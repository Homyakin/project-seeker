const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const {spawn} = require('node:child_process');
const {pipeline} = require('node:stream/promises');
const assert = require('node:assert/strict');
const backup = process.argv[2];
assert(backup && path.isAbsolute(backup), 'Specify a new or existing task-owned metadata backup directory');
fs.mkdirSync(backup, {recursive: true});
const indexPath = path.join(__dirname, 'compressed-metadata.json');
const index = fs.existsSync(indexPath) ? JSON.parse(fs.readFileSync(indexPath)) : [];
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
(async () => {
  const receipts = fs.readdirSync(__dirname).filter(f => f.endsWith('-pack-receipt.json'))
    .map(file => JSON.parse(fs.readFileSync(path.join(__dirname, file))));
  receipts.push({artifacts: ['pilot-reference-inputs.json', 'wide-reference-inputs.json']
    .map(label => 'bnorm-confirmation-parent-' + label).filter(file => fs.existsSync(path.join(__dirname, file)))
    .map(file => ({file, sha256: sha(path.join(__dirname, file))}))});
  for (const receipt of receipts) {
    for (const artifact of receipt.artifacts) {
      assert.equal(path.basename(artifact.file), artifact.file);
      const file = path.join(__dirname, artifact.file);
      if (!artifact.file.endsWith('.json') || !fs.existsSync(file) || fs.statSync(file).size <= 1024 * 1024) continue;
      assert.equal(sha(file), artifact.sha256);
      const archived = file + '.xz';
      if (!fs.existsSync(archived)) {
        const child = spawn('xz', ['-T1', '-3', '-c', file]);
        const completed = new Promise((resolve, reject) => {
          child.on('error', reject); child.on('close', code => code === 0 ? resolve() : reject(new Error('xz exited ' + code)));
        });
        await Promise.all([completed, pipeline(child.stdout, fs.createWriteStream(archived, {flags: 'wx'}))]);
      }
      const verification = spawn('xz', ['-dc', archived]);
      const hash = crypto.createHash('sha256'); verification.stdout.on('data', data => {hash.update(data);});
      await new Promise((resolve, reject) => {
        verification.on('error', reject); verification.on('close', code => {
          try {assert.equal(code, 0); assert.equal(hash.digest('hex'), artifact.sha256); resolve();} catch(error) {reject(error);}
        });
      });
      const preserved = path.join(backup, artifact.file);
      if (!fs.existsSync(preserved)) fs.copyFileSync(file, preserved, fs.constants.COPYFILE_EXCL);
      assert.equal(sha(preserved), artifact.sha256);
      if (!index.some(row => row.file === artifact.file)) index.push({file: artifact.file,
        archiveFile: artifact.file + '.xz', rawSha256: artifact.sha256, rawBytes: fs.statSync(file).size,
        archiveSha256: sha(archived), archivedBytes: fs.statSync(archived).size});
      fs.writeFileSync(indexPath, JSON.stringify(index, null, 2) + '\n');
      // Only this task's generated expanded metadata, already retained twice, is removed.
      fs.unlinkSync(file);
    }
  }
  fs.writeFileSync(indexPath, JSON.stringify(index, null, 2) + '\n');
  console.log(JSON.stringify({status: 'METADATA_LOSSLESSLY_COMPRESSED_ORIGINALS_RETAINED', files: index.length,
    rawBytes: index.reduce((n, x) => n + x.rawBytes, 0), archivedBytes: index.reduce((n, x) => n + x.archivedBytes, 0)}));
})().catch(error => {console.error(error); process.exitCode = 1;});
