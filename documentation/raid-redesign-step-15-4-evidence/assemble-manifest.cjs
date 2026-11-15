const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const names = ['q-p-sum-screen', 'b-screen', 'b-wide-screen', 'b-confirmation',
  'bnorm-wide-screen', 'bnorm-confirmation'];
const runs = names.map(name => JSON.parse(fs.readFileSync(path.join(__dirname, name + '-pack-receipt.json'))));
const totalBattles = runs.reduce((sum, run) => sum + run.battles, 0);
assert.equal(totalBattles, 60080);
const manifest = {step: '15.4', date: '2026-11-08', status: 'NO_USABLE_DESIGN_SELECTED', totalBattles,
  sourceBasis: {reference: '../raid-redesign-step-15-2-evidence/inputs.sha256', selectedFiles: 203,
    snapshot: '../raid-redesign-step-15-2-evidence/sources.tar.gz',
    snapshotSha256: '88cb5b11036788a0f211f417d2f2cde81c2268aed373afa31c447686c38cf842'},
  metadataIndex: 'compressed-metadata.json', runs,
  limits: ['No production or test-source changes in15.4', 'No usable replacement formula selected',
    '60080 executions are not one independent sample; mirrors and shared seeds are correlated',
    'Each confirmation has its own predeclared nine-comparison family, not joint coverage across studies',
    'No new Maven build or acceptance claim', 'Lossless xz replaces only containers, never raw content']};
fs.writeFileSync(path.join(__dirname, 'manifest.json'), JSON.stringify(manifest, null, 2) + '\n', {flag: 'wx'});
console.log(JSON.stringify({status: manifest.status, runs: runs.length, totalBattles}));
