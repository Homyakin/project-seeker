const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const directory = process.argv[2];
assert(directory, 'Specify completed causal run directory');
const summary = JSON.parse(fs.readFileSync(path.join(directory, 'summary.json')));
const canonical = (value, key) => {
  if (Array.isArray(value)) return ['slots', 'availableOnSlots'].includes(key)
    ? [...value].sort() : value.map(v => canonical(v));
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort()
    .map(k => [k, canonical(value[k], k)]));
  return value;
};
const encode = value => JSON.stringify(canonical(value));
const same = (a, b, message) => assert.equal(encode(a), encode(b), message);
const close = (a, b, message) => assert(Math.abs(a - b) <= 1e-10 * Math.max(1, Math.abs(a), Math.abs(b)), message);
const slots = ['BODY', 'GLOVES', 'HELMET', 'MAIN_HAND', 'OFF_HAND', 'PANTS', 'SHOES'];
const cases = new Map(summary.cases.map(c => [c.caseId, c]));
const baseInputs = new Map();
const baseBasis = new Map();
const seen = new Map();
const outcomes = new Map();
const changes = {prefixRowsChecked: 0, weakLiveRowsChecked: 0, enhanceExactlyOneRowsChecked: 0, rarityExactlyOneRowsChecked: 0};
const weakItemCodes = new Set();
const score = outcome => outcome === 'WIN' ? 0.5 : outcome === 'DRAW' ? 0.25 : 0;
const q = people => {
  const health = [], offense = [];
  for (const p of people) {
    assert(p.health > 0, 'Participant starts dead');
    const mu = ['SLASH', 'BLUNT', 'PIERCE', 'MAGICAL'].reduce((n, t) => n + p.damageTakenMultipliers[t], 0) / 4;
    assert(mu > 0 && mu <= 1);
    const attack = Math.max(0, ...p.attacksByRange.map(r => Object.values(r.attack).reduce((a, b) => a + b, 0)));
    health.push(p.health / mu);
    offense.push(attack * (1 + Math.max(0, Math.min(1, p.critChance / 100)) * (p.critMultiplier - 1))
      * Math.min(p.initiative, 1000) / 1000);
    assert(p.skills.every(s => s.formulaVersion === 'SCALING_SKILLS_V3'));
  }
  return health.sort((a, b) => a - b).reduce((a, b) => a + b, 0)
    * offense.sort((a, b) => a - b).reduce((a, b) => a + b, 0);
};

