'use strict';
// Exploratory descriptions of saved results; these rankings are not acceptance thresholds.
const fs = require('node:fs');
const summaries = process.argv.slice(2).flatMap(file => {
  const run = JSON.parse(fs.readFileSync(file));
  return run.summaries.map(cell => ({ ...cell, run: file }));
});
const groups = new Map();
for (const cell of summaries) {
  if (cell.family !== 'MATRIX') continue;
  const key = `${cell.budget}/N${cell.size}/+${cell.level}`;
  if (!groups.has(key)) groups.set(key, new Map());
  const group = groups.get(key);
  for (const [name, opponent, score] of [[cell.leftBuild, cell.rightBuild, cell.score],
    [cell.rightBuild, cell.leftBuild, 1 - cell.score]]) {
    if (!group.has(name)) group.set(name, []);
    group.get(name).push({ opponent, score, pairs: cell.pairs, case: cell.case });
  }
}
console.log(JSON.stringify({
  scope: 'Descriptive, sampled opponent pools only; unequal opponent coverage and duplicate runs must not be read as a global rating.',
  groups: [...groups].map(([context, profiles]) => ({ context,
    profiles: [...profiles].map(([profile, cells]) => ({ profile,
      testedOpponents: new Set(cells.map(c => c.opponent)).size,
      meanObservedScore: cells.reduce((sum, c) => sum + c.score, 0) / cells.length,
      strongestOpponents: [...cells].sort((a, b) => a.score - b.score).slice(0, 5),
    })).sort((a, b) => b.meanObservedScore - a.meanObservedScore),
  })),
}, null, 2));
