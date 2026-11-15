#!/usr/bin/env node
// Read-only exact replay comparison; array ordering is preserved, including ItemObject.slots.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const originalDirectory = path.resolve(process.argv[2] || '');
const replayDirectory = path.resolve(process.argv[3] || '');
assert.equal(process.argv.length, 4, 'Expected ORIGINAL_RUN_DIR REPLAY_DIR');
const protocol = JSON.parse(fs.readFileSync(path.join(originalDirectory, 'protocol.json'), 'utf8'));
const selectedIds = protocol.replayCaseIds;
assert.deepEqual(selectedIds, [1,5,35,70,105,106,142,249,250,267,268,270,273,339,340,359]);
assert.deepEqual(protocol.replayPairIndices, [0]);
function canonical(value, location = '') {
  if (Array.isArray(value)) {
    return value.map((entry, index) => canonical(entry, `${location}/${index}`));
  }
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort()
    .map(key => [key, canonical(value[key], `${location}/${key}`)]));
  return value;
}
const rowKey = row => `${row.caseId}/${row.pair}/${row.reverse}`;
async function* rows(file) {
  const source = fs.createReadStream(file).pipe(zlib.createGunzip());
  for await (const line of readline.createInterface({input: source, crlfDelay: Infinity})) {
    assert.ok(line.length, 'No empty raw rows');
    yield JSON.parse(line);
  }
}
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
(async () => {
  const originals = new Map();
  let originalRowCount = 0;
  for await (const row of rows(path.join(originalDirectory, 'battles.jsonl.gz'))) {
    originalRowCount++;
    if (selectedIds.includes(row.caseId) && row.pair === 0) {
      assert.ok(!originals.has(rowKey(row)), 'No duplicate original replay keys');
      originals.set(rowKey(row), canonical(row));
    }
  }
  assert.equal(originalRowCount, 8620);
  assert.equal(originals.size, 32);
  const seen = new Set();
  for await (const row of rows(path.join(replayDirectory, 'battles.jsonl.gz'))) {
    const key = rowKey(row);
    assert.ok(originals.has(key), `Only frozen replay keys: ${key}`);
    assert.ok(!seen.has(key), `No duplicate replay keys: ${key}`);
    assert.deepEqual(canonical(row), originals.get(key), `Exact all-field replay at ${key}`);
    seen.add(key);
  }
  assert.equal(seen.size, 32);
  console.log(JSON.stringify({status: 'REPLAY_VALID', originalRows: originalRowCount, comparedRows: seen.size,
    comparedPairs: 16, protocolSha256: sha(path.join(originalDirectory, 'protocol.json')),
    originalRawSha256: sha(path.join(originalDirectory, 'battles.jsonl.gz')),
    replayRawSha256: sha(path.join(replayDirectory, 'battles.jsonl.gz')),
    allowedNormalization: 'JSON map keys only; every array, number, outcome, action/trace hash exact'}, null, 2));
})().catch(error => { console.error(error.message); process.exitCode = 1; });
