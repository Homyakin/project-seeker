'use strict';

// Independently written from the producing helper, public model sources and the frozen phase agreement.
// Does not read or invoke an owning validator. No Java, battles, target or repository writes.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const ROOT = process.argv[2] || '/tmp/seeker-step15-4-1-counter-confirm.j9vtbv';
const read = file => JSON.parse(fs.readFileSync(path.join(ROOT, file), 'utf8'));
const frozen = read('frozen-preparation/manifest.json');
const inputs = read('frozen-preparation/inputs.json');
const seeds = read('frozen-preparation/seeds.json');
const summary = read('screen/summary.json');
const MODELS = ['SAME', 'CROSS'];
const PAIRS = 500;
const policy = read('inference-policy.json');
const ATTACKS = ['SLASH', 'BLUNT', 'PIERCE', 'MAGICAL'];
const DEFENSES = ['CLOTH', 'LEATHER', 'PLATE', 'ARCANE'];
const MATRIX = { CLOTH: [0.75, 1.25, 0.9, 1.1], LEATHER: [0.9, 1.1, 1.25, 0.75],
  PLATE: [1.25, 0.75, 1.1, 0.9], ARCANE: [1.1, 0.9, 0.75, 1.25] };
const POINTS = { COMMON: 0, UNCOMMON: 1, RARE: 2, EPIC: 3, LEGENDARY: 4 };
const REF = { Hplayer: 11735.6070634692, Dplayer: 154.2861432,
  Hnpc: 9616.368128260692, Dnpc: 235.56672000000003 };
const LIMIT = Math.floor(2147483647 / 16);
let assertions = 0;
const counts = { actorMetrics: 0, playerPreparedFromItems: 0, npcFactory: 0, exactUnitSeeds: 0,
  exactPlayerRng: 0, exactNpcRng: 0, prefixes: 0, mirroredTraits: 0, counterTwinComparisons: 0,
  sameCrossAnchorComparisons: 0, exactSameCrossBattles: 0, weakActors: 0, outcomesFromFinalHp: 0 };
