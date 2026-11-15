// Usage: node validate-study.cjs <study-directory> [independent-replay-directory]
// Reads only explicitly selected evidence, never application configuration or a database.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const readline = require('node:readline');
const {createGunzip} = require('node:zlib');
const {createHash} = require('node:crypto');

async function validate(directory) {
  const summary = JSON.parse(fs.readFileSync(path.join(directory, 'summary.json'), 'utf8'));
  assert.equal(summary.purpose, 'STRUCTURAL_CHECK_NOT_BALANCE_ACCEPTANCE');
  assert.equal(summary.playerSkills, 'SCALING_SKILLS_V3');
  assert.equal(summary.npcSkills, 'SCALING_SKILLS_V3');
  assert.equal(summary.tempo, 'ACCELERATED_V1');
  const cells = new Map(summary.cases.map(cell => [cell.caseId, cell]));
  assert.equal(cells.size, summary.cases.length);
  const seen = new Map();
  const uniqueTrials = new Set();
  const outcomes = {};
  const modes = {};
  const calibration = {};
  let battles = 0, maxRounds = 0, attempts = 0, maxRelativeCalibrationError = 0;
  let previous;
  const input = fs.createReadStream(path.join(directory, 'battles.jsonl.gz')).pipe(createGunzip());
  const digest = createHash('sha256');
  input.on('data', chunk => digest.update(chunk));
  for await (const line of readline.createInterface({input, crlfDelay: Infinity})) {
    const row = JSON.parse(line);
    const cell = cells.get(row.caseId);
    assert.ok(cell, `Unknown case ${row.caseId}`);
    assert.ok(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < summary.pairs);
    assert.equal(typeof row.reverse, 'boolean');
    const trialKey = `${row.caseId}:${row.pair}:${row.reverse}`;
    assert.ok(!uniqueTrials.has(trialKey), `Duplicate trial ${trialKey}`);
    uniqueTrials.add(trialKey);
    assert.deepEqual(row.cell, cell.cell);
    assert.equal(row.generationSeed, summary.generationRoot + row.caseId * 1000 + row.pair);
    assert.equal(row.battleSeed, summary.battleRoot + row.caseId * 1000 + row.pair);
    assert.equal(row.playerSkills, summary.playerSkills);
    assert.equal(row.npcSkills, summary.npcSkills);
    assert.equal(row.tempo, summary.tempo);
    assert.equal(row.catalog, summary.catalog);
    assert.ok(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(row.outcome));
    assert.ok(Number.isInteger(row.rounds) && row.rounds > 0 && row.rounds <= summary.roundLimit);
    assert.equal(row.members.length, row.cell.size);
    assert.equal(row.playerBasis.length, row.cell.size);
    const n = row.cell.size;
    let expectedTarget;
    if (row.cell.mode === 'RAID') {
      const log = (base, value) => Math.log(value) / Math.log(base);
      const count = Math.max(2, n);
      let group, correction = 1;
      switch (row.cell.encounter) {
        case 'WOLFPACK': group = n <= 13 ? 1 + log(2.2, n) * 0.045 : 1.1 + log(10, n - 13) * 0.045; break;
        case 'ZOMBIE_HORDE': group = 1 + log(n <= 10 ? 2.2 : n <= 13 ? 4 : 8, n) * 0.05; break;
        case 'MYCONID_COLONY': group = 1 + log(count <= 7 ? 2.2 : 4, count) * 0.05; correction = 0.77; break;
        case 'MAGGEESE_FLOCK': group = 1 + log(count <= 10 ? 2.2 : 4, count) * 0.05; correction = 0.75; break;
        default: assert.fail(`Unknown raid ${row.cell.encounter}`);
      }
      expectedTarget = row.playerPower * (1 + (row.cell.raidLevel - 10) * 0.06 + 0.14) * group * correction;
      assert.equal(row.enemyBasis.length, ['WOLFPACK', 'ZOMBIE_HORDE'].includes(row.cell.encounter) ? n + 1 : count);
    } else {
      assert.ok(['ANOMALY_SAFE', 'ANOMALY_FALLBACK'].includes(row.cell.mode));
      expectedTarget = row.playerPower * 0.92;
      assert.ok((n === 2 ? [2, 3] : [n === 1 ? 2 : n]).includes(row.enemyBasis.length));
    }
    assert.ok(Math.abs(expectedTarget - row.calibration.targetPower) / expectedTarget < 1e-12);
    const relativeError = Math.abs(row.enemyPower - row.calibration.achievedPower) / row.enemyPower;
    assert.ok(relativeError < 1e-12, `Final/trial power differs: ${relativeError}`);
    maxRelativeCalibrationError = Math.max(maxRelativeCalibrationError,
      Math.abs(row.calibration.targetPower - row.enemyPower) / row.calibration.targetPower);
    assert.ok(row.calibration.evaluations > 0 && row.calibration.evaluations <= 1090);
    for (const enemy of row.enemyBasis) {
      assert.equal(enemy.skills.length, 1);
      assert.equal(enemy.skills[0].formulaVersion, summary.npcSkills);
      assert.equal(enemy.skills[0].points, 4);
    }
    for (const player of row.playerBasis) {
      assert.ok(player.skills.every(skill => skill.formulaVersion === summary.playerSkills));
    }
    for (const member of row.members) {
      const expected = member.sourceItems.map(item => row.cell.mode === 'RAID' ? item : {...item, enhanceLevel: 0});
      assert.deepEqual(member.combatItems, expected);
      assert.ok(member.sourceItems.every(item => item.enhanceLevel === {BEGINNER: 0, MID: 3, STRONG: 6}[row.cell.tier]));
    }
    assert.equal(Object.keys(row.sourceEffects).length, row.cell.effects ? 2 : 0);
    assert.equal(Object.keys(row.combatEffects).length, row.cell.mode === 'RAID' && row.cell.effects ? 2 : 0);
    if (row.outcome !== 'TIMEOUT') {
      assert.equal(row.initState.tempoVersion, summary.tempo);
      assert.equal(row.actualEnemyPositions.length, row.enemyBasis.length);
      for (let i = 0; i < row.enemyBasis.length; i++) {
        const snapshot = row.initState.personagesById[row.enemyBasis[i].id];
        assert.equal(row.initState.lines[snapshot.lineIndex].position, row.actualEnemyPositions[i]);
      }
      assert.ok(/^[a-f0-9]{64}$/.test(row.actionSha256));
      assert.ok(/^[a-f0-9]{64}$/.test(row.traceSha256));
      assert.ok(row.actionCount > 0 && row.normalAttempts > 0);
      attempts += row.normalAttempts;
    }
    if (row.reverse) {
      assert.ok(previous && !previous.reverse && previous.caseId === row.caseId && previous.pair === row.pair);
      assert.deepEqual(row.calibration, previous.calibration);
      assert.deepEqual(row.enemyBasis, previous.enemyBasis);
      assert.deepEqual(row.playerBasis, previous.playerBasis);
      assert.deepEqual(row.members, previous.members);
    }
    previous = row;
    const aggregate = seen.get(row.caseId) || {outcomes: {}, rounds: 0, maxRounds: 0, count: 0};
    aggregate.outcomes[row.outcome] = (aggregate.outcomes[row.outcome] || 0) + 1;
    aggregate.rounds += row.rounds;
    aggregate.maxRounds = Math.max(aggregate.maxRounds, row.rounds);
    aggregate.count++;
    seen.set(row.caseId, aggregate);
    outcomes[row.outcome] = (outcomes[row.outcome] || 0) + 1;
    modes[row.cell.mode] = (modes[row.cell.mode] || 0) + 1;
    calibration[row.calibration.status] = (calibration[row.calibration.status] || 0) + 1;
    maxRounds = Math.max(maxRounds, row.rounds);
    battles++;
  }
  assert.equal(battles, cells.size * summary.pairs * 2);
  for (const [id, cell] of cells) {
    const aggregate = seen.get(id);
    assert.ok(aggregate);
    assert.equal(aggregate.count, summary.pairs * 2);
    assert.deepEqual(aggregate.outcomes, cell.outcomes);
    assert.equal(aggregate.rounds / aggregate.count, cell.meanRounds);
    assert.equal(aggregate.maxRounds, cell.maxRounds);
  }
  return {cells: cells.size, battles, outcomes, modes, calibration, maxRounds, attempts,
    maxRelativeCalibrationError, uncompressedBattlesSha256: digest.digest('hex')};
}

(async () => {
  assert.ok(process.argv.length === 3 || process.argv.length === 4,
    'Specify a study directory and optionally an independent replay directory');
  const result = await validate(process.argv[2]);
  if (process.argv[3]) {
    const replay = await validate(process.argv[3]);
    assert.deepEqual(result, replay, 'Independent process replay differs');
    assert.deepEqual(JSON.parse(fs.readFileSync(path.join(process.argv[2], 'summary.json'), 'utf8')),
      JSON.parse(fs.readFileSync(path.join(process.argv[3], 'summary.json'), 'utf8')));
    result.independentReplayMatched = true;
  }
  console.log(JSON.stringify(result, null, 2));
})().catch(error => { console.error(error); process.exitCode = 1; });
