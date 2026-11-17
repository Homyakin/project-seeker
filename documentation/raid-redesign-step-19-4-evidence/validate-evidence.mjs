import assert from 'node:assert/strict';
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {fileURLToPath} from 'node:url';

const directory = path.dirname(fileURLToPath(import.meta.url));
const finalFile = process.argv[2] ?? 'final-completion.jsonl';
const read = name => fs.readFileSync(path.join(directory, name), 'utf8').trim().split('\n').map(JSON.parse);
const baseline = read('baseline-completion-v5.jsonl');
const final = read(finalFile);
const counts = rows => Object.fromEntries([...new Set(rows.map(row => row.scenario))]
  .sort().map(scenario => [scenario, rows.filter(row => row.scenario === scenario).length]));
const expected = {
  database: 1, concurrent: 2, 'empty-filtered-fallback': 1, 'exact-payout': 20,
  'reduced-fallback-roster': 4, 'ready-mid-transition-fault': 4, 'phase-rollback': 2,
  'notification-failure': 2, 'stale-final': 10, 'fault-restart': 20,
  'stale-deadline': 2, 'stale-phase': 2, 'mid-transition-fault': 4, 'historical-final': 2,
};
assert.deepEqual(counts(baseline), Object.fromEntries(Object.entries(expected).sort()));
assert.deepEqual(counts(final), Object.fromEntries(Object.entries({...expected,
  idempotence: 2, 'accepted-completion': 4, 'accepted-noop': 5,
  'accepted-rollback': 1, 'accepted-expiry-concurrency': 1,
}).sort()));
assert.equal(baseline.length, 76);
assert.equal(final.length, 89);
baseline.forEach(row => assert.equal(row.stage, 'BASELINE'));
final.forEach(row => assert.equal(row.stage, 'FINAL'));
const jsonRows = (snapshot, key) => snapshot[key].map(JSON.parse);
const status = snapshot => jsonRows(snapshot, 'event')[0].status_id;
const reward = (route, victory) => route === 'SAFE' ? (victory ? [10, 2] : [5, 1])
  : (victory ? [20, 5] : [15, 3]);
function committed(snapshot, route, victory, size) {
  assert.equal(status(snapshot), victory ? 3 : 2);
  assert.equal(snapshot.log.length, 1);
  assert.equal(snapshot.results.length, size);
  assert.equal(snapshot.personages.length, size);
  const [money, shards] = reward(route, victory);
  for (const personage of jsonRows(snapshot, 'personages')) {
    assert.equal(personage.money, 200 + money);
    assert.equal(personage.storm_shards, 7 + shards);
  }
  for (const result of jsonRows(snapshot, 'results')) {
    assert.equal(result.reward, money);
    assert.equal(result.storm_shards, shards);
    assert.ok(result.stats);
    assert.equal(result.generated_item_id, null);
    assert.equal(result.generated_contraband_id, null);
  }
}
const invariant = (before, after) => {
  for (const key of ['items', 'ratings', 'groupStats']) assert.deepEqual(after[key], before[key]);
};
for (const row of baseline.filter(row => row.scenario === 'fault-restart')) {
  const d = row.details;
  if (d.fault === 'LOG_INSIDE') assert.deepEqual(d.afterFailure, d.before);
  else assert.notDeepEqual(d.afterFailure, d.before);
}
const retryCounts = counts(baseline.filter(row => row.scenario === 'fault-restart')
  .map(row => ({scenario: row.details.retry})));
