'use strict';

// Numerical project check only. This is not a battle simulator or production unit test.
// Configuration is deliberately frozen before any generated cases.
const CONFIG = Object.freeze({
  seed: 0x1504b001,
  randomCases: 10000,
  maxN: 50,
  permutationsPerCase: 3,
  exactWeakAppendCases: 1000,
  log10HealthRange: [-6, 6],
  log10DamageRange: [-6, 6],
  relativeTolerance: 2e-12,
});

const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const fs = require('node:fs');
let state = CONFIG.seed >>> 0;
const counters = {};
let maximumRelativePairwiseDifference = 0;
let maximumRelativeBoundSlack = 0;

function random() {
  state = (state + 0x6d2b79f5) >>> 0;
  let value = state;
  value = Math.imul(value ^ (value >>> 15), value | 1);
  value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
  return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
}

function check(name, condition, details) {
  counters[name] = (counters[name] || 0) + 1;
  assert.ok(condition, `${name}: ${JSON.stringify(details)}`);
}

function close(name, actual, expected, details) {
  const scale = Math.max(Math.abs(actual), Math.abs(expected), Number.MIN_VALUE);
  check(name, Math.abs(actual - expected) <= CONFIG.relativeTolerance * scale,
    { actual, expected, ...details });
}

function lessOrEqual(name, left, right, details) {
  const scale = Math.max(Math.abs(left), Math.abs(right), Number.MIN_VALUE);
  const slack = Math.max(0, left - right) / scale;
  maximumRelativeBoundSlack = Math.max(maximumRelativeBoundSlack, slack);
  check(name, left <= right + CONFIG.relativeTolerance * scale,
    { left, right, ...details });
}

function contributions(rows) {
  if (!Array.isArray(rows) || rows.length === 0) {
    throw new TypeError('A nonempty team is required');
  }
  return rows.map(row => {
    if (row === null || typeof row !== 'object'
        || !Number.isFinite(row.H) || row.H < 0
        || !Number.isFinite(row.D) || row.D < 0) {
      throw new TypeError('H and D must be finite and nonnegative');
    }
    const p = row.H * row.D;
    if (!Number.isFinite(p)) {
      throw new RangeError('A personal product must be finite');
    }
    return p;
  });
}

function sum(values) {
  return values.toSorted((a, b) => a - b).reduce((total, value) => total + value, 0);
}

function sortedB(rows) {
  const sorted = contributions(rows).sort((a, b) => a - b);
  let total = 0;
  for (let index = 0; index < sorted.length; index++) {
    const term = (2 * (sorted.length - index) - 1) * sorted[index];
    total += term;
    if (!Number.isFinite(term) || !Number.isFinite(total)) {
      throw new RangeError('B must be finite');
    }
  }
  return total;
}

function pairwiseB(rows) {
  const p = contributions(rows);
  let total = 0;
  for (let i = 0; i < p.length; i++) {
    for (let j = 0; j < p.length; j++) {
      total += Math.min(p[i], p[j]);
    }
  }
  return total;
}

function shuffled(rows) {
  const result = rows.slice();
  for (let index = result.length - 1; index > 0; index--) {
    const other = Math.floor(random() * (index + 1));
    [result[index], result[other]] = [result[other], result[index]];
  }
  return result;
}

function logUniform(range) {
  return 10 ** (range[0] + random() * (range[1] - range[0]));
}

function inspect(rows, label) {
  const p = contributions(rows);
  const b = sortedB(rows);
  const direct = pairwiseB(rows);
  const relativeDifference = Math.abs(b - direct)
    / Math.max(Math.abs(b), Math.abs(direct), Number.MIN_VALUE);
  maximumRelativePairwiseDifference = Math.max(maximumRelativePairwiseDifference, relativeDifference);
  close('sortedEqualsPairwise', b, direct, { label });
  check('nonnegativeFinite', Number.isFinite(b) && b >= 0, { label, b });
  const own = sum(p);
  const squareRootSum = sum(p.map(Math.sqrt)) ** 2;
  const q = sum(rows.map(row => row.H)) * sum(rows.map(row => row.D));
  lessOrEqual('sumPAtMostB', own, b, { label });
  lessOrEqual('BAtMostNsumP', b, rows.length * own, { label });
  lessOrEqual('BAtMostRootSumSquared', b, squareRootSum, { label });
  lessOrEqual('rootSumSquaredAtMostQ', squareRootSum, q, { label });
  for (let trial = 0; trial < CONFIG.permutationsPerCase; trial++) {
    const permuted = sortedB(shuffled(rows));
    check('permutationBitIdentical', Object.is(b, permuted), { label, b, permuted });
  }
  return { p, b };
}

