// Independent raw-data validator; no Java calculator or simulator is invoked.
// From the repository root: node validate-zombie-retry.mjs RUN REPLAY NEW_REPORT.
// A single fixed retry; the first failed confirmation is neither overwritten nor pooled.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';
import readline from 'node:readline';
import assert from 'node:assert/strict';

const args = process.argv.slice(2), phase = 'FINAL';
assert.equal(args.length,3,'Expected RUN REPLAY NEW_REPORT');
const [run,replay,output] = args;
const protocol = JSON.parse(fs.readFileSync(path.join(run, 'protocol.json')));
const summary = JSON.parse(fs.readFileSync(path.join(run, 'summary.json')));
const raids = ['ZOMBIE_HORDE'];
const tiers = ['BEGINNER', 'MID', 'STRONG'];
const sizes = [1, 2, 3, 7, 8, 10, 11, 13, 14, 15, 30];
const candidates = {
  WOLFPACK: [1, .9, .8, .75, .7, .65, .6, .55, .5],
  ZOMBIE_HORDE: [.8, .78, .76, .74, .72],
  MYCONID_COLONY: [.77, .82, .87, .92, .97, 1.02],
  MAGGEESE_FLOCK: [.75, .725, .7, .675, .65]
};
const stable = value => Array.isArray(value) ? value.map(stable) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, stable(value[key])])) : value;
const same = (a, b, label) => assert.deepEqual(stable(a), stable(b), label);
const near = (a, b, label) => assert(Number.isFinite(a) && Number.isFinite(b)
  && Math.abs(a - b) <= Math.max(1, Math.abs(a), Math.abs(b)) * 1e-11, `${label}: ${a} != ${b}`);
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const sortedSum = values => [...values].sort((a, b) => a - b).reduce((a, b) => a + b, 0);
const mean = values => values.reduce((a, b) => a + b, 0) / values.length;
const add = (object, key, amount = 1) => { object[key] = (object[key] || 0) + amount; };
const distribution = values => {
  if (!values.length) return {count: 0};
  const ordered = [...values].sort((a, b) => a - b), q = p => ordered[Math.ceil(p * ordered.length) - 1];
  return {count: values.length, mean: mean(values), min: ordered[0], p50: q(.5), p90: q(.9),
    p95: q(.95), max: ordered.at(-1)};
};
const interval = (values, delta = false) => {
  const m = mean(values), width = Math.sqrt(Math.log(40) * (delta ? 2 : .5) / values.length);
  return {low: Math.max(delta ? -1 : 0, m - width), high: Math.min(1, m + width), unclippedHalfWidth: width};
};
const compare = (actual, expected, label) => {
  if (typeof expected === 'number') return near(actual, expected, label);
  if (expected === null || typeof expected !== 'object') return same(actual, expected, label);
  same(Object.keys(actual).sort(), Object.keys(expected).sort(), `${label} keys`);
  for (const key of Object.keys(expected)) compare(actual[key], expected[key], `${label}.${key}`);
};
const weights = {CLOTH: [.75, 1.25, .9, 1.1], LEATHER: [.9, 1.1, 1.25, .75],
  PLATE: [1.25, .75, 1.1, .9], ARCANE: [1.1, .9, .75, 1.25]};
const power = (health, speed, crit, dodge, multiplier, defenses, attacks) => {
  assert(health >= 0 && speed >= 0 && crit >= 0 && crit <= 100 && dodge >= 0 && dodge < 100);
  const taken = mean([0, 1, 2, 3].map(type => {
    const defense = Object.entries(defenses).reduce((sum, [kind, value]) => sum + value * weights[kind][type], 0);
    return 1 - defense / (defense + 500);
  }));
  const deltas = new Map();
  for (const attack of attacks) {
    assert(attack.minRange >= 1 && attack.maxRange >= attack.minRange && attack.attack >= 0);
    deltas.set(attack.minRange, (deltas.get(attack.minRange) || 0) + attack.attack);
    if (attack.maxRange < 2_147_483_647)
      deltas.set(attack.maxRange + 1, (deltas.get(attack.maxRange + 1) || 0) - attack.attack);
  }
  let current = 0, maximum = 0;
  for (const [, delta] of [...deltas].sort((a, b) => a[0] - b[0])) {
    current += delta; maximum = Math.max(maximum, current);
  }
  const result = health / ((1 - dodge / 100) * taken)
    * (Math.min(speed, 1000) / 1000 * (1 + crit / 100 * (multiplier - 1)) * maximum);
  assert(Number.isFinite(result) && result >= 0);
  return result;
};
const itemPower = items => {
  let health = 0, speed = 0, crit = 0, dodge = 0, multiplier = 1.2;
  const defenses = {}, attacks = [];
  for (const item of items) {
    const object = item.object;
    assert.equal(object.progressionVersion, 'V1');
    assert(Number.isInteger(item.enhanceLevel) && item.enhanceLevel >= 0);
    const enhance = base => base + Math.floor((item.enhanceLevel * base + 30) / 60);
    health += enhance(object.health); speed += object.speed;
    crit += object.critChance; dodge += object.dodgeChance; multiplier += object.critMultiplier;
    if (object.defense) {
      const d = object.defense;
      defenses[d.defenseType] = (defenses[d.defenseType] || 0) + enhance(d.defense);
    }
    for (const attack of object.attacks) attacks.push({...attack, attack: enhance(attack.attack)});
  }
  return power(health, speed, crit, dodge, multiplier, defenses, attacks);
};
const log = (base, value) => Math.log(value) / Math.log(base);
const groupScale = (raid, count) => {
  if (raid === 'MYCONID_COLONY') return 1 + .05 * log(4, count);
  if (raid === 'WOLFPACK') return count <= 13 ? 1 + .045 * log(2.2, count)
    : (1 + .045 * log(2.2, 13)) + .045 * log(10, count / 13);
  const at10 = 1 + .05 * log(2.2, 10);
  if (count <= 10) return 1 + .05 * log(2.2, count);
  if (raid === 'MAGGEESE_FLOCK' || count <= 13) return at10 + .05 * log(4, count / 10);
  return (at10 + .05 * log(4, 13 / 10)) + .05 * log(8, count / 13);
};
const npc = (raid, index, count) => {
  // health, attack, defense, speed, critical chance, dodge, critical bonus, armor, range, position, skill, attack
  if (raid === 'WOLFPACK') return index === 0
    ? [1500, 300, 100, 200, 15, 15, 1, 'LEATHER', 1, 'FRONT', 'BERSERK', 'SLASH']
    : [400, 100, 50, 300, 5, 5, .5, 'LEATHER', 1, 'FRONT', 'BLEEDING', 'SLASH'];
  if (raid === 'ZOMBIE_HORDE') return index === 0
    ? [2000, 180, 350, 150, 8, 3, .5, 'PLATE', 1, 'FRONT', 'SELF_HEAL', null]
    : [500, 80, 200, 200, 3, 2, .3, 'PLATE', 1, 'FRONT', 'THORNS', null];
  if (raid === 'MYCONID_COLONY') return index < Math.floor(count / 2)
    ? [420, 70, 160, 190, 4, 3, .3, 'ARCANE', 1, 'FRONT', 'KNOCKBACK', 'MAGICAL']
    : [280, 130, 70, 160, 8, 5, .5, 'ARCANE', 3, 'BACK', 'PRECISE_STRIKE', 'MAGICAL'];
  assert.equal(raid, 'MAGGEESE_FLOCK');
  return index < Math.floor(count / 2)
    ? [320, 95, 70, 280, 6, 8, .4, 'CLOTH', 1, 'FRONT', 'DOUBLE_ATTACK', 'SLASH']
    : [270, 120, 50, 220, 10, 8, .6, 'CLOTH', 2, 'MID', 'HIT_AND_RUN', 'MAGICAL'];
};

