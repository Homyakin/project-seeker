// Standalone validation of exploratory scaling runs, not a balance acceptance test.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');

const directory = process.argv[2];
assert(directory, 'Specify an evidence run directory');
const summary = JSON.parse(fs.readFileSync(path.join(directory, 'summary.json')));
const manifest = JSON.parse(fs.readFileSync(path.join(directory, 'manifest.json')));
assert.deepEqual(summary.manifest, manifest, 'Manifest and embedded summary metadata differ');
const cases = new Map(summary.cases.map(c => [c.caseId, c]));
assert.equal(cases.size, summary.cases.length, 'Duplicate summary case');
const seen = new Map();
const rawTotals = new Map();
const close = (a, b, message) => {
  assert(Number.isFinite(a) && Number.isFinite(b), message + ': nonfinite');
  assert(Math.abs(a - b) <= 1e-9 * Math.max(1, Math.abs(a), Math.abs(b)),
    `${message}: ${a} != ${b}`);
};
function partsQ(parts) {
  let health = 0;
  let damage = 0;
  for (const p of parts) {
    assert(p.hp > 0 && p.mu > 0 && p.mu <= 1);
    close(p.ehp, p.hp / p.mu, 'EHP');
    const attack = Math.max(0, ...p.attacks.map(r =>
      Object.values(r.attack).reduce((a, b) => a + b, 0)));
    assert.equal(p.maxAttack, attack);
    assert(p.frequency >= 0 && p.frequency <= 1 && p.critFactor >= 0);
    close(p.offense, attack * p.critFactor * p.frequency, 'DPS');
    assert(p.skills.every(s => s.formulaVersion === 'SCALING_SKILLS_V3'));
    health += p.ehp;
    damage += p.offense;
  }
  return health * damage;
}

