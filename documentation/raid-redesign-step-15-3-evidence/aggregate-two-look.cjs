const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const root = process.argv[2];
assert(root);
const canonical = (value, key) => {
  if (Array.isArray(value)) return ['slots', 'availableOnSlots'].includes(key) ? [...value].sort() : value.map(v => canonical(v));
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k], k)]));
  return value;
};
const encoded = value => JSON.stringify(canonical(value));
const sha = value => crypto.createHash('sha256').update(encoded(value)).digest('hex');
const key = (cell, variant) => `${cell.tier}/N${cell.size}/L${cell.raidLevel}/${variant}`;
async function firstInputs(directory) {
  const rows = new Map();
  const lines = readline.createInterface({input: fs.createReadStream(path.join(directory, 'battles.jsonl.gz')).pipe(zlib.createGunzip()), crlfDelay: Infinity});
  for await (const line of lines) {
    if (!line.includes('"pair":0,') || !line.includes('"reverse":false')) continue;
    const row = JSON.parse(line);
    assert.equal(row.pair, 0); assert.equal(row.reverse, false);
    rows.set(key(row.baseCell, row.variant), {members: row.members, diagnostics: row.generationDiagnostics});
  }
  return rows;
}
(async () => {
  const initial = JSON.parse(fs.readFileSync(path.join(root, 'confirmation/summary.json')));
  const independent = JSON.parse(fs.readFileSync(path.join(root, 'independent-confirmation/summary.json')));
  const initialInputs = await firstInputs(path.join(root, 'confirmation'));
  const independentInputs = await firstInputs(path.join(root, 'independent-confirmation'));
  const pooled = [];
  for (const first of initial.comparisons) {
    const second = independent.comparisons.find(c => key(c.baseCell, c.variant) === key(first.baseCell, first.variant));
    assert(second, 'Missing independent confirmation condition');
    const initialBase = initial.cases.find(c => c.caseId === first.baseCaseId);
    const independentBase = independent.cases.find(c => c.caseId === second.baseCaseId);
    const initialVariant = initial.cases.find(c => c.caseId === first.variantCaseId);
    const independentVariant = independent.cases.find(c => c.caseId === second.variantCaseId);
    assert.equal(initial.pairs, 500); assert.equal(independent.pairs, 500);
    const firstStart = initial.generationRoot + (10000 + first.causalGroupId) * 1000;
    const secondStart = independent.generationRoot + (10000 + second.causalGroupId) * 1000;
    assert(Math.abs(firstStart - secondStart) > 500, 'Overlapping generation cohorts');
    assert(Math.abs((initial.battleRoot - initial.generationRoot) - (independent.battleRoot - independent.generationRoot)) === 0);
    for (const variant of ['BASE', first.variant]) {
      const a = initialInputs.get(key(first.baseCell, variant));
      const b = independentInputs.get(key(first.baseCell, variant));
      assert(a && b); assert.equal(encoded(a.members), encoded(b.members), 'Pooling changed inputs');
      assert.equal(encoded(a.diagnostics), encoded(b.diagnostics), 'Pooling changed generation parameters');
    }
    const deltas = [...first.pairedScoreDeltas, ...second.pairedScoreDeltas];
    assert.equal(deltas.length, 1000);
    const mean = deltas.reduce((a, b) => a + b, 0) / deltas.length;
    const singleLookHalf = Math.sqrt(2 * Math.log(40) / deltas.length);
    const half = Math.sqrt(2 * Math.log(2 * 2 / .05) / deltas.length);
    const low = Math.max(-1, mean - half), high = Math.min(1, mean + half);
    const wins = c => c.outcomes.WIN ?? 0;
    pooled.push({tier: first.baseCell.tier, size: first.baseCell.size, level: first.baseCell.raidLevel,
      variant: first.variant, pairs: 1000, fightsPerCondition: 2000,
      baseWins: wins(initialBase) + wins(independentBase), variantWins: wins(initialVariant) + wins(independentVariant),
      baseWinrate: (wins(initialBase) + wins(independentBase)) / 2000,
      variantWinrate: (wins(initialVariant) + wins(independentVariant)) / 2000,
      delta: mean, pointwise95Hoeffding: [low, high], halfWidth: half,
      ordinaryMajorHarmCriterionApplies: first.variant === 'NORMAL_APPEND',
      ordinary20ppHarmRuledOutPointwise: first.variant === 'NORMAL_APPEND' ? low > -0.2 : null,
      weakThresholdPolicy: first.variant === 'WEAK_APPEND' ? 'Separate repeated penalty; do not silently apply ordinary-participant acceptance policy' : null,
      prefixInputSha256: sha(initialInputs.get(key(first.baseCell, 'BASE')).members),
      variantInputSha256: sha(initialInputs.get(key(first.baseCell, first.variant)).members),
      cohorts: [{run: 'confirmation', groupId: first.causalGroupId, seedOffset: initial.seedOffset, generationStart: firstStart,
        generationEnd: firstStart + 499}, {run: 'independent-confirmation', groupId: second.causalGroupId,
        seedOffset: independent.seedOffset, generationStart: secondStart, generationEnd: secondStart + 499}],
      singleLook95Hoeffding: [Math.max(-1, mean - singleLookHalf), Math.min(1, mean + singleLookHalf)],
      singleLookHalfWidth: singleLookHalf, maxLooks: 2, perComparisonAlpha: .05, alphaPerLook: .025});
  }
  const broad = JSON.parse(fs.readFileSync(path.join(root, 'broad-validation.json')));
  const development = ['NORMAL_APPEND', 'WEAK_APPEND', 'SINGLE_ITEM_ENHANCE', 'SINGLE_MODIFIER_RARITY'].map(variant => {
    const rows = broad.comparisons.filter(c => c.variant === variant).sort((a, b) => a.delta - b.delta);
    return {variant, comparisons: rows.length, pairsPerCondition: 20, minimumPointDelta: rows[0].delta,
      maximumPointDelta: rows.at(-1).delta, pointDropsOver20pp: rows.filter(c => c.pointMajorHarm).length,
      worstCondition: rows[0], limitation: 'Short exploratory screen; absence of a large point drop is not a simultaneous guarantee'};
  });
  console.log(JSON.stringify({status: 'POOLED_DISJOINT_CONFIRMED_CAUSAL_INPUTS_NOT_UNIVERSAL_BALANCE_ACCEPTANCE',
    note: 'Second500-pair plan fixed after inspecting first500; final pooled confidence intervals explicitly allow at most two views. Selected follow-up conditions use separate new fixed500-pair cohorts. Not universal balance acceptance.',
    pooled, broadVariantSummary: development,
    poolingConfidencePolicy: 'At most two data views per comparison:500 then1000 pairs; Bonferroni alpha/2; not simultaneous across conditions or variants'}, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