assert.equal(protocol.phase, phase); assert.equal(summary.phase, phase);
assert.equal(protocol.metric, 'EQUIPMENT_POWER_SUM_V1');
assert.equal(protocol.tempoDamageMultiplier, 1.75);
const expectedCases = phase === 'SCREEN' ? 825 : 251, expectedBattles = phase === 'SCREEN' ? 16500 : 5900;
assert.equal(protocol.caseCount, expectedCases); assert.equal(summary.caseCount, expectedCases);
assert.equal(protocol.attemptedBattles, expectedBattles); assert.equal(summary.attemptedBattles, expectedBattles);
assert.equal(protocol.generationRoot, phase === 'SCREEN' ? 20_261_122_000_000 : 20_261_126_000_000);
assert.equal(protocol.battleRoot, phase === 'SCREEN' ? 30_261_122_000_000 : 30_261_126_000_000);
same(Object.keys(protocol.productionCorrections).sort(), [...raids].sort(), 'four production corrections');
same(protocol.productionCorrections, protocol.expectedProductionCorrections, 'explicit expected production corrections');
for (const c of Object.values(protocol.productionCorrections)) assert(Number.isFinite(c) && c > 0);
if (phase === 'SCREEN') same(protocol.correctionCandidates, candidates, 'fixed bounded candidate grids');
else same(protocol.correctionCandidates, {}, 'confirmation is not another candidate grid');
for (const [file, expected] of Object.entries(protocol.publicSourceSha256)) assert.equal(hash(file), expected, file);
assert.equal(summary.protocolSha256, hash(path.join(run, 'protocol.json')));
assert.equal(summary.rawSha256, hash(path.join(run, 'battles.jsonl.gz')));
const samples = new Map(protocol.cases.map(entry => [entry.sample.caseId, entry.sample]));
assert.equal(samples.size, expectedCases); assert.equal(summary.cases.length, expectedCases);
const tuple = s => [s.cell.encounter, s.family, s.cell.style, s.cell.tier, s.cell.size, s.cell.raidLevel,
  s.cell.effects, phase === 'SCREEN' ? s.correction : s.variant].join('|');
const expectedTuples = [];
const expected = (raid, family, style, tier, n, level, effects = false, variant = 'BASE') =>
  expectedTuples.push(tuple({family, variant, correction: variant, cell:{encounter:raid, style, tier,
    size:n, raidLevel:level, effects}}));