(async () => {
  let rows = 0;
  const lines = readline.createInterface({input: fs.createReadStream(
    path.join(directory, 'battles.jsonl.gz')).pipe(zlib.createGunzip()), crlfDelay: Infinity});
  for await (const line of lines) {
    assert(line.length, 'Empty raw line');
    const r = JSON.parse(line);
    const c = cases.get(r.caseId);
    assert(c, 'Unknown case');
    for (const key of ['tier', 'size', 'level']) assert.equal(r[key], c[key], key);
    assert.equal(r.style, c.style);
    assert.equal(r.model ?? r.mode, c.model ?? c.mode);
    assert(Number.isInteger(r.pair) && r.pair >= 0 && r.pair < c.pairs);
    assert.equal(typeof r.reverse, 'boolean');
    const key = `${r.caseId}/${r.pair}/${r.reverse}`;
    assert(!seen.has(key), 'Duplicate row');
    seen.set(key, r.outcome);
    const seedKey = (r.tier === 'MID' ? 0 : 100000) + r.level * 1000 + r.pair;
    assert.equal(r.generationSeed, manifest.generationRoot + seedKey);
    assert.equal(r.battleSeed, manifest.battleRoot + seedKey);
    assert(r.calibration.multiplier >= 0 && Number.isFinite(r.calibration.multiplier));
    assert(Number.isInteger(r.rounds) && r.rounds > 0 && r.rounds <= 2000);
    assert(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(r.outcome));
    close(partsQ(r.enemyParts), r.npcQ, 'NPC Q');
    assert(r.enemyParts.every(p => p.skills.length === 1 && p.skills[0].points === 4), 'NPC skill points');
    if (r.playerParts) {
      assert.equal(r.playerParts.length, r.size);
      close(partsQ(r.playerParts), r.playerQ, 'Player Q');
      if (r.style === 'WEAK_ONLY' || r.style === 'STRONG_PLUS_WEAK') {
        const weakStart = r.style === 'STRONG_PLUS_WEAK' ? 1 : 0;
        assert(r.playerParts.slice(weakStart).every(p => p.hp === 840), 'Weak participant health');
      }
    }
    if (r.model !== 'OLD') {
      const correction = manifest.correction ?? manifest.C;
      const countForGroupBonus = Math.max(2, r.size);
      const attenuation = manifest.a !== undefined
        ? manifest.a + (1 - manifest.a) * Math.pow(3 / countForGroupBonus, manifest.p)
        : manifest.groupSizeExponent === undefined ? 1
          : Math.pow(3 / countForGroupBonus, manifest.groupSizeExponent);
      const target = r.playerQ * (0.54 + 0.06 * r.level)
        * (1 + 0.05 * Math.log(countForGroupBonus) / Math.log(4)) * correction * attenuation;
      close(r.calibration.targetPower, target, 'Target');
      close(r.calibration.achievedPower, r.npcQ, 'Achieved');
    }
    if (r.front !== undefined) {
      assert.equal(r.front + r.back, r.count);
      assert(r.front > 0 && r.back > 0);
      assert.equal(r.enemyParts.length, r.count);
    }
    const totals = rawTotals.get(r.caseId) ?? {WIN: 0, LOSS: 0, DRAW: 0, TIMEOUT: 0, rounds: 0};
    totals[r.outcome]++;
    if (r.outcome !== 'TIMEOUT') totals.rounds += r.rounds;
    rawTotals.set(r.caseId, totals);
    rows++;
  }
  for (const c of cases.values()) {
    const totals = rawTotals.get(c.caseId);
    assert(totals);
    assert.equal(totals.WIN, c.wins);
    assert.equal(totals.LOSS, c.losses);
    assert.equal(totals.DRAW, c.draws);
    assert.equal(totals.TIMEOUT, c.timeouts);
    assert.equal(totals.WIN + totals.LOSS + totals.DRAW + totals.TIMEOUT, 2 * c.pairs);
    const scores = [];
    for (let pair = 0; pair < c.pairs; pair++) {
      const outcomes = [false, true].map(reverse => seen.get(`${c.caseId}/${pair}/${reverse}`));
      assert(outcomes.every(Boolean), 'Missing mirrored row');
      if (!outcomes.includes('TIMEOUT')) scores.push(outcomes.reduce((a, b) =>
        a + (b === 'WIN' ? 0.5 : b === 'DRAW' ? 0.25 : 0), 0));
    }
    assert.deepEqual(scores, c.pairScores);
    close(c.winrate, totals.WIN / (2 * c.pairs), 'Win rate');
    close(c.pairMean, scores.reduce((a, b) => a + b, 0) / scores.length, 'Pair mean');
    close(c.meanRounds, totals.rounds / (totals.WIN + totals.LOSS + totals.DRAW), 'Rounds');
    const half = Math.sqrt(Math.log(40) / (2 * scores.length));
    close(c.pairHoeffding95[0], Math.max(0, c.pairMean - half), 'Lower bound');
    close(c.pairHoeffding95[1], Math.min(1, c.pairMean + half), 'Upper bound');
  }
  assert.equal(rows, summary.cases.reduce((n, c) => n + c.pairs * 2, 0));
  const differences = [];
  for (const base of cases.values()) {
    if (base.size !== 3) continue;
    const added = summary.cases.find(c => c.size === 4 && c.tier === base.tier
      && c.level === base.level && c.style === base.style
      && (c.model ?? c.mode) === (base.model ?? base.mode));
    if (!added || added.pairs !== base.pairs || base.timeouts || added.timeouts) continue;
    const mean = added.pairMean - base.pairMean;
    const half = Math.sqrt(2 * Math.log(40) / base.pairs);
    differences.push({tier: base.tier, level: base.level, model: base.model ?? base.mode,
      pairs: base.pairs, baseWins: base.wins, addedWins: added.wins,
      delta: mean, pointwiseHoeffding95: [Math.max(-1, mean - half), Math.min(1, mean + half)]});
  }
  const report = JSON.stringify({status: 'CONSISTENT_EXPLORATORY_DATA_NOT_ACCEPTANCE',
    cases: cases.size, rows, timeouts: summary.cases.reduce((n, c) => n + c.timeouts, 0),
    differences}, null, 2);
  if (process.argv[3]) fs.writeFileSync(process.argv[3], report + '\n', {flag: 'wx'});
  console.log(report);
})().catch(error => { console.error(error); process.exitCode = 1; });
