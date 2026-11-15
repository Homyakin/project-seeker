const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');

function canonical(value) {
  if (Array.isArray(value)) return value.map(canonical);
  if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort().map(key => [key, canonical(value[key])]));
  return value;
}

async function lines(file, consume) {
  const reader = readline.createInterface({ input: fs.createReadStream(file).pipe(zlib.createGunzip()), crlfDelay: Infinity });
  for await (const line of reader) if (line) consume(JSON.parse(line));
}

(async () => {
  const folder = process.argv[2];
  if (!folder) throw new Error('Specify an extracted baseline directory');
  const summary = JSON.parse(fs.readFileSync(path.join(folder, 'summary.json'), 'utf8'));
  assert.equal(summary.purpose, 'PRE_ADAPTATION_DIAGNOSTIC_NOT_ACCEPTANCE');
  assert.equal(summary.playerSkills, 'SCALING_SKILLS_V3');
  assert.equal(summary.npcSkills, 'LEGACY_SKILLS_V1');
  assert.equal(summary.tempo, 'ACCELERATED_V1');
  const cases = new Map(summary.cases.map(c => [c.caseId, c]));
  assert.equal(cases.size, summary.cases.length);
  const definitions = new Map();
  await lines(path.join(folder, 'cases.jsonl.gz'), c => {
    assert(!definitions.has(c.caseId));
    assert.deepEqual(c.cell, cases.get(c.caseId).cell);
    assert.equal(c.members.length, c.cell.size);
    for (const member of c.members) {
      assert.equal(member.catalog, 'SCALING_V5');
      for (const item of member.items) {
        const level = c.cell.mode === 'ANOMALY' ? 0 : ({ BEGINNER: 0, MID: 3, STRONG: 6 })[c.cell.tier];
        assert.equal(item.enhanceLevel, level);
      }
    }
    definitions.set(c.caseId, c);
  });
  assert.equal(definitions.size, cases.size);
  const totals = new Map();
  const pairs = new Map();
  const positions = new Map();
  const firstPairs = new Map();
  let records = 0;
  await lines(path.join(folder, 'battles.jsonl.gz'), row => {
    const cell = cases.get(row.caseId).cell;
    if (row.pair === 0) firstPairs.set(row.caseId + ':' + row.reverse, JSON.stringify(canonical(row)));
    assert(Number.isSafeInteger(row.generationSeed) && Number.isSafeInteger(row.battleSeed));
    assert.equal(row.generationSeed, summary.generationRoot + row.caseId * 1000 + row.pair);
    assert.equal(row.battleSeed, summary.battleRoot + row.caseId * 1000 + row.pair);
    assert(row.pair >= 0 && row.pair < summary.pairs && typeof row.reverse === 'boolean');
    assert(row.rounds >= 1 && row.rounds <= summary.roundLimit);
    assert(row.playerPower > 0 && Number.isFinite(row.playerPower));
    assert(row.enemyPower > 0 && Number.isFinite(row.enemyPower));
    assert.equal(row.enemyBasis.length, cell.mode === 'RAID'
      ? ['WOLFPACK', 'ZOMBIE_HORDE'].includes(cell.encounter) ? cell.size + 1 : Math.max(2, cell.size)
      : cell.size === 1 ? 2 : cell.size === 2 ? row.enemyBasis.length : cell.size);
    if (cell.mode === 'ANOMALY' && cell.size === 2) assert([2, 3].includes(row.enemyBasis.length));
    for (const npc of row.enemyBasis) {
      assert(npc.health >= 1 && npc.attack >= 1 && npc.defense >= 1);
      assert.equal(npc.skills.length, 1);
      assert.equal(npc.skills[0].formulaVersion, 'LEGACY_SKILLS_V1');
      assert.equal(npc.skills[0].points, 4);
    }
    const expectedOutcome = row.playerTotals.survivors > 0 ? 'WIN' : row.enemyTotals.survivors > 0 ? 'LOSS' : 'DRAW';
    if (row.outcome !== 'TIMEOUT') assert.equal(row.outcome, expectedOutcome);
    assert(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(row.outcome));
    const total = totals.get(row.caseId) || { WIN: 0, LOSS: 0, DRAW: 0, TIMEOUT: 0, rounds: 0, max: 0, ratio: 0, counts: new Set() };
    total[row.outcome]++; total.rounds += row.rounds; total.max = Math.max(total.max, row.rounds);
    total.ratio += row.enemyPower / row.playerPower; total.counts.add(row.enemyBasis.length);
    totals.set(row.caseId, total);
    const key = row.caseId + ':' + row.pair;
    const pair = pairs.get(key) || [];
    assert(!pair.some(previous => previous.reverse === row.reverse));
    pair.push({ reverse: row.reverse, basis: row.enemyBasis, outcome: row.outcome });
    pairs.set(key, pair);
    row.enemyBasis.forEach((npc, index) => {
      const key = cell.encounter + ':' + npc.skills[0].code + ':' + npc.declaredPosition + '->' + row.actualEnemyPositions?.[index];
      positions.set(key, (positions.get(key) || 0) + 1);
    });
    records++;
  });
  for (const [id, total] of totals) {
    const c = cases.get(id);
    assert.equal(total.WIN, c.wins); assert.equal(total.LOSS, c.losses);
    assert.equal(total.DRAW, c.draws); assert.equal(total.TIMEOUT, c.timeouts);
    assert.equal(c.battles, 2 * summary.pairs);
    assert.equal(total.WIN + total.LOSS + total.DRAW + total.TIMEOUT, c.battles);
    assert.equal(total.max, c.maxRounds);
    assert(Math.abs(total.rounds / c.battles - c.meanRounds) < 1e-10);
    assert(Math.abs(total.ratio / c.battles - c.meanEnemyPowerRatio) < 1e-10);
    assert.deepEqual([...total.counts].sort((a, b) => a - b), c.enemyCounts);
    for (let n = 0; n < summary.pairs; n++) {
      const pair = pairs.get(id + ':' + n);
      assert.equal(pair.length, 2);
      assert.deepEqual(pair[0].basis, pair[1].basis);
      const score = pair.reduce((sum, row) => sum + (row.outcome === 'WIN' ? 1 : row.outcome === 'DRAW' ? 0.5 : 0), 0) / 2;
      assert.equal(score, c.pairScores[n]);
    }
  }
  assert.equal(totals.size, cases.size);
  assert.equal(records, cases.size * summary.pairs * 2);
  const anomaliesTwo = summary.cases.filter(c => c.cell.mode === 'ANOMALY' && c.cell.size === 2);
  const result = {
    status: 'PASS', purpose: summary.purpose, cells: cases.size, records,
    raidCells: summary.cases.filter(c => c.cell.mode === 'RAID').length,
    anomalyCells: summary.cases.filter(c => c.cell.mode === 'ANOMALY').length,
    pairsPerCell: summary.pairs, timeouts: summary.cases.reduce((sum, c) => sum + c.timeouts, 0),
    draws: summary.cases.reduce((sum, c) => sum + c.draws, 0), maxRounds: Math.max(...summary.cases.map(c => c.maxRounds)),
    anomalyN2Cells: anomaliesTwo.length, anomalyN2BothCounts: anomaliesTwo.filter(c => c.enemyCounts.join(',') === '2,3').length,
    positions: Object.fromEntries([...positions].sort()),
    perCellHoeffding95HalfWidth: Math.sqrt(Math.log(40) / (2 * summary.pairs)),
    caveat: 'Unadapted mixed player V3/NPC LEGACY diagnostic, not target PvE acceptance. Paired orders are dependent.'
  };
  if (process.argv[3]) {
    let compared = 0;
    await lines(path.join(process.argv[3], 'battles.jsonl.gz'), row => {
      assert.equal(row.pair, 0);
      const key = row.caseId + ':' + row.reverse;
      assert.equal(JSON.stringify(canonical(row)), firstPairs.get(key), 'Replay mismatch: ' + key);
      compared++;
    });
    assert.equal(compared, firstPairs.size);
    result.replayComparedBattles = compared;
    result.replayMatches = true;
  }
  console.log(JSON.stringify(result, null, 2));
})().catch(error => { console.error(error); process.exit(1); });