for (const raid of raids) {
  if (phase === 'SCREEN') {
    for (const t of tiers) for (const n of sizes) for (const c of candidates[raid])
      expected(raid, 'S_MAIN', 'MAIN', t, n, 10, false, c);
  } else {
    for (const t of tiers) for (const n of sizes) expected(raid, 'A_MAIN', 'MAIN', t, n, 10);
    for (const n of [1, 3, 7, 13, 30]) for (const l of [1, 5, 15, 20, 25, 50])
      expected(raid, 'H_LEVELS', 'MAIN', 'MID', n, l);
    for (const attack of ['SLASH', 'PIERCE']) for (const armor of ['LEATHER', 'CLOTH'])
      for (const t of tiers) for (const n of [3, 13]) for (const l of [10, 25])
        expected(raid, 'B_TYPES', `${attack}_${armor}`, t, n, l);
    for (const style of ['RITUAL', 'COMMAND_CUIRASS']) for (const t of tiers)
      for (const n of [1, 3]) for (const l of [10, 25]) expected(raid, 'G_WORKABLE', style, t, n, l);
    for (const t of tiers) for (const n of [3, 13]) for (const l of [10, 25]) {
      for (const v of ['BASE', 'NORMAL_APPEND', 'WEAK_APPEND', 'ONE_ENHANCE', 'ONE_RARITY', 'UNEQUIP_ARMOR'])
        expected(raid, 'E_CAUSAL', `CAUSAL_${v}`, t, n + (v.endsWith('_APPEND') ? 1 : 0), l, false, v);
      expected(raid, 'D_EFFECTS', 'MAIN', t, n, l);
      expected(raid, 'D_EFFECTS', 'MAIN', t, n, l, true, 'EFFECTS');
    }
    for (const n of [1, 3, 13, 14, 30]) for (const l of [1, 10, 25, 50])
      expected(raid, 'F_DEFAULTS', 'FULL_DEFAULTS', 'BEGINNER', n, l);
  }
}
same([...samples.values()].map(tuple).sort(), expectedTuples.sort(), 'complete fixed matrix dimensions');
const seedGroups = new Map(), familyCounts = {};
for (const [index, entry] of protocol.cases.entries()) {
  const s = entry.sample;
  assert.equal(s.caseId, index + 1); assert.equal(s.phase, phase);
  assert.equal(s.members.length, s.cell.size); assert.equal(entry.seeds.length, s.pairs);
  near(s.productionCorrection, protocol.productionCorrections[s.cell.encounter], 'sample production correction');
  const expectedPairs = phase === 'SCREEN' ? 10 : s.family === 'A_MAIN' ? 50
    : s.family === 'H_LEVELS' ? 10 : s.family === 'F_DEFAULTS' ? 8 : 5;
  assert.equal(s.pairs, expectedPairs);
  add(familyCounts, s.family);
  if (!seedGroups.has(s.seedGroup)) seedGroups.set(s.seedGroup, []);
  seedGroups.get(s.seedGroup).push(s);
  for (const [pair, seed] of entry.seeds.entries()) same(seed, {pair,
    generationSeed:protocol.generationRoot + s.seedGroup * 1000 + pair,
    battleSeed:protocol.battleRoot + s.seedGroup * 1000 + pair}, 'fixed explicit seed');
}
same(familyCounts, phase === 'SCREEN' ? {S_MAIN:825}
  : {A_MAIN:33, H_LEVELS:30, B_TYPES:48, G_WORKABLE:24, E_CAUSAL:72, D_EFFECTS:24, F_DEFAULTS:20}, 'family counts');
assert.equal([...samples.values()].reduce((sum, s) => sum + s.pairs * 2, 0), expectedBattles);
if (phase === 'SCREEN') {
  assert.equal(seedGroups.size, 132);
  for (const group of seedGroups.values()) {
    same(group.map(s => s.correction), candidates[group[0].cell.encounter], 'all fixed candidates per context');
    for (const s of group) { same(s.members, group[0].members, 'coupled equipment'); same(s.cell, group[0].cell, 'coupled context'); }
  }
} else {
  assert.equal(seedGroups.size, 179);
  for (const group of seedGroups.values()) {
    if (group.length === 1) continue;
    const base = group.find(s => s.variant === 'BASE'); assert(base, 'shared groups require BASE');
    if (base.family === 'E_CAUSAL') {
      same(group.map(s => s.variant).sort(), ['BASE','NORMAL_APPEND','WEAK_APPEND','ONE_ENHANCE','ONE_RARITY','UNEQUIP_ARMOR'].sort(), 'causal variants');
      for (const s of group.filter(s => s.variant.endsWith('_APPEND'))) same(s.members.slice(0,-1), base.members, 'appending preserves previous members');
    } else {
      assert.equal(base.family, 'D_EFFECTS'); assert.equal(group.length, 2);
      const changed = group.find(s => s.variant === 'EFFECTS'); assert(changed);
      same(base.members, changed.members, 'temporary effects preserve permanent equipment');
    }
  }
}
const expectedReplayIds = [];
if (phase === 'FINAL') for (const raid of raids) {
  for (const n of [1, 3, 13, 30]) expectedReplayIds.push([...samples.values()].find(s => s.cell.encounter === raid
    && s.family === 'A_MAIN' && s.cell.tier === 'MID' && s.cell.size === n && s.cell.raidLevel === 10).caseId);
  for (const family of ['B_TYPES', 'G_WORKABLE', 'E_CAUSAL', 'D_EFFECTS', 'F_DEFAULTS', 'H_LEVELS'])
    expectedReplayIds.push([...samples.values()].find(s => s.cell.encounter === raid && s.family === family).caseId);
}
same([...protocol.replayCaseIds].sort((a,b) => a-b), expectedReplayIds.sort((a,b) => a-b), 'fixed replay cases');
same(protocol.replayPairIndices, [0], 'fixed replay pairs');
assert.equal(new Set(expectedReplayIds).size, phase === 'SCREEN' ? 0 : 10);

const freshStats = () => ({outcomes:{}, rounds:[], skills:{}, skillsByKind:{}, bossesDefeated:0,
  byLevel:{}, byStyle:{}, bySize:{}, byFamily:{}, largestRelativeError:0});
