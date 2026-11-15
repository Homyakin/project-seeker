'use strict';

// Independent read-only audit of saved final17k: raw paired outcomes, fixed NPC identity and geometry.
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const root = process.argv[2] || '/tmp/seeker-step15-4-composition-confirm.O9o40T';
const read = relative => JSON.parse(fs.readFileSync(path.join(root, relative), 'utf8'));
const summary = read('screen/summary.json');
const analysis = read('screen/analysis.json');
const manifest = read('screen/manifest.json');
const inputs = read('screen/inputs.json');
const policy = read('inference-policy.json');
const pairs = 500;
const family = 9;
const pointwiseHalf = 100 * Math.sqrt(2 * Math.log(40) / pairs);
const familyHalf = 100 * Math.sqrt(2 * Math.log(2 * family / 0.05) / pairs);
let assertions = 0;
function check(condition, message) { assertions++; assert.ok(condition, message); }
function close(actual, expected, message) {
  check(Number.isFinite(actual) && Math.abs(actual - expected) <= 1e-12, message);
}
function canonical(value, key) {
  if (Array.isArray(value)) {
    const result = value.map(item => canonical(item));
    return ['slots', 'availableOnSlots'].includes(key) ? result.sort() : result;
  }
  if (value && typeof value === 'object') {
    return Object.fromEntries(Object.keys(value).sort().map(k => [k, canonical(value[k], k)]));
  }
  return value;
}
function digest(value) {
  return crypto.createHash('sha256').update(JSON.stringify(canonical(value))).digest('hex');
}
function snapshotWithoutGeometry(snapshot, mirror = false) {
  const result = { ...snapshot };
  delete result.lineIndex;
  if (mirror) { delete result.firstTeam; delete result.advanceDirection; }
  return result;
}
function seed(namespace, group, pair) {
  const raw = crypto.createHash('sha256').update(`${namespace}/${group}/${pair}`).digest();
  return raw.readUIntBE(0, 6);
}
function ownScore(parts) {
  const sorted = parts.map(part => part.selfProduct).sort((a, b) => a - b);
  return sorted.reduce((total, p, index) => total + (2 * (sorted.length - index) - 1) * p, 0);
}
function damage(stats) { return stats.normalDamageDealt + stats.critDamageDealt + stats.skillDamageDealt; }

check(summary.cases.length === 17 && analysis.cases.length === 17, '17 conditions');
check(policy.conditions === 17 && policy.pairsPerCondition === pairs
  && policy.pairedComparisons === family, 'fixed size and confidence family');
check(fs.readFileSync(path.join(root, 'preparation/inputs.json'), 'utf8')
  === fs.readFileSync(path.join(root, 'screen/inputs.json'), 'utf8'), 'predumped inputs unchanged');
check(fs.readFileSync(path.join(root, 'preparation/seeds.json'), 'utf8')
  === fs.readFileSync(path.join(root, 'screen/seeds.json'), 'utf8'), 'predumped seeds unchanged');
const summaryById = new Map(summary.cases.map(item => [item.case.id, item]));
const inputsById = new Map(inputs.map(item => [item.id, item]));
const bases = new Map(inputs.filter(item => item.variant === 'BASE').map(item => [item.groupId, item]));
const rawCases = new Map(inputs.map(item => [item.id, {
  id: item.id,
  rows: 0,
  wins: 0,
  losses: 0,
  rounds: 0,
  pairScores: Array.from({ length: pairs }, () => 0),
  seen: new Set(),
  firstPlayerDistanceCounts: {},
  firstPlayerDamage: 0,
  firstPlayerTurns: 0,
  appendedDamage: 0,
  appendedTurns: 0,
  appendedNormalAttackCount: 0,
  representative: null,
} ]));
const fixedReferences = new Map();
const mirrors = new Map();
const geometryComparisons = new Map();
const compressedHash = crypto.createHash('sha256');
const plainHash = crypto.createHash('sha256');
let plainBytes = 0;
let rows = 0;
let fixedNpcComparisons = 0;
let fixedPlayerPrefixComparisons = 0;
let mirroredConstructionComparisons = 0;
let minimumWeakHp = Infinity;
let maximumWeakHp = -Infinity;
const compressed = fs.createReadStream(path.join(root, 'screen/battles.jsonl.gz'));
compressed.on('data', chunk => compressedHash.update(chunk));
const uncompressed = compressed.pipe(zlib.createGunzip());
uncompressed.on('data', chunk => { plainHash.update(chunk); plainBytes += chunk.length; });
const lines = readline.createInterface({ input: uncompressed, crlfDelay: Infinity });

