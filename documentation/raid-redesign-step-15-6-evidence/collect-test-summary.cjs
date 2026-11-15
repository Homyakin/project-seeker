const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');

const [reports, buildLog, destination, phase] = process.argv.slice(2);
assert(reports && buildLog && destination && phase, 'Specify reports, log, new destination and phase');
const affected = new Set([
  'EquipmentPowerCalculatorTest', 'ReferencePowerCalculatorTest', 'PowerDisplayServiceTest',
  'BattlePersonageEquipmentPowerTest', 'BattlePersonagePersonageEffectsTest', 'PowerDependencyGuardTest',
  'MyconidTeamScalingTest', 'MyconidColonyAdaptationTest', 'RaidProcessingCombatVersionTest',
  'RaidNpcCombatVersionTest', 'PveCharacteristicScalingTest', 'PveScalingDiagnosticsTest',
  'PveTeamAssessmentTest', 'PveAdaptationStudyTest',
]);
const suites = fs.readdirSync(reports).filter(name => /^TEST-.*\.xml$/.test(name)).sort().map(name => {
  const xml = fs.readFileSync(path.join(reports, name), 'utf8');
  const tag = xml.match(/<testsuite\s+[^>]*>/)?.[0];
  assert(tag, name);
  const attrs = Object.fromEntries([...tag.matchAll(/([\w.-]+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
  const result = {name: attrs.name};
  for (const key of ['tests', 'failures', 'errors', 'skipped']) {
    result[key] = Number(attrs[key]);
    assert(Number.isInteger(result[key]) && result[key] >= 0, `${name}: ${key}`);
  }
  return result;
});
const totals = {tests: 0, failures: 0, errors: 0, skipped: 0};
for (const suite of suites) for (const key of Object.keys(totals)) totals[key] += suite[key];
totals.passed = totals.tests - totals.failures - totals.errors - totals.skipped;
const log = fs.readFileSync(buildLog);
assert(log.includes('[INFO] BUILD SUCCESS'), 'Maven did not succeed');
assert(log.includes('You have 0 Checkstyle violations.'), 'Checkstyle did not report zero violations');
assert.equal(totals.failures + totals.errors, 0);
const summary = [...log.toString('utf8').matchAll(
  /^\[[A-Z]+\] Tests run: (\d+), Failures: (\d+), Errors: (\d+), Skipped: (\d+)\s*$/gm
)].at(-1);
assert(summary, 'Missing final Maven test totals');
assert.deepEqual([totals.tests, totals.failures, totals.errors, totals.skipped], summary.slice(1).map(Number));
const affectedSuites = suites.filter(suite => affected.has(suite.name.split('.').at(-1)));
assert.equal(affectedSuites.length, affected.size);
assert(affectedSuites.every(suite => suite.skipped === 0 && suite.tests > 0));
if (phase === 'TARGETED_CLEAN_TEST') assert.equal(suites.length, affected.size);
const result = {
  phase, java: '25.0.1; compilation target 24', maven: '3.9.12', mockitoAgent: '5.18.0',
  totals, checkstyleViolations: 0,
  affectedTests: affectedSuites.reduce((sum, suite) => sum + suite.tests, 0), affectedSuites,
  buildLogSha256: crypto.createHash('sha256').update(log).digest('hex'), suites,
  applicationStarted: false, databaseStarted: false, releaseEnabled: false,
};
fs.writeFileSync(destination, JSON.stringify(result, null, 2) + '\n', {flag: 'wx'});
console.log(JSON.stringify({phase, totals, affectedTests: result.affectedTests, checkstyle: 0}));