const caseStats = new Map(), raidStats = new Map(), seen = new Set(), paired = new Map(), coupled = new Map();
const replayIds = new Set(expectedReplayIds), originalRepeats = new Map();
let rowCount = 0, completeCoupledContexts = 0;
async function* rows(file) {
  const lines = readline.createInterface({input:fs.createReadStream(file).pipe(zlib.createGunzip()), crlfDelay:Infinity});
  for await (const line of lines) { assert(line.length > 0); yield JSON.parse(line); }
}
for await (const row of rows(path.join(run, 'battles.jsonl.gz'))) {
  ++rowCount;
  const sample = samples.get(row.caseId); assert(sample, 'unknown case');
  const raid = sample.cell.encounter, production = protocol.productionCorrections[raid];
  for (const key of ['family', 'variant', 'change', 'seedGroup', 'cell', 'phase']) same(row[key], sample[key], key);
  assert.equal(typeof row.reverse, 'boolean');
  assert(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < sample.pairs);
  const key = `${row.caseId}/${row.pair}/${row.reverse}`;
  assert(!seen.has(key), 'duplicate raw row'); seen.add(key);
  assert.equal(row.generationSeed, protocol.generationRoot + sample.seedGroup * 1000 + row.pair);
  assert.equal(row.battleSeed, protocol.battleRoot + sample.seedGroup * 1000 + row.pair);
  assert.equal(row.catalog, 'SCALING_V5'); assert.equal(row.playerSkills, 'SCALING_SKILLS_V3');
  assert.equal(row.npcSkills, 'SCALING_SKILLS_V3'); assert.equal(row.tempo, 'ACCELERATED_V1');
  assert.equal(row.roundLimit, 2000); assert.equal(row.calibrationMetric, 'EQUIPMENT_POWER_SUM_V1');
  assert.equal(row.members.length, sample.cell.size);
  assert.equal(row.playerEquipmentPowers.length, sample.cell.size);
  same(row.members.map(m => ({build:m.build, position:m.position, tactic:m.tactic, items:m.sourceItems})), sample.members,
    'registered permanent members');
  for (const [i, member] of row.members.entries()) {
    same(member.sourceItems, member.combatItems, 'raid equipment preserved');
    const slots = member.sourceItems.flatMap(item => item.object.slots);
    assert.equal(slots.length, 7); assert.equal(new Set(slots).size, 7);
    near(itemPower(member.sourceItems), row.playerEquipmentPowers[i], 'independent permanent player power');
  }
  same(row.sourceEffects, row.combatEffects, 'effects preserved');
  if (!sample.cell.effects) same(row.combatEffects, {}, 'no fabricated effects');
  const playerSum = sortedSum(row.playerEquipmentPowers), b = 1 + ((sample.cell.raidLevel - 10) * .06 + .14);
  near(playerSum, row.playerEquipmentPowerSum, 'player power sum');
  const count = ['MAGGEESE_FLOCK', 'MYCONID_COLONY'].includes(raid) ? Math.max(2, sample.cell.size) : sample.cell.size + 1;
  const g = groupScale(raid, ['MAGGEESE_FLOCK', 'MYCONID_COLONY'].includes(raid) ? count : sample.cell.size);
  const c = phase === 'SCREEN' ? sample.correction : production;
  if (phase === 'SCREEN') assert(candidates[raid].includes(c));
  else near(sample.correction, production, 'normal production correction');
  const requested = phase === 'SCREEN' ? b * c / production : b;
  near(row.baseLevelBonus, b, 'true level bonus');
  near(row.requestedPowerBonus, requested, 'explicit requested bonus');
  near(row.expectedProductionCorrection, production, 'expected production correction');
  near(row.effectiveEncounterCorrection, c, 'effective encounter correction');
  const target = playerSum * requested * g * production;
  near(target, row.calibration.targetPower, 'independent encounter target');
  const d = row.generationDiagnostics;
  assert.equal(d.assessmentVersion, 'EQUIPMENT_POWER_SUM_V1');
  near(d.participantAssessment, playerSum, 'diagnostic participant sum');
  near(d.parameters.groupSizeScaling, g, 'diagnostic group scaling');
  near(d.parameters.powerBonus, requested, 'diagnostic actual bonus');
  near(d.parameters.encounterCorrection, production, 'diagnostic actual production correction');
  assert.equal(d.parameters.enemyCount, count); assert.equal(row.enemyBasis.length, count);
  assert.equal(row.enemyEquipmentPowers.length, count);
  if (raid === 'WOLFPACK') { assert.equal(d.parameters.wolfCount, sample.cell.size); assert.equal(d.parameters.alphaCount, 1); }
  if (['MAGGEESE_FLOCK', 'MYCONID_COLONY'].includes(raid)) {
    assert.equal(d.parameters.frontCount, Math.floor(count / 2));
    assert.equal(d.parameters[raid === 'MAGGEESE_FLOCK' ? 'midCount' : 'backCount'], count - Math.floor(count / 2));
  }
  assert(Number.isFinite(row.calibration.multiplier) && row.calibration.multiplier >= 0);
  assert(Number.isInteger(row.calibration.evaluations) && row.calibration.evaluations > 0);
  assert(['MATCHED', 'CLOSEST_STEP', 'MINIMUM_EXCEEDS_TARGET'].includes(row.calibration.status));
  const boss = ['WOLFPACK', 'ZOMBIE_HORDE'].includes(raid);
  assert.equal(row.hasEnemyBoss, boss);
  if (!boss) assert.equal(row.enemyBossDefeated, false);
  for (const [i, enemy] of row.enemyBasis.entries()) {
    const [h, a, def, speed, crit, dodge, bonus, armor, range, position, skill, type] = npc(raid, i, count);
    const scale = base => Math.max(1, Math.ceil(base * row.calibration.multiplier));
    assert.equal(enemy.health, scale(h)); assert.equal(enemy.attack, scale(a)); assert.equal(enemy.defense, scale(def));
    assert.equal(enemy.range, range); assert.equal(enemy.declaredPosition, position);
    if (type) same(enemy.attackTypes, [type], 'NPC attack type');
    else assert(enemy.attackTypes.length === 1 && ['SLASH', 'BLUNT'].includes(enemy.attackTypes[0]));
    same(enemy.skills, [{code:skill, points:4, formulaVersion:'SCALING_SKILLS_V3'}], 'NPC V3 skill');
    near(power(scale(h), speed, crit, dodge, 1.2 + bonus, {[armor]:scale(def)},
      [{minRange:1, maxRange:range, attack:scale(a)}]), row.enemyEquipmentPowers[i], 'independent factory NPC power');
    if (row.outcome !== 'TIMEOUT') {
      const snapshot = row.initState.personagesById[enemy.id];
      assert(snapshot, 'NPC present in actual battle');
      assert.equal(snapshot.health, scale(h)); assert.equal(snapshot.initiative, speed);
      assert.equal(snapshot.critChance, crit); assert.equal(snapshot.dodgeChance, dodge);
      near(snapshot.critMultiplier, 1.2 + bonus, 'NPC critical multiplier');
      assert.equal(snapshot.range, range); same(snapshot.defenses, {[armor]:scale(def)}, 'NPC battle armor');
      same(snapshot.skills, enemy.skills, 'NPC battle skills');
      same(snapshot.attacksByRange, Array.from({length:range}, (_, j) => ({range:j + 1,
        attack:{[enemy.attackTypes[0]]:scale(a)}})), 'NPC actual per-range attacks');
      assert.equal(row.actualEnemyPositions[i], position);
      assert.equal(row.initState.lines[snapshot.lineIndex].position, position);
    }
  }
  const enemySum = sortedSum(row.enemyEquipmentPowers);
  near(enemySum, row.enemyEquipmentPowerSum, 'enemy power sum');
  near(enemySum, row.calibration.achievedPower, 'trial versus actual NPC sum');
  if (raid === 'ZOMBIE_HORDE') {
    const blunt = row.enemyBasis.filter(e => e.attackTypes[0] === 'BLUNT').length;
    assert.equal(d.parameters.bluntCount, blunt); assert.equal(d.parameters.slashCount, count - blunt);
  }
  assert(Number.isInteger(row.postGenerationRandomDraw) && row.postGenerationRandomDraw >= 0
    && row.postGenerationRandomDraw < 1_000_000);
  assert.equal(row.playerGenerationGauges.length, sample.cell.size); assert.equal(row.enemyGenerationGauges.length, count);
  if (phase === 'SCREEN') {
    const commonKey = `${raid}/${sample.seedGroup}/${row.pair}/${row.reverse}`;
    const shared = {cell:row.cell, members:row.members, generationSeed:row.generationSeed, battleSeed:row.battleSeed,
      playerBasis:row.playerBasis, playerPowers:row.playerEquipmentPowers,
      enemies:row.enemyBasis.map(e => ({id:e.id, range:e.range, declaredPosition:e.declaredPosition,
        attackTypes:e.attackTypes, skills:e.skills})), playerGenerationGauges:row.playerGenerationGauges,
      enemyGenerationGauges:row.enemyGenerationGauges, postGenerationRandomDraw:row.postGenerationRandomDraw};
    const prior = coupled.get(commonKey);
    if (prior) {
      same(shared, prior.shared, 'same inputs/attack vector/UUIDs/gauges/outer RNG across candidates');
      assert(!prior.corrections.has(c), 'repeated candidate within shared random group'); prior.corrections.add(c);
      if (prior.corrections.size === candidates[raid].length) { coupled.delete(commonKey); ++completeCoupledContexts; }
    } else coupled.set(commonKey, {shared, corrections:new Set([c])});
  }
  const pairKey = `${row.caseId}/${row.pair}`;
  if (paired.has(pairKey)) {
    const first = paired.get(pairKey);
    for (const field of ['members','sourceEffects','combatEffects','calibration','generationDiagnostics','playerBasis',
      'enemyBasis','playerEquipmentPowers','enemyEquipmentPowers','playerGenerationGauges','enemyGenerationGauges',
      'postGenerationRandomDraw']) same(first[field], row[field], `mirrored ${field}`);
    paired.delete(pairKey);
  } else paired.set(pairKey, row);
  assert(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(row.outcome));
  const actualOutcome = row.outcome === 'TIMEOUT' ? 'TIMEOUT'
    : row.playerTotals.survivors > 0 ? 'WIN' : row.enemyTotals.survivors > 0 ? 'LOSS' : 'DRAW';
  assert.equal(row.outcome, actualOutcome);
  const cs = caseStats.get(row.caseId) || {...freshStats(), pairs:{}, byOrder:{}, metrics:{}, statuses:{}, errors:[]};
  caseStats.set(row.caseId, cs);
  const rs = raidStats.get(raid) || freshStats(); raidStats.set(raid, rs);
  (cs.pairs[row.pair] ||= []).push(row.outcome);
  add(cs.byOrder[row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'] ||= {}, row.outcome);
  add(cs.statuses, row.calibration.status); cs.errors.push(Math.abs(enemySum / target - 1));
  for (const item of [cs, rs]) {
    add(item.outcomes, row.outcome);
    if (row.outcome !== 'TIMEOUT') {
      item.rounds.push(row.rounds);
      for (const [skill, amount] of Object.entries(row.skillEvents)) add(item.skills, skill, amount);
      for (const [kind, amount] of Object.entries(row.skillEventsByKind)) add(item.skillsByKind, kind, amount);
      item.bossesDefeated += Number(row.enemyBossDefeated);
    }
    item.largestRelativeError = Math.max(item.largestRelativeError, Math.abs(enemySum / target - 1));
    for (const [field, value] of [['byLevel',row.cell.raidLevel],['byStyle',row.cell.style],
      ['bySize',row.cell.size],['byFamily',sample.family]]) add(item[field][value] ||= {}, row.outcome);
  }
  if (row.outcome !== 'TIMEOUT') {
    assert.equal(row.initState.tempoVersion, row.tempo); assert(row.rounds >= 0 && row.rounds <= 2000);
    assert.match(row.actionSha256, /^[0-9a-f]{64}$/); assert.match(row.traceSha256, /^[0-9a-f]{64}$/);
    for (const metric of ['rounds','actionCount','normalAttempts','moves','forcedMoves']) (cs.metrics[metric] ||= []).push(row[metric]);
    for (const side of ['playerTotals','enemyTotals']) for (const metric of ['survivors','initialHealth','remainHealth',
      'normalAttacksHit','normalDamage','critDamage','skillDamage','turns']) (cs.metrics[`${side}.${metric}`] ||= []).push(row[side][metric]);
  }
  if (replayIds.has(row.caseId) && row.pair === 0) {
    assert(row.actionLog && row.traceEvents, 'selected original rows retain full logs');
    originalRepeats.set(key, row);
  }
}
assert.equal(rowCount, expectedBattles); assert.equal(paired.size, 0); assert.equal(caseStats.size, expectedCases);
if (phase === 'SCREEN') { assert.equal(coupled.size, 0); assert.equal(completeCoupledContexts, 2640); }
for (const sample of samples.values()) {
  const result = summary.cases[sample.caseId - 1], actual = caseStats.get(sample.caseId);
  for (const key of ['caseId','family','variant','change','cell','seedGroup','phase']) same(result[key], sample[key], `summary ${key}`);
  near(result.effectiveEncounterCorrection, sample.correction, 'summary effective correction');
  near(result.expectedProductionCorrection, sample.productionCorrection, 'summary expected correction');
  same(result.outcomes, actual.outcomes, 'summary outcomes'); same(result.outcomesByOrder, actual.byOrder, 'summary orders');
  const scores = Array.from({length:sample.pairs}, (_, index) => {
    const outcomes = actual.pairs[index]; assert.equal(outcomes.length, 2);
    return outcomes.includes('TIMEOUT') ? null : outcomes.reduce((sum, outcome) => sum
      + (outcome === 'WIN' ? .5 : outcome === 'DRAW' ? .25 : 0), 0);
  });
  const complete = scores.filter(value => value !== null);
  same(result.pairScoresByIndex, scores, 'summary scores'); assert.equal(result.completePairs, complete.length);
  assert.equal(result.incompletePairs, sample.pairs - complete.length);
  compare(result.pairedScore, distribution(complete), 'summary paired distribution');
  if (complete.length) compare(result.pairedScore95Hoeffding, interval(complete), 'summary pair interval');
  same(result.skillEvents, actual.skills, 'summary skill counts'); same(result.skillEventsByKind, actual.skillsByKind, 'summary skill-kind counts');
  assert.equal(result.enemyBossDefeatedBattles, actual.bossesDefeated); assert.equal(result.attemptedBattles, sample.pairs * 2);
  assert.equal(result.completedBattles, sample.pairs * 2 - (actual.outcomes.TIMEOUT || 0));
  near(result.playerWinsPerAttempt, (actual.outcomes.WIN || 0) / (sample.pairs * 2), 'summary win fraction');
  compare(result.completedBattleMetrics, Object.fromEntries(Object.entries(actual.metrics).map(([k,v]) => [k,distribution(v)])), 'all completed metrics');
  same(result.calibrationStatuses, actual.statuses, 'calibration statuses');
  compare(result.calibrationAbsoluteRelativeError, distribution(actual.errors), 'calibration error distribution');
}
const changedSamples = [...samples.values()].filter(s => phase === 'SCREEN' ? s.variant !== 'BASE'
  : (s.family === 'E_CAUSAL' && s.variant !== 'BASE') || (s.family === 'D_EFFECTS' && s.variant === 'EFFECTS'));
assert.equal(summary.comparisons.length, phase === 'SCREEN' ? 693 : 72);
assert.equal(changedSamples.length, summary.comparisons.length);
const compared = new Set();
for (const comparison of summary.comparisons) {
  assert(!compared.has(comparison.variantCaseId)); compared.add(comparison.variantCaseId);
  const sample = changedSamples.find(s => s.caseId === comparison.variantCaseId); assert(sample, 'unplanned comparison');
  const base = [...samples.values()].find(s => s.seedGroup === sample.seedGroup && s.variant === 'BASE');
  assert(base, 'comparison requires an actual registered BASE'); assert.equal(comparison.baseCaseId, base.caseId);
  same(comparison.family, sample.family, 'comparison family'); same(comparison.variant, sample.variant, 'comparison variant');
  const a = summary.cases[base.caseId - 1], b = summary.cases[sample.caseId - 1];
  const deltas = a.pairScoresByIndex.flatMap((score, i) => score === null || b.pairScoresByIndex[i] === null
    ? [] : [b.pairScoresByIndex[i] - score]);
  same(comparison.pairedDeltas, deltas, 'paired comparison deltas'); compare(comparison.distribution, distribution(deltas), 'comparison distribution');
  if (deltas.length) compare(comparison.pairedDelta95Hoeffding, interval(deltas, true), 'comparison interval');
}

const assessment = (contexts, timeoutContexts = contexts) => {
  assert.equal(contexts.length, 33);
  const rate = s => (caseStats.get(s.caseId).outcomes.WIN || 0) / (s.pairs * 2);
  const byTier = Object.fromEntries(tiers.map(t => [t, mean(contexts.filter(s => s.cell.tier === t).map(rate))]));
  const bySize = Object.fromEntries(sizes.map(n => [n, mean(contexts.filter(s => s.cell.size === n).map(rate))]));
  const m = mean(Object.values(byTier));
  const timeouts = timeoutContexts.reduce((sum,s) => sum + (caseStats.get(s.caseId).outcomes.TIMEOUT || 0), 0);
  return {mainContextCount:33, balancedMainWinFraction:m, balancedTierWinFractions:byTier,
    balancedSizeWinFractions:bySize,
    rawMainWinsNotAcceptanceMetric:contexts.reduce((sum,s) => sum + (caseStats.get(s.caseId).outcomes.WIN || 0),0),
    rawMainBattlesNotAcceptanceMetric:contexts.reduce((sum,s) => sum + s.pairs * 2,0), timeoutsAllCases:timeouts,
    passesPredeclaredRule:m >= .45 && m <= .55 && timeouts === 0};
};
const independentSelection = {}, finalAcceptance = {};
for (const raid of raids) {
  if (phase === 'SCREEN') {
    const rows = candidates[raid].map(correction => {
      const contexts = [...samples.values()].filter(s => s.cell.encounter === raid && s.correction === correction);
      const actual = assessment(contexts);
      return {...actual, correction, distanceFromHalf:Math.abs(actual.balancedMainWinFraction - .5)};
    });
    let chosen = rows[0];
    for (const row of rows.slice(1)) if (row.distanceFromHalf < chosen.distanceFromHalf - 1e-12
      || (Math.abs(row.distanceFromHalf - chosen.distanceFromHalf) <= 1e-12 && row.correction > chosen.correction)) chosen = row;
    independentSelection[raid] = {candidates:rows, selectedCorrection:chosen.correction, selectedAssessment:chosen,
      passesPredeclaredRule:chosen.passesPredeclaredRule,
      status:chosen.passesPredeclaredRule ? 'CLOSEST_PREDECLARED_CANDIDATE_IN_BAND' : 'STOP_NO_SELECTION_IN_BAND'};
  } else {
    const contexts = [...samples.values()].filter(s => s.cell.encounter === raid && s.family === 'A_MAIN');
    finalAcceptance[raid] = {...assessment(contexts,[...samples.values()].filter(s => s.cell.encounter === raid)),
      selectedCorrection:protocol.productionCorrections[raid]};
  }
}
const globalTimeouts = [...raidStats.values()].reduce((sum,s) => sum + (s.outcomes.TIMEOUT || 0),0);
const allPassed = globalTimeouts === 0 && Object.values(phase === 'SCREEN' ? independentSelection : finalAcceptance)
  .every(s => s.passesPredeclaredRule);
const decision = phase === 'SCREEN' ? {
  purpose:'DESCRIPTIVE_SCREEN_ONLY_NOT_HOLDOUT_OR_AUTOMATIC_RELEASE', raids:independentSelection,
  passesPredeclaredRule:allPassed,status:allPassed ? 'READY_FOR_FRESH_HOLDOUT' : 'STOP_SCREEN_FAILED'
} : {
  purpose:'SINGLE_FIXED_ZOMBIE_RETRY_AFTER_FAILED_FIRST_CONFIRMATION', raids:finalAcceptance,
  passesPredeclaredRule:allPassed,status:allPassed ? 'CONFIRMED_ON_FRESH_SEEDS' : 'STOP_HOLDOUT_NOT_CONFIRMED'
};
compare(summary[phase === 'SCREEN' ? 'exploratorySelection' : 'finalAcceptance'], decision,
  'independently recomputed complete decision');
const retryPurpose = 'SINGLE_FIXED_ZOMBIE_RETRY_AFTER_FAILED_FIRST_CONFIRMATION';
assert.equal(protocol.purpose,retryPurpose); assert.equal(summary.purpose,retryPurpose);
same(protocol.productionCorrections,{ZOMBIE_HORDE:.76},'one fixed retry, no new search');
const evidenceDirectory = path.dirname(run), selectionFile = path.join(evidenceDirectory,'zombie-retry-selection.json');
const selection = JSON.parse(fs.readFileSync(selectionFile));
const expectedSelection = {purpose:retryPurpose,raid:'ZOMBIE_HORDE',previousCorrection:.78,
  previousMainWins:1431,previousMainBattles:3300,previousConfirmationPassed:false,selectedCorrection:.76,
  originalScreenWinsAtSelectedCorrection:354,originalScreenBattlesAtSelectedCorrection:660,
  confirmationBounds:{min:.45,max:.55},generationRoot:20_261_126_000_000,battleRoot:30_261_126_000_000,
  additionalSearchAllowed:false,previousResultsPooled:false,
  unchangedCorrections:{WOLFPACK:.75,MYCONID_COLONY:.82,MAGGEESE_FLOCK:.725}};
same(selection,expectedSelection,'explicit fixed retry recorded before new seeds');
const retrySelectionSha256 = hash(selectionFile);
assert.equal(protocol.publicSourceSha256[path.relative(process.cwd(),path.resolve(selectionFile))],retrySelectionSha256,
  'retry selection is a frozen public source input');
const previousDirectory = path.join(evidenceDirectory,'final'), previousReportFile = path.join(evidenceDirectory,'final-validation.json');
const previousReport = JSON.parse(fs.readFileSync(previousReportFile));
const previousProtocol = JSON.parse(fs.readFileSync(path.join(previousDirectory,'protocol.json')));
const previousSummary = JSON.parse(fs.readFileSync(path.join(previousDirectory,'summary.json')));
assert.equal(previousReport.phase,'FINAL'); assert.equal(previousReport.cases,1004);
assert.equal(previousReport.battles,23600); assert.equal(previousReport.exactReplayBattles,80);
assert.equal(previousReport.passesPredeclaredRule,false,'first confirmation remains failed');
const previousProtocolSha256 = hash(path.join(previousDirectory,'protocol.json'));
const previousRawSha256 = hash(path.join(previousDirectory,'battles.jsonl.gz'));
assert.equal(previousReport.protocolSha256,previousProtocolSha256); assert.equal(previousReport.rawSha256,previousRawSha256);
assert.equal(previousSummary.protocolSha256,previousProtocolSha256); assert.equal(previousSummary.rawSha256,previousRawSha256);
compare(previousSummary.finalAcceptance.raids,previousReport.finalAcceptance,'previous summary remains independently checked');
assert.equal(previousSummary.finalAcceptance.passesPredeclaredRule,false);
const failed = previousReport.finalAcceptance.ZOMBIE_HORDE;
assert.equal(failed.selectedCorrection,.78); assert.equal(failed.rawMainWinsNotAcceptanceMetric,1431);
assert.equal(failed.rawMainBattlesNotAcceptanceMetric,3300); assert.equal(failed.passesPredeclaredRule,false);
near(failed.balancedMainWinFraction,1431/3300,'first failed zombie confirmation');
for (const [raid,c] of Object.entries(selection.unchangedCorrections)) {
  assert.equal(previousReport.finalAcceptance[raid].selectedCorrection,c);
  assert.equal(previousReport.finalAcceptance[raid].passesPredeclaredRule,true);
}
const originalScreenReportFile = path.join(evidenceDirectory,'screen-validation.json');
const originalScreenReport = JSON.parse(fs.readFileSync(originalScreenReportFile));
const oldCandidate = originalScreenReport.independentSelection.ZOMBIE_HORDE.candidates.find(c => c.correction === .76);
assert(oldCandidate,'retry correction was in the original finite grid');
assert.equal(oldCandidate.rawMainWinsNotAcceptanceMetric,354); assert.equal(oldCandidate.rawMainBattlesNotAcceptanceMetric,660);
near(oldCandidate.balancedMainWinFraction,354/660,'original descriptive .76 result, not a fresh confirmation');
const firstArchive = path.join(evidenceDirectory,'first-final-zombie-sources.tar.gz');
const changedSources = new Set([
  'game/src/main/java/ru/homyakin/seeker/game/event/raid/generator/ZombieHordeGenerator.java',
  'game/src/test/java/ru/homyakin/seeker/game/event/raid/generator/ZombieEquipmentScalingTest.java',
  'game/src/test/java/ru/homyakin/seeker/game/event/raid/generator/RaidNpcCombatVersionTest.java'
]);
// Read regular tar members in memory; no extraction and no child-process permission is required.
const tar = zlib.gunzipSync(fs.readFileSync(firstArchive)), archivedSources = new Map();
const tarText = buffer => buffer.toString('utf8').split('\0')[0];
let nextLongName = null, nextPaxPath = null;
for (let offset = 0; offset + 512 <= tar.length;) {
  const header = tar.subarray(offset,offset + 512);
  if (header.every(byte => byte === 0)) break;
  const length = parseInt(tarText(header.subarray(124,136)).trim() || '0',8);
  assert(Number.isSafeInteger(length) && length >= 0 && offset + 512 + length <= tar.length,'valid archived member length');
  const payload = tar.subarray(offset + 512,offset + 512 + length), type = String.fromCharCode(header[156]);
  let name = tarText(header.subarray(0,100));
  const prefix = tarText(header.subarray(345,500));
  if (prefix) name = prefix + '/' + name;
  if (type === 'L') nextLongName = tarText(payload).replace(/\n$/,'');
  else if (type === 'x') {
    for (const record of payload.toString('utf8').split('\n')) {
      const match = /^\d+ path=(.*)$/.exec(record); if (match) nextPaxPath = match[1];
    }
  } else {
    name = nextPaxPath || nextLongName || name; nextPaxPath = null; nextLongName = null;
    if (type === '0' || type === '\0') {
      assert(!archivedSources.has(name),'no duplicate archived source'); archivedSources.set(name,payload);
    }
  }
  offset += 512 + Math.ceil(length/512)*512;
}
same([...archivedSources.keys()].sort(),[...changedSources].sort(),'exactly the three preserved source files');
assert.equal(Object.keys(previousProtocol.publicSourceSha256).length,229);
for (const [file,expected] of Object.entries(previousProtocol.publicSourceSha256)) {
  const actual = changedSources.has(file) ? crypto.createHash('sha256')
    .update(archivedSources.get(file)).digest('hex') : hash(file);
  assert.equal(actual,expected,'first confirmation source preserved: '+file);
}
const firstFinalReportSha256 = hash(previousReportFile), firstFinalSourceArchiveSha256 = hash(firstArchive),
  originalScreenReportSha256 = hash(originalScreenReportFile);
let exactReplayBattles = 0;
if (phase === 'FINAL') {
  const repeated = new Set();
  for await (const row of rows(path.join(replay, 'battles.jsonl.gz'))) {
    const key = `${row.caseId}/${row.pair}/${row.reverse}`;
    assert(!repeated.has(key), 'duplicate replay row'); repeated.add(key);
    assert(originalRepeats.has(key), 'unregistered replay row');
    same(originalRepeats.get(key), row, 'strict ALL-JSON replay, no fields removed');
  }
  assert.equal(repeated.size, 20); assert.equal(originalRepeats.size, 20); exactReplayBattles = repeated.size;
}
assert.equal(globalTimeouts,0,
  'technical acceptance requires no incomplete battles in any candidate or diagnostic');
const report = {phase, cases:caseStats.size, battles:rowCount, exactReplayBattles, passesPredeclaredRule:allPassed,
  checkedCommonRandomGroups:completeCoupledContexts, checkedSourceFiles:Object.keys(protocol.publicSourceSha256).length,
  protocolSha256:hash(path.join(run, 'protocol.json')), rawSha256:hash(path.join(run, 'battles.jsonl.gz')),
  ...(phase === 'SCREEN' ? {independentSelection} : {finalAcceptance,retrySelectionSha256,firstFinalReportSha256,firstFinalSourceArchiveSha256,
    previousProtocolSha256,previousRawSha256,originalScreenReportSha256,
    replaySha256:hash(path.join(replay,'battles.jsonl.gz'))}),
  raids:Object.fromEntries([...raidStats].map(([raid,s]) => [raid,{...s,rounds:distribution(s.rounds)}]))};
fs.writeFileSync(output, JSON.stringify(report, null, 2) + '\n', {flag:'wx'});
console.log(JSON.stringify({phase,cases:report.cases,battles:report.battles,exactReplayBattles,
  ...(phase === 'SCREEN' ? {independentSelection} : {finalAcceptance})}));
if (!allPassed) process.exitCode = 2;