(async () => {
  const compressedHash = crypto.createHash('sha256');
  const source = fs.createReadStream(path.join(directory, 'battles.jsonl.gz'));
  source.on('data', chunk => compressedHash.update(chunk));
  const lines = readline.createInterface({input: source.pipe(zlib.createGunzip()), crlfDelay: Infinity});
  let rows = 0;
  for await (const line of lines) {
    const row = JSON.parse(line);
    const c = cases.get(row.caseId);
    assert(c, 'Unknown case');
    for (const key of ['causalGroupId', 'variant']) assert.equal(row[key], c[key]);
    same(row.baseCell, c.baseCell, 'Base condition mismatch');
    same(row.cell, c.cell, 'Variant condition mismatch');
    assert(row.pair >= 0 && row.pair < summary.pairs);
    const key = `${row.caseId}/${row.pair}/${row.reverse}`;
    assert(!seen.has(key), 'Duplicate row'); seen.set(key, row.outcome);
    const seed = (10000 + row.causalGroupId) * 1000 + row.pair;
    assert.equal(row.generationSeed, summary.generationRoot + seed);
    assert.equal(row.battleSeed, summary.battleRoot + seed);
    assert.equal(row.members.length, row.cell.size);
    assert(row.playerBasis.every(p => p.health > 0), 'Dead prepared player');
    const n = row.baseCell.size;
    const basisKey = `${row.causalGroupId}/${row.pair}/${row.reverse}`;
    if (row.variant === 'BASE') {
      if (baseInputs.has(row.causalGroupId)) same(baseInputs.get(row.causalGroupId), row.members, 'Base gear varies with seed');
      else baseInputs.set(row.causalGroupId, row.members);
      baseBasis.set(basisKey, row.playerBasis);
    } else {
      const old = baseInputs.get(row.causalGroupId);
      assert(old && old.length === n, 'Missing base inputs');
      if (['NORMAL_APPEND', 'WEAK_APPEND'].includes(row.variant)) {
        assert.equal(row.members.length, n + 1);
        same(old, row.members.slice(0, n), 'Appending changed existing gear/position/tactic');
        same(baseBasis.get(basisKey), row.playerBasis.slice(0, n), 'Appending changed existing prepared player basis');
        changes.prefixRowsChecked++;
        if (row.variant === 'WEAK_APPEND') {
          const weak = row.members[n];
          assert.equal(weak.build, 'WEAK_WITH_DEFAULTS');
          assert.equal(row.playerBasis[n].health, 840);
          assert.equal(weak.sourceItems.length, 7);
          same(weak.sourceItems, weak.combatItems, 'Raid stripped weak gear');
          assert.equal(weak.sourceItems.filter(i => i.object.code === 'sword').length, 1);
          const occupied = weak.sourceItems.flatMap(i => i.object.slots);
          assert.equal(occupied.length, 7);
          same([...new Set(occupied)].sort(), slots, 'Weak slots incomplete or overlapping');
          for (const item of weak.sourceItems) {
            assert.equal(item.enhanceLevel, 0); assert.equal(item.rarity, 'COMMON'); assert.equal(item.modifier, null);
            weakItemCodes.add(item.object.code);
          }
          changes.weakLiveRowsChecked++;
        }
      } else {
        assert.equal(row.members.length, n);
        let itemChanges = 0;
        for (let member = 0; member < n; member++) {
          for (const field of ['build', 'position', 'tactic']) same(old[member][field], row.members[member][field], 'Development changed member setup');
          assert.equal(old[member].sourceItems.length, row.members[member].sourceItems.length);
          for (let item = 0; item < old[member].sourceItems.length; item++) {
            const before = old[member].sourceItems[item], after = row.members[member].sourceItems[item];
            if (encode(before) !== encode(after)) {
              itemChanges++;
              const expected = JSON.parse(JSON.stringify(before));
              if (row.variant === 'SINGLE_ITEM_ENHANCE') expected.enhanceLevel++;
              else {
                assert.equal(row.variant, 'SINGLE_MODIFIER_RARITY');
                const rarities = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'];
                expected.rarity = rarities[rarities.indexOf(before.rarity) + 1];
              }
              same(expected, after, 'Development changed more than one intended field');
            }
          }
          same(row.members[member].sourceItems, row.members[member].combatItems, 'Raid combat items differ');
        }
        assert.equal(itemChanges, 1, 'Development did not change exactly one item');
        changes[row.variant === 'SINGLE_ITEM_ENHANCE' ? 'enhanceExactlyOneRowsChecked' : 'rarityExactlyOneRowsChecked']++;
      }
    }
    assert.equal(row.calibrationMetric, 'NEUTRAL_TEAM_V1');
    const diag = row.generationDiagnostics;
    assert.equal(diag.assessmentVersion, 'NEUTRAL_TEAM_V1');
    const people = Object.values(row.initState.personagesById);
    close(q(people.filter(p => p.firstTeam === row.reverse)), diag.participantAssessment, 'Player Q does not match saved initial state');
    close(q(people.filter(p => p.firstTeam !== row.reverse)), row.calibration.achievedPower, 'NPC Q does not match saved initial state');
    const p = diag.parameters;
    close(row.calibration.targetPower, diag.participantAssessment * p.powerBonus * p.groupSizeScaling * p.encounterCorrection * p.sizeCorrection, 'Target mismatch');
    assert.equal(p.encounterCorrection, 0.5900075417594784);
    const count = Math.max(2, row.cell.size);
    assert.equal(p.enemyCount, count);
    close(p.sizeCorrection, 0.7 + (1 - 0.7) * (3 / count) ** 2, 'Count correction changed');
    const totals = outcomes.get(row.caseId) ?? {WIN: 0, LOSS: 0, DRAW: 0, TIMEOUT: 0};
    assert(Object.hasOwn(totals, row.outcome)); totals[row.outcome]++; outcomes.set(row.caseId, totals);
    rows++;
  }
  assert.equal(compressedHash.digest('hex'), summary.rawSha256, 'Compressed data hash mismatch');
  for (const c of cases.values()) {
    const totals = outcomes.get(c.caseId);
    for (const outcome of ['WIN', 'LOSS', 'DRAW', 'TIMEOUT']) assert.equal(totals[outcome], c.outcomes[outcome] ?? 0);
    assert.equal(c.attemptedBattles, summary.pairs * 2);
    const scores = [];
    for (let pair = 0; pair < summary.pairs; pair++) {
      const values = [false, true].map(reverse => seen.get(`${c.caseId}/${pair}/${reverse}`));
      assert(values.every(Boolean), 'Missing mirror');
      scores.push(values.includes('TIMEOUT') ? null : values.reduce((a, b) => a + score(b), 0));
    }
    same(scores, c.pairScoresByIndex, 'Pair scores disagree with raw');
  }
  const comparisons = summary.comparisons.map(c => {
    const base = cases.get(c.baseCaseId), variant = cases.get(c.variantCaseId);
    const deltas = base.pairScoresByIndex.flatMap((v, i) => v !== null && variant.pairScoresByIndex[i] !== null
      ? [variant.pairScoresByIndex[i] - v] : []);
    same(deltas, c.pairedScoreDeltas, 'Paired differences disagree with raw');
    const mean = deltas.reduce((a, b) => a + b, 0) / deltas.length;
    const half = Math.sqrt(2 * Math.log(40) / deltas.length);
    close(mean, c.distribution.mean, 'Mean delta');
    close(Math.max(-1, mean - half), c.pairedDelta95Hoeffding.low, 'Delta lower confidence');
    close(Math.min(1, mean + half), c.pairedDelta95Hoeffding.high, 'Delta upper confidence');
    const low = Math.max(-1, mean - half), high = Math.min(1, mean + half);
    return {groupId: c.causalGroupId, tier: c.baseCell.tier, size: c.baseCell.size, level: c.baseCell.raidLevel,
      variant: c.variant, pairs: deltas.length, baseWinrate: base.playerWinsPerAttempt,
      variantWinrate: variant.playerWinsPerAttempt, delta: mean, ci95: [low, high],
      pointMajorHarm: mean < -0.2, confirmedMajorHarm: high < -0.2, majorHarmRuledOutPointwise: low > -0.2,
      harmBelowZero: high < 0};
  });
  console.log(JSON.stringify({status: 'CAUSAL_INPUTS_RAW_Q_AND_POINTWISE_INTERVALS_VALIDATED', rows, cases: cases.size,
    groups: baseInputs.size, pairsPerCondition: summary.pairs, seedOffset: summary.seedOffset,
    ...changes, weakItemCodes: [...weakItemCodes].sort(), comparisons,
    timeouts: [...outcomes.values()].reduce((n, o) => n + o.TIMEOUT, 0),
    majorPointSignals: comparisons.filter(c => c.pointMajorHarm),
    confirmedMajorHarm: comparisons.filter(c => c.confirmedMajorHarm)}, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