async function main() {
  for await (const line of lines) {
    const row = JSON.parse(line);
    const cell = row.case;
    const savedInput = inputsById.get(cell.id);
    const current = rawCases.get(cell.id);
    check(current && digest(cell) === digest(savedInput), 'raw input equals frozen complete input');
    check(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < pairs
      && typeof row.reverse === 'boolean', 'pair/reverse domain');
    const pairKey = `${row.pair}/${row.reverse}`;
    check(!current.seen.has(pairKey), 'raw mirror unique'); current.seen.add(pairKey);
    check(row.model === 'B_NORM_TWO_ANCHOR', 'one frozen candidate');
    check(row.generationSeed === seed(manifest.generationNamespace, cell.groupId, row.pair), 'GEN SHA48');
    check(row.battleSeed === seed(manifest.battleNamespace, cell.groupId, row.pair), 'BATTLE SHA48');
    check(row.npcGenerationSeed === seed(manifest.npcNamespace, cell.groupId, row.pair), 'NPC SHA48');
    check(row.npcSeparateSeedUsed === cell.diagnosticFixedNpc, 'separate NPC seed is diagnostic-only');
    check(row.C === manifest.anchors.Cnew && row.a === manifest.anchors.screenA, 'frozen common C/a');
    check(row.playerParts.length === cell.members.length && row.enemyParts.length === row.calibrationEnemyCount,
      'player/NPC counts');
    const base = bases.get(cell.groupId);
    const expectedCount = Math.max(2, cell.diagnosticFixedNpc ? base.members.length : cell.members.length);
    check(row.calibrationEnemyCount === expectedCount, 'actual or fixed-base NPC count');
    check(row.concentrationCorrection === manifest.fixedConcentrationCorrection.byCount[String(expectedCount)],
      'fixed composition coefficient');
    check(row.playerScore === ownScore(row.playerParts) && row.enemyScore === ownScore(row.enemyParts), 'exact B from stored personal components');
    if (!cell.diagnosticFixedNpc) check(row.calibrationParticipantScore === row.playerScore, 'actual path uses current participant score');

    const state = row.initState.personagesById;
    const playerSnapshots = row.playerParts.map(part => state[part.id]);
    const enemySnapshots = row.enemyParts.map(part => state[part.id]);
    check(playerSnapshots.every(Boolean) && enemySnapshots.every(Boolean), 'all ordered actors in init snapshot');
    check(new Set([...row.playerParts, ...row.enemyParts].map(part => part.id)).size
      === row.playerParts.length + row.enemyParts.length, 'no player/NPC identity collisions');
    check(playerSnapshots.every(item => item.firstTeam === row.reverse)
      && enemySnapshots.every(item => item.firstTeam !== row.reverse), 'correct mirror-side labels');
    const prefixLength = base.members.length;
    const geometry = playerSnapshots.slice(0, prefixLength).map(player =>
      enemySnapshots.map(enemy => Math.abs(player.lineIndex - enemy.lineIndex)));
    const geometryKey = JSON.stringify(geometry[0]);
    current.firstPlayerDistanceCounts[geometryKey] = (current.firstPlayerDistanceCounts[geometryKey] || 0) + 1;
    if (cell.variant === 'WEAK_APPEND') {
      const hp = row.playerParts.at(-1).hp;
      minimumWeakHp = Math.min(minimumWeakHp, hp); maximumWeakHp = Math.max(maximumWeakHp, hp);
      check(hp === 840 && row.playerParts.at(-1).skills.length === 0, 'actual living840HP modifierless weak');
    }
    const mirrorKey = `${cell.id}/${row.pair}`;
    const mirrorValue = digest({
      players: playerSnapshots.map(item => snapshotWithoutGeometry(item, true)),
      npcs: enemySnapshots.map(item => snapshotWithoutGeometry(item, true)),
    });
    if (!row.reverse) mirrors.set(mirrorKey, mirrorValue);
    else {
      check(mirrors.get(mirrorKey) === mirrorValue, 'fresh mirrored construction retains actor identity/gauge/traits');
      mirrors.delete(mirrorKey); mirroredConstructionComparisons++;
    }

    if (cell.diagnosticFixedNpc) {
      const key = `${cell.groupId}/${row.pair}/${row.reverse}`;
      const fingerprints = {
        npcs: digest(enemySnapshots.map(item => snapshotWithoutGeometry(item))),
        npcParts: digest(row.enemyParts),
        prefix: digest(playerSnapshots.slice(0, prefixLength).map(item => snapshotWithoutGeometry(item))),
        calibration: digest(row.calibration),
        calibrationScore: row.calibrationParticipantScore,
        enemyCount: row.calibrationEnemyCount,
        geometry,
      };
      if (cell.variant === 'BASE') fixedReferences.set(key, fingerprints);
      else {
        const reference = fixedReferences.get(key);
        check(Boolean(reference), 'fixed BASE already read');
        check(reference.npcs === fingerprints.npcs && reference.npcParts === fingerprints.npcParts,
          'fixed full ordered NPC identity/gauge/traits apart from absolute lineIndex'); fixedNpcComparisons++;
        check(reference.prefix === fingerprints.prefix, 'fixed original player identity/gauge/traits apart from absolute lineIndex');
        fixedPlayerPrefixComparisons++;
        check(reference.calibration === fingerprints.calibration
          && reference.calibrationScore === fingerprints.calibrationScore
          && reference.enemyCount === fingerprints.enemyCount, 'fixed BASE target/m/count');
        const comparisonKey = `${cell.groupId}/${cell.variant}`;
        const result = geometryComparisons.get(comparisonKey) || { rows: 0, unchanged: 0, changed: 0 };
        result.rows++;
        result[digest(reference.geometry) === digest(geometry) ? 'unchanged' : 'changed']++;
        geometryComparisons.set(comparisonKey, result);
      }
    }

    check(row.outcome === 'WIN' || row.outcome === 'LOSS', 'completed no draw/timeout');
    if (row.outcome === 'WIN') { current.wins++; current.pairScores[row.pair] += 0.5; }
    else current.losses++;
    current.rows++; current.rounds += row.rounds; rows++;
    current.firstPlayerDamage += damage(row.playerStats[0]);
    current.firstPlayerTurns += row.playerStats[0].turnsCount;
    if (cell.variant.endsWith('APPEND')) {
      current.appendedDamage += damage(row.playerStats.at(-1));
      current.appendedTurns += row.playerStats.at(-1).turnsCount;
      current.appendedNormalAttackCount += row.playerStats.at(-1).normalAttackCount;
    }
    if (row.pair === 0 && !row.reverse) {
      const roleRows = new Map();
      enemySnapshots.forEach((npc, index) => {
        const role = index < Math.floor(enemySnapshots.length / 2) ? 'GUARDIAN' : 'SLINGER';
        const key = digest({ hp: npc.health, attack: row.enemyParts[index].maximumAttack,
          defenses: npc.defenses, range: npc.range, speed: npc.initiative, skills: npc.skills });
        const existing = roleRows.get(key);
        if (existing) existing.count++;
        else roleRows.set(key, { role, count: 1, hp: npc.health,
          attack: row.enemyParts[index].maximumAttack, defenses: npc.defenses,
          range: npc.range, speed: npc.initiative, skills: npc.skills });
      });
      current.representative = {
        calibration: row.calibration,
        calibrationParticipantScore: row.calibrationParticipantScore,
        playerScore: row.playerScore,
        enemyScore: row.enemyScore,
        concentrationCorrection: row.concentrationCorrection,
        originalPlayerToEnemyDistanceMatrix: geometry.length <= 8 ? geometry : geometry.slice(0, 1),
        geometryMatrixRows: geometry.length,
        geometryMatrixSha256: digest(geometry),
        fullOrderedNpcTraitsExceptLineIndexSha256: digest(enemySnapshots.map(item => snapshotWithoutGeometry(item))),
        orderedNpcIdsSha256: digest(enemySnapshots.map(item => item.id)),
        orderedNpcGaugesSha256: digest(enemySnapshots.map(item => item.initiativeGauge)),
        npcs: [...roleRows.values()],
      };
    }
  }

  check(rows === 17000 && mirrors.size === 0, 'all17000 rows and8500 mirror constructions');
  for (const current of rawCases.values()) {
    const expected = summaryById.get(current.id);
    check(current.rows === 1000 && current.seen.size === 1000, '1000 unique rows per condition');
    check(current.wins === (expected.outcomes.WIN || 0) && current.losses === (expected.outcomes.LOSS || 0), 'raw outcomes equal saved summary');
    check(JSON.stringify(current.pairScores) === JSON.stringify(expected.pairScores), 'all500 raw pair scores equal saved vector');
    close(current.wins / 1000, expected.winrate, 'raw winrate');
    close(current.rounds / 1000, expected.meanRounds, 'raw mean rounds');
    delete current.seen;
    delete current.pairScores;
    current.winrate = current.wins / current.rows;
    current.meanFirstPlayerDamage = current.firstPlayerDamage / current.rows;
    current.meanFirstPlayerTurns = current.firstPlayerTurns / current.rows;
    current.meanAppendedDamage = current.appendedDamage / current.rows;
    current.meanAppendedTurns = current.appendedTurns / current.rows;
    current.meanAppendedNormalAttackCount = current.appendedNormalAttackCount / current.rows;
  }

  const comparisons = [];
  for (const row of analysis.comparisons) {
    const appended = summaryById.get(row.id);
    const base = summaryById.get(row.baseCaseId);
    if (row.variant.endsWith('APPEND')) {
      check(JSON.stringify(base.case.members) === JSON.stringify(appended.case.members.slice(0, base.case.members.length)), 'saved input prefix retained');
      check(appended.case.members.length === base.case.members.length + 1, 'exactly one appended');
    } else check(digest(base.case.members) === digest(appended.case.members), 'effect-only comparison retains all items/builds');
    const differences = appended.pairScores.map((score, pair) => score - base.pairScores[pair]);
    check(JSON.stringify(differences) === JSON.stringify(row.pairedDeltas), 'all raw-backed paired differences');
    const delta = differences.reduce((a, b) => a + b, 0) / pairs * 100;
    close(delta, row.deltaPP, 'paired delta');
    close(row.pointwise95HoeffdingPP[0], delta - pointwiseHalf, 'pointwise lower');
    close(row.pointwise95HoeffdingPP[1], delta + pointwiseHalf, 'pointwise upper');
    close(row.simultaneous9Family95HoeffdingPP[0], delta - familyHalf, 'family lower');
    close(row.simultaneous9Family95HoeffdingPP[1], delta + familyHalf, 'family upper');
    const ordinary = !row.diagnosticFixedNpc && row.variant === 'NORMAL_APPEND';
    check(row.actualOrdinaryLargeHarmConfirmed === (ordinary && delta + familyHalf < -20), 'ordinary20pp rule only on actual normal append');
    check(row.negativeEffectConfirmed === (delta + familyHalf < 0), 'negative confidence flag');
    check(row.positiveEffectConfirmed === (delta - familyHalf > 0), 'positive confidence flag');
    comparisons.push({
      caseId: row.id, groupId: row.groupId, style: row.style, tier: row.tier,
      originalSize: row.originalSize, level: row.level, variant: row.variant,
      fixedNpc: row.diagnosticFixedNpc, baseWinrate: base.winrate, appendedWinrate: appended.winrate,
      deltaPP: delta, simultaneousNine95PP: [delta - familyHalf, delta + familyHalf],
    });
  }
  check(comparisons.length === family, 'nine fixed differences');
  const report = {
    status: 'PASS',
    purpose: 'INDEPENDENT_FINAL17K_RAW_IDENTITY_GEOMETRY_OUTCOME_AND_SUMMARY_AUDIT_NO_NEW_BATTLES',
    root, conditions: 17, pairsPerCondition: pairs, rows, assertions,
    fixedNpcComparisons, fixedPlayerPrefixComparisons, mirroredConstructionComparisons,
    minimumWeakHp, maximumWeakHp,
    rawCompressedSha256: compressedHash.digest('hex'),
    rawPlainSha256: plainHash.digest('hex'), rawPlainBytes: plainBytes,
    scriptSha256: crypto.createHash('sha256').update(fs.readFileSync(__filename)).digest('hex'),
    geometryComparisons: Object.fromEntries(geometryComparisons),
    pointwiseDifferenceHalfPP: pointwiseHalf,
    simultaneousNineDifferenceHalfPP: familyHalf,
    comparisons,
    cases: [...rawCases.values()],
    limitations: [
      'All17k complete rows were audited, but this is not a rerun of any battle.',
      'Raw neutral components and the complete factory formula are separately checked by the owning validator; this audit focuses on identity, geometry and inference.',
      'FixedNPC snapshots differ naturally in absolute lineIndex when player rows change; all other ordered NPC fields including IDs/gauge were compared.',
      'Actual versus diagnostic groups have different group seeds; do not subtract their effects as a paired cross-model estimate.',
      'Actor damage/turns are recorded descriptive outcomes, not a causal separation of targeting, healing, control or range effects.',
      'No neutral-scalar impossibility theorem and no accepted production candidate follow from these finite results.',
    ],
  };
  process.stdout.write(JSON.stringify(report) + '\n');
}
main().catch(error => { process.stderr.write(error.stack + '\n'); process.exitCode = 1; });
