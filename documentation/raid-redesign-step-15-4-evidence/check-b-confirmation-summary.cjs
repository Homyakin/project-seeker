'use strict';

// Independent arithmetic audit of completed saved summaries only. No Java/battles.
const fs = require('node:fs');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const path = require('node:path');
const root = process.argv[2] || '/tmp/seeker-step15-4-b-confirm.9rPIEP';
const load = relative => JSON.parse(fs.readFileSync(path.join(root, relative), 'utf8'));
const sha = absolute => crypto.createHash('sha256').update(fs.readFileSync(absolute)).digest('hex');
const summary = load('screen/summary.json');
const analysis = load('screen/analysis.json');
const policy = load('inference-policy.json');
const inputs = load('screen/inputs.json');
const pairs = 500;
const family = 9;
const pointwiseHalf = Math.sqrt(2 * Math.log(40) / pairs) * 100;
const familyHalf = Math.sqrt(2 * Math.log(2 * family / 0.05) / pairs) * 100;
let assertions = 0;
function check(condition, message) {
  assertions++;
  assert.ok(condition, message);
}
function close(actual, expected, message) {
  check(Number.isFinite(actual) && Math.abs(actual - expected) <= 1e-12, message);
}

check(summary.cases.length === 18 && analysis.cases.length === 18, '18 fixed conditions');
check(policy.conditions === 18 && policy.pairsPerCondition === pairs, 'frozen inference size');
check(policy.causalComparisons === family, 'frozen nine-difference family');
check(fs.readFileSync(path.join(root, 'preparation/inputs.json'), 'utf8')
  === fs.readFileSync(path.join(root, 'screen/inputs.json'), 'utf8'), 'preparation inputs equal screen inputs');
const byId = new Map();
let battles = 0;
for (const item of summary.cases) {
  check(item.pairs === pairs && item.pairScores.length === pairs, '500 pair scores');
  check(!byId.has(item.case.id), 'unique case');
  const expectedInput = inputs.find(input => input.id === item.case.id);
  check(JSON.stringify(item.case) === JSON.stringify(expectedInput), 'summary case equals frozen input');
  check(item.pairScores.every(value => value === 0 || value === 0.5 || value === 1), 'complete no-draw mirror scores');
  const mean = item.pairScores.reduce((a, b) => a + b, 0) / pairs;
  const wins = item.outcomes.WIN || 0;
  const losses = item.outcomes.LOSS || 0;
  check(wins + losses === 2 * pairs, '1000 completed wins/losses');
  check(Object.keys(item.outcomes).every(key => key === 'WIN' || key === 'LOSS'), 'no draw/timeout outcome');
  close(item.pairMean, mean, 'pair mean');
  close(item.winrate, wins / (2 * pairs), 'winrate');
  close(item.winrate, mean, 'mirror-pair and raw-outcome rates agree');
  const listed = analysis.cases.find(row => row.id === item.case.id);
  close(listed.winratePP, wins / (2 * pairs) * 100, 'analysis case winrate');
  byId.set(item.case.id, item);
  battles += wins + losses;
}
check(battles === 18000 && analysis.battles === battles, '18000 completed battles');

const comparisons = [];
for (const row of analysis.comparisons) {
  const appended = byId.get(row.id);
  const base = byId.get(row.baseCaseId);
  check(base.case.groupId === appended.case.groupId, 'causal pair shares group');
  check(base.case.members.length + 1 === appended.case.members.length, 'one member appended');
  check(JSON.stringify(base.case.members)
    === JSON.stringify(appended.case.members.slice(0, base.case.members.length)), 'original participant prefix unchanged');
  const differences = appended.pairScores.map((value, index) => value - base.pairScores[index]);
  check(JSON.stringify(differences) === JSON.stringify(row.pairedDeltas), 'paired differences exactly match saved vectors');
  const delta = differences.reduce((a, b) => a + b, 0) / pairs * 100;
  close(row.deltaPP, delta, 'delta mean');
  close(row.pointwise95HoeffdingPP[0], delta - pointwiseHalf, 'pointwise lower');
  close(row.pointwise95HoeffdingPP[1], delta + pointwiseHalf, 'pointwise upper');
  close(row.simultaneous9Family95HoeffdingPP[0], delta - familyHalf, 'family lower');
  close(row.simultaneous9Family95HoeffdingPP[1], delta + familyHalf, 'family upper');
  const ordinary = appended.case.variant === 'NORMAL_APPEND';
  check(row.ordinaryLargeHarmConfirmed === (ordinary && delta + familyHalf < -20), 'ordinary-only20pp policy');
  check(row.weakNegativeConfirmed === (!ordinary && delta + familyHalf < 0), 'weak negative is not a20pp acceptance rule');
  comparisons.push({
    caseId: row.id,
    style: row.style,
    tier: row.tier,
    level: row.level,
    originalSize: row.originalSize,
    variant: row.variant,
    baseWinrate: base.winrate,
    appendedWinrate: appended.winrate,
    deltaPP: delta,
    simultaneousNine95PP: [delta - familyHalf, delta + familyHalf],
  });
}
check(comparisons.length === family, 'all nine comparisons audited');
check(comparisons.filter(row => row.variant === 'NORMAL_APPEND'
  && row.simultaneousNine95PP[1] < -20).length === 3, 'three ordinary drops beyond20pp');

const report = {
  status: 'PASS',
  purpose: 'INDEPENDENT_SAVED_SUMMARY_ARITHMETIC_NOT_NEW_BATTLES_NOT_RAW_STREAM_VALIDATION',
  root,
  conditions: 18,
  pairsPerCondition: pairs,
  battles,
  assertions,
  familySize: family,
  pointwiseHalfPP: pointwiseHalf,
  simultaneousFamilyHalfPP: familyHalf,
  comparisons,
  ritualBeginnerN15L50: byId.get(18).winrate,
  sourceSha256: Object.fromEntries([
    'screen/summary.json', 'screen/analysis.json', 'screen/inputs.json', 'inference-policy.json',
  ].map(relative => [relative, sha(path.join(root, relative))])),
  scriptSha256: sha(__filename),
  limitations: [
    'This independently checks saved pairs, arithmetic, input prefixes and the stated nine-comparison family; raw rows are checked by a separate validator.',
    'The confidence family is only these nine fixed differences, not all earlier pilot/wide screens.',
    'RITUAL weak delta excludes zero but does not establish a loss greater than20pp; no blanket weak20pp tolerance is inferred.',
    'The rejected B evidence does not show that every neutral scalar is impossible.',
  ],
};
process.stdout.write(JSON.stringify(report, null, 2) + '\n');
