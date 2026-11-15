// Separate screen arithmetic and raw-row checks, without invoking the Java power calculator.
// Run from the repository root with SCREEN_DIRECTORY NEW_REPORT; existing outputs are never overwritten.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';
import readline from 'node:readline';
import assert from 'node:assert/strict';

const [run, output] = process.argv.slice(2);
assert(run && output && process.argv.length === 4, 'Expected SCREEN_DIRECTORY NEW_REPORT');
const protocol = JSON.parse(fs.readFileSync(path.join(run, 'protocol.json')));
const summary = JSON.parse(fs.readFileSync(path.join(run, 'summary.json')));
const stable = value => Array.isArray(value) ? value.map(stable) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, stable(value[key])])) : value;
const same = (a, b, label) => assert.deepEqual(stable(a), stable(b), label);
const near = (a, b, label) => assert(Number.isFinite(a) && Number.isFinite(b)
  && Math.abs(a - b) <= Math.max(1, Math.abs(a), Math.abs(b)) * 1e-11, `${label}: ${a} != ${b}`);
const sortedSum = values => [...values].sort((a, b) => a - b).reduce((a, b) => a + b, 0);
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const weights = {CLOTH: [.75, 1.25, .9, 1.1], LEATHER: [.9, 1.1, 1.25, .75],
  PLATE: [1.25, .75, 1.1, .9], ARCANE: [1.1, .9, .75, 1.25]};
const power = (health, speed, crit, dodge, multi, defenses, attacks) => {
  const average = [0, 1, 2, 3].map(type => {
    const defense = Object.entries(defenses).reduce((sum, [kind, value]) => sum + value * weights[kind][type], 0);
    return 1 - defense / (defense + 500);
  }).reduce((a, b) => a + b, 0) / 4;
  const deltas = new Map();
  for (const attack of attacks) {
    deltas.set(attack.minRange, (deltas.get(attack.minRange) || 0) + attack.attack);
    deltas.set(attack.maxRange + 1, (deltas.get(attack.maxRange + 1) || 0) - attack.attack);
  }
  let current = 0, maximum = 0;
  for (const [, delta] of [...deltas].sort((a, b) => a[0] - b[0])) {
    current += delta; maximum = Math.max(maximum, current);
  }
  const durability = health / ((1 - dodge / 100) * average);
  return durability * (Math.min(speed, 1000) / 1000 * (1 + crit / 100 * (multi - 1)) * maximum);
};
const itemPower = items => {
  let health = 0, speed = 0, crit = 0, dodge = 0, multi = 1.2;
  const defenses = {}, attacks = [];
  for (const item of items) {
    const object = item.object;
    assert.equal(object.progressionVersion, 'V1');
    const enhance = base => base + Math.floor((item.enhanceLevel * base + 30) / 60);
    health += enhance(object.health); speed += object.speed;
    crit += object.critChance; dodge += object.dodgeChance; multi += object.critMultiplier;
    if (object.defense) {
      const defense = object.defense;
      defenses[defense.defenseType] = (defenses[defense.defenseType] || 0) + enhance(defense.defense);
    }
    for (const attack of object.attacks) attacks.push({...attack, attack: enhance(attack.attack)});
  }
  return power(health, speed, crit, dodge, multi, defenses, attacks);
};
const log = (base, value) => Math.log(value) / Math.log(base);
const group = (raid, count) => {
  if (raid === 'WOLFPACK') return count <= 13 ? 1 + log(2.2, count) * .045
    : (1 + log(2.2, 13) * .045) + log(10, count / 13) * .045;
  const at10 = 1 + log(2.2, 10) * .05;
  if (count <= 10) return 1 + log(2.2, count) * .05;
  if (raid === 'MAGGEESE_FLOCK' || count <= 13) return at10 + log(4, count / 10) * .05;
  return (at10 + log(4, 13 / 10) * .05) + log(8, count / 13) * .05;
};
const npc = (raid, index, count) => {
  // health, attack, defense, speed, crit, dodge, crit bonus, armor, range, position, skill
  if (raid === 'WOLFPACK') return index === 0
    ? [1500, 300, 100, 200, 15, 15, 1, 'LEATHER', 1, 'FRONT', 'BERSERK']
    : [400, 100, 50, 300, 5, 5, .5, 'LEATHER', 1, 'FRONT', 'BLEEDING'];
  if (raid === 'ZOMBIE_HORDE') return index === 0
    ? [2000, 180, 350, 150, 8, 3, .5, 'PLATE', 1, 'FRONT', 'SELF_HEAL']
    : [500, 80, 200, 200, 3, 2, .3, 'PLATE', 1, 'FRONT', 'THORNS'];
  assert.equal(raid, 'MAGGEESE_FLOCK');
  return index < Math.floor(count / 2)
    ? [320, 95, 70, 280, 6, 8, .4, 'CLOTH', 1, 'FRONT', 'DOUBLE_ATTACK']
    : [270, 120, 50, 220, 10, 8, .6, 'CLOTH', 2, 'MID', 'HIT_AND_RUN'];
};
const samples = new Map(protocol.cases.map(entry => [entry.sample.caseId, entry.sample]));
assert.equal(samples.size, 156);
assert.equal(protocol.phase, "SCREEN");
assert.equal(summary.phase, "SCREEN");
assert.equal(summary.caseCount, 156);
assert.equal(summary.attemptedBattles, 3120);
assert.equal(protocol.generationRoot, 20_261_120_000_000);
assert.equal(protocol.battleRoot, 30_261_120_000_000);
same(protocol.correctionCandidates, [1, .8, .65, .5], "fixed correction grid");
same(protocol.replayCaseIds, [], "No independent holdout replay is claimed for screening");
const production = protocol.productionCorrection;
assert(production > 0 && Number.isFinite(production));
assert.equal(protocol.expectedProductionCorrection, production);
assert.equal(protocol.attemptedBattles, 3120);
for (const [file, expected] of Object.entries(protocol.publicSourceSha256)) assert.equal(hash(file), expected, file);
assert.equal(summary.protocolSha256, hash(path.join(run, 'protocol.json')));
assert.equal(summary.rawSha256, hash(path.join(run, 'battles.jsonl.gz')));
const seen = new Set(), paired = new Map(), cases = new Map(), coupled = new Map();
let completeCoupledContexts = 0;
const raids = new Map();
const addCount = (counts, key, amount = 1) => { counts[key] = (counts[key] || 0) + amount; };
const stats = () => ({outcomes: {}, rounds: [], skills: {}, skillsByKind: {}, bossesDefeated: 0,
  byLevel: {}, byStyle: {}, bySize: {}, byFamily: {}, largestRelativeError: 0});
