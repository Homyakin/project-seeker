const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const root = path.resolve(__dirname, '../..');
const read = name => JSON.parse(fs.readFileSync(path.join(__dirname, name)));
const close = (a, b) => assert(Math.abs(a - b) <= 1e-12 * Math.max(1, Math.abs(a), Math.abs(b)), `${a} != ${b}`);
const manifest = read('manifest.json');
let battles = 0, draws = 0, timeouts = 0;
for (const run of manifest.runs) {
  const summary = read(run.name + '-summary.json');
  let count = 0;
  for (const c of summary.cases) {
    const outcomes = c.outcomes || {WIN: c.wins, LOSS: c.losses, DRAW: c.draws, TIMEOUT: c.timeouts};
    for (const n of Object.values(outcomes)) assert(Number.isInteger(n) && n >= 0);
    count += Object.values(outcomes).reduce((a, b) => a + b, 0);
    draws += outcomes.DRAW || 0; timeouts += outcomes.TIMEOUT || 0;
  }
  assert.equal(count, run.battles); battles += count;
}
assert.equal(manifest.runs.length, 9); assert.equal(battles, 91200);
assert.equal(manifest.totalBattles, battles); assert.equal(draws + timeouts, 0);
const aggregate = read('causal-aggregate.json');
const single = read('causal-aggregate-single-look.json');
for (const row of aggregate.pooled) {
  const original = single.pooled.find(r => r.tier === row.tier && r.level === row.level && r.variant === row.variant);
  assert(original); assert.equal(row.pairs, 1000); assert.equal(row.maxLooks, 2);
  close(row.delta, original.delta); close(row.delta, row.variantWinrate - row.baseWinrate);
  close(row.halfWidth, Math.sqrt(2 * Math.log(80) / row.pairs));
  close(row.pointwise95Hoeffding[0], row.delta - row.halfWidth);
  close(row.pointwise95Hoeffding[1], row.delta + row.halfWidth);
  assert.deepEqual(row.singleLook95Hoeffding, original.pointwise95Hoeffding);
  if (row.variant === 'NORMAL_APPEND') assert(row.pointwise95Hoeffding[0] > -.2);
}
const weak = read('fillers-mechanics-summary.json');
assert.equal(weak.results.length, 3);
for (const row of weak.results) {
  const name = {MAIN: 'main', PIERCE_ARCANE: 'pierce', RITUAL: 'ritual'}[row.style];
  const summary = read(`fillers-${name}-confirmation-summary.json`);
  const base = summary.cases.find(c => c.size === 1), added = summary.cases.find(c => c.size === 2);
  assert.equal(base.pairs, 500); assert.equal(added.pairs, 500);
  close(row.difference, added.winrate - base.winrate);
  const half = Math.sqrt(2 * Math.log(120) / 500);
  close(row.simultaneousThreeConfirmations95BonferroniHoeffding.halfWidth, half);
  close(row.simultaneousThreeConfirmations95BonferroniHoeffding.interval[1], row.difference + half);
  assert(row.difference + half < -.2);
}
const build = read('test-summary.json');
assert.deepEqual(build.totals, {tests: 1733, failures: 0, errors: 0, skipped: 29, passed: 1704});
assert.equal(build.checkstyleViolations, 0);
const sourceLines = fs.readFileSync(path.join(__dirname, '../raid-redesign-step-15-2-evidence/inputs.sha256'), 'utf8').trim().split('\n');
for (const line of sourceLines) {
  const match = line.match(/^([a-f0-9]{64})  (.+)$/); assert(match);
  const hash = crypto.createHash('sha256').update(fs.readFileSync(path.join(root, match[2]))).digest('hex');
  assert.equal(hash, match[1], match[2]);
}
assert.equal(sourceLines.length, 203);
let recomputedAggregateExact = null;
if (process.argv[2]) {
  assert.deepEqual(JSON.parse(fs.readFileSync(process.argv[2])), aggregate);
  recomputedAggregateExact = true;
}
console.log(JSON.stringify({status: 'SUMMARY_AND_SOURCE_CHECKS_VALID_BALANCE_NOT_ACCEPTED', runs: 9, battles,
  draws, timeouts, sourceFilesUnchanged: sourceLines.length, repeatedCleanBuild: build.totals,
  twoLookIntervalsVerified: aggregate.pooled.length, fixedWeakConfirmationsVerified: weak.results.length,
  recomputedAggregateExact, rawContentIntegrityIsSeparate: 'verify-raw.cjs',
  productionOrParametersChangedThisPart: false}, null, 2));
