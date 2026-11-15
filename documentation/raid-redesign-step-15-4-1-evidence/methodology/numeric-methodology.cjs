'use strict';

// Pure numeric design checks. No Java, battles, database, target or repository writes.
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
const REF = Object.freeze({ Hplayer: 11735.6070634692, Dplayer: 154.2861432,
  Hnpc: 9616.368128260692, Dnpc: 235.56672000000003, level: 25,
  guardianDefense: 707, slingerDefense: 309 });
const CONFIG = Object.freeze({ seed: 1515001, randomCases: 10000, relativeTolerance: 2e-12 });
const MATRIX = Object.freeze({
  CLOTH: [0.75, 1.25, 0.9, 1.1], LEATHER: [0.9, 1.1, 1.25, 0.75],
  PLATE: [1.25, 0.75, 1.1, 0.9], ARCANE: [1.1, 0.9, 0.75, 1.25],
});
let state = CONFIG.seed >>> 0;
let checks = 0;
function random() {
  state = (state + 0x6d2b79f5) >>> 0;
  let x = state; x = Math.imul(x ^ (x >>> 15), x | 1);
  x ^= x + Math.imul(x ^ (x >>> 7), x | 61);
  return ((x ^ (x >>> 14)) >>> 0) / 4294967296;
}
function check(value, message) { checks++; assert.ok(value, message); }
function close(a, b, message) {
  check(Math.abs(a - b) <= CONFIG.relativeTolerance * Math.max(Math.abs(a), Math.abs(b), Number.MIN_VALUE), message);
}
function q(level) { return 1 + ((level - 10) * 0.06 + 0.14); }
function mu(amount, weight) { const defense = amount * weight; return 1 - defense / (defense + 500); }
function neutralMu(amount, type, stable) {
  const values = MATRIX[type].map(weight => mu(amount, weight));
  if (stable) values.sort((a, b) => a - b);
  return values.reduce((a, b) => a + b, 0) / 4;
}
function goals(hRatio, dRatio, level, model) {
  const scale = Math.sqrt(q(level)) / Math.sqrt(q(REF.level));
  return model === 'SAME'
    ? [REF.Hnpc * scale * hRatio, REF.Dnpc * scale * dRatio]
    : [REF.Hnpc * scale * dRatio, REF.Dnpc * scale * hRatio];
}

const floatingPermutationExamples = [];
for (let armor = 1; armor <= 1000; armor++) {
  const reference = neutralMu(armor, 'ARCANE', true);
  for (const type of Object.keys(MATRIX)) {
    check(neutralMu(armor, type, true) === reference, 'stable mean invariant under a single armor type swap');
  }
  const ordinaryArcane = neutralMu(armor, 'ARCANE', false);
  const ordinaryLeather = neutralMu(armor, 'LEATHER', false);
  if (ordinaryArcane !== ordinaryLeather && floatingPermutationExamples.length < 4) {
    floatingPermutationExamples.push({ armor, ordinaryArcane, ordinaryLeather });
  }
  check(mu(armor, 1.25) < mu(armor, 0.75), 'ARCANE takes less MAGICAL damage than same LEATHER');
}

for (let index = 0; index < CONFIG.randomCases; index++) {
  const level = 1 + index % 50;
  const h = 10 ** (-2 + random() * 4), d = 10 ** (-2 + random() * 4);
  const same = goals(h, d, level, 'SAME'), cross = goals(h, d, level, 'CROSS');
  check(same.every(x => x > 0 && Number.isFinite(x)) && cross.every(x => x > 0 && Number.isFinite(x)), 'positive finite goals');
  close(same[0] * same[1], cross[0] * cross[1], 'SAME and CROSS products equal');
  close(same[0] * same[1], REF.Hnpc * REF.Dnpc * (q(level) / q(REF.level)) * h * d, 'same product is scaled reference Q');
  const equalSame = goals(h, h, level, 'SAME'), equalCross = goals(h, h, level, 'CROSS');
  check(equalSame[0] === equalCross[0] && equalSame[1] === equalCross[1], 'identical relative H/D gives exact same goals');
  const growth = 1.001 + random();
  const hGrowth = goals(h * growth, d, level, 'SAME');
  const dGrowth = goals(h, d * growth, level, 'SAME');
  check(hGrowth[0] >= same[0] && hGrowth[1] === same[1], 'SAME isolated health response');
  check(dGrowth[0] === same[0] && dGrowth[1] >= same[1], 'SAME isolated damage response');
  const crossHGrowth = goals(h * growth, d, level, 'CROSS');
  const crossDGrowth = goals(h, d * growth, level, 'CROSS');
  check(crossHGrowth[0] === cross[0] && crossHGrowth[1] >= cross[1], 'CROSS isolated health response');
  check(crossDGrowth[0] >= cross[0] && crossDGrowth[1] === cross[1], 'CROSS isolated damage response');
}
const anchorSame = goals(1, 1, 25, 'SAME'), anchorCross = goals(1, 1, 25, 'CROSS');
check(anchorSame[0] === REF.Hnpc && anchorSame[1] === REF.Dnpc, 'reference total goals exact');
check(JSON.stringify(anchorSame) === JSON.stringify(anchorCross), 'anchor models exact to each other');

