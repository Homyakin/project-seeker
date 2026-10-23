import { createReadStream, readFileSync, readdirSync, existsSync } from 'node:fs';
import { createGunzip, gunzipSync } from 'node:zlib';
import { createHash } from 'node:crypto';
import { createInterface } from 'node:readline';
import { dirname, resolve, relative } from 'node:path';
import { fileURLToPath } from 'node:url';

const evidence = dirname(fileURLToPath(import.meta.url)), repo = resolve(evidence, '../..');
const check = (condition, message) => { if (!condition) throw new Error(message); };
const text = path => readFileSync(path, 'utf8');
const sha = bytes => createHash('sha256').update(bytes).digest('hex');
const props = path => Object.fromEntries(text(path).trim().split(/\r?\n/).map(line => line.split(/=(.*)/s).slice(0, 2)));
const json = path => JSON.parse(path.endsWith('.gz') ? gunzipSync(readFileSync(path)) : text(path));
function table(path) {
  const [header, ...rows] = text(path).trim().split(/\r?\n/), keys = header.split('\t');
  return rows.map(row => Object.fromEntries(row.split('\t').map((value, index) => [keys[index], value])));
}
function close(actual, expected, context) {
  check(Number.isFinite(Number(actual)) && Math.abs(Number(actual) - expected) < 0.00000051, context);
}
const u64 = value => BigInt.asUintN(64, value);
function mix(value) {
  value = u64((value ^ (value >> 30n)) * 0xbf58476d1ce4e5b9n);
  value = u64((value ^ (value >> 27n)) * 0x94d049bb133111ebn);
  return value ^ (value >> 31n);
}
function expectedSeed(root, group, level, pair) {
  let hash = 0xcbf29ce484222325n;
  for (const byte of Buffer.from(group)) hash = u64((hash ^ BigInt(byte)) * 0x100000001b3n);
  const domain = mix(mix(BigInt(root)) ^ hash ^ (BigInt(level) << 32n));
  return BigInt.asIntN(64, mix(u64(domain + 0x9e3779b97f4a7c15n * BigInt(pair)))).toString();
}
const cachedHashes = new Map();
function verifyHashes(path) {
  let count = 0;
  for (const line of text(path).trim().split(/\r?\n/)) {
    const match = line.match(/^([a-f0-9]{64})\s+(.+)$/);
    check(match, `Invalid source manifest line: ${line}`);
    const target = resolve(repo, match[2].startsWith('game/') ? match[2] : `game/${match[2]}`);
    check(!relative(repo, target).startsWith('..'), `Manifest path escaped repository: ${target}`);
    if (!cachedHashes.has(target)) cachedHashes.set(target, sha(readFileSync(target)));
    check(cachedHashes.get(target) === match[1], `Source hash differs: ${target}`);
    count++;
  }
  return { file: relative(evidence, path), entries: count, sha256: sha(readFileSync(path)) };
}
async function verifyRun(name, prefix, niche, expectedRoot, expectedCases) {
  const path = suffix => resolve(evidence, prefix + suffix);
  const run = props(path('run.properties'));
  const complete = props(path('complete.properties'));
  const root = run.rootSeed ?? run.seed, pairs = Number(run.pairs);
  check(root === String(expectedRoot), `${name}: unexpected root seed`);
  check(Number.isInteger(pairs) && pairs >= 2, `${name}: invalid pair count`);
  const manifest = json(path(niche ? 'cases.json.gz' : 'cases.json'));
  if (!niche) check(manifest.rootSeed === expectedRoot && manifest.pairsPerCondition === pairs && manifest.maxRounds === Number(run.maxRounds), `${name}: manifest/run`);
  const inputs = niche ? manifest : manifest.cases;
  const cases = new Map(inputs.map(entry => { const c = entry.condition ?? entry.case; return [c.code, { c, pairs: new Map(), rows: 0, sums: {}, outcomes: { WIN: 0, LOSS: 0, DRAW: 0, TIMEOUT: 0 }, maxRounds: 0, maxDistance: 0 }]; }));
  check(cases.size === expectedCases && inputs.length === expectedCases, `${name}: case count`);
  const summaries = table(path('summary.tsv')), summary = new Map(summaries.map(row => [row.case, row]));
  check(summary.size === cases.size && summaries.length === cases.size, `${name}: summary cases`);
  const seeds = new Set(), stream = createReadStream(path('battles.tsv.gz')).pipe(createGunzip());
  let keys;
  for await (const line of createInterface({ input: stream, crlfDelay: Infinity })) {
    if (!keys) { keys = line.split('\t'); continue; }
    const values = line.split('\t');
    check(values.length === keys.length, `${name}: raw column count`);
    const row = Object.fromEntries(keys.map((key, index) => [key, values[index]])), state = cases.get(row.case);
    check(state && Object.hasOwn(state.outcomes, row.outcome), `${name}: unknown case/outcome`);
    for (const field of ['family', 'tier', 'level']) if (field in row) check(row[field] === String(state.c[field]), `${row.case}: raw ${field}`);
    const pair = Number(row.pair), reverse = row.reverse === 'true' ? 2 : row.reverse === 'false' ? 1 : 0;
    check(Number.isInteger(pair) && pair >= 0 && pair < pairs && reverse, `${row.case}: pair/orientation`);
    check(row.seed === expectedSeed(root, state.c.seedGroup, state.c.level, pair), `${row.case}: seed derivation`);
    const observation = state.pairs.get(pair) ?? { seed: row.seed, orientations: 0, score: 0, timeout: false };
    check(observation.seed === row.seed && !(observation.orientations & reverse), `${row.case}: duplicate/mismatched pair`);
    observation.orientations |= reverse;
    observation.score += row.outcome === 'WIN' ? 0.5 : row.outcome === 'DRAW' ? 0.25 : 0;
    observation.timeout ||= row.outcome === 'TIMEOUT';
    state.pairs.set(pair, observation); state.rows++; state.outcomes[row.outcome]++; seeds.add(row.seed);
    for (const key of keys.filter(key => !['case', 'family', 'tier', 'level', 'pair', 'seed', 'reverse', 'outcome'].includes(key))) {
      check(Number.isFinite(Number(row[key])) && Number(row[key]) >= 0, `${row.case}: invalid ${key}`);
      state.sums[key] = (state.sums[key] ?? 0) + Number(row[key]);
    }
    state.maxRounds = Math.max(state.maxRounds, Number(row.rounds));
    state.maxDistance = Math.max(state.maxDistance, Number(row.maxForcedDistance ?? 0));
    check(Number(row.rounds) <= Number(run.maxRounds), `${row.case}: rounds cap`);
    if (!niche) check(Number(row.forcedMoveViolations) === 0 && Number(row.maxForcedDistance) <= 1, `${row.case}: movement violation`);
  }
  const totals = { battles: 0, timeouts: 0, rounds: 0, maxRounds: 0, hitAndRunMoves: 0, forcedMovesAudited: 0, forcedMoveViolations: 0, maxForcedDistance: 0 };
  for (const [code, state] of cases) {
    const s = summary.get(code), resolved = [...state.pairs.values()].filter(pair => !pair.timeout);
    check(s && state.rows === 2 * pairs && state.pairs.size === pairs, `${code}: raw count`);
    for (const field of ['family', 'tier', 'level']) if (field in s) check(s[field] === String(state.c[field]), `${code}: summary ${field}`);
    if (!niche) close(s.size, state.c.left.length, `${code}: summary team size`);
    check([...state.pairs.values()].every(pair => pair.orientations === 3), `${code}: missing orientation`);
    for (const [raw, field] of [['WIN', 'wins'], ['LOSS', 'losses'], ['DRAW', 'draws'], ['TIMEOUT', 'timeouts']]) close(s[field], state.outcomes[raw], `${code}: ${field}`);
    close(s.pairs, pairs, `${code}: pairs`); close(s.resolvedPairs, resolved.length, `${code}: resolvedPairs`);
    const score = resolved.reduce((sum, pair) => sum + pair.score, 0) / resolved.length;
    check(resolved.length > 0, `${code}: no resolved pairs`);
    close(s.score, score, `${code}: score`); close(s.meanRounds, state.sums.rounds / state.rows, `${code}: rounds sum`);
    close(s.maxRounds, state.maxRounds, `${code}: max rounds`);
    const radius = Math.sqrt(Math.log(40) / (2 * resolved.length));
    close(s.lower, Math.max(0, score - radius), `${code}: score lower`); close(s.upper, Math.min(1, score + radius), `${code}: score upper`);
    if (niche) close(s.hitRunMoves, state.sums.hitRunMoves / state.rows, `${code}: mean hitRunMoves`);
    else for (const key of ['forcedMoves', 'forcedMoveViolations', 'hitAndRunMoves', 'retreatMoves', 'knockbackMoves']) close(s[key], state.sums[key], `${code}: ${key}`);
    if (!niche) close(s.maxForcedDistance, state.maxDistance, `${code}: maximum distance`);
    for (const key of keys.filter(key => /^(left_|right_)/.test(key))) close(s[niche ? key : `mean${key[0].toUpperCase()}${key.slice(1)}`], state.sums[key] / state.rows, `${code}: mean ${key}`);
    for (const side of ['Left', 'Right']) if (!niche) close(s[`mean${side}ReactionDamage`], state.sums[`${side.toLowerCase()}ReactionDamage`] / state.rows, `${code}: reactions`);
    totals.battles += state.rows; totals.timeouts += state.outcomes.TIMEOUT; totals.rounds += state.sums.rounds;
    totals.maxRounds = Math.max(totals.maxRounds, state.maxRounds); totals.hitAndRunMoves += state.sums.hitRunMoves ?? state.sums.hitAndRunMoves;
    totals.forcedMovesAudited += state.sums.forcedMoves ?? 0; totals.forcedMoveViolations += state.sums.forcedMoveViolations ?? 0;
    totals.maxForcedDistance = Math.max(totals.maxForcedDistance, state.maxDistance);
  }
  close(run.cases, cases.size, `${name}: run cases`); close(complete.cases, cases.size, `${name}: complete cases`);
  close(complete.battles, totals.battles, `${name}: completed battles`); close(complete.timeouts, totals.timeouts, `${name}: completed timeouts`);
  if (!niche) { close(complete.rootSeed, expectedRoot, `${name}: completed root`); close(complete.pairs, pairs, `${name}: completed pairs`); close(complete.forcedMoveViolations, 0, `${name}: completed movement`); check(complete.movementAuditPassed === 'true', `${name}: audit flag`); }
  return { name, rootSeed: root, pairsPerCondition: pairs, conditions: cases.size, uniqueSeeds: seeds.size, ...totals, sourceManifest: verifyHashes(path('inputs.sha256')), cases, seeds };
}
function contrast(run, onCode, offCode) {
  const on = run.cases.get(onCode), off = run.cases.get(offCode), differences = [];
  check(on && off && on.pairs.size === off.pairs.size, `Missing contrast case: ${onCode}`);
  for (const [index, a] of on.pairs) {
    const b = off.pairs.get(index);
    check(b && a.seed === b.seed, `${onCode}: unpaired contrast`);
    if (!a.timeout && !b.timeout) differences.push(a.score - b.score);
  }
  const n = differences.length, delta = differences.reduce((sum, value) => sum + value, 0) / n;
  check(n > 0, `${onCode}: no resolved contrast pairs`);
  const radius = Math.sqrt(2 * Math.log(40) / n);
  return { on: onCode, off: offCode, resolvedSeedPairs: n, delta, pointwise95: [Math.max(-1, delta - radius), Math.min(1, delta + radius)], radius };
}
const runs = [await verifyRun('diagnostic', 'diagnostic/', false, 2026102405, 94),
  await verifyRun('confirmation', 'confirmation/', false, 2026102406, 94),
  await verifyRun('mobile-niche', 'mobile-niche-', true, 2026102407, 12)];
