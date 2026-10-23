import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';
import {fileURLToPath} from 'node:url';

// Run after all experiments from any directory. Read-only; JSON on stdout is the audit record.
const evidence = path.dirname(fileURLToPath(import.meta.url));
const game = path.resolve(evidence, '../../game');
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const require = (condition, message) => { if (!condition) throw new Error(message); };
const table = text => {
  const lines = text.trim().split('\n'), columns = lines.shift().split('\t');
  return lines.map(line => Object.fromEntries(line.split('\t').map((value, index) => [columns[index], value])));
};
const sourceRows = fs.readFileSync(path.join(evidence, 'final-source-inputs.sha256'), 'utf8').trim().split('\n');
for (const row of sourceRows) {
  require(row.slice(0, 64) === sha(fs.readFileSync(path.join(game, row.slice(66)))), `Source differs: ${row.slice(66)}`);
}
const seedsByRun = new Map(), runs = [];
for (const prefix of ['diagnostic', 'mobile-line', 'mobile-niche', 'confirmation', 'confirmation-slash',
  'mobile-answers', 'accessible-pressure']) {
  const summary = table(fs.readFileSync(path.join(evidence, `${prefix}-summary.tsv`), 'utf8'));
  JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(evidence, `${prefix}-cases.json.gz`))));
  const seeds = new Set(), counts = new Map();
  const key = row => `${row.case}:${row.level ?? row.case.match(/_L(\d+)(?:_OFF)?$/)?.[1]}`;
  const raw = zlib.gunzipSync(fs.readFileSync(path.join(evidence, `${prefix}-battles.tsv.gz`))).toString('utf8').trim().split('\n');
  const columns = raw.shift().split('\t');
  for (const line of raw) {
    const fields = line.split('\t'), row = Object.fromEntries(columns.map((column, index) => [column, fields[index]]));
    const code = key(row);
    if (!counts.has(code)) counts.set(code, {WIN: 0, LOSS: 0, DRAW: 0, TIMEOUT: 0, max: 0, pairs: new Map()});
    const count = counts.get(code);
    require(Object.hasOwn(count, row.outcome), `Unknown outcome ${row.outcome}`);
    count[row.outcome]++;
    count.max = Math.max(count.max, Number(row.rounds));
    const pair = count.pairs.get(row.pair) ?? {seed: row.seed, orders: new Set()};
    require(pair.seed === row.seed && !pair.orders.has(row.reverse), `Duplicate order or mismatched pair ${code}`);
    pair.orders.add(row.reverse); count.pairs.set(row.pair, pair); seeds.add(row.seed);
  }
  let battles = 0, timeouts = 0, draws = 0, maximumRounds = 0;
  require(counts.size === summary.length, `Missing summary rows in ${prefix}`);
  for (const row of summary) {
    const count = counts.get(key(row));
    require(count, `Missing observations ${prefix}:${row.case}`);
    for (const [outcome, field] of [['WIN', 'wins'], ['LOSS', 'losses'], ['DRAW', 'draws'], ['TIMEOUT', 'timeouts']]) {
      require(count[outcome] === Number(row[field]), `Outcome count differs ${prefix}:${row.case}:${outcome}`);
    }
    require(count.pairs.size === Number(row.pairs) && [...count.pairs.values()].every(pair => pair.orders.size === 2),
      `Incomplete paired observations ${prefix}:${row.case}`);
    require(count.max === Number(row.maxRounds), `Maximum rounds differs ${prefix}:${row.case}`);
    const size = count.WIN + count.LOSS + count.DRAW + count.TIMEOUT;
    require(size === 2 * Number(row.pairs), `Wrong observation count ${prefix}:${row.case}`);
    require(count.TIMEOUT === 0, `Unfinished outcomes need explicit review ${prefix}:${row.case}`);
    require(Math.abs(Number(row.score) - (count.WIN + count.DRAW / 2) / size) < 0.000001,
      `Score differs ${prefix}:${row.case}`);
    battles += size; timeouts += count.TIMEOUT; draws += count.DRAW; maximumRounds = Math.max(maximumRounds, count.max);
  }
  const completion = fs.readFileSync(path.join(evidence, `${prefix}-complete.properties`), 'utf8');
  require(completion.includes(`battles=${battles}\n`) && completion.includes(`cases=${summary.length}\n`),
    `Completion differs ${prefix}`);
  for (const [previous, previousSeeds] of seedsByRun) {
    require(![...seeds].some(seed => previousSeeds.has(seed)), `Seed overlap ${previous}/${prefix}`);
  }
  seedsByRun.set(prefix, seeds);
  runs.push({prefix, conditions: summary.length, battles, timeouts, draws, maximumRounds, uniqueSeeds: seeds.size});
}
const reports = path.join(game, 'target/surefire-reports');
const totals = {tests: 0, failures: 0, errors: 0, skipped: 0};
let postgres;
for (const file of fs.readdirSync(reports).filter(file => /^TEST-.*\.xml$/.test(file))) {
  const xml = fs.readFileSync(path.join(reports, file), 'utf8');
  const header = xml.match(/<testsuite\s[^>]*>/)?.[0];
  require(header, `Missing suite ${file}`);
  const attributes = Object.fromEntries([...header.matchAll(/([\w]+)="([^"]*)"/g)].map(match => [match[1], match[2]]));
  for (const field of Object.keys(totals)) totals[field] += Number(attributes[field]);
  if (file.endsWith('.EquipmentCatalogPostgresIntegrationTest.xml')) postgres = attributes;
}
require(totals.tests === 1256 && totals.failures === 0 && totals.errors === 0 && totals.skipped === 19,
  `Unexpected test result ${JSON.stringify(totals)}`);
require(postgres?.tests === '7' && postgres.skipped === '0' && postgres.failures === '0' && postgres.errors === '0',
  'PostgreSQL scenarios did not all run successfully');
require(fs.readFileSync(path.join(evidence, 'full-build-final.log'), 'utf8').includes('BUILD SUCCESS'), 'No successful package');
const references = JSON.parse(fs.readFileSync(path.join(evidence, 'reference-teams.json'), 'utf8'));
require(references.teams.length === 18, 'Unexpected reference team export');
process.stdout.write(JSON.stringify({sourceFilesVerified: sourceRows.length, runs,
  seedOverlapBetweenRuns: 0, totalBattles: runs.reduce((sum, run) => sum + run.battles, 0),
  tests: {...totals, executed: totals.tests - totals.skipped}, postgresExecuted: 7, referenceTeams: 18}, null, 2) + '\n');