for (const armor of [REF.guardianDefense, REF.slingerDefense]) {
  for (let attack = 1; attack <= 1000; attack++) {
    for (const crit of [1, 1.5, 1.7]) {
      const rolled = Math.trunc(attack * crit);
      check(Math.trunc(rolled * mu(armor, 0.75)) >= Math.trunc(rolled * mu(armor, 1.1)),
        'normal post-defense floor preserves PIERCE advantage over SLASH');
    }
    // V3 four-point PRECISE_STRIKE basis=54/100 saved attack, no normal critical multiplier.
    function skillProtected(weightNumerator, weightDenominator) {
      const numerator = BigInt(attack) * 54n * 500n * BigInt(weightDenominator);
      const denominator = 100n * (500n * BigInt(weightDenominator) + BigInt(armor) * BigInt(weightNumerator));
      return Math.max(1, Number(numerator / denominator));
    }
    check(skillProtected(3, 4) >= skillProtected(11, 10), 'exact skill-defense arithmetic preserves type advantage');
  }
}

const meanG = neutralMu(REF.guardianDefense, 'ARCANE', true);
const meanS = neutralMu(REF.slingerDefense, 'ARCANE', true);
const continuousH = REF.Hnpc / (420 / meanG + 2 * 280 / meanS);
const continuousD = REF.Dnpc / (70 * 1.02 * 0.19 + 2 * 130 * 1.056 * 0.16);
const hpG = Math.ceil(420 * continuousH), hpS = Math.ceil(280 * continuousH);
const attackG = Math.ceil(70 * continuousD), attackS = Math.ceil(130 * continuousD);
const achievedH = hpG / meanG + 2 * hpS / meanS;
const achievedD = attackG * 1.02 * 0.19 + 2 * attackS * 1.056 * 0.16;
const hCeilBound = 1 / meanG + 2 / meanS;
const dCeilBound = 1.02 * 0.19 + 2 * 1.056 * 0.16;
check(achievedH >= REF.Hnpc && achievedH - REF.Hnpc <= hCeilBound, 'independent HP ceil bound');
check(achievedD >= REF.Dnpc && achievedD - REF.Dnpc <= dCeilBound, 'independent attack ceil bound');
const report = {
  status: 'PASS_NUMERICAL_METHODOLOGY_ONLY_NOT_BATTLES_OR_BALANCE_ACCEPTANCE',
  reference: REF, configuration: CONFIG, checks,
  coefficientForms: {
    SAME: { kH: REF.Hnpc / (Math.sqrt(q(25)) * REF.Hplayer), kD: REF.Dnpc / (Math.sqrt(q(25)) * REF.Dplayer) },
    CROSS: { kH: REF.Hnpc / (Math.sqrt(q(25)) * REF.Dplayer), kD: REF.Dnpc / (Math.sqrt(q(25)) * REF.Hplayer) },
  },
  floatingPermutationExamples,
  referenceIndependentCeilIllustration: { continuousH, continuousD, hpG, hpS, attackG, attackS,
    achievedH, achievedD, hCeilBound, dCeilBound,
    limitation: 'Continuous targets plus ceil, not an actual candidate factory or closest-step search; old role traits are not reproduced.' },
  sameCrossReferenceGoals: anchorSame,
  rawTimeRatiosAtReferenceBeforeTempoDodgeSkillsDeaths: { npcHOverPlayerD: REF.Hnpc / REF.Dplayer,
    playerHOverNpcD: REF.Hplayer / REF.Dnpc },
  pointwiseDifferenceHalfAt30pairsPP: 100 * Math.sqrt(2 * Math.log(40) / 30),
  scriptSha256: crypto.createHash('sha256').update(fs.readFileSync(__filename)).digest('hex'),
  limitations: [
    'The goal checks do not evaluate a Java factory, random effects, skills, attack opportunities or any win probability.',
    'Single armor type neutrality does not extend to arbitrary mixed-defense maps.',
    'Normal attacks round each damage component before variance; the skill illustration instead uses exact rational defense and minimum1.',
    'Phase screen is50conditions*30mirroredpairs*2models=6000battles, not a balance acceptance sample.',
    'No additional battle conditions or outcome-based constants are proposed here.',
  ],
};
process.stdout.write(JSON.stringify(report, null, 2) + '\n');
