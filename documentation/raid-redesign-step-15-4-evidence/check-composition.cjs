'use strict';

// Read-only arithmetic mirror of the documented NPC stat formulas; no Java or battles.
const fs = require('node:fs');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const manifestPath = '/tmp/seeker-step15-4-b-min.siriKz/preparation/manifest.json';
const manifest = JSON.parse(fs.readFileSync(manifestPath, 'utf8'));
const anchors = manifest.anchors;
const mReference = anchors.referenceMultiplierMID3L25;
const C = anchors.Cnew;
const oldA = anchors.screenA;
const maxCount = 50;
const multipliers = Object.freeze([
  0, 1e-8, 0.01, 0.05, 0.1, 0.25, 0.5, 1, 1.5, 2,
  anchors.referenceMultiplierSTRONG1L5, 3, 4, mReference, 5, 7, 10, 15, 25, 50, 100,
]);
const defenseWeights = Object.freeze([1.1, 0.9, 0.75, 1.25]);
const sourcePaths = Object.freeze([
  'game/src/main/java/ru/homyakin/seeker/game/event/raid/generator/MyconidColonyGenerator.java',
  'game/src/main/java/ru/homyakin/seeker/game/event/pve/PveTeamAssessment.java',
  'game/src/main/java/ru/homyakin/seeker/game/event/pve/PveCharacteristicScaling.java',
  'game/src/main/java/ru/homyakin/seeker/game/battle/CombatRules.java',
  'game/src/main/java/ru/homyakin/seeker/game/item/models/AttackType.java',
]);

function hash(path) {
  return crypto.createHash('sha256').update(fs.readFileSync(path)).digest('hex');
}

function multiply(base, m, weight) {
  return Math.max(1, Math.ceil(base * (m * weight)));
}

function personal(hp, attack, defense, crit, frequency) {
  let reductionSum = 0;
  for (const weight of defenseWeights) {
    const effectiveDefense = defense * weight;
    reductionSum += 1 - effectiveDefense / (effectiveDefense + 500);
  }
  const meanMu = reductionSum / 4;
  const H = hp / meanMu;
  const D = attack * crit * frequency;
  return { hp, attack, defense, meanMu, H, D, p: H * D };
}

function npcParts(count, m) {
  if (!Number.isInteger(count) || count < 2 || !Number.isFinite(m) || m < 0) {
    throw new TypeError('count>=2 integer and finite m>=0 required');
  }
  const front = Math.floor(count / 2);
  const back = count - front;
  const wg = count / (2 * front);
  const ws = count / (2 * back);
  const guardian = personal(multiply(420, m, wg), multiply(70, m, wg), multiply(160, m, 1), 1.02, 0.19);
  const slinger = personal(multiply(280, m, ws), multiply(130, m, ws), multiply(70, m, 1), 1.056, 0.16);
  return Array.from({ length: count }, (_, index) => index < front ? guardian : slinger);
}

function B(parts) {
  const p = parts.map(part => part.p).sort((a, b) => a - b);
  let total = 0;
  for (let index = 0; index < p.length; index++) {
    total += (2 * (p.length - index) - 1) * p[index];
  }
  return total;
}

const referenceParts = npcParts(3, mReference);
const sourceReferenceParts = anchors.midNpcParts;
for (let index = 0; index < referenceParts.length; index++) {
  assert.equal(referenceParts[index].hp, sourceReferenceParts[index].hp);
  assert.equal(referenceParts[index].attack, sourceReferenceParts[index].maximumAttack);
  assert.equal(referenceParts[index].p, sourceReferenceParts[index].selfProduct);
}
const sourceSoloParts = anchors.soloNpcParts;
const mirroredSoloParts = npcParts(2, anchors.referenceMultiplierSTRONG1L5);
for (let index = 0; index < mirroredSoloParts.length; index++) {
  assert.equal(mirroredSoloParts[index].hp, sourceSoloParts[index].hp);
  assert.equal(mirroredSoloParts[index].attack, sourceSoloParts[index].maximumAttack);
  assert.equal(mirroredSoloParts[index].p, sourceSoloParts[index].selfProduct);
}

const B3 = B(referenceParts);
function r(count) {
  return B(npcParts(count, mReference)) / (((count / 3) ** 2) * B3);
}
function h(count, a) {
  return a + (1 - a) * (3 / count) ** 2;
}
const r2 = r(2);
const a = (2.25 - h(2, oldA) / r2) / 1.25;
assert.ok(Number.isFinite(a) && a > 0 && a <= 1);
assert.equal(r(3), 1);
assert.equal(h(2, a) * r2, h(2, oldA));
assert.equal(h(3, a) * r(3), h(3, oldA));