function check(condition, message) { assertions++; assert.ok(condition, message); }
function close(a, b, message) {
  check(Number.isFinite(a) && Number.isFinite(b)
    && Math.abs(a - b) <= 2e-12 * Math.max(1, Math.abs(a), Math.abs(b)), `${message}: ${a} vs ${b}`);
}
function canonical(value, key) {
  if (Array.isArray(value)) {
    const result = value.map(item => canonical(item));
    return ['slots', 'availableOnSlots'].includes(key) ? result.sort() : result;
  }
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k], k)]));
  return value;
}
function digest(value) { return crypto.createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex'); }
function hashFile(file) { return crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'); }
function stable(values) { return values.toSorted((a, b) => a - b).reduce((a, b) => a + b, 0); }
function q(level) { return 1 + ((level - 10) * 0.06 + 0.14); }
function shaSeed(namespace, group, pair) { return crypto.createHash('sha256').update(`${namespace}/${group}/${pair}`).digest().readUIntBE(0, 6); }
function withoutGeometry(snapshot, mirror = false) {
  const result = { ...snapshot }; delete result.lineIndex;
  if (mirror) { delete result.firstTeam; delete result.advanceDirection; }
  return result;
}

// java.util.Random and RandomSupport.nextInt(origin,bound), checked against JDK25 src.zip.
class JavaRandom {
  constructor(seed) { this.state = (BigInt(seed) ^ 0x5deece66dn) & ((1n << 48n) - 1n); }
  bits(n) {
    this.state = (this.state * 0x5deece66dn + 11n) & ((1n << 48n) - 1n);
    return Number(this.state >> BigInt(48 - n));
  }
  long() {
    const high = BigInt.asIntN(32, BigInt(this.bits(32)));
    const low = BigInt.asIntN(32, BigInt(this.bits(32)));
    return BigInt.asIntN(64, (high << 32n) + low);
  }
  actor() {
    const high = (this.long() & 0xffffffffffff0fffn) | 0x4000n;
    const low = (this.long() & 0x3fffffffffffffffn) | 0x8000000000000000n;
    const h = BigInt.asUintN(64, high).toString(16).padStart(16, '0');
    const l = BigInt.asUintN(64, low).toString(16).padStart(16, '0');
    let gauge;
    for (;;) {
      const u = Math.floor(this.bits(32) / 2); gauge = u % 501;
      if (u + 500 - gauge <= 2147483647) break;
    }
    return { id: `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12)}-${l.slice(0, 4)}-${l.slice(4)}`, gauge };
  }
  unitSeed() { return Number(this.long() & ((1n << 48n) - 1n)); }
}

function damageMultipliers(defenses) {
  return ATTACKS.map((_, index) => {
    let effective = 0;
    for (const defense of DEFENSES) effective += (defenses[defense] || 0) * MATRIX[defense][index];
    return 1 - effective / (effective + 500);
  });
}
function metricsFromSnapshot(snapshot) {
  check(Number.isInteger(snapshot.health) && snapshot.health > 0, 'prepared actor positive maximum health');
  check(Object.keys(snapshot.damageTakenMultipliers).length === 4, 'complete mitigation map');
  const calculated = damageMultipliers(snapshot.defenses);
  ATTACKS.forEach((type, index) => close(snapshot.damageTakenMultipliers[type], calculated[index], 'actual mitigation from defense matrix'));
  const mu = stable(calculated) / 4;
  const maximumAttack = Math.max(0, ...snapshot.attacksByRange.map(profile => {
    check(Number.isInteger(profile.range) && profile.range >= 1, 'positive range profile');
    check(Object.values(profile.attack).every(x => Number.isInteger(x) && x >= 0), 'nonnegative attack components');
    return stable(Object.values(profile.attack));
  }));
  check(Number.isInteger(snapshot.initiative) && snapshot.initiative >= 0, 'speed domain');
  const critical = 1 + Math.max(0, Math.min(1, snapshot.critChance / 100)) * (snapshot.critMultiplier - 1);
  const frequency = Math.min(snapshot.initiative, 1000) / 1000;
  counts.actorMetrics++;
  return { hp: snapshot.health, mu, H: snapshot.health / mu, maximumAttack,
    criticalFactor: critical, frequency, D: maximumAttack * critical * frequency };
}
function compareParts(parts, snapshots) {
  return parts.map((part, index) => {
    check(part.id === snapshots[index].id, 'ordered metric identity');
    const calculated = metricsFromSnapshot(snapshots[index]);
    for (const key of Object.keys(calculated)) close(part[key], calculated[key], `stored actor component ${key}`);
    check(digest(part.skills) === digest(snapshots[index].skills), 'metric skills equal actual snapshot');
    return calculated;
  });
}
function totals(parts) { return { H: stable(parts.map(p => p.H)), D: stable(parts.map(p => p.D)) }; }

function grow(base, item) {
  check(Number.isInteger(base) && base >= 0, 'nonnegative item channel');
  if (item.enhanceLevel === 0 || base === 0) return base;
  check(item.object.progressionVersion === 'V1', 'upgraded fixture uses independently checked V1 progression');
  return base + Math.floor((item.enhanceLevel * base + 30) / 60);
}
function preparedGear(member, effects) {
  let health = 0, critChance = 0, dodgeChance = 0, critMultiplier = 1.2, speed = 0, threat = 0, impact = 80;
  const defenses = {}, skills = {}, itemSnapshots = [];
  let maxRange = 1, sumVisibleAttack = 0;
  const parts = [];
  for (const item of member.items) {
    const object = item.object;
    health += grow(object.health, item); critChance += object.critChance; dodgeChance += object.dodgeChance;
    critMultiplier += object.critMultiplier; speed += object.speed; threat += object.baseThreat; impact += object.impact;
    if (object.defense) defenses[object.defense.defenseType] = (defenses[object.defense.defenseType] || 0) + grow(object.defense.defense, item);
    const attacks = object.attacks.map(a => ({ ...a, attack: grow(a.attack, item) })); parts.push(...attacks);
    for (const attack of attacks) maxRange = Math.max(maxRange, attack.maxRange);
    let visible = 0;
    for (let distance = 1; distance <= maxRange; distance++) visible = Math.max(visible,
      attacks.filter(a => a.minRange <= distance && distance <= a.maxRange).reduce((sum, a) => sum + a.attack, 0));
    sumVisibleAttack += visible;
    if (item.modifier && POINTS[item.rarity] > 0) skills[item.modifier.activeEnum] = (skills[item.modifier.activeEnum] || 0) + POINTS[item.rarity] * object.slots.length;
    if (object.code !== null) itemSnapshots.push({ code: object.code, rarity: item.rarity, skill: item.modifier ? item.modifier.activeEnum : null });
  }
  const factor = effects && sumVisibleAttack > 0 ? Math.trunc(sumVisibleAttack * 1.2) / sumVisibleAttack : 1;
  if (effects) health = Math.trunc(health * 1.15);
  const profiles = [];
  for (let distance = 1; distance <= maxRange; distance++) {
    const attack = {};
    for (const part of parts) if (part.minRange <= distance && distance <= part.maxRange) attack[part.attackType] = (attack[part.attackType] || 0) + part.attack;
    if (effects) for (const type of Object.keys(attack)) attack[type] = Math.max(0, Math.trunc(attack[type] * factor));
    profiles.push({ range: distance, attack });
  }
  return { health, critChance, dodgeChance, critMultiplier, initiative: speed, totalThreat: threat,
    impactStrength: Math.min(100, impact), defenses, attacksByRange: profiles, range: maxRange,
    targetingTactic: member.tactic, items: itemSnapshots,
    skills: Object.keys(skills).sort().map(code => ({ code, points: skills[code], formulaVersion: 'SCALING_SKILLS_V3' })) };
}
function checkPlayer(snapshot, member, effects) {
  const expected = preparedGear(member, effects);
  for (const key of ['health', 'critChance', 'dodgeChance', 'initiative', 'totalThreat', 'impactStrength', 'range', 'targetingTactic'])
    check(snapshot[key] === expected[key], `prepared item statistic ${key}`);
  close(snapshot.critMultiplier, expected.critMultiplier, 'prepared critical multiplier');
  for (const key of ['defenses', 'attacksByRange', 'items']) check(digest(snapshot[key]) === digest(expected[key]), `prepared items ${key}`);
  const skills = snapshot.skills.toSorted((a, b) => a.code.localeCompare(b.code));
  check(digest(skills) === digest(expected.skills), 'actual player V3 skill points from item rarity/slots');
  counts.playerPreparedFromItems++;
}
function npc(role, hpMultiplier, attackMultiplier) {
  const g = role === 'GUARDIAN';
  return { health: Math.max(1, Math.ceil((g ? 420 : 280) * hpMultiplier)),
    attack: Math.max(1, Math.ceil((g ? 70 : 130) * attackMultiplier)), defense: g ? 707 : 309,
    speed: g ? 190 : 160, critChance: g ? 4 : 8, dodgeChance: g ? 3 : 5,
    critMultiplier: g ? 1.5 : 1.7, threat: g ? 35 : 15, range: g ? 1 : 3,
    skill: g ? 'KNOCKBACK' : 'PRECISE_STRIKE', position: g ? 'FRONT' : 'BACK' };
}
function natural(count) {
  const parts = Array.from({ length: count }, (_, index) => {
    const n = npc(index < Math.floor(count / 2) ? 'GUARDIAN' : 'SLINGER', 1, 1);
    const mu = stable(damageMultipliers({ ARCANE: n.defense })) / 4;
    return { H: n.health / mu, D: n.attack * (1 + n.critChance / 100 * (n.critMultiplier - 1)) * n.speed / 1000 };
  });
  return totals(parts);
}
function initialGeometry(playerPositions, playerRanges, npcPositions, npcRanges, reverse) {
  const teamLines = positions => ['BACK', ...(positions.some(p => p === 'MID' || p === 'FRONT') ? ['MID'] : []), ...(positions.includes('FRONT') ? ['FRONT'] : [])];
  const pLines = teamLines(playerPositions), nLines = teamLines(npcPositions);
  const first = reverse ? pLines : nLines, second = (reverse ? nLines : pLines).toReversed();
  const total = first.length + second.length;
  function actor(position, range, isFirst) {
    let at = isFirst ? first.indexOf(position) : first.length + second.indexOf(position);
    const nearest = isFirst ? first.length : first.length - 1;
    if (Math.abs(at - nearest) > range) at = isFirst
      ? Math.min(first.length - 1, Math.max(0, nearest - range))
      : Math.max(first.length, Math.min(total - 1, nearest + range));
    return at;
  }
  return { players: playerPositions.map((p, i) => actor(p, playerRanges[i], reverse)),
    npcs: npcPositions.map((p, i) => actor(p, npcRanges[i], !reverse)) };
}

// Frozen confirmation inventory independently specified from prebattle policy, not copied from producer output.
const inventory = [];
function add(groupId, style, tier, size, variant) { inventory.push({ id: inventory.length + 1,
  groupId, style, tier, originalSize: size, level: 25, variant, effects: false }); }
for (const attack of ['PIERCE', 'SLASH']) for (const defense of ['ARCANE', 'LEATHER']) {
  add(1, `COUNTER_${attack}_${defense}`, 'MID', 3, 'BASE');
  add(1, `COUNTER_${attack}_${defense}`, 'MID', 3, 'NORMAL_APPEND');
}
add(2, 'MAIN', 'MID', 3, 'BASE'); add(2, 'MAIN', 'MID', 3, 'NORMAL_APPEND');
add(3, 'RITUAL', 'STRONG', 1, 'BASE'); add(3, 'RITUAL', 'STRONG', 1, 'WEAK_APPEND');
check(inventory.length === 12 && inputs.length === 12, 'approved last confirmation size');
inputs.forEach((cell, index) => check(digest(Object.fromEntries(Object.keys(inventory[index]).map(k => [k, cell[k]]))) === digest(inventory[index]), 'approved exact confirmation inventory'));
check(policy.conditions === 12 && policy.mirroredPairsPerCondition === PAIRS && policy.executions === 24000, 'explicit frozen inference size');
check(digest(policy.models) === digest(MODELS), 'explicit frozen model order');
check(digest(policy.families.append.comparisonsPerModel) === digest([[1,2],[3,4],[5,6],[7,8],[9,10],[11,12]]), 'frozen append family');
check(digest(policy.families.counterChoice.comparisonsPerModel) === digest([[5,1],[3,1],[7,1],[6,2],[4,2],[8,2]]), 'frozen counter family');
check(policy.generationNamespace === 'MYCONID-15.4.1-SPLIT-CONFIRM-GEN-20261108-v1'
  && policy.battleNamespace === 'MYCONID-15.4.1-SPLIT-CONFIRM-BATTLE-20261108-v1', 'explicit fresh namespace agreement');
check(frozen.generationNamespace === policy.generationNamespace && frozen.battleNamespace === policy.battleNamespace, 'manifest namespace agreement');
// Retag changes only declared attack/defense types, including appended fourth actor.
function stripTypes(members) {
  const result = structuredClone(members);
  for (const member of result) for (const item of member.items) {
    for (const attack of item.object.attacks) delete attack.attackType;
    if (item.object.defense) delete item.object.defense.defenseType;
  }
  return result;
}
for (const cell of inputs.filter(c => c.style.startsWith('COUNTER_'))) {
  const original = inputs[cell.variant === 'BASE' ? 8 : 9];
  check(digest(stripTypes(cell.members)) === digest(stripTypes(original.members)), 'counter exact same numeric items/skills/position/tactic and order');
  const [, attack, defense] = cell.style.split('_');
  for (const member of cell.members) for (const item of member.items) {
    check(item.object.attacks.every(a => a.attackType === attack), 'all counter attacks retagged including new actor');
    check(!item.object.defense || item.object.defense.defenseType === defense, 'all counter defenses retagged including new actor');
  }
}
for (const name of ['manifest', 'inputs', 'seeds']) check(digest(read(`screen/${name}.json`)) === digest(read(`frozen-preparation/${name}.json`)), 'frozen preparation equals actual screen');
const freezeLines = fs.readFileSync(path.join(ROOT, 'prebattle-freeze.sha256'), 'utf8').trim().split('\n');
const originalRoot = path.dirname(freezeLines[0].split(/\s+/, 2)[1]);
for (const line of freezeLines) {
  const [expected, file] = line.split(/\s+/, 2);
  const relative = path.relative(originalRoot, file);
  check(!relative.startsWith('..') && !path.isAbsolute(relative), 'freeze entry inside producing root');
  check(hashFile(path.join(ROOT, relative)) === expected, 'prebattle source/input hash unchanged after portable root resolution');
}
check(frozen.conditions === 12 && frozen.pairs === PAIRS && frozen.battles === 24000, 'manifest last confirmation counts');
check(seeds.length === 3 * PAIRS, 'all1500 pre-dumped seed pairs');
const usedSeeds = new Set();
for (const entry of seeds) {
  for (const [key, namespace] of [['generationSeed', frozen.generationNamespace], ['battleSeed', frozen.battleNamespace]]) {
    check(entry[key] === shaSeed(namespace, entry.groupId, entry.pair), 'pre-dumped SHA48 seed');
    check(!usedSeeds.has(entry[key]), 'no collisions across generation/battle namespaces'); usedSeeds.add(entry[key]);
  }
}
for (let count = 2; count <= 30; count++) {
  const calculated = natural(count); close(frozen.naturalByCount[count].H, calculated.H, 'natural HP coefficient');
  close(frozen.naturalByCount[count].D, calculated.D, 'natural damage coefficient');
}
close(frozen.anchors.referencePlayers.H, REF.Hplayer, 'frozen player H anchor');
close(frozen.anchors.referencePlayers.D, REF.Dplayer, 'frozen player D anchor');
close(frozen.anchors.referenceNPC.H, REF.Hnpc, 'frozen NPC H anchor');
close(frozen.anchors.referenceNPC.D, REF.Dnpc, 'frozen NPC D anchor');
const inputById = new Map(inputs.map(c => [c.id, c]));
const bases = new Map(inputs.filter(c => c.variant === 'BASE').map(c => [`${c.groupId}/${c.style}`, c]));
const states = new Map(inputs.flatMap(c => MODELS.map(model => [`${c.id}/${model}`, { id: c.id, model, rows: 0, wins: 0, losses: 0, draws: 0,
  rounds: 0, pairScores: Array.from({ length: PAIRS }, () => 0), seen: new Set(), firstDistances: {}, representative: null,
  recordedMemberContributions: c.members.map(member => ({ build: member.build, turns: 0, normalDamage: 0, criticalDamage: 0,
    skillDamage: 0, died: 0, zeroTurns: 0 })) }])));
const mirrorReferences = new Map(), prefixReferences = new Map(), twinReferences = new Map(), anchorReferences = new Map(), anchorBattleReferences = new Map();
const compressedHash = crypto.createHash('sha256'), plainHash = crypto.createHash('sha256');
let plainBytes = 0, rowCount = 0;
const compressed = fs.createReadStream(path.join(ROOT, 'screen/battles.jsonl.gz'));
compressed.on('data', chunk => compressedHash.update(chunk));
const unzip = compressed.pipe(zlib.createGunzip());
unzip.on('data', chunk => { plainHash.update(chunk); plainBytes += chunk.length; });
const lines = readline.createInterface({ input: unzip, crlfDelay: Infinity });

async function main() {
  for await (const line of lines) {
    const row = JSON.parse(line), cell = row.case, state = states.get(`${cell.id}/${row.model}`);
    check(state && digest(cell) === digest(inputById.get(cell.id)), 'raw complete case equals frozen input');
    check(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < PAIRS && typeof row.reverse === 'boolean', 'raw pair/reverse domain');
    const key = `${row.pair}/${row.reverse}`; check(!state.seen.has(key), 'unique raw mirror'); state.seen.add(key);
    check(row.generationSeed === shaSeed(frozen.generationNamespace, cell.groupId, row.pair)
      && row.battleSeed === shaSeed(frozen.battleNamespace, cell.groupId, row.pair), 'raw SHA48 cohort');
    check(row.outcome !== 'TIMEOUT' && row.initState.tempoVersion === 'ACCELERATED_V1', 'finished accelerated battle');
    const count = Math.max(2, cell.members.length), front = Math.floor(count / 2);
    check(row.playerParts.length === cell.members.length && row.enemyParts.length === count && row.unitSeeds.length === count, 'actual actor counts');
    const byId = row.initState.personagesById;
    const players = row.playerParts.map(p => byId[p.id]), enemies = row.enemyParts.map(p => byId[p.id]);
    check(players.every(Boolean) && enemies.every(Boolean) && Object.keys(byId).length === players.length + enemies.length, 'all actors present exactly once');
    check(new Set([...players, ...enemies].map(p => p.id)).size === players.length + enemies.length, 'unique actor IDs');
    check(players.every(p => p.firstTeam === row.reverse) && enemies.every(p => p.firstTeam !== row.reverse), 'mirror side labels');
    const generator = new JavaRandom(row.generationSeed);
    players.forEach((player, index) => {
      const expected = generator.actor(); check(player.id === expected.id && player.initiativeGauge === expected.gauge, 'exact player UUID/gauge from GEN'); counts.exactPlayerRng++;
      checkPlayer(player, cell.members[index], cell.effects);
    });
    const expectedUnits = Array.from({ length: count }, () => generator.unitSeed());
    check(digest(row.unitSeeds) === digest(expectedUnits) && new Set(row.unitSeeds).size === count, 'exact isolated per-unit seeds after players'); counts.exactUnitSeeds++;
    enemies.forEach((enemy, index) => {
      const expected = new JavaRandom(expectedUnits[index]).actor(); check(enemy.id === expected.id && enemy.initiativeGauge === expected.gauge, 'exact NPC UUID/gauge from replayed unitseed'); counts.exactNpcRng++;
    });
    const pp = compareParts(row.playerParts, players), np = compareParts(row.enemyParts, enemies), p = totals(pp), n = totals(np);
    close(row.playerMetrics.H, p.H, 'raw player H total'); close(row.playerMetrics.D, p.D, 'raw player D total');
    const cal = row.calibration, level = Math.sqrt(q(cell.level)) / Math.sqrt(q(25));
    const h = p.H / REF.Hplayer, d = p.D / REF.Dplayer;
    const targetH = REF.Hnpc * level * (row.model === 'SAME' ? h : d), targetD = REF.Dnpc * level * (row.model === 'SAME' ? d : h);
    const nat = natural(count), hp = targetH / nat.H, attack = targetD / nat.D;
    for (const [field, value] of Object.entries({ levelScale: level, hRatio: h, dRatio: d, targetH, targetD, hpMultiplier: hp, attackMultiplier: attack, achievedH: n.H, achievedD: n.D })) close(cal[field], value, `calibration ${field}`);
    check(cal.policy === 'MIN1_CEIL_CLOSED_FORM' && Number.isFinite(cal.hpMultiplier) && cal.hpMultiplier >= 0
      && Number.isFinite(cal.attackMultiplier) && cal.attackMultiplier >= 0, 'independent finite nonnegative axes');
    enemies.forEach((enemy, index) => {
      const expected = npc(index < front ? 'GUARDIAN' : 'SLINGER', cal.hpMultiplier, cal.attackMultiplier);
      check(enemy.health === expected.health && enemy.health <= LIMIT, 'NPC weight1 HP single ceil');
      check(digest(enemy.defenses) === digest({ ARCANE: expected.defense }), 'fixed ARCANE defense');
      const profiles = Array.from({ length: expected.range }, (_, i) => ({ range: i + 1, attack: { MAGICAL: expected.attack } }));
      check(expected.attack <= LIMIT && digest(enemy.attacksByRange) === digest(profiles), 'NPC weight1 attack single ceil and actual V3 range');
      check(enemy.critChance === expected.critChance && enemy.dodgeChance === expected.dodgeChance && enemy.critMultiplier === expected.critMultiplier
        && enemy.initiative === expected.speed && enemy.totalThreat === expected.threat && enemy.range === expected.range
        && enemy.impactStrength === 80 && enemy.targetingTactic === 'THREAT', 'unchanged NPC secondary statistics');
      check(digest(enemy.skills) === digest([{ code: expected.skill, points: 4, formulaVersion: 'SCALING_SKILLS_V3' }]), 'actual four-point V3 NPC skill');
      check(row.actualEnemyPositions[index] === expected.position, 'actual NPC relative row'); counts.npcFactory++;
    });
    const boundH = stable(np.map(a => 1 / a.mu)), boundD = stable(np.map(a => a.criticalFactor * a.frequency));
    close(cal.healthRoundingBound, boundH, 'HP rounding bound'); close(cal.damageRoundingBound, boundD, 'damage rounding bound');
    check(n.H + 1e-8 >= targetH && n.H <= targetH + boundH + 1e-7 && n.D + 1e-8 >= targetD && n.D <= targetD + boundD + 1e-7, 'achieved goals within separate rounding bounds');
    const geometry = initialGeometry(cell.members.map(m => m.position), players.map(p => p.range), enemies.map((_, i) => i < front ? 'FRONT' : 'BACK'), enemies.map(p => p.range), row.reverse);
    players.forEach((player, i) => check(player.lineIndex === geometry.players[i], 'player initial geometry'));
    enemies.forEach((enemy, i) => check(enemy.lineIndex === geometry.npcs[i], 'NPC initial geometry'));
    const distances = enemies.map(enemy => Math.abs(enemy.lineIndex - players[0].lineIndex));
    state.firstDistances[JSON.stringify(distances)] = (state.firstDistances[JSON.stringify(distances)] || 0) + 1;

    const mirrorKey = `${cell.id}/${row.model}/${row.pair}`;
    const allTraits = digest({ p: players.map(x => withoutGeometry(x, true)), n: enemies.map(x => withoutGeometry(x, true)), unitSeeds: row.unitSeeds });
    if (!row.reverse) mirrorReferences.set(mirrorKey, allTraits);
    else { check(mirrorReferences.get(mirrorKey) === allTraits, 'fresh mirrors exact identity/gauge/traits'); mirrorReferences.delete(mirrorKey); counts.mirroredTraits++; }
    const prefixKey = `${cell.groupId}/${cell.style}/${row.model}/${row.pair}/${row.reverse}`;
    if (cell.variant === 'BASE') prefixReferences.set(prefixKey, digest(players.map(x => withoutGeometry(x))));
    if (cell.variant.endsWith('APPEND')) {
      const base = bases.get(`${cell.groupId}/${cell.style}`);
      check(digest(cell.members.slice(0, base.members.length)) === digest(base.members), 'frozen original input prefix retained');
      check(prefixReferences.get(prefixKey) === digest(players.slice(0, base.members.length).map(x => withoutGeometry(x))), 'actual original player prefix retained'); counts.prefixes++;
    }
    if (cell.style.startsWith('COUNTER_')) {
      if (cell.variant === 'BASE') check(p.H === REF.Hplayer && p.D === REF.Dplayer, 'counter BASE exact neutral reference');
      const twinKey = `${cell.variant}/${row.model}/${row.pair}/${row.reverse}`;
      const current = digest({ enemies, unitSeeds: row.unitSeeds, cal, p });
      if (!twinReferences.has(twinKey)) twinReferences.set(twinKey, current);
      else { check(twinReferences.get(twinKey) === current, 'all four countertypes exact H/D and NPC traits/UUID/gauge/calibration within each model/count'); counts.counterTwinComparisons++; }
    }
    if (cell.variant === 'BASE' && cell.originalSize === 3) {
      const anchorKey = `${cell.id}/${row.pair}/${row.reverse}`;
      const current = digest({ enemies, unitSeeds: row.unitSeeds, cal });
      const battleCurrent = digest({ initState: row.initState, outcome: row.outcome, rounds: row.rounds,
        actionSha256: row.actionSha256, traceSha256: row.traceSha256,
        playerStats: row.playerStats, enemyStats: row.enemyStats, actionCount: row.actionCount,
        normalAttempts: row.normalAttempts, moves: row.moves, forcedMoves: row.forcedMoves });
      if (!anchorReferences.has(anchorKey)) { anchorReferences.set(anchorKey, current); anchorBattleReferences.set(anchorKey, battleCurrent); }
      else {
        check(anchorReferences.get(anchorKey) === current, 'SAME/CROSS N3 actual anchor exact traits/UUID/gauge/calibration'); counts.sameCrossAnchorComparisons++;
        check(anchorBattleReferences.get(anchorKey) === battleCurrent, 'SAME/CROSS N3 entire initial state and final stats/action/trace identical, not independent evidence'); counts.exactSameCrossBattles++;
      }
    }
    cell.members.forEach((member, index) => {
      const isWeak = cell.variant === 'WEAK_APPEND' && index === cell.members.length - 1 || cell.style === 'WEAK_ONLY' || cell.style === 'STRONG_PLUS_WEAK' && index > 0;
      if (isWeak) {
        check(member.items.length === 7 && member.items.every(i => i.rarity === 'COMMON' && i.enhanceLevel === 0 && i.modifier === null), 'seven COMMON+0 modifierless weak inputs');
        check(new Set(member.items.flatMap(i => i.object.slots)).size === 7 && players[index].health === 840 && players[index].skills.length === 0, 'actual weak seven slots alive840HP'); counts.weakActors++;
      }
    });
    check(digest(row.sourceEffects) === digest(row.combatEffects), 'source/combat effects agree');
    if (cell.effects) {
      check(Object.keys(row.sourceEffects).length === 2, 'two actual permanent effect types');
      const effects = Object.values(row.sourceEffects).map(x => x.effect).map(e => `${e.characteristic}/${e.percent}`).sort();
      check(digest(effects) === digest(['ATTACK/20', 'HEALTH/15']) && Object.values(row.sourceEffects).every(x => x.expireDateTime === null), 'actual20attack15HP permanent effects');
    } else check(Object.keys(row.sourceEffects).length === 0, 'no unintended effects');
    check(row.playerStats.length === players.length && row.enemyStats.length === enemies.length, 'final-stat actor counts');
    [...row.playerStats, ...row.enemyStats].forEach((stat, i) => {
      const initial = [...players, ...enemies][i]; check(stat.initialHealth === initial.health
        && Number.isInteger(stat.remainHealth) && stat.remainHealth >= 0 && stat.remainHealth <= initial.health, 'final HP valid against exact initial actor');
    });
    row.playerStats.forEach((stat, i) => {
      const total = state.recordedMemberContributions[i];
      for (const key of ['turnsCount', 'normalDamageDealt', 'critDamageDealt', 'skillDamageDealt'])
        check(Number.isInteger(stat[key]) && stat[key] >= 0, 'recorded contribution nonnegative integer domain');
      total.turns += stat.turnsCount; total.normalDamage += stat.normalDamageDealt;
      total.criticalDamage += stat.critDamageDealt; total.skillDamage += stat.skillDamageDealt;
      total.died += stat.remainHealth === 0 ? 1 : 0; total.zeroTurns += stat.turnsCount === 0 ? 1 : 0;
    });
    const pa = row.playerStats.some(p => p.remainHealth > 0), ea = row.enemyStats.some(p => p.remainHealth > 0);
    check(!(pa && ea), 'completed battle cannot leave both teams alive');
    const outcome = pa ? 'WIN' : ea ? 'LOSS' : 'DRAW'; check(row.outcome === outcome, 'outcome from final HP'); counts.outcomesFromFinalHp++;
    check(Number.isInteger(row.rounds) && row.rounds > 0 && row.rounds <= 2000, 'finished round domain');
    check(/^[0-9a-f]{64}$/.test(row.actionSha256) && /^[0-9a-f]{64}$/.test(row.traceSha256), 'action/trace hashes present');
    state.rows++; state.rounds += row.rounds; rowCount++;
    if (outcome === 'WIN') { state.wins++; state.pairScores[row.pair] += 0.5; }
    else if (outcome === 'LOSS') state.losses++;
    else { state.draws++; state.pairScores[row.pair] += 0.25; }
    if (row.pair === 0 && !row.reverse) {
      const guardian = npc('GUARDIAN', cal.hpMultiplier, cal.attackMultiplier), slinger = npc('SLINGER', cal.hpMultiplier, cal.attackMultiplier);
      state.representative = { playerH: p.H, playerD: p.D, targetH, targetD, achievedH: n.H, achievedD: n.D,
        hpMultiplier: cal.hpMultiplier, attackMultiplier: cal.attackMultiplier,
        playerComponents: pp,
        guardian: { count: front, hp: guardian.health, attack: guardian.attack, defense: guardian.defense },
        slinger: { count: count - front, hp: slinger.health, attack: slinger.attack, defense: slinger.defense } };
    }
  }
  check(rowCount === 24000 && mirrorReferences.size === 0, 'complete24000 rows');
  check(summary.cases.length === 24, 'all12conditions both models in saved summary');
  const outputStates = [];
  for (const saved of summary.cases) {
    const state = states.get(`${saved.case.id}/${saved.model}`);
    check(state && state.rows === 2 * PAIRS && state.seen.size === 2 * PAIRS && saved.pairs === PAIRS, 'all1000 mirrors per condition-model');
    check(digest(saved.case) === digest(inputById.get(saved.case.id)), 'saved summary input');
    check(digest(saved.pairScores) === digest(state.pairScores), 'all raw pair vectors equal summary');
    check((saved.outcomes.WIN || 0) === state.wins && (saved.outcomes.LOSS || 0) === state.losses && (saved.outcomes.DRAW || 0) === state.draws
      && !('TIMEOUT' in saved.outcomes), 'all saved outcome counts from raw');
    const mean = state.pairScores.reduce((a, b) => a + b, 0) / PAIRS, half = Math.sqrt(Math.log(40) / (2 * PAIRS));
    close(saved.pairMean, mean, 'saved pair mean'); close(saved.winrate, state.wins / (2 * PAIRS), 'saved winrate');
    close(saved.meanRounds, state.rounds / (2 * PAIRS), 'saved mean rounds');
    close(saved.pointwise95Hoeffding[0], Math.max(0, mean - half), 'pointwise mean lower');
    close(saved.pointwise95Hoeffding[1], Math.min(1, mean + half), 'pointwise mean upper');
    delete state.seen; state.winrate = state.wins / state.rows; state.meanRounds = state.rounds / state.rows;
    outputStates.push(state);
  }
  const families = {};
  for (const name of ['append', 'counterChoice']) {
    const family = policy.families[name];
    check(family.comparisonsIncludingBothModels === 12 && family.alpha === 0.05, 'fixed independent family scope');
    const half = Math.sqrt(2 * Math.log(2 * 12 / family.alpha) / PAIRS);
    families[name] = { comparisons: 12, simultaneousCoverage: 0.95, independentPairs: PAIRS,
      halfWidth: half, note: 'Separate family95 coverage; no joint95 coverage across both families.',
      results: MODELS.flatMap(model => family.comparisonsPerModel.map(([base, added]) => {
        const a = states.get(`${base}/${model}`), b = states.get(`${added}/${model}`);
        const differences = b.pairScores.map((x, i) => x - a.pairScores[i]);
        check(differences.every(x => Number.isFinite(x) && x >= -1 && x <= 1), 'paired difference domain');
        const mean = differences.reduce((sum, x) => sum + x, 0) / PAIRS;
        return { model, first: base, second: added, firstWinrate: a.winrate, secondWinrate: b.winrate,
          pairedMeanDifference: mean, simultaneousInterval: [Math.max(-1, mean - half), Math.min(1, mean + half)],
          pairDifferences: differences };
      })) };
  }
  const report = { status: 'PASS', purpose: 'INDEPENDENT_COMPLETE_24000_RAW_VALIDATION_NO_NEW_BATTLES',
    root: ROOT, rows: rowCount, assertions, counts, rawGzipSha256: compressedHash.digest('hex'),
    rawPlainSha256: plainHash.digest('hex'), rawPlainBytes: plainBytes,
    producerSha256: hashFile(path.join(ROOT, 'Step1541SeparateScalingProbe.java')),
    validatorSha256: hashFile(__filename), policySha256: hashFile(path.join(ROOT, 'inference-policy.json')), states: outputStates, families,
    independentMethods: ['frozen12conditionAgreementAndTwoSeparate12ComparisonFamilies', 'SHA48Namespaces', 'JDK25Random48Reimplementation',
      'playerItemV1ProgressionAndActualPreparedEffects', 'defenseMatrixFromActualSources', 'sortedMuAndMaxRangeProfile',
      'naturalWeight1FactoryAndIndependentHPAxes', 'actualNPCFourPointV3Skills', 'initialLineGeometry',
      'countertwinsAndCrossModelAnchorIdentity', 'unchangedPlayerPrefixes', 'finalHPOutcomesAndAllPairScores'],
    limitations: ['No new Java or battle executions, no production/target/DB changes.',
      'Action/trace hashes are present, but full action logs are unavailable for independent engine replay.',
      '500fresh mirrored pairs per condition do not imply universal balance acceptance or a50percent target; the two families separately have95 coverage, not jointly.',
      'The changes combine fixedDEF, split axes, natural role distribution and removed group corrections; outcomes do not isolate one cause.'] };
  process.stdout.write(JSON.stringify(report) + '\n');
}
main().catch(error => { process.stderr.write(error.stack + '\n'); process.exitCode = 1; });

