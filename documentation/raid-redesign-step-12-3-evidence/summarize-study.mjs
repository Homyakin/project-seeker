import fs from 'node:fs';
import zlib from 'node:zlib';

// Usage: node summarize-study.mjs /path/to/study-prefix
// Derived tables do not replace the immutable raw observations or define acceptance gates.
const prefix = process.argv[2];
if (!prefix) throw new Error('Expected a study output prefix');
const readTable = text => {
  const lines = text.trim().split('\n');
  const columns = lines.shift().split('\t');
  return lines.map(line => Object.fromEntries(line.split('\t').map((value, index) => [columns[index], value])));
};
const summary = readTable(fs.readFileSync(`${prefix}-summary.tsv`, 'utf8'));
const observations = new Map();
const rawLines = zlib.gunzipSync(fs.readFileSync(`${prefix}-battles.tsv.gz`)).toString('utf8').trim().split('\n');
rawLines.shift();
for (const line of rawLines) {
  const [code, pair, seed, reverse, outcome] = line.split('\t', 5);
  if (!observations.has(code)) observations.set(code, new Map());
  observations.get(code).set(`${pair}:${reverse}`, {pair, seed, outcome});
}
const score = outcome => {
  if (outcome === 'WIN') return 1;
  if (outcome === 'LOSS') return 0;
  if (outcome === 'DRAW') return 0.5;
  throw new Error(`Unresolved outcome ${outcome}; do not silently drop it`);
};
const compare = (left, right, label) => {
  const a = observations.get(left.case), b = observations.get(right.case);
  if (a.size !== b.size) throw new Error('Unmatched observations');
  const pairs = new Map();
  for (const [key, x] of a) {
    const y = b.get(key);
    if (!y || x.seed !== y.seed) throw new Error(`Unmatched seed ${key}`);
    pairs.set(x.pair, (pairs.get(x.pair) ?? 0) + (score(x.outcome) - score(y.outcome)) / 2);
  }
  const values = [...pairs.values()];
  const delta = values.reduce((sum, value) => sum + value, 0) / values.length;
  // Each independent paired difference lies in [-1, 1]. Pointwise 95% Hoeffding bounds.
  const margin = Math.sqrt(2 * Math.log(40) / values.length);
  return [label, left.case, right.case, values.length, left.score, right.score, delta,
    Math.max(-1, delta - margin), Math.min(1, delta + margin)].join('\t');
};
const byCase = new Map(summary.map(row => [row.case, row]));
const result = ['comparison\tleft\tright\tpairs\tleftScore\trightScore\tdelta\tlower\tupper'];
for (const row of summary) {
  if ((row.family === 'MOBILE' || row.family === 'MOBILE_NICHE') && !row.case.endsWith('_OFF')) {
    const off = byCase.get(`${row.case}_OFF`);
    if (off) result.push(compare(row, off, 'HIT_AND_RUN_ON_MINUS_OFF'));
  }
  if (row.case.startsWith('CLOTH_') && row.case.includes('_NEW_V4_')) {
    for (const previous of ['OLD', 'NEW_V3']) {
      const old = byCase.get(row.case.replace('_NEW_V4_', `_${previous}_`));
      if (old) result.push(compare(row, old, `V4_MINUS_${previous}`));
    }
  }
  if (row.family === 'SINGLE_CLOTH' && row.case.includes('_NEW_V4_')) {
    const old = byCase.get(row.case.replace('_NEW_V4_', '_OLD_'));
    if (old) result.push(compare(row, old, 'SINGLE_V4_MINUS_OLD'));
  }
}
process.stdout.write(`${result.join('\n')}\n`);