// The analytic comparison keeps the reference's rounded defenses but omits only HP/attack ceil.
const u = personal(420 * mReference, 70 * mReference, multiply(160, mReference, 1), 1.02, 0.19).p;
const v = personal(280 * mReference, 130 * mReference, multiply(70, mReference, 1), 1.056, 0.16).p;
assert.ok(u >= v, 'The conditional analytic formula requires guardian p >= slinger p');
function analyticR(count) {
  const k = Math.floor(count / 2);
  const coefficient = count % 2 === 0 ? 3 : 3 - 2 / (k + 1);
  return (u + coefficient * v) / (u + 2 * v);
}

const rows = [];
let maxRoundingRelativeDifference = { value: -1 };
let residualAll = { minimum: { value: Infinity }, maximum: { value: -Infinity } };
let residualFromOneToTen = { minimum: { value: Infinity }, maximum: { value: -Infinity } };
let referencePlaneMaximumError = 0;
let checkedMonotonicTransitions = 0;
function include(extrema, value, count, multiplier) {
  if (value < extrema.minimum.value) extrema.minimum = { value, count, multiplier };
  if (value > extrema.maximum.value) extrema.maximum = { value, count, multiplier };
}

for (let count = 2; count <= maxCount; count++) {
  const rawR = r(count);
  assert.ok(Number.isFinite(rawR) && rawR > 0);
  const idealR = analyticR(count);
  const difference = Math.abs(rawR / idealR - 1);
  if (difference > maxRoundingRelativeDifference.value) {
    maxRoundingRelativeDifference = { value: difference, count, rawR, analyticR: idealR };
  }
  const newH = h(count, a) * rawR;
  const oldH = h(count, oldA);
  rows.push({
    count,
    rawR,
    analyticR: idealR,
    oldH,
    newH,
    targetMultiplierRelativeToPriorB: newH / oldH,
    fixedReferenceNpcB: B(npcParts(count, mReference)),
  });
  let previous = -Infinity;
  for (const multiplier of multipliers) {
    const npcB = B(npcParts(count, multiplier));
    const normalized = npcB / rawR;
    assert.ok(Number.isFinite(normalized) && normalized > 0);
    assert.ok(normalized >= previous, `NPC monotonicity failed M${count}, m${multiplier}`);
    previous = normalized;
    checkedMonotonicTransitions++;
    const relativeToReferenceCountAtSameM = npcB
      / (((count / 3) ** 2) * B(npcParts(3, multiplier)) * rawR);
    include(residualAll, relativeToReferenceCountAtSameM, count, multiplier);
    if (multiplier >= 1 && multiplier <= 10) {
      include(residualFromOneToTen, relativeToReferenceCountAtSameM, count, multiplier);
    }
    if (multiplier === mReference) {
      referencePlaneMaximumError = Math.max(referencePlaneMaximumError,
        Math.abs(relativeToReferenceCountAtSameM - 1));
    }
  }
}

const report = {
  purpose: 'NUMERICAL_COMPOSITION_NORMALIZATION_REVIEW_NOT_NEW_BATTLES_NOT_PRODUCTION',
  method: 'Independent JavaScript arithmetic mirror; NPC source factory is authoritative for any future prototype',
  status: 'PASS_FOR_DOCUMENTED_IDENTITIES_NOT_BALANCE_ACCEPTANCE',
  sourceManifest: { path: manifestPath, sha256: hash(manifestPath) },
  sourceSha256: Object.fromEntries(sourcePaths.map(path => [path, hash(path)])),
  scriptSha256: hash(__filename),
  mReference,
  unchangedC: C,
  priorB_a: oldA,
  correctedA: a,
  Bnpc3Reference: B3,
  r2,
  effectiveH2: h(2, a) * r2,
  effectiveH3: h(3, a) * r(3),
  exactSourceBridge: {
    conditions: 2, npcs: 5,
    compared: 'hp, attack, personal neutral product; exact equality to saved actual factory parts',
  },
  normalizedReferencePlaneMaximumError: referencePlaneMaximumError,
  conditionalAnalyticUnroundedPersonalProducts: { u, v, ratio: u / v },
  maxRoundingRelativeDifference,
  checkedMonotonicTransitions,
  multiplierGrid: multipliers,
  residualDefinition: 'B_M(m) / ((M/3)^2 * B_3(m) * r_M); equals 1 at m_reference, not in general',
  residualAll,
  residualFromOneToTen,
  rows,
  limitations: [
    'The source bridge covers two saved reference conditions, not a full factory equivalence proof.',
    'The residual grid is deterministic numerical exploration, not battle evidence or bounds between grid points.',
    'The reference m is fixed once from actual 15.2 MID MAIN N3 L25; it must not become a function of live players or level.',
    'The altered tail changes actual generated difficulty, even though old q(level), rewards and displayed power stay unchanged.',
    'For future Java use, reference factory calls must not consume the final random stream.',
  ],
};
process.stdout.write(JSON.stringify(report, null, 2) + '\n');