const nCoverage = Array.from({ length: CONFIG.maxN }, () => 0);
for (let caseIndex = 0; caseIndex < CONFIG.randomCases; caseIndex++) {
  const n = 1 + caseIndex % CONFIG.maxN;
  nCoverage[n - 1]++;
  const rows = Object.freeze(Array.from({ length: n }, () => Object.freeze({
    H: logUniform(CONFIG.log10HealthRange),
    D: logUniform(CONFIG.log10DamageRange),
  })));
  const { p, b } = inspect(rows, `random-${caseIndex}`);
  const changedIndex = Math.floor(random() * n);
  for (const stat of ['H', 'D']) {
    const changed = rows.map((row, index) => index === changedIndex
      ? { ...row, [stat]: row[stat] * (1.001 + 1000 * random()) }
      : row);
    const after = sortedB(changed);
    check(`monotone${stat}`, after >= b, { caseIndex, changedIndex, b, after });
  }
  const weak = Math.min(...p) * (0.01 + 0.98 * random());
  const appended = sortedB([...rows, { H: weak, D: 1 }]);
  close('weakAppendIdentityWithinFinalScale', appended, b + (2 * n + 1) * weak,
    { caseIndex, n, weak });
  const h = logUniform(CONFIG.log10HealthRange);
  const d = logUniform(CONFIG.log10DamageRange);
  const homogeneous = sortedB(Array.from({ length: n }, () => ({ H: h, D: d })));
  close('randomHomogeneousN2p', homogeneous, n * n * (h * d), { caseIndex, n });
}

for (let caseIndex = 0; caseIndex < CONFIG.exactWeakAppendCases; caseIndex++) {
  const n = 1 + caseIndex % CONFIG.maxN;
  const rows = Array.from({ length: n }, () => ({ H: 2 + Math.floor(random() * 1000000), D: 1 }));
  const weak = Math.floor(Math.min(...rows.map(row => row.H)) / 2);
  const before = sortedB(rows);
  const after = sortedB([...rows, { H: weak, D: 1 }]);
  check('weakAppendExactIntegerDelta', after - before === (2 * n + 1) * weak,
    { caseIndex, n, before, after, weak });
}

for (let n = 1; n <= CONFIG.maxN; n++) {
  for (const p of [0, 1, 7, 100, 2000]) {
    const b = sortedB(Array.from({ length: n }, () => ({ H: p, D: 1 })));
    check('homogeneousExactN2p', b === n * n * p, { n, p, b });
  }
}

const zeroCases = [
  [{ H: 0, D: 0 }],
  [{ H: 0, D: 10 }],
  [{ H: 10, D: 0 }],
  [{ H: 1, D: 0 }, { H: 0, D: 1 }],
  [{ H: 10, D: 0 }, { H: 2, D: 3 }, { H: 0, D: 100 }],
  Array.from({ length: 50 }, () => ({ H: 0, D: 0 })),
];
zeroCases.forEach((rows, index) => inspect(rows, `zero-${index}`));

const invalidCases = [
  null, [], [null], [{ H: -1, D: 1 }], [{ H: 1, D: -1 }],
  [{ H: NaN, D: 1 }], [{ H: 1, D: NaN }],
  [{ H: Infinity, D: 1 }], [{ H: 1, D: Infinity }],
  [{ H: Number.MAX_VALUE, D: Number.MAX_VALUE }],
  [{ H: 5e307, D: 1 }, { H: 5e307, D: 1 }],
];
invalidCases.forEach((rows, index) => {
  let rejected = false;
  try {
    sortedB(rows);
  } catch (error) {
    rejected = error instanceof TypeError || error instanceof RangeError;
  }
  check('invalidOrOverflowRejected', rejected, { index });
});
const largeFiniteB = sortedB([{ H: 1e300, D: 1e-100 }]);
check('finiteLargeAccepted', Number.isFinite(largeFiniteB) && largeFiniteB > 0, {});
close('finiteLargeExpectedRoundedProduct', largeFiniteB, 1e200, {});
check('finiteSubnormalAccepted', sortedB([{ H: Number.MIN_VALUE, D: 1 }]) === Number.MIN_VALUE, {});
check('underflowIsNonnegativeNotStrictMonotonicity',
  sortedB([{ H: 1e-300, D: 1e-300 }]) === 0, {});

const report = {
  status: 'PASS',
  kind: 'NUMERICAL_DESIGN_CHECK_NOT_BATTLES_NOT_PRODUCTION_UNIT_TESTS',
  formula: 'B = sum_i sum_j min(H_i*D_i, H_j*D_j)',
  sortedFormula: 'ascending p, zero-based i: sum_i (2*(N-i)-1)*p_i',
  configuration: CONFIG,
  generator: 'Mulberry32 with explicit unsigned 32-bit state; cyclic N ensures 200 random cases per N',
  nCoverage,
  counters,
  totalChecks: Object.values(counters).reduce((a, b) => a + b, 0),
  maximumRelativePairwiseDifference,
  maximumRelativeBoundSlack,
  scriptSha256: crypto.createHash('sha256').update(fs.readFileSync(__filename)).digest('hex'),
  limitations: [
    'Finite numerical examples support implementation identities; they do not replace algebraic proofs.',
    'Floating comparisons use the fixed relative tolerance in configuration; permutation checks are bit-exact.',
    'Weak append delta is exact in 1000 integer cases; wide-dynamic-range cases compare final totals because subtraction loses tiny increments.',
    'Positive mathematical products can underflow to zero in IEEE754; monotonic means nondecreasing, not strictly increasing.',
    'No Battle, Java, target, database, production source, level, reward, or displayed-power changes are involved.',
    'No result is evidence that B equals win probability, universal combat value, or accepted balance.',
  ],
};
process.stdout.write(JSON.stringify(report, null, 2) + '\n');