let rowCount = 0;
async function* rows(file) {
  const lines = readline.createInterface({input: fs.createReadStream(file).pipe(zlib.createGunzip()), crlfDelay: Infinity});
  for await (const line of lines) if (line) yield JSON.parse(line);
}
for await (const row of rows(path.join(run, 'battles.jsonl.gz'))) {
  ++rowCount;
  const sample = samples.get(row.caseId);
  assert(sample, 'Unknown case'); same(row.cell, sample.cell, 'cell');
  assert.equal(typeof row.reverse, 'boolean');
  assert(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < sample.pairs);
  const key = `${row.caseId}/${row.pair}/${row.reverse}`;
  assert(!seen.has(key), 'Duplicate row'); seen.add(key);
  assert.equal(row.generationSeed, protocol.generationRoot + sample.seedGroup * 1000 + row.pair);
  assert.equal(row.battleSeed, protocol.battleRoot + sample.seedGroup * 1000 + row.pair);
  assert.equal(row.catalog, 'SCALING_V5'); assert.equal(row.playerSkills, 'SCALING_SKILLS_V3');
  assert.equal(row.npcSkills, 'SCALING_SKILLS_V3'); assert.equal(row.tempo, 'ACCELERATED_V1');
  assert.equal(row.calibrationMetric, 'EQUIPMENT_POWER_SUM_V1');
  assert.equal(row.members.length, row.cell.size);
  row.members.forEach((member, i) => {
    same(member.sourceItems, sample.members[i].items, 'source equipment');
    same(member.sourceItems, member.combatItems, 'raid equipment preserved');
    const slots = member.sourceItems.flatMap(item => item.object.slots);
    assert.equal(slots.length, 7); assert.equal(new Set(slots).size, 7);
    near(itemPower(member.sourceItems), row.playerEquipmentPowers[i], 'independent player power');
  });
  const playerSum = sortedSum(row.playerEquipmentPowers);
  near(playerSum, row.playerEquipmentPowerSum, 'player sum');
  const raid = row.cell.encounter;
  assert.equal(raid, 'ZOMBIE_HORDE');
  assert.equal(row.phase, 'SCREEN');
  assert.equal(sample.phase, 'SCREEN');
  assert.equal(row.cell.raidLevel, 10);
  assert.equal(sample.pairs, 10);
  assert([1, .8, .65, .5].includes(sample.correction));
  near(row.effectiveEncounterCorrection, sample.correction, 'candidate correction');
  near(row.expectedProductionCorrection, production, 'production metadata');
  near(sample.productionCorrection, production, 'sample production metadata');
  const count = raid === 'MAGGEESE_FLOCK' ? Math.max(2, row.cell.size) : row.cell.size + 1;
  const g = group(raid, raid === 'MAGGEESE_FLOCK' ? count : row.cell.size);
  const b = 1 + ((row.cell.raidLevel - 10) * .06 + .14);
  const requested = b * sample.correction / production;
  near(row.baseLevelBonus, b, 'true level bonus');
  near(row.requestedPowerBonus, requested, 'explicit screening override');
  const target = playerSum * requested * g * production;
  near(target, row.calibration.targetPower, 'independent target');
  const diagnostics = row.generationDiagnostics;
  assert.equal(diagnostics.assessmentVersion, 'EQUIPMENT_POWER_SUM_V1');
  near(playerSum, diagnostics.participantAssessment, 'diagnostic sum');
  near(g, diagnostics.parameters.groupSizeScaling, 'diagnostic group');
  near(requested, diagnostics.parameters.powerBonus, 'actual requested diagnostic bonus');
  near(production, diagnostics.parameters.encounterCorrection, 'actual production correction');
  assert.equal(count, row.enemyBasis.length); assert.equal(count, diagnostics.parameters.enemyCount);
  row.enemyBasis.forEach((enemy, i) => {
    const [h, a, d, speed, crit, dodge, bonus, armor, range, position, skill] = npc(raid, i, count);
    const scale = value => Math.max(1, Math.ceil(value * row.calibration.multiplier));
    assert.equal(enemy.health, scale(h)); assert.equal(enemy.attack, scale(a)); assert.equal(enemy.defense, scale(d));
    assert.equal(enemy.range, range); assert.equal(enemy.declaredPosition, position);
    if (row.outcome !== 'TIMEOUT') assert.equal(row.actualEnemyPositions[i], position);
    same(enemy.skills, [{code: skill, points: 4, formulaVersion: 'SCALING_SKILLS_V3'}], 'NPC skills');
    near(power(scale(h), speed, crit, dodge, 1.2 + bonus, {[armor]: scale(d)},
      [{minRange: 1, maxRange: range, attack: scale(a)}]), row.enemyEquipmentPowers[i], 'independent NPC power');
    if (row.outcome !== 'TIMEOUT') {
      const snapshot = row.initState.personagesById[enemy.id];
      assert.equal(snapshot.initiative, speed); assert.equal(snapshot.health, scale(h));
      same(snapshot.defenses, {[armor]: scale(d)}, 'NPC armor');
    }
  });
  const enemySum = sortedSum(row.enemyEquipmentPowers);
  near(enemySum, row.enemyEquipmentPowerSum, 'enemy sum');
  near(enemySum, row.calibration.achievedPower, 'trial vs actual NPC sum');
  if (raid === 'ZOMBIE_HORDE') {
    const blunt = row.enemyBasis.filter(enemy => enemy.attackTypes.length === 1 && enemy.attackTypes[0] === 'BLUNT').length;
    assert(row.enemyBasis.every(enemy => enemy.attackTypes.length === 1 && ['BLUNT', 'SLASH'].includes(enemy.attackTypes[0])));
    assert.equal(blunt, diagnostics.parameters.bluntCount); assert.equal(count - blunt, diagnostics.parameters.slashCount);
  }
  const commonKey = `${sample.seedGroup}/${row.pair}/${row.reverse}`;
  const shared = {
    cell: row.cell, members: row.members, generationSeed: row.generationSeed, battleSeed: row.battleSeed,
    playerBasis: row.playerBasis, playerPowers: row.playerEquipmentPowers,
    enemies: row.enemyBasis.map(enemy => ({id: enemy.id, range: enemy.range,
      declaredPosition: enemy.declaredPosition, attackTypes: enemy.attackTypes, skills: enemy.skills})),
    playerGenerationGauges: row.playerGenerationGauges, enemyGenerationGauges: row.enemyGenerationGauges,
    postGenerationRandomDraw: row.postGenerationRandomDraw
  };
  assert(Number.isInteger(row.postGenerationRandomDraw) && row.postGenerationRandomDraw >= 0
    && row.postGenerationRandomDraw < 1_000_000);
  const prior = coupled.get(commonKey);
  if (prior) {
    same(shared, prior.shared, 'same input actors/attack vector/UUIDs/initiative/outer RNG across all C');
    assert(!prior.corrections.has(sample.correction), 'Repeated candidate within a coupled context');
    prior.corrections.add(sample.correction);
    if (prior.corrections.size === 4) { coupled.delete(commonKey); ++completeCoupledContexts; }
  } else coupled.set(commonKey, {shared, corrections: new Set([sample.correction])});
  const pairKey = `${row.caseId}/${row.pair}`;
  if (paired.has(pairKey)) {
    const first = paired.get(pairKey);
    for (const field of ['members', 'sourceEffects', 'combatEffects', 'calibration', 'generationDiagnostics',
      'playerBasis', 'enemyBasis', 'playerEquipmentPowers', 'enemyEquipmentPowers']) same(first[field], row[field], field);
    paired.delete(pairKey);
  } else paired.set(pairKey, row);
  const actualOutcome = row.outcome === 'TIMEOUT' ? 'TIMEOUT'
    : row.playerTotals.survivors > 0 ? 'WIN' : row.enemyTotals.survivors > 0 ? 'LOSS' : 'DRAW';
  assert.equal(row.outcome, actualOutcome);
  const cellStats = cases.get(row.caseId) || stats(); cases.set(row.caseId, cellStats);
  const raidStats = raids.get(raid) || stats(); raids.set(raid, raidStats);
  cellStats.pairs ||= {};
  cellStats.pairs[row.pair] ||= [];
  cellStats.pairs[row.pair].push(row.outcome);
  cellStats.byOrder ||= {};
  cellStats.byOrder[row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'] ||= {};
  addCount(cellStats.byOrder[row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'], row.outcome);
  for (const item of [cellStats, raidStats]) {
    addCount(item.outcomes, row.outcome);
    if (row.outcome !== 'TIMEOUT') item.rounds.push(row.rounds);
    if (row.outcome !== 'TIMEOUT') {
      for (const [skill, amount] of Object.entries(row.skillEvents || {})) addCount(item.skills, skill, amount);
      for (const [skill, amount] of Object.entries(row.skillEventsByKind || {})) addCount(item.skillsByKind, skill, amount);
      item.bossesDefeated += Number(row.enemyBossDefeated);
    }
    item.largestRelativeError = Math.max(item.largestRelativeError, Math.abs(enemySum / target - 1));
    for (const [field, value] of [['byLevel', row.cell.raidLevel], ['byStyle', row.cell.style],
      ['bySize', row.cell.size], ['byFamily', sample.family]]) {
      const counts = item[field][value] || {}; item[field][value] = counts; addCount(counts, row.outcome);
    }
  }

}
assert.equal(rowCount, 3120); assert.equal(paired.size, 0);
assert.equal(coupled.size, 0); assert.equal(completeCoupledContexts, 39 * 10 * 2);
for (const sample of samples.values()) {
  const result = summary.cases.find(item => item.caseId === sample.caseId);
  const actual = cases.get(sample.caseId);
  same(result.outcomes, actual.outcomes, 'summary counts');
  same(result.outcomesByOrder, actual.byOrder, 'summary order counts');
  const scores = Object.keys(actual.pairs).map(Number).sort((a, b) => a - b).map(index => {
    const outcomes = actual.pairs[index]; assert.equal(outcomes.length, 2);
    return outcomes.includes('TIMEOUT') ? null : outcomes.reduce((sum, outcome) => sum
      + (outcome === 'WIN' ? .5 : outcome === 'DRAW' ? .25 : 0), 0);
  });
  same(result.pairScoresByIndex, scores, 'summary pair scores');
  assert.equal(result.completePairs, scores.filter(score => score !== null).length);
  assert.equal(result.incompletePairs, scores.filter(score => score === null).length);
  same(result.skillEvents, actual.skills, 'summary skill counts');
  same(result.skillEventsByKind, actual.skillsByKind, 'summary skill-kind counts');
  assert.equal(result.enemyBossDefeatedBattles, actual.bossesDefeated);
  assert.equal(result.attemptedBattles, sample.pairs * 2);
  near(result.playerWinsPerAttempt, (actual.outcomes.WIN || 0) / (sample.pairs * 2), 'summary win rate');
  assert.equal(result.completedBattles, sample.pairs * 2 - (actual.outcomes.TIMEOUT || 0));
  assert.equal(result.completedBattleMetrics.rounds?.count || 0, actual.rounds.length);
  if (actual.rounds.length) near(result.completedBattleMetrics.rounds.mean,
    actual.rounds.reduce((a, b) => a + b, 0) / actual.rounds.length, 'summary rounds');
}
assert.equal(summary.comparisons.length, 117);
const expectedChanges = [...samples.values()].filter(sample => sample.variant !== 'BASE');
const compared = new Set();
for (const comparison of summary.comparisons) {
  assert(!compared.has(comparison.variantCaseId)); compared.add(comparison.variantCaseId);
  const sample = expectedChanges.find(item => item.caseId === comparison.variantCaseId);
  assert(sample, 'Unplanned comparison');
  assert.equal(sample.family, comparison.family); assert.equal(sample.variant, comparison.variant);
  const baseSample = [...samples.values()].find(item => item.seedGroup === sample.seedGroup && item.variant === 'BASE');
  assert.equal(baseSample.caseId, comparison.baseCaseId);
  const base = summary.cases.find(item => item.caseId === comparison.baseCaseId);
  const changed = summary.cases.find(item => item.caseId === comparison.variantCaseId);
  assert.equal(base.seedGroup, changed.seedGroup);
  const deltas = base.pairScoresByIndex.flatMap((score, index) => score === null
    || changed.pairScoresByIndex[index] === null ? [] : [changed.pairScoresByIndex[index] - score]);
  same(comparison.pairedDeltas, deltas, 'paired comparison');
  if (deltas.length) near(comparison.distribution.mean, deltas.reduce((a, b) => a + b, 0) / deltas.length,
    'comparison mean');
}
assert.equal(compared.size, expectedChanges.length);

const familyCounts = {}, seedGroups = new Map();
for (const sample of samples.values()) {
  assert.equal(sample.cell.encounter, 'ZOMBIE_HORDE');
  assert.equal(sample.cell.raidLevel, 10);
  assert.equal(sample.pairs, 10);
  addCount(familyCounts, sample.family);
  if (!seedGroups.has(sample.seedGroup)) seedGroups.set(sample.seedGroup, []);
  seedGroups.get(sample.seedGroup).push(sample);
}
same(familyCounts, {S_MAIN: 108, S_DIAGNOSTIC: 48}, 'fixed screening families');
assert.equal(seedGroups.size, 39);
for (const group of seedGroups.values()) {
  same(group.map(sample => sample.correction), [1, .8, .65, .5], 'complete fixed candidate set per context');
  for (const sample of group) {
    same(sample.members, group[0].members, 'protocol members shared across candidates');
    same(sample.cell, group[0].cell, 'protocol context shared across candidates');
  }
}
const computedCandidates = [];
let selected = null;
for (const correction of [1, .8, .65, .5]) {
  let wins = 0, attempts = 0, timeouts = 0;
  const tierWins = {}, tierAttempts = {}, diagnosticWins = {}, diagnosticAttempts = {};
  const mainContexts = new Set();
  for (const sample of samples.values()) {
    if (sample.correction !== correction) continue;
    const actual = cases.get(sample.caseId);
    timeouts += actual.outcomes.TIMEOUT || 0;
    const battles = Object.values(actual.outcomes).reduce((a, b) => a + b, 0);
    if (sample.family === 'S_MAIN') {
      assert.equal(sample.cell.style, 'MAIN');
      assert([1, 3, 7, 10, 11, 13, 14, 15, 30].includes(sample.cell.size));
      assert(['BEGINNER', 'MID', 'STRONG'].includes(sample.cell.tier));
      const context = `${sample.cell.tier}/${sample.cell.size}`;
      assert(!mainContexts.has(context)); mainContexts.add(context);
      wins += actual.outcomes.WIN || 0; attempts += battles;
      addCount(tierWins, sample.cell.tier, actual.outcomes.WIN || 0);
      addCount(tierAttempts, sample.cell.tier, battles);
    } else {
      assert(['RITUAL', 'BLUNT_LEATHER'].includes(sample.cell.style));
      assert([3, 13].includes(sample.cell.size));
      addCount(diagnosticWins, sample.cell.style, actual.outcomes.WIN || 0);
      addCount(diagnosticAttempts, sample.cell.style, battles);
    }
  }
  assert.equal(mainContexts.size, 27);
  assert.equal(attempts, 540);
  same(tierAttempts, {BEGINNER: 180, MID: 180, STRONG: 180}, 'balanced main counts per tier');
  same(diagnosticAttempts, {RITUAL: 120, BLUNT_LEATHER: 120}, 'separate diagnostic counts');
  const passed = wins >= 135 && Object.values(tierWins).filter(value => value >= 36).length >= 2 && timeouts === 0;
  if (selected === null && passed) selected = correction;
  computedCandidates.push({correction, mainWins: wins, mainBattles: attempts, mainWinsByTier: tierWins,
    mainBattlesPerTier: 180, diagnosticWinsByStyle: diagnosticWins, diagnosticBattlesPerStyle: 120,
    timeoutsAllStyles: timeouts, passesPredeclaredRule: passed});
}
const computedSelection = {purpose: 'DESCRIPTIVE_SCREEN_ONLY_NOT_HOLDOUT_OR_AUTOMATIC_RELEASE',
  candidates: computedCandidates, selectedCorrection: selected,
  status: selected === null ? 'STOP_NO_CANDIDATE_PASSES' : 'LARGEST_PASSING_CANDIDATE'};
same(summary.exploratorySelection, computedSelection, 'independent largest-C decision from raw battles');

const distribution = values => {
  const sorted = values.toSorted((a, b) => a - b);
  return {count: sorted.length, min: sorted[0], p50: sorted[Math.ceil(sorted.length * .5) - 1],
    p95: sorted[Math.ceil(sorted.length * .95) - 1], max: sorted.at(-1)};
};
const report = {cases: cases.size, battles: rowCount, independentHoldoutBattles: 0,
  checkedCommonRandomGroups: completeCoupledContexts, exploratorySelection: computedSelection,
  checkedSourceFiles: Object.keys(protocol.publicSourceSha256).length,
  protocolSha256: hash(path.join(run, 'protocol.json')), rawSha256: hash(path.join(run, 'battles.jsonl.gz')),
  raids: Object.fromEntries([...raids].map(([raid, item]) => [raid, {...item, rounds: distribution(item.rounds)}]))};
fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n', {flag: 'wx'});
console.log(JSON.stringify({cases: report.cases, battles: report.battles, selection: computedSelection,
  raids: Object.fromEntries([...raids].map(([raid, item]) => [raid, {outcomes: item.outcomes, rounds: distribution(item.rounds)}]))}));