const seedOverlap = [];
for (let i = 0; i < runs.length; i++) for (let j = i + 1; j < runs.length; j++) {
  const overlap = [...runs[i].seeds].filter(seed => runs[j].seeds.has(seed)).length;
  check(overlap === 0, `Overlapping root-seed domains: ${runs[i].name}/${runs[j].name}`);
  seedOverlap.push({ runs: [runs[i].name, runs[j].name], overlappingSeeds: overlap });
}
const nicheDeltas = [...runs[2].cases.keys()].filter(code => !code.endsWith('_OFF')).sort().map(code => contrast(runs[2], code, `${code}_OFF`));
const halberdDeltas = [...runs[1].cases.keys()].filter(code => /^HALBERD_VS_(CROSSBOW|STAFF_ORB)_(DUEL|ANCHORED3)_SCALING_V5_/.test(code)).sort()
  .map(code => contrast(runs[1], code, code.replace('_SCALING_V5_', '_SCALING_V4_')));
check(nicheDeltas.length === 6 && halberdDeltas.length === 12, 'Contrast coverage differs');
const referenceConfirmation = [...runs[1].cases.values()].filter(state => state.c.family === 'REFERENCE').map(state => {
  const resolved = [...state.pairs.values()].filter(pair => !pair.timeout), n = resolved.length;
  const score = resolved.reduce((sum, pair) => sum + pair.score, 0) / n, radius = Math.sqrt(Math.log(40) / (2 * n));
  return { case: state.c.code, tier: state.c.tier, resolvedSeedPairs: n, score, pointwise95: [Math.max(0, score - radius), Math.min(1, score + radius)] };
});
const nicheUsedCodes = [...new Set([...runs[2].cases.values()].flatMap(state => [...state.c.left, ...state.c.right].flatMap(gear => gear.build.itemCodes)))].sort();
const catalogFile = (version, file) => resolve(repo, `game/src/main/resources/game-data/catalog/scaling_v${version}/${file}.toml`);
const catalogBlocks = version => new Map(text(catalogFile(version, 'item_objects')).split('[[item]]').slice(1)
  .map(block => [block.match(/^code\s*=\s*"([^"]+)"/m)?.[1], block.trim()]));
const v4 = catalogBlocks(4), v5 = catalogBlocks(5);
check(!nicheUsedCodes.includes('halberd') && nicheUsedCodes.every(code => v4.has(code) && v4.get(code) === v5.get(code)), 'Niche V4/V5 equipped objects differ');
check(sha(readFileSync(catalogFile(4, 'item_modifiers'))) === sha(readFileSync(catalogFile(5, 'item_modifiers'))), 'Niche V4/V5 modifiers differ');
const fullFinished = process.argv.includes('--full-build-finished');
let tests = { status: 'full_build_unconfirmed', note: 'Rerun with --full-build-finished after the full Maven build exits.' };
if (fullFinished) {
  tests = { status: 'complete', suites: 0, tests: 0, failures: 0, errors: 0, skipped: 0 };
  for (const file of readdirSync(resolve(repo, 'game/target/surefire-reports')).filter(file => /^TEST-.*\.xml$/.test(file))) {
    const tag = text(resolve(repo, 'game/target/surefire-reports', file)).match(/<testsuite\b[^>]*>/)?.[0];
    check(tag, `Invalid test XML: ${file}`); tests.suites++;
    for (const key of ['tests', 'failures', 'errors', 'skipped']) tests[key] += Number(tag.match(new RegExp(`\\b${key}="([^"]+)"`))?.[1] ?? 0);
  }
  check(tests.suites > 0 && tests.failures === 0 && tests.errors === 0, 'Full test XML has failures or no suites');
  tests.executed = tests.tests - tests.skipped;
}
const sourceManifests = ['final-source-inputs.sha256', 'unchanged-enhancement-inputs.sha256'].filter(file => existsSync(resolve(evidence, file))).map(file => verifyHashes(resolve(evidence, file)));
const aggregate = { battles: 0, timeouts: 0, rounds: 0, maxRounds: 0, hitAndRunMoves: 0, forcedMovesAudited: 0, forcedMoveViolations: 0, maxForcedDistance: 0 };
for (const run of runs) for (const key of Object.keys(aggregate)) aggregate[key] = key.startsWith('max') ? Math.max(aggregate[key], run[key]) : aggregate[key] + run[key];
console.log(JSON.stringify({ schema: 'Step123CorrectionsVerification/v1', verified: true, tests, aggregate,
  forcedMovementAuditedBattles: runs[0].battles + runs[1].battles,
  runs: runs.map(({ cases, seeds, ...run }) => run), seedOverlap, sourceManifests,
  intervals: { confidence: 0.95, unit: 'one seed pair averaged across both orientations', scoreRange: [0, 1], scoreRadius: 'sqrt(ln(40)/(2*n))', differenceRange: [-1, 1], differenceRadius: 'sqrt(2*ln(40)/n)', scope: 'Pointwise bounds for each listed estimate; no simultaneous or multiple-comparison guarantee. Contrasts exclude any seed pair with a timeout in either variant.' },
  nicheScope: '12 historical Study V4-gear conditions, six ON-minus-OFF contrasts; current corrected engine. Their equipped objects/modifiers match V5 (no halberd); no claim about other builds or PvE wins. Niche raw logs count HIT_AND_RUN moves but do not audit all forced movement distances.',
  nicheUsedCodes, referenceConfirmation,
  referenceScope: 'MAIN3 is the frozen Guardian/physical-ranger/magical-ranger composition. BEGINNER/MID/STRONG identify equipment inputs, not guaranteed combat effectiveness. These PvP observations do not justify tuning PvE difficulty from MAIN alone; use the set of alternative reference compositions. Fixtures were not retuned after observing these outcomes.',
  nicheDeltas, halberdV5MinusV4: { duel: halberdDeltas.filter(row => row.on.includes('_DUEL_')), anchored3: halberdDeltas.filter(row => row.on.includes('_ANCHORED3_')) }
}, null, 2));
