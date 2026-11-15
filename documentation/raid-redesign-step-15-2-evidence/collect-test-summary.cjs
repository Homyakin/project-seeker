const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');

const [reports, buildLog, destination] = process.argv.slice(2);
assert(reports && buildLog && destination, 'Specify Surefire directory, final build log and new output file');
const affected = new Set(['MyconidColonyAdaptationTest', 'PveAdaptationStudyTest', 'PveTeamAssessmentTest',
  'PveScalingDiagnosticsTest', 'PveCharacteristicScalingTest', 'RaidNpcCombatVersionTest', 'MyconidTeamScalingTest']);
const suites = fs.readdirSync(reports).filter(name => /^TEST-.*\.xml$/.test(name)).sort().map(name => {
  const xml = fs.readFileSync(path.join(reports, name), 'utf8');
  const tag = xml.match(/<testsuite\s+[^>]*>/)?.[0];
  assert(tag, name);
  const attributes = Object.fromEntries([...tag.matchAll(/([\w.-]+)="([^"]*)"/g)].map(m => [m[1], m[2]]));
  return {name: attributes.name, tests: Number(attributes.tests), failures: Number(attributes.failures),
    errors: Number(attributes.errors), skipped: Number(attributes.skipped)};
});
const totals = {tests: 0, failures: 0, errors: 0, skipped: 0};
for (const suite of suites) for (const key of Object.keys(totals)) {
  assert(Number.isInteger(suite[key]) && suite[key] >= 0);
  totals[key] += suite[key];
}
const log = fs.readFileSync(buildLog);
assert(log.includes('[INFO] BUILD SUCCESS'), 'Maven did not report success');
assert(log.includes('You have 0 Checkstyle violations.'), 'Checkstyle did not report zero violations');
assert.equal(totals.failures + totals.errors, 0);
const affectedSuites = suites.filter(s => affected.has(s.name.split('.').at(-1)));
assert.equal(affectedSuites.length, affected.size);
assert(affectedSuites.every(s => s.skipped === 0));
const result = {status: 'FINAL_CLEAN_PACKAGE_SUCCESS', command: 'mvn -o clean package',
  java: '25.0.1, compilation target 24', maven: '3.9.12', explicitMockitoAgent: '5.18.0',
  totals: {...totals, passed: totals.tests - totals.failures - totals.errors - totals.skipped},
  checkstyleViolations: 0, postgresEnabled: false, botOrApplicationStarted: false,
  affectedTests: affectedSuites.reduce((sum, s) => sum + s.tests, 0), affectedSuites,
  buildLogSha256: crypto.createHash('sha256').update(log).digest('hex'), suites};
fs.writeFileSync(destination, JSON.stringify(result, null, 2) + '\n', {flag: 'wx'});
console.log(JSON.stringify({totals: result.totals, affectedTests: result.affectedTests, checkstyle: 0}));
