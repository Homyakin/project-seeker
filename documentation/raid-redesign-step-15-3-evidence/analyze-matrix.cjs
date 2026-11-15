const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const crypto = require('node:crypto');

const root = process.argv[2];
const expectedCases = Number(process.argv[3] || 525);
const expectedPairs = Number(process.argv[4] || 20);
const expectedOffset = Number(process.argv[5] || 3000000000);
const expectedPhase = process.argv[6] || 'STEP153_SCREEN';
const manifest = JSON.parse(fs.readFileSync(path.join(root, 'summary.json'), 'utf8'));
const types = ['SLASH', 'BLUNT', 'PIERCE', 'MAGICAL'];
const assert = (condition, message) => { if (!condition) throw new Error(message); };
const close = (actual, expected, label) => assert(Math.abs(actual - expected) <= Math.max(1e-10, Math.abs(expected) * 1e-11),
  `${label}: actual=${actual} expected=${expected}`);
const sum = xs => xs.sort((a, b) => a - b).reduce((a, b) => a + b, 0);
function assess(personages) {
  return sum(personages.map(p => p.health / (types.reduce((s, t) => s + p.damageTakenMultipliers[t], 0) / 4)))
    * sum(personages.map(p => Math.max(0, ...p.attacksByRange.map(a => types.reduce((s, t) => s + (a.attack[t] || 0), 0)))
      * (1 + Math.min(1, Math.max(0, p.critChance / 100)) * (p.critMultiplier - 1))
      * Math.min(p.initiative, 1000) / 1000));
}
function compact(c) {
  return {id: c.caseId, ...c.cell, win: c.playerWinsPerAttempt, outcomes: c.outcomes,
    interval: c.pairedScore95Hoeffding, byOrder: c.outcomesByOrder,
    rounds: c.completedBattleMetrics.rounds, actions: c.completedBattleMetrics.actionCount,
    forced: c.completedBattleMetrics.forcedMoves};
}
function aggregate(cells) {
  const outcomes = {}, byOrder = {}, metrics = {};
  for (const c of cells) {
    for (const [k, n] of Object.entries(c.outcomes)) outcomes[k] = (outcomes[k] || 0) + n;
    for (const [order, o] of Object.entries(c.outcomesByOrder)) for (const [k, n] of Object.entries(o)) {
      byOrder[order] ||= {};
      byOrder[order][k] = (byOrder[order][k] || 0) + n;
    }
    for (const key of ['rounds', 'actionCount', 'normalAttempts', 'moves', 'forcedMoves']) {
      const m = c.completedBattleMetrics[key];
      if (!m.count) continue;
      metrics[key] ||= {count: 0, total: 0, min: Infinity, max: -Infinity, maxCellP95: 0};
      const a = metrics[key];
      a.count += m.count; a.total += m.mean * m.count; a.min = Math.min(a.min, m.min); a.max = Math.max(a.max, m.max);
      a.maxCellP95 = Math.max(a.maxCellP95, m.p95);
    }
  }
  for (const a of Object.values(metrics)) { a.mean = a.total / a.count; delete a.total; }
  return {conditions: cells.length, outcomes, byOrder, metrics};
}
(async () => {
  assert(manifest.cases.length === expectedCases && manifest.pairs === expectedPairs, 'Unexpected matrix');
  assert(manifest.seedOffset === expectedOffset && manifest.phase === expectedPhase, 'Unexpected seeds/phase');
  const rowsByCase = new Map(), seen = new Set(), rounds = [], actions = [], timeoutCells = [];
  let rows = 0;
  const rawHash = crypto.createHash('sha256');
  const stream = fs.createReadStream(path.join(root, 'battles.jsonl.gz'));
  stream.on('data', chunk => rawHash.update(chunk));
  const lines = readline.createInterface({input: stream.pipe(zlib.createGunzip()), crlfDelay: Infinity});
  for await (const line of lines) {
    if (!line) continue;
    const r = JSON.parse(line);
    assert(r.phase === expectedPhase && r.pair >= 0 && r.pair < expectedPairs && typeof r.reverse === 'boolean', 'Bad phase/pair/order');
    assert(r.generationSeed === manifest.generationRoot + r.caseId * 1000 + r.pair, 'Bad generation seed');
    assert(r.battleSeed === manifest.battleRoot + r.caseId * 1000 + r.pair, 'Bad battle seed');
    const key = `${r.caseId}:${r.pair}:${r.reverse}`;
    assert(!seen.has(key), `Duplicate ${key}`); seen.add(key);
    const counts = rowsByCase.get(r.caseId) || {};
    counts[r.outcome] = (counts[r.outcome] || 0) + 1; rowsByCase.set(r.caseId, counts);
    assert(r.calibrationMetric === 'NEUTRAL_TEAM_V1', `Wrong assessment ${key}`);
    const d = r.generationDiagnostics, p = d.parameters;
    assert(d.assessmentVersion === 'NEUTRAL_TEAM_V1', `Wrong diagnostic version ${key}`);
    close(p.encounterCorrection, 0.5900075417594784, 'C');
    const n = Math.max(2, r.cell.size), f = Math.floor(n / 2);
    assert(r.enemyBasis.length === n && p.enemyCount === n && p.frontCount === f && p.backCount === n - f, `Counts ${key}`);
    close(p.frontWeight, n / (2 * f), 'frontWeight'); close(p.backWeight, n / (2 * (n - f)), 'backWeight');
    close(p.sizeCorrection, 0.7 + (1 - 0.7) * (3 / n) ** 2, 'h');
    if (r.outcome === 'TIMEOUT') { timeoutCells.push(r.caseId); ++rows; continue; }
    const pcs = Object.values(r.initState.personagesById);
    const playerIds = new Set(r.playerBasis.map(x => x.id)), enemyIds = new Set(r.enemyBasis.map(x => x.id));
    const pq = assess(pcs.filter(p => playerIds.has(p.id))), eq = assess(pcs.filter(p => enemyIds.has(p.id)));
    close(d.participantAssessment, pq, 'playerQ'); close(r.calibration.achievedPower, eq, 'enemyQ');
    const b = 1 + ((r.cell.raidLevel - 10) * 0.06 + 0.14);
    close(r.calibration.targetPower, pq * b * p.groupSizeScaling * p.encounterCorrection * p.sizeCorrection, 'target');
    for (let i = 0; i < r.enemyBasis.length; ++i) {
      const e = r.enemyBasis[i];
      assert(e.skills.length === 1 && e.skills[0].points === 4 && e.skills[0].formulaVersion === 'SCALING_SKILLS_V3', `NPC skill ${key}`);
      assert(e.declaredPosition === (i < f ? 'FRONT' : 'BACK') && e.range === (i < f ? 1 : 3), `NPC basis ${key}`);
      assert(r.actualEnemyPositions[i] === (i < f ? 'FRONT' : 'BACK'), `Actual position ${key}`);
      const weight = i < f ? p.frontWeight : p.backWeight, m = r.calibration.multiplier;
      assert(e.health === Math.max(1, Math.ceil((i < f ? 420 : 280) * (m * weight))), `HP rounding ${key}`);
      assert(e.attack === Math.max(1, Math.ceil((i < f ? 70 : 130) * (m * weight))), `ATK rounding ${key}`);
      assert(e.defense === Math.max(1, Math.ceil((i < f ? 160 : 70) * m)), `DEF rounding ${key}`);
    }
    rounds.push(r.rounds); actions.push(r.actionCount);
    ++rows;
  }
  assert(rows === expectedCases * expectedPairs * 2, `Unexpected row count ${rows}`);
  assert(rawHash.digest('hex') === manifest.rawSha256, 'Raw SHA256 mismatch');
  for (const c of manifest.cases) assert(JSON.stringify(rowsByCase.get(c.caseId)) === JSON.stringify(c.outcomes)
    || Object.keys({...rowsByCase.get(c.caseId), ...c.outcomes}).every(k => (rowsByCase.get(c.caseId)[k] || 0) === (c.outcomes[k] || 0)), `Summary mismatch ${c.caseId}`);
  const distribution = xs => { xs.sort((a, b) => a - b); return {count: xs.length, min: xs[0],
    mean: xs.reduce((a, b) => a + b, 0) / xs.length, p50: xs[Math.ceil(xs.length * .5) - 1],
    p90: xs[Math.ceil(xs.length * .9) - 1], p95: xs[Math.ceil(xs.length * .95) - 1], max: xs.at(-1)}; };
  const cells = manifest.cases;
  const levels = [1, 10, 20, 25, 30, 40, 50];
  const grouped = [];
  for (const tier of ['BEGINNER', 'MID', 'STRONG']) for (const level of levels) {
    const selected = cells.filter(c => c.cell.tier === tier && c.cell.raidLevel === level && c.cell.style === 'MAIN' && !c.cell.effects);
    grouped.push({tier, level, cases: selected.map(compact)});
  }
  const report = {purpose: 'INDEPENDENT_EXPLORATORY_NOT_BALANCE_ACCEPTANCE', rawRows: rows,
    rawSha256: manifest.rawSha256, inputPolicy: 'ACTUAL_PRODUCTION_NO_OVERLAY', validation: 'PASS',
    intervals: `${expectedPairs} independent pairs per cell; pointwise Hoeffding halfwidth ${Math.sqrt(Math.log(40)/(2*expectedPairs))}, not simultaneous`,
    overall: aggregate(cells), distributions: {rounds: distribution(rounds), actions: distribution(actions)}, timeoutCells,
    mainByTierLevel: grouped,
    upperLevelNonzeroWins: cells.filter(c => c.cell.raidLevel >= 30 && (c.outcomes.WIN || 0) > 0).map(compact),
    lowerLevelWeakCells: cells.filter(c => c.cell.raidLevel <= 10 && c.playerWinsPerAttempt < .2).map(compact),
    extraStyles: cells.filter(c => ['COMMAND_CUIRASS','RITUAL','PIERCE_ARCANE','RAPIER_ARCANE'].includes(c.cell.style)).map(compact),
    specializations: cells.filter(c => ['MOBILE','COMMON_FRONT_PRESSURE','PRESSURE','FLANK'].includes(c.cell.style)).map(compact),
    effects: cells.filter(c => c.cell.effects).map(compact),
    largestSideOrderGaps: cells.map(c => { const o=c.outcomesByOrder; const a=(o.ENEMIES_FIRST.WIN||0)/expectedPairs,
      b=(o.PLAYERS_FIRST.WIN||0)/expectedPairs; return {...compact(c), orderGap: b-a}; }).sort((a,b)=>Math.abs(b.orderGap)-Math.abs(a.orderGap)).slice(0,20)};
  console.log(JSON.stringify(report, null, 2));
})().catch(e => { console.error(e); process.exitCode = 1; });