assert.deepEqual(retryCounts, {'AlreadyFinal': 6, 'ERROR: PSQLException': 12, 'PveBattleFinished': 2});
for (const {scenario, details: d} of final) {
  if (scenario === 'exact-payout') {
    committed(d.after, d.route, d.victory, d.size);
    invariant(d.before, d.after);
  } else if (scenario === 'fault-restart') {
    assert.deepEqual(d.afterFailure, d.before);
    assert.equal(d.retry, 'PveBattleFinished');
    committed(d.afterRetry, d.route, true, 3);
    invariant(d.before, d.afterRetry);
  } else if (['phase-rollback', 'mid-transition-fault', 'ready-mid-transition-fault'].includes(scenario)) {
    assert.deepEqual(d.afterFailure, d.before);
    assert.equal(status(d.afterRetry), 0);
    assert.equal(d.afterRetry.log.length, 0);
    assert.equal(jsonRows(d.afterRetry, 'anomaly')[0].phase, d.route === 'SAFE' ? 'PVE_WAITING' : 'SEARCHING');
  } else if (scenario === 'stale-deadline') {
    assert.equal(d.result, 'NotDue');
    assert.deepEqual(d.before, d.after);
  } else if (scenario === 'stale-phase') {
    assert.equal(d.secondResult, 'NotDue');
    assert.deepEqual(d.afterFirst, d.afterSecond);
  } else if (scenario === 'stale-final') {
    assert.equal(d.result, 'AlreadyFinal');
    assert.deepEqual(d.before, d.after);
  } else if (scenario === 'concurrent') {
    assert.deepEqual([...d.outcomes].sort(), ['AlreadyFinal', 'PveBattleFinished']);
    assert.equal(d.secondWaitedOnDatabaseRowLock, true);
    committed(d.after, d.route, true, 5);
  } else if (scenario === 'idempotence') committed(d.after, d.route, true, 5);
  else if (scenario === 'notification-failure') {
    committed(d.after, d.route, true, 3);
    invariant(d.before, d.after);
  } else if (scenario === 'empty-filtered-fallback') assert.deepEqual(d.before, d.after);
  else if (scenario === 'reduced-fallback-roster') {
    assert.equal(d.after.results.length, d.remaining);
    assert.equal(d.after.log.length, 1);
    const people = jsonRows(d.after, 'personages');
    people.forEach((p, i) => {
      assert.equal(p.money, i < d.remaining ? 220 : 200);
      assert.equal(p.storm_shards, i < d.remaining ? 12 : 7);
    });
    invariant(d.before, d.after);
  } else if (['accepted-completion', 'accepted-rollback', 'accepted-expiry-concurrency'].includes(scenario)) {
    const after = d.after ?? d.afterRetry;
    assert.equal(status(after.host), 3);
    assert.equal(status(after.guest), 5);
    assert.equal(after.host.log.length, 1);
    assert.equal(after.host.results.length, 6);
    assert.deepEqual(after.host.items, d.before.host.items);
    assert.equal(jsonRows(after.host, 'personages').reduce((sum, p) => sum + p.money, 0), 6 * 200 + 3 * 30 + 3 * 25);
    assert.equal(jsonRows(after.host, 'personages').reduce((sum, p) => sum + p.storm_shards, 0), 6 * 7 + 3 * 10 + 3 * 8);
    if (scenario === 'accepted-expiry-concurrency') assert.equal(d.secondWaitedOnDatabaseRowLock, true);
  }
}
const hash = name => crypto.createHash('sha256').update(fs.readFileSync(path.join(directory, name))).digest('hex');
const output = {status: 'PASS', baselineRows: baseline.length, finalRows: final.length,
  baselineScenarios: counts(baseline), finalScenarios: counts(final), baselineFaultRetryResults: retryCounts,
  inputFiles: {'baseline-completion-v5.jsonl': hash('baseline-completion-v5.jsonl'), [finalFile]: hash(finalFile)},
  limitations: ['Controlled firstWin exercises payout branches, not balance.',
    'No automatic repair of pre-existing partially completed events.',
    'Empty fallback remains rejected without writes.', 'No exactly-once Telegram delivery guarantee.']};
fs.writeFileSync(path.join(directory, finalFile === 'final-completion.jsonl' ? 'validation.json' : 'validation-clean.json'),
  JSON.stringify(output, null, 2) + '\n');
console.log(JSON.stringify(output, null, 2));
