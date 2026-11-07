#!/usr/bin/env node
'use strict';
// Read-only consistency audit of Nov07ComparativeStudy artifacts. No engine replay is implied.
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const repo = path.resolve(__dirname, '../..');
const prices = { COMMON: 0, UNCOMMON: 150, RARE: 450, EPIC: 975, LEGENDARY: 1725 };
const points = { COMMON: 0, UNCOMMON: 1, RARE: 2, EPIC: 3, LEGENDARY: 4 };
const allSlots = ['BODY', 'GLOVES', 'HELMET', 'MAIN_HAND', 'OFF_HAND', 'PANTS', 'SHOES'];
const catalogs = new Map();
const statFields = {
    rounds: r => r.rounds, actions: r => r.left.actions + r.right.actions,
    firstDeath: r => r.firstDeath,
    leftMaxNonAttackOwnTurns: r => r.leftMaxNonAttackOwnTurns,
    rightMaxNonAttackOwnTurns: r => r.rightMaxNonAttackOwnTurns,
    leftNeverAttacked: r => r.leftNeverAttacked, rightNeverAttacked: r => r.rightNeverAttacked,
    leftHealing: r => r.left.healing, rightHealing: r => r.right.healing,
    leftMovement: r => r.left.movementTurns, rightMovement: r => r.right.movementTurns,
    leftInitiativeRemoved: r => r.left.initiativeRemoved,
    rightInitiativeRemoved: r => r.right.initiativeRemoved,
};
function eq(actual, expected, label) { assert.deepStrictEqual(actual, expected, label); }
function near(actual, expected, label) {
    assert(Number.isFinite(actual) && Math.abs(actual - expected) <= 1e-9 * Math.max(1, Math.abs(expected)), label);
}
function integer(value, min, max, label) {
    assert(Number.isSafeInteger(value) && value >= min && value <= max, label);
}
function javaHash(s) {
    let hash = 0;
    for (let i = 0; i < s.length; i++) hash = (Math.imul(hash, 31) + s.charCodeAt(i)) | 0;
    return BigInt(hash);
}
function seed(root, group, pair) {
    const u64 = value => BigInt.asUintN(64, value);
    let x = u64(root + javaHash(group) + 0x9e3779b97f4a7c15n * (BigInt(pair) + 1n));
    x = u64((x ^ (x >> 30n)) * 0xbf58476d1ce4e5b9n);
    x = u64((x ^ (x >> 27n)) * 0x94d049bb133111ebn);
    return BigInt.asIntN(64, x ^ (x >> 31n));
}
function stats(values) {
    const a = [...values].sort((x, y) => x - y), n = a.length;
    return { mean: a.reduce((s, v) => s + v, 0) / n,
        median: (a[Math.floor((n - 1) / 2)] + a[Math.floor(n / 2)]) / 2,
        p95: a[Math.ceil(n * .95) - 1], min: a[0], max: a[n - 1] };
}
function catalog(version) {
    if (!catalogs.has(version)) {
        assert(/^SCALING_V\d+$/.test(version), 'Unknown catalog name');
        const source = fs.readFileSync(path.join(repo,
            'game/src/main/resources/game-data/catalog', version.toLowerCase(), 'item_objects.toml'), 'utf8');
        const map = new Map();
        for (const block of source.split(/^\[\[item\]\]\s*$/m).slice(1)) {
            const code = block.match(/^code\s*=\s*"([^"]+)"/m)?.[1];
            const slots = block.match(/^slots\s*=\s*\[([^\]]+)\]/m)?.[1].match(/"[^"]+"/g)?.map(s => JSON.parse(s));
            assert(code && slots && !map.has(code), 'Invalid catalog code/slots');
            map.set(code, slots);
        }
        assert(map.size > 0, 'Empty catalog'); catalogs.set(version, map);
    }
    return catalogs.get(version);
}
function equipment(member, gear, level, version) {
    eq(member.build, gear.build, 'Equipment build differs from declared gear');
    eq(member.catalog, version, 'Equipment catalog differs'); eq(gear.catalog, version, 'Gear catalog differs');
    eq(member.items.map(i => i.code), gear.build.itemCodes, 'Equipped item list differs');
    const slots = [], assignments = gear.build.assignments;
    let gold = 0;
    for (const item of member.items) {
        const occupied = catalog(version).get(item.code); assert(occupied, 'Unknown equipped item');
        slots.push(...occupied); eq(item.enhanceLevel, level, 'Enhancement differs');
        const assignment = assignments[item.code];
        eq(item.rarity, assignment?.rarity ?? 'COMMON', 'Rarity differs from assignment');
        eq(item.skill, assignment?.skill ?? 'NONE', 'Skill differs from assignment');
        eq(item.skillPoints, assignment ? points[item.rarity] * occupied.length : 0, 'Skill points differ');
        assert(Object.hasOwn(prices, item.rarity), 'Unknown rarity');
        gold += occupied.length * 100 + prices[item.rarity];
    }
    eq(slots.sort(), allSlots, 'Build must fill seven distinct slots');
    for (const carrier of Object.keys(assignments)) assert(gear.build.itemCodes.includes(carrier), 'Unequipped modifier');
    eq(member.nominalGold, gold, 'Member nominal gold differs');
    return gold;
}
async function validate(prefix) {
    const summaryText = fs.readFileSync(prefix + '-summary.json', 'utf8');
    const s = JSON.parse(summaryText), root = BigInt(summaryText.match(/"rootSeed"\s*:\s*(-?\d+)/)[1]);
    const cases = JSON.parse(zlib.gunzipSync(fs.readFileSync(prefix + '-cases.json.gz')));
    integer(s.pairs, 1, 1e7, 'Invalid pair count');
    assert(/^SCALING_V\d+$/.test(s.catalog), 'Invalid scaling catalog');
    eq(s.combat, 'SCALING_SKILLS_V3', 'Unexpected combat formula');
    eq(s.tempo, 'ACCELERATED_V1', 'Unexpected tempo'); eq(s.multiplier, 1.75, 'Unexpected multiplier');
    eq(s.cases, cases.length, 'Condition count differs'); eq(s.summaries.length, cases.length, 'Summary count differs');
    eq(s.battles, cases.length * s.pairs * 2, 'Declared battle count differs');
    const summaries = new Map(), states = new Map();
    for (const row of s.summaries) { assert(!summaries.has(row.case), 'Duplicate summary'); summaries.set(row.case, row); }
    for (const def of cases) {
        const c = def.case, row = summaries.get(c.code); assert(row && !states.has(c.code), 'Unknown/duplicate case');
        eq(Object.keys(row).filter(k => row[k] && typeof row[k] === 'object' && 'mean' in row[k]).sort(),
            Object.keys(statFields).sort(), 'Unknown or missing summary statistic');
        eq(row.family, c.family, 'Family differs'); eq(row.budget, c.budget, 'Budget differs');
        eq(row.level, c.level, 'Level differs'); eq(row.size, c.left.length, 'Size differs');
        eq(c.left.length, c.right.length, 'Unequal team sizes'); eq(row.pairs, s.pairs, 'Pair count differs');
        for (const side of ['left', 'right']) {
            const members = def[side + 'Equipment']; eq(members.length, c[side].length, 'Roster differs');
            const gold = members.reduce((n, m, i) => n + equipment(m, c[side][i], c.level, s.catalog), 0);
            eq(def[side + 'NominalGold'], gold, 'Case nominal gold differs');
            eq(row[side + 'NominalGold'], gold, 'Summary nominal gold differs');
            eq(row[side + 'Build'], c[side].at(-1).build.code, 'Summary member label differs');
        }
        if (c.family === 'MATRIX') eq(def.leftNominalGold, def.rightNominalGold, 'Matrix nominal budgets unequal');
        states.set(c.code, { c, row, seen: new Set(), outcomes: {}, scores: Array(s.pairs).fill(0),
            fields: Object.fromEntries(Object.keys(statFields).map(k => [k, []])), moves: 0, violations: 0 });
    }
    let battles = 0, timeouts = 0, violations = 0;
    const input = fs.createReadStream(prefix + '-pairs.jsonl.gz').pipe(zlib.createGunzip());
    for await (const line of readline.createInterface({ input, crlfDelay: Infinity })) {
        if (!line.trim()) continue;
        const raw = JSON.parse(line), state = states.get(raw.case), r = raw.result;
        assert(state && r, 'Unknown raw case or missing nested result');
        integer(raw.pair, 0, s.pairs - 1, 'Pair index out of range'); eq(typeof raw.reverse, 'boolean', 'Invalid order');
        const key = raw.pair * 2 + Number(raw.reverse); assert(!state.seen.has(key), 'Duplicate pair/order'); state.seen.add(key);
        const lexemes = [...line.matchAll(/"seed"\s*:\s*(-?\d+)(?=\s*[,}])/g)];
        eq(lexemes.length, 1, 'Expected one integer seed lexeme'); const lexeme = lexemes[0][1];
        assert(lexeme && BigInt(lexeme) === seed(root, state.c.seedGroup, raw.pair), 'Exact Java long seed differs');
        assert(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(r.outcome), 'Unknown outcome');
        integer(r.rounds, 1, s.maxRounds, 'Invalid rounds');
        for (const side of ['left', 'right']) {
            for (const value of Object.values(r[side])) integer(value, 0, Number.MAX_SAFE_INTEGER, 'Invalid team measure');
            integer(r[side].survivors, 0, state.c[side].length, 'Invalid survivors');
        }
        const outcome = r.left.survivors > 0 && r.right.survivors > 0 ? 'TIMEOUT'
            : r.left.survivors > 0 ? 'WIN' : r.right.survivors > 0 ? 'LOSS' : 'DRAW';
        eq(r.outcome, outcome, 'Outcome contradicts survivors');
        state.outcomes[r.outcome] = (state.outcomes[r.outcome] ?? 0) + 1;
        state.scores[raw.pair] += (r.outcome === 'WIN' ? 1 : r.outcome === 'LOSS' ? 0 : .5) / 2;
        for (const [field, get] of Object.entries(statFields)) { const value = get(r);
            integer(value, field === 'firstDeath' ? -1 : 0, Number.MAX_SAFE_INTEGER, 'Invalid raw stat'); state.fields[field].push(value); }
        integer(r.forcedMoves, 0, Number.MAX_SAFE_INTEGER, 'Invalid forced moves');
        integer(r.violations, 0, r.forcedMoves, 'Invalid movement violations');
        state.moves += r.forcedMoves; state.violations += r.violations;
        battles++; timeouts += Number(r.outcome === 'TIMEOUT'); violations += r.violations;
    }
    let hoeffdingCells = 0, exploratoryCells = 0;
    for (const state of states.values()) {
        const { row, scores } = state;
        eq(state.seen.size, s.pairs * 2, 'Missing pair/order'); eq(row.outcomes, state.outcomes, 'Outcomes differ');
        const mean = scores.reduce((a, b) => a + b, 0) / s.pairs; near(row.score, mean, 'Score differs');
        for (const [field, values] of Object.entries(state.fields)) {
            const derived = stats(values); eq(Object.keys(row[field]).sort(), Object.keys(derived).sort(), 'Stat schema differs');
            for (const [key, value] of Object.entries(derived)) near(row[field][key], value, field + '.' + key);
        }
        eq(row.forcedMoves, state.moves, 'Move count differs'); eq(row.forcedMoveViolations, state.violations, 'Violations differ');
        if (row.pairedHoeffding95) { const delta = Math.sqrt(Math.log(40) / (2 * s.pairs));
            near(row.pairedHoeffding95.low, Math.max(0, mean - delta), 'Hoeffding low differs');
            near(row.pairedHoeffding95.high, Math.min(1, mean + delta), 'Hoeffding high differs'); hoeffdingCells++; }
        if (row.pairMean95) { const variance = scores.reduce((a, x) => a + (x - mean) ** 2, 0) / (s.pairs - 1);
            const delta = 2.1 * Math.sqrt(variance / s.pairs);
            near(row.pairMean95.low, s.pairs < 2 ? 0 : Math.max(0, mean - delta), 'Old exploratory low differs');
            near(row.pairMean95.high, s.pairs < 2 ? 1 : Math.min(1, mean + delta), 'Old exploratory high differs'); exploratoryCells++; }
    }
    eq(battles, s.battles, 'Raw battle count differs');
    return { ok: true, prefix: path.resolve(prefix), cases: states.size, battles, mirroredPairs: battles / 2,
        timeouts, forcedMoveViolations: violations, hoeffdingCells, exploratoryCells,
        oldIntervals: exploratoryCells ? 'Exploratory empirical t intervals; degenerate all-win bounds are not confidence evidence' : 'none',
        limitations: 'Validates compact raw records and manifests, not full event journals or engine execution provenance' };
}
(async () => {
    assert(process.argv.length >= 3, 'Usage: node validate-study.cjs PREFIX [PREFIX...]');
    const results = []; for (const prefix of process.argv.slice(2)) results.push(await validate(prefix));
    console.log(JSON.stringify(results.length === 1 ? results[0] : results, null, 2));
})().catch(error => { console.log(JSON.stringify({ ok: false, error: error.message })); process.exitCode = 1; });
