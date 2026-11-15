#!/usr/bin/env node
'use strict';

// Validate archived runs without loading the uncompressed battle stream into memory.
// Usage: node validate-myconids.cjs RUN_DIRECTORY [EXACT_REPLAY_DIRECTORY]
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const readline = require('node:readline');

const GENERATION_ROOT = 2026110815000;
const BATTLE_ROOT = 3026110815000;
const FORMULA = 'SCALING_SKILLS_V3';
const TEMPO = 'ACCELERATED_V1';
const TIERS = ['BEGINNER', 'MID', 'STRONG'];
const RARITIES = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'];
const SPECIALIZATIONS = ['MOBILE', 'COMMON_FRONT_PRESSURE', 'PRESSURE', 'FLANK'];
const CAUSAL_VARIANTS = ['BASE', 'NORMAL_APPEND', 'WEAK_APPEND', 'SINGLE_ITEM_ENHANCE', 'SINGLE_MODIFIER_RARITY'];
const MIRROR_KEYS = ['members', 'sourceEffects', 'combatEffects', 'calibration', 'playerPower',
    'enemyPower', 'playerBasis', 'enemyBasis'];
const METRICS = ['rounds', 'actionCount', 'normalAttempts', 'moves', 'forcedMoves',
    ...['playerTotals', 'enemyTotals'].flatMap(side => ['survivors', 'initialHealth', 'remainHealth',
        'normalAttacksHit', 'normalDamage', 'critDamage', 'skillDamage', 'turns'].map(key => `${side}.${key}`))];
const HEX = /^[a-f0-9]{64}$/;
const LIVE_WEAK_MODEL = 'WEAK_WITH_DEFAULTS_V1_LIVE_COMMON_SWORD_PLUS_SIX_DEFAULTS_ALL_ZERO';
const OPEN_STREAMS = new Set();
const HISTORICAL_PHASES = new Set(['BEFORE', 'FIXED_RANGE', 'ADAPTED_077', 'CONFIRMATION_077',
    'CURVE_077', 'GUARD_RISK_077', 'LIVE_CAUSAL_077']);
const HISTORICAL_BASES = {guardian: {attack: 70, defense: 160, health: 420},
    slinger: {attack: 130, defense: 70, health: 280}, correction: 0.77};

function canonical(value) {
    if (Array.isArray(value)) return value.map(canonical);
    if (value && typeof value === 'object') return Object.fromEntries(Object.keys(value).sort()
        .map(key => [key, canonical(value[key])]));
    return value;
}

function equal(actual, expected, message) {
    assert.deepEqual(canonical(actual), canonical(expected), message);
}

function near(actual, expected, message) {
    assert(Number.isFinite(actual) && Number.isFinite(expected), `${message}: non-finite number`);
    assert(Math.abs(actual - expected) <= 1e-10 * Math.max(1, Math.abs(actual), Math.abs(expected)),
        `${message}: ${actual} != ${expected}`);
}

function integer(value, message, minimum = 0) {
    assert(Number.isSafeInteger(value) && value >= minimum, `${message}: ${value}`);
}

function add(counts, key) {
    counts[key] = (counts[key] || 0) + 1;
}

function distribution(values) {
    if (!values.length) return {count: 0};
    const sorted = [...values].sort((a, b) => a - b);
    const percentile = fraction => sorted[Math.max(0, Math.ceil(fraction * sorted.length) - 1)];
    return {count: values.length, mean: values.reduce((sum, value) => sum + value, 0) / values.length,
        min: sorted[0], p50: percentile(0.5), p90: percentile(0.9), p95: percentile(0.95), max: sorted.at(-1)};
}

function numericTree(actual, expected, where) {
    if (typeof expected === 'number') return near(actual, expected, where);
    if (expected === null || typeof expected !== 'object') return equal(actual, expected, where);
    equal(Object.keys(actual).sort(), Object.keys(expected).sort(), `${where}: keys`);
    for (const key of Object.keys(expected)) numericTree(actual[key], expected[key], `${where}.${key}`);
}

function cell(style, tier, size, raidLevel, effects = false) {
    return {mode: 'RAID', encounter: 'MYCONID_COLONY', style, tier, size, raidLevel, effects};
}

function matrix(styles, extraLevels) {
    const cells = [];
    for (const tier of TIERS) for (const size of [...Array.from({length: 15}, (_, index) => index + 1), 30]) {
        for (const level of [1, 10, 25, 50]) cells.push(cell('MAIN', tier, size, level));
    }
    for (const tier of TIERS) {
        for (const style of SPECIALIZATIONS) for (const level of [1, 10, 50]) cells.push(cell(style, tier, 3, level));
        for (const size of [3, 15]) for (const level of [1, 10, 50]) cells.push(cell('MAIN', tier, size, level, true));
    }
    for (const style of styles) for (const tier of TIERS) for (const size of [1, 3, 7, 15]) {
        for (const level of [1, 10, 50]) cells.push(cell(style, tier, size, level));
    }
    for (const style of ['MAIN', ...styles]) for (const tier of TIERS) for (const size of [3, 7, 15]) {
        for (const level of extraLevels) cells.push(cell(style, tier, size, level));
    }
    return cells.map((value, index) => ({caseId: index + 1, cell: value}));
}

function causalBase(groupId, extraLevels) {
    let current = 0;
    for (const tier of ['MID', 'STRONG']) for (const size of [1, 2, 3, 7, 8, 15]) {
        for (const level of [1, 10]) if (++current === groupId) return cell('MAIN', tier, size, level);
    }
    for (const tier of ['MID', 'STRONG']) for (const size of [1, 2, 3, 7, 8, 15]) {
        for (const level of extraLevels) if (++current === groupId) return cell('MAIN', tier, size, level);
    }
    throw new Error(`Invalid causal group ${groupId}`);
}

async function parametersFor(directory, phase, warnings) {
    let parameters;
    try {
        parameters = JSON.parse(await fs.promises.readFile(path.join(directory, 'parameters.json'), 'utf8'));
    } catch (error) {
        if (error.code !== 'ENOENT') throw error;
        assert(HISTORICAL_PHASES.has(phase), `Unknown phase ${phase} requires explicit parameters.json; no historical fallback`);
        return {...structuredClone(HISTORICAL_BASES), g: phase === 'BEFORE' ? 'LEGACY_CUTOFF_7' : 'LOG4',
            origin: 'EXPLICIT_HISTORICAL_PHASE_PROFILE'};
    }
    assert(parameters && typeof parameters === 'object' && !Array.isArray(parameters), 'parameters.json must contain an object');
    for (const side of ['guardian', 'slinger']) {
        equal(Object.keys(parameters[side]).sort(), ['attack', 'defense', 'health'], `parameters.${side}: exactly the three scaled bases`);
        for (const key of ['attack', 'defense', 'health']) {
            integer(parameters[side][key], `parameters.${side}.${key}`, 1);
            assert(parameters[side][key] <= Math.floor(2147483647 / 16), `parameters.${side}.${key}: above safe NPC bound`);
        }
    }
    assert(Number.isFinite(parameters.correction) && parameters.correction >= 0, 'parameters.correction must be finite and nonnegative');
    equal(parameters.g, 'LOG4', 'Explicit profiles currently support only the agreed LOG4 group correction');
    if (parameters.phase !== undefined) equal(parameters.phase, phase, 'Parameter phase');
    assert(HEX.test(parameters.generatorSourceSha256), 'parameters.generatorSourceSha256 must contain the generator source hash');
    if (HISTORICAL_PHASES.has(phase)) {
        assert(phase !== 'BEFORE', 'BEFORE has the legacy cutoff profile and cannot use a LOG4 parameter override');
        for (const key of ['guardian', 'slinger', 'correction']) equal(parameters[key], HISTORICAL_BASES[key],
            `Historical phase ${phase} cannot be silently relabeled with different ${key}`);
    }
    let generatorSourceVerified = false;
    try {
        const source = await fs.promises.readFile(path.join(directory, 'generator-source.java'));
        equal(crypto.createHash('sha256').update(source).digest('hex'), parameters.generatorSourceSha256,
            'Archived generator-source.java SHA256');
        generatorSourceVerified = true;
    } catch (error) {
        if (error.code !== 'ENOENT') throw error;
        warnings.push('generator-source.java is absent; the declared generator source hash could not be verified in this directory');
    }
    return {...parameters, origin: 'EXPLICIT_PARAMETERS_FILE', generatorSourceVerified};
}

function validateManifest(summary, warnings) {
    integer(summary.pairs, 'pairs', 1);
    assert(summary.pairs <= 1000, 'pairs exceeds helper limit');
    assert(['screen', 'full', 'causal'].includes(summary.suite), 'Unknown suite');
    assert(typeof summary.phase === 'string' && summary.phase.length, 'phase missing');
    const seedOffset = summary.seedOffset ?? 0;
    integer(seedOffset, 'seedOffset');
    integer(summary.generationRoot, 'generationRoot', 1);
    integer(summary.battleRoot, 'battleRoot', 1);
    equal(summary.generationRoot, GENERATION_ROOT + seedOffset, 'generationRoot');
    equal(summary.battleRoot, BATTLE_ROOT + seedOffset, 'battleRoot');
    equal(summary.catalog, 'SCALING_V5', 'catalog');
    equal(summary.playerSkills, FORMULA, 'playerSkills');
    equal(summary.npcSkills, FORMULA, 'npcSkills');
    equal(summary.tempo, TEMPO, 'tempo');
    assert(HEX.test(summary.rawSha256), 'rawSha256 missing');
    assert(Array.isArray(summary.cases) && summary.cases.length, 'No cases');
    const ids = summary.cases.map(entry => entry.caseId);
    equal(new Set(ids).size, ids.length, 'Duplicate case IDs');
    equal(ids, [...ids].sort((a, b) => a - b), 'Case IDs must be increasing');
    if (summary.suite !== 'causal') {
        equal(summary.purpose, 'MYCONID_ADAPTATION_COMPARISON_NOT_UNIVERSAL_BALANCE_ACCEPTANCE', 'purpose');
        equal(summary.caseIdPolicy, 'ONE_BASED_INDEX_IN_FULL_MATRIX_STABLE_ACROSS_PHASES_AND_SCREEN_SUBSETS', 'caseIdPolicy');
        equal(summary.seedPolicy, 'ROOT + caseId * 1000 + pair; both mirrors reconstruct fresh participants', 'seedPolicy');
        equal(summary.confidencePolicy, '95% Hoeffding by complete independent pairs, per cell, not simultaneous', 'confidencePolicy');
        equal(summary.timeoutPolicy, 'TIMEOUT is not LOSS; incomplete pairs are excluded from score intervals', 'timeoutPolicy');
        equal(summary.durationPolicy, 'Rounds and action metrics summarize completed battles only', 'durationPolicy');
        assert(Array.isArray(summary.extraStyles), 'extraStyles missing');
        equal(new Set(summary.extraStyles).size, summary.extraStyles.length, 'Duplicate extra styles');
        assert(typeof summary.onlyMain === 'boolean', 'onlyMain missing');
        const extraLevels = summary.extraLevels || [];
        assert(Array.isArray(extraLevels), 'extraLevels must be an array');
        extraLevels.forEach(level => {integer(level, 'extra level', 1); assert(level <= 50 && ![1, 10, 25, 50].includes(level));});
        equal(new Set(extraLevels).size, extraLevels.length, 'Duplicate extra levels');
        const requested = summary.caseIds || [];
        assert(Array.isArray(requested), 'caseIds must be an array');
        equal(new Set(requested).size, requested.length, 'Duplicate requested cases');
        const allCases = matrix(summary.extraStyles, extraLevels);
        requested.forEach(id => {integer(id, 'requested case ID', 1); assert(id <= allCases.length);});
        const expected = allCases.filter(entry =>
            (!requested.length || requested.includes(entry.caseId)) &&
            (summary.suite !== 'screen' || [1, 2, 3, 7, 8, 15, 30].includes(entry.cell.size)
                && entry.cell.raidLevel !== 25) && (!summary.onlyMain || entry.cell.style === 'MAIN'));
        equal(new Set(expected.map(entry => JSON.stringify(entry.cell))).size, expected.length, 'Duplicate cells');
        equal(summary.cases.map(entry => ({caseId: entry.caseId, cell: entry.cell})), expected, 'Full matrix/subset');
    } else {
        equal(summary.purpose, 'CONTROLLED_SAME_OLD_MEMBERS_COMPARISON_NOT_CANONICAL_TEAM_SIZE_COMPARISON', 'purpose');
        equal(summary.seedPolicy, 'ROOT + (10000 + causalGroupId) * 1000 + pair, shared by all group variants', 'seedPolicy');
        equal(summary.confidencePolicy, 'Per-cell 95% Hoeffding; paired-delta range [-1,1], not simultaneous', 'confidencePolicy');
        equal(summary.timeoutPolicy, 'TIMEOUT is not LOSS; comparisons use only indices complete in both variants', 'timeoutPolicy');
        equal(summary.couplingCaveat,
            'Adding a member consumes IDs and changes enemy IDs; seeds do not promise identical random draws', 'couplingCaveat');
        for (const key of ['causalGroupIds', 'caseIdPolicy']) {
            if (summary[key] === undefined) warnings.push(`Older causal helper omitted ${key}; checked stable IDs from rows`);
        }
        if (summary.caseIdPolicy !== undefined) equal(summary.caseIdPolicy,
            'Five reserved variants per stable group; filtering does not change IDs or seeds', 'caseIdPolicy');
        const extraLevels = summary.causalExtraLevels || [];
        assert(Array.isArray(extraLevels), 'causalExtraLevels must be an array');
        extraLevels.forEach(level => {integer(level, 'causal extra level', 1); assert(level <= 50 && ![1, 10].includes(level));});
        equal(new Set(extraLevels).size, extraLevels.length, 'Duplicate causal extra levels');
        const maxGroupId = 24 + 12 * extraLevels.length;
        const requested = summary.causalGroupIds || [];
        assert(Array.isArray(requested), 'causalGroupIds must be an array');
        requested.forEach(group => {integer(group, 'requested causalGroupId', 1); assert(group <= maxGroupId);});
        equal(new Set(requested).size, requested.length, 'Duplicate requested causal groups');
        const groups = [...new Set(summary.cases.map(entry => entry.causalGroupId))];
        equal(groups, requested.length ? [...requested].sort((a, b) => a - b)
            : Array.from({length: maxGroupId}, (_, index) => index + 1), 'Causal group coverage');
        const variants = summary.causalVariants || CAUSAL_VARIANTS;
        assert(Array.isArray(variants) && variants.includes('BASE'), 'causalVariants must include BASE');
        assert(variants.every(variant => CAUSAL_VARIANTS.includes(variant)), 'Unknown causal variant');
        equal(new Set(variants).size, variants.length, 'Duplicate causal variants');
        if (summary.weakParticipantModel !== undefined) equal(summary.weakParticipantModel, LIVE_WEAK_MODEL, 'Weak participant model');
    }
}

function npcPower(basis, guardian) {
    const averageReduction = [1.1, 0.9, 0.75, 1.25]
        .map(weight => 1 - basis.defense * weight / (basis.defense * weight + 500))
        .reduce((sum, value) => sum + value, 0) / 4;
    return basis.health * basis.attack * (1 + (guardian ? 0.04 : 0.08) * (guardian ? 0.5 : 0.7))
        / averageReduction * ((guardian ? 190 : 160) / 1000);
}

function validateRow(row, entry, summary, parameters, pair, reverse, where) {
    equal(row.caseId, entry.caseId, `${where}: caseId/order`);
    equal(row.cell, entry.cell, `${where}: cell`);
    equal(row.phase, summary.phase, `${where}: phase`);
    equal(row.pair, pair, `${where}: pair/order`);
    equal(row.reverse, reverse, `${where}: reverse/order`);
    const seedId = summary.suite === 'causal' ? 10000 + entry.causalGroupId : entry.caseId;
    integer(row.generationSeed, `${where}: safe generation seed`, 1);
    integer(row.battleSeed, `${where}: safe battle seed`, 1);
    equal(row.generationSeed, summary.generationRoot + seedId * 1000 + pair, `${where}: generation seed`);
    equal(row.battleSeed, summary.battleRoot + seedId * 1000 + pair, `${where}: battle seed`);
    for (const key of ['catalog', 'playerSkills', 'npcSkills', 'tempo']) equal(row[key], summary[key], `${where}: ${key}`);
    equal(row.roundLimit, 2000, `${where}: round limit`);
    if (summary.suite === 'causal') for (const key of ['causalGroupId', 'variant', 'sourceChange', 'baseCell']) {
        equal(row[key], entry[key], `${where}: ${key}`);
    }
    const count = Math.max(2, row.cell.size);
    equal(row.members.length, row.cell.size, `${where}: member count`);
    equal(row.playerBasis.length, row.cell.size, `${where}: player count`);
    equal(row.enemyBasis.length, count, `${where}: enemy count`);
    if (row.variant === 'WEAK_APPEND') {
        assert(row.members.at(-1).build !== 'WEAK_SWORD_ONLY' && row.playerBasis.at(-1).health > 0,
            `${where}: DEAD_APPEND_EXCLUDED: sword-only HP0 is not a living weak-player control`);
        equal(row.playerBasis.at(-1).health, 840, `${where}: live weak participant starts with 840 HP`);
    }
    equal(row.combatEffects, row.sourceEffects, `${where}: raid effects retained`);
    equal(Object.keys(row.sourceEffects).length > 0, row.cell.effects, `${where}: effect presence`);
    for (let index = 0; index < row.members.length; index++) {
        const member = row.members[index];
        equal(member.sourceItems, member.combatItems, `${where}: raid keeps gear and enhancement`);
        const points = {};
        for (const item of member.combatItems) {
            const rarity = RARITIES.indexOf(item.rarity);
            assert(rarity >= 0, `${where}: invalid rarity`);
            integer(item.enhanceLevel, `${where}: enhancement`);
            if (item.modifier && rarity) points[item.modifier.activeEnum] = (points[item.modifier.activeEnum] || 0)
                + rarity * item.object.slots.length;
        }
        equal([...row.playerBasis[index].skills].sort((a, b) => a.code.localeCompare(b.code)), Object.keys(points).sort().map(code =>
            ({code, points: points[code], formulaVersion: FORMULA})), `${where}: player modifier points`);
    }
    const before = summary.phase === 'BEFORE';
    const group = 1 + 0.05 * Math.log(count) / Math.log(parameters.g === 'LEGACY_CUTOFF_7' && count <= 7 ? 2.2 : 4);
    const levelBonus = 1 + ((row.cell.raidLevel - 10) * 0.06 + 0.14);
    const target = row.playerPower * levelBonus * group * parameters.correction;
    near(row.calibration.targetPower, target, `${where}: target formula`);
    near(row.calibration.achievedPower, row.enemyPower, `${where}: trial/final power`);
    assert(Number.isFinite(row.calibration.multiplier) && row.calibration.multiplier >= 0, `${where}: multiplier`);
    integer(row.calibration.evaluations, `${where}: evaluations`, 1);
    assert(['MATCHED', 'CLOSEST_STEP', 'MINIMUM_EXCEEDS_TARGET'].includes(row.calibration.status), `${where}: status`);
    let power = 0;
    for (let index = 0; index < count; index++) {
        const guardian = index < Math.floor(count / 2);
        const enemy = row.enemyBasis[index];
        const range = !before && !guardian ? 3 : 1;
        equal(enemy.declaredPosition, guardian ? 'FRONT' : 'BACK', `${where}: declared position`);
        equal(enemy.range, range, `${where}: range`);
        equal(enemy.attackTypes, ['MAGICAL'], `${where}: attack type`);
        equal(enemy.skills, [{code: guardian ? 'KNOCKBACK' : 'PRECISE_STRIKE', points: 4,
            formulaVersion: FORMULA}], `${where}: NPC points and version`);
        for (const [key, base] of Object.entries(guardian ? parameters.guardian : parameters.slinger)) {
            equal(enemy[key], Math.max(1, Math.ceil(base * row.calibration.multiplier)), `${where}: scaled ${key}`);
        }
        power += npcPower(enemy, guardian);
    }
    near(row.enemyPower, power, `${where}: independently recomputed NPC power`);
    assert(['WIN', 'LOSS', 'DRAW', 'TIMEOUT'].includes(row.outcome), `${where}: outcome`);
    integer(row.rounds, `${where}: rounds`, 1);
    assert(row.rounds <= row.roundLimit, `${where}: rounds exceed cap`);
    for (const [side, basis] of [['playerTotals', row.playerBasis], ['enemyTotals', row.enemyBasis]]) {
        const totals = row[side];
        equal(Object.keys(totals).sort(), METRICS.filter(key => key.startsWith(`${side}.`))
            .map(key => key.split('.')[1]).sort(), `${where}: totals keys`);
        for (const [key, value] of Object.entries(totals)) integer(value, `${where}: ${side}.${key}`);
        equal(totals.initialHealth, basis.reduce((sum, member) => sum + member.health, 0), `${where}: initial health`);
        assert(totals.remainHealth <= totals.initialHealth && totals.survivors <= basis.length, `${where}: totals bounds`);
        equal(totals.survivors > 0, totals.remainHealth > 0, `${where}: survivors versus health`);
    }
    if (row.outcome === 'TIMEOUT') {
        equal(row.rounds, row.roundLimit, `${where}: timeout rounds`);
        assert(row.initState === undefined && row.actualEnemyPositions === undefined, `${where}: timeout has no result`);
        return;
    }
    equal(row.outcome, row.playerTotals.survivors ? 'WIN' : row.enemyTotals.survivors ? 'LOSS' : 'DRAW', `${where}: outcome totals`);
    assert(!row.playerTotals.survivors || !row.enemyTotals.survivors, `${where}: completed battle has both sides alive`);
    for (const metric of METRICS.filter(key => !key.includes('.'))) integer(row[metric], `${where}: ${metric}`);
    for (const key of ['actionSha256', 'traceSha256']) assert(HEX.test(row[key]), `${where}: ${key}`);
    equal(row.initState.tempoVersion, TEMPO, `${where}: init tempo`);
    if (row.variant === 'WEAK_APPEND') equal(row.initState.personagesById[row.playerBasis.at(-1).id].health,
        840, `${where}: live weak participant has 840 HP in actual init`);
    equal(Object.keys(row.initState.personagesById).length, count + row.cell.size, `${where}: init count`);
    const placed = row.initState.lines.flatMap(line => line.personageIds);
    equal(new Set(placed).size, placed.length, `${where}: duplicate placements`);
    equal(placed.length, count + row.cell.size, `${where}: all participants placed`);
    const actual = [];
    for (let index = 0; index < count; index++) {
        const enemy = row.enemyBasis[index];
        const initial = row.initState.personagesById[enemy.id];
        const guardian = index < Math.floor(count / 2);
        assert(initial, `${where}: enemy missing in init`);
        const line = row.initState.lines[initial.lineIndex];
        assert(line.personageIds.includes(enemy.id), `${where}: line membership`);
        equal(initial.firstTeam, !reverse, `${where}: enemy team`);
        equal(line.firstTeam, !reverse, `${where}: enemy line side`);
        equal(line.position, before || guardian ? 'FRONT' : 'BACK', `${where}: actual NPC position`);
        equal(initial.skills, enemy.skills, `${where}: init skills`);
        equal(initial.defenses, {ARCANE: enemy.defense}, `${where}: ARCANE defense`);
        equal(initial.attacksByRange, Array.from({length: enemy.range}, (_, slot) =>
            ({range: slot + 1, attack: {MAGICAL: enemy.attack}})), `${where}: attack basis at every supported distance`);
        actual.push(line.position);
    }
    equal(row.actualEnemyPositions, actual, `${where}: actual position list`);
}

function newAccumulator() {
    return {outcomes: {}, outcomesByOrder: {}, metrics: Object.fromEntries(METRICS.map(key => [key, []])),
        positions: {}, statuses: {}, errors: [], scoresByIndex: [], rows: 0, first: null, members: null};
}

function accumulate(accumulator, row) {
    accumulator.rows++;
    add(accumulator.outcomes, row.outcome);
    const order = row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST';
    add(accumulator.outcomesByOrder[order] ||= {}, row.outcome);
    add(accumulator.statuses, row.calibration.status);
    if (row.calibration.targetPower > 0) accumulator.errors.push(Math.abs(row.calibration.achievedPower / row.calibration.targetPower - 1));
    if (row.outcome !== 'TIMEOUT') {
        for (const key of METRICS) accumulator.metrics[key].push(key.split('.').reduce((value, part) => value[part], row));
        row.actualEnemyPositions.forEach(position => add(accumulator.positions, position));
    }
    if (!accumulator.members) accumulator.members = row.members;
    else equal(row.members, accumulator.members, 'Input gear must not vary between seeded pairs');
    if (!row.reverse) accumulator.first = row;
    else {
        for (const key of MIRROR_KEYS) equal(row[key], accumulator.first[key], `Mirrored ${key}`);
        const outcomes = [accumulator.first.outcome, row.outcome];
        accumulator.scoresByIndex.push(outcomes.includes('TIMEOUT') ? null : outcomes.reduce((score, outcome) =>
            score + (outcome === 'WIN' ? 0.5 : outcome === 'DRAW' ? 0.25 : 0), 0));
        accumulator.first = null;
    }
}

function validateCase(entry, accumulator, pairs) {
    const where = `case ${entry.caseId}`;
    const scores = accumulator.scoresByIndex.filter(score => score !== null);
    equal(entry.outcomes, accumulator.outcomes, `${where}: outcomes`);
    equal(entry.outcomesByOrder, accumulator.outcomesByOrder, `${where}: order outcomes`);
    equal(entry.attemptedBattles, accumulator.rows, `${where}: attempted`);
    equal(entry.completedBattles, accumulator.rows - (accumulator.outcomes.TIMEOUT || 0), `${where}: completed`);
    near(entry.playerWinsPerAttempt, (accumulator.outcomes.WIN || 0) / (2 * pairs), `${where}: win fraction`);
    equal(entry.completePairs, scores.length, `${where}: complete pairs`);
    equal(entry.incompletePairs, pairs - scores.length, `${where}: incomplete pairs`);
    equal(entry.pairScores, scores, `${where}: complete scores`);
    if (entry.pairScoresByIndex !== undefined) equal(entry.pairScoresByIndex, accumulator.scoresByIndex, `${where}: indexed scores`);
    if (scores.length) {
        const mean = scores.reduce((sum, score) => sum + score, 0) / scores.length;
        const halfWidth = Math.sqrt(Math.log(40) / (2 * scores.length));
        near(entry.pairedScoreMean, mean, `${where}: pair mean`);
        numericTree(entry.pairedScore95Hoeffding, {low: Math.max(0, mean - halfWidth),
            high: Math.min(1, mean + halfWidth), unclippedHalfWidth: halfWidth}, `${where}: interval`);
    } else assert(entry.pairedScoreMean === undefined && entry.pairedScore95Hoeffding === undefined);
    const metrics = Object.fromEntries(Object.entries(accumulator.metrics).filter(([, values]) => values.length)
        .map(([key, values]) => [key, distribution(values)]));
    numericTree(entry.completedBattleMetrics, metrics, `${where}: metrics`);
    equal(entry.actualEnemyPositionCounts, accumulator.positions, `${where}: positions`);
    equal(entry.calibrationStatuses, accumulator.statuses, `${where}: statuses`);
    numericTree(entry.calibrationAbsoluteRelativeError, distribution(accumulator.errors), `${where}: calibration errors`);
}

function validateWeakDefaults(member) {
    const fields = (code, slot, health, speed, threat, critChance = 0, critMultiplier = 0, attack = null) => ({
        code, slots: [slot], attacks: attack ? [attack] : [], defense: health ? {defenseType: 'CLOTH', defense: 60} : null,
        health, critChance, dodgeChance: 1, critMultiplier, speed, baseThreat: threat, impact: 0, progressionVersion: 'V1'
    });
    const expected = [
        fields('sword', 'MAIN_HAND', 0, 18, 8, 2, 0.05, {attackType: 'SLASH', minRange: 1, maxRange: 1, attack: 360}),
        fields('off-fists', 'OFF_HAND', 0, 8, 1, 1, 0.03, {attackType: 'BLUNT', minRange: 1, maxRange: 1, attack: 60}),
        fields('torn_jacket', 'BODY', 300, 24, 2), fields('leaky_pants', 'PANTS', 180, 20, 2),
        fields('footcloths', 'SHOES', 120, 16, 0), fields('panama', 'HELMET', 120, 16, 0),
        fields('bandages', 'GLOVES', 120, 16, 0)
    ];
    equal(member.sourceItems.length, 7, 'Live weak participant has sword and six defaults');
    equal(member.sourceItems.map(item => item.object.code).sort(), expected.map(object => object.code).sort(), 'Exact V5 weak item codes');
    const slots = member.sourceItems.flatMap(item => item.object.slots);
    equal(new Set(slots).size, 7, 'Exactly seven distinct occupied weak slots');
    for (const item of member.sourceItems) {
        equal([item.rarity, item.enhanceLevel, item.modifier], ['COMMON', 0, null], 'Weak gear is common +0 without modifiers');
        const {locales, ...object} = item.object;
        equal(object, expected.find(candidate => candidate.code === object.code), 'Exact V5 weak combat characteristics');
    }
    equal(member.sourceItems.reduce((sum, item) => sum + item.object.health, 0), 840, 'V5 defaults contribute 840 HP');
}

function validateCausal(summary, accumulators) {
    const expectedComparisons = [];
    const expectedSkipped = [];
    const requestedVariants = summary.causalVariants || CAUSAL_VARIANTS;
    if (requestedVariants.includes('WEAK_APPEND')) equal(summary.weakParticipantModel, LIVE_WEAK_MODEL,
        'Live weak model must be explicitly recorded in the manifest');
    for (const group of [...new Set(summary.cases.map(entry => entry.causalGroupId))]) {
        const entries = summary.cases.filter(entry => entry.causalGroupId === group);
        const baseCell = causalBase(group, summary.causalExtraLevels || []);
        const baseEntry = entries[0];
        equal(baseEntry.variant, 'BASE', `causal group ${group}: base first`);
        const base = accumulators.get(baseEntry.caseId).members;
        const canRaise = base.some(member => member.sourceItems.some(item => item.modifier && item.rarity !== 'LEGENDARY'));
        equal(entries.map(entry => entry.variant), CAUSAL_VARIANTS.filter(variant => requestedVariants.includes(variant)
            && (variant !== 'SINGLE_MODIFIER_RARITY' || canRaise)), `causal group ${group}: variants`);
        for (const entry of entries) {
            equal(entry.caseId, (group - 1) * 5 + CAUSAL_VARIANTS.indexOf(entry.variant) + 1, 'Stable causal case ID');
            equal(entry.baseCell, baseCell, 'Causal base cell');
            const appended = ['NORMAL_APPEND', 'WEAK_APPEND'].includes(entry.variant);
            equal(entry.cell, cell(`CAUSAL_${entry.variant}`, baseCell.tier,
                baseCell.size + (appended ? 1 : 0), baseCell.raidLevel), 'Causal variant cell');
            const members = accumulators.get(entry.caseId).members;
            if (entry.variant === 'BASE') {
                equal(entry.sourceChange, 'Unchanged main reference input', 'Base change description');
                continue;
            }
            if (appended) {
                equal(members.slice(0, base.length), base, 'Appending must retain all original members exactly');
                equal(members.length, base.length + 1, 'Only one appended member');
                if (entry.variant === 'NORMAL_APPEND') equal(entry.sourceChange,
                    'Append only the next canonical member; retain every existing item, position and tactic', 'Normal append description');
                else {
                    equal(entry.sourceChange,
                        'WEAK_WITH_DEFAULTS: append common V5 sword +0 and six default items +0; no modifiers', 'Live weak change description');
                    const weak = members.at(-1);
                    equal([weak.build, weak.position, weak.tactic], ['WEAK_WITH_DEFAULTS', 'FRONT', 'THREAT'], 'Weak member identity');
                    validateWeakDefaults(weak);
                }
            } else {
                equal(members.length, base.length, 'Single-item change does not add members');
                const differences = [];
                for (let memberIndex = 0; memberIndex < base.length; memberIndex++) {
                    const old = base[memberIndex];
                    const changed = members[memberIndex];
                    equal([changed.build, changed.position, changed.tactic], [old.build, old.position, old.tactic], 'Unchanged member identity');
                    equal(changed.sourceItems.length, old.sourceItems.length, 'Unchanged item count');
                    for (let itemIndex = 0; itemIndex < old.sourceItems.length; itemIndex++) {
                        if (JSON.stringify(canonical(old.sourceItems[itemIndex])) !== JSON.stringify(canonical(changed.sourceItems[itemIndex]))) {
                            differences.push({memberIndex, itemIndex, old: old.sourceItems[itemIndex], changed: changed.sourceItems[itemIndex]});
                        }
                    }
                }
                equal(differences.length, 1, 'Exactly one source item changes');
                const difference = differences[0];
                const {old, changed, memberIndex, itemIndex} = difference;
                const expected = structuredClone(old);
                if (entry.variant === 'SINGLE_ITEM_ENHANCE') {
                    equal([memberIndex, itemIndex], [0, 0], 'Enhance only first member first item');
                    expected.enhanceLevel++;
                    equal(entry.sourceChange, `Enhance only member 0 item ${old.object.code} from +${old.enhanceLevel} to +${expected.enhanceLevel}`,
                        'Enhancement description');
                } else {
                    assert(old.modifier && RARITIES.indexOf(old.rarity) < 4, 'Rarity mutation requires a modifier below legendary');
                    expected.rarity = RARITIES[RARITIES.indexOf(old.rarity) + 1];
                    equal(entry.sourceChange, `Raise only member ${memberIndex} item ${old.object.code} rarity from ${old.rarity}`
                        + ` to ${expected.rarity}; keep modifier and enhancement`, 'Rarity description');
                }
                equal(changed, expected, 'Only the declared enhancement or rarity field may change');
            }
            const deltas = [];
            for (let index = 0; index < summary.pairs; index++) {
                if (baseEntry.pairScoresByIndex[index] !== null && entry.pairScoresByIndex[index] !== null) {
                    deltas.push(entry.pairScoresByIndex[index] - baseEntry.pairScoresByIndex[index]);
                }
            }
            const comparison = {causalGroupId: group, baseCell, baseCaseId: baseEntry.caseId,
                variantCaseId: entry.caseId, variant: entry.variant, pairedScoreDeltas: deltas, distribution: distribution(deltas)};
            if (deltas.length) {
                const mean = deltas.reduce((sum, delta) => sum + delta, 0) / deltas.length;
                const halfWidth = Math.sqrt(2 * Math.log(40) / deltas.length);
                comparison.pairedDelta95Hoeffding = {low: Math.max(-1, mean - halfWidth),
                    high: Math.min(1, mean + halfWidth), unclippedHalfWidth: halfWidth};
            }
            expectedComparisons.push(comparison);
        }
        if (!canRaise) expectedSkipped.push({causalGroupId: group, cell: baseCell,
            variant: 'SINGLE_MODIFIER_RARITY', reason: 'NO_MODIFIER_BELOW_LEGENDARY'});
    }
    numericTree(summary.comparisons, expectedComparisons, 'Causal comparison statistics');
    equal(summary.skippedVariants, expectedSkipped, 'Skipped causal variants');
}

async function validate(directory) {
    const summary = JSON.parse(await fs.promises.readFile(path.join(directory, 'summary.json'), 'utf8'));
    const warnings = [];
    validateManifest(summary, warnings);
    const parameters = await parametersFor(directory, summary.phase, warnings);
    if (summary.cases.some(entry => entry.pairScoresByIndex === undefined)) {
        assert(summary.suite !== 'causal', 'Causal comparisons require indexed pair scores');
        warnings.push('Older non-causal helper omitted pairScoresByIndex; complete scores were recomputed from raw mirrors');
    }
    const gzipHash = crypto.createHash('sha256');
    const rawHash = crypto.createHash('sha256');
    const source = fs.createReadStream(path.join(directory, 'battles.jsonl.gz'));
    const gunzip = zlib.createGunzip();
    OPEN_STREAMS.add(source);
    OPEN_STREAMS.add(gunzip);
    source.on('data', chunk => gzipHash.update(chunk));
    source.on('error', error => gunzip.destroy(error));
    gunzip.on('data', chunk => rawHash.update(chunk));
    const lines = readline.createInterface({input: source.pipe(gunzip), crlfDelay: Infinity});
    const accumulators = new Map();
    let rows = 0;
    const totals = {};
    for await (const line of lines) {
        assert(line.length, `Empty JSONL row ${rows + 1}`);
        const entry = summary.cases[Math.floor(rows / (2 * summary.pairs))];
        assert(entry, 'More raw rows than declared');
        const row = JSON.parse(line);
        const pair = Math.floor(rows / 2) % summary.pairs;
        const reverse = rows % 2 === 1;
        validateRow(row, entry, summary, parameters, pair, reverse, `row ${rows + 1}/case ${entry.caseId}`);
        if (!accumulators.has(entry.caseId)) accumulators.set(entry.caseId, newAccumulator());
        accumulate(accumulators.get(entry.caseId), row);
        add(totals, row.outcome);
        rows++;
    }
    equal(rows, summary.cases.length * summary.pairs * 2, 'Raw row count');
    equal(gzipHash.digest('hex'), summary.rawSha256, 'Compressed raw SHA256');
    for (const entry of summary.cases) validateCase(entry, accumulators.get(entry.caseId), summary.pairs);
    if (summary.suite === 'causal') validateCausal(summary, accumulators);
    OPEN_STREAMS.delete(source);
    OPEN_STREAMS.delete(gunzip);
    return {directory: path.resolve(directory), phase: summary.phase, suite: summary.suite,
        cases: summary.cases.length, pairsPerCase: summary.pairs, battles: rows, outcomes: totals,
        rawSha256: summary.rawSha256, uncompressedSha256: rawHash.digest('hex'), parameters, warnings, summary};
}

(async () => {
    const directories = process.argv.slice(2);
    assert(directories.length >= 1 && directories.length <= 2,
        'Usage: node validate-myconids.cjs RUN_DIRECTORY [EXACT_REPLAY_DIRECTORY]');
    const first = await validate(directories[0]);
    let replay;
    if (directories[1]) {
        replay = await validate(directories[1]);
        equal(replay.uncompressedSha256, first.uncompressedSha256, 'Entire uncompressed JSONL replay');
        equal(replay.summary, first.summary, 'Entire summary replay, including compressed SHA256');
        equal(replay.parameters, first.parameters, 'Entire explicit/historical parameter profile replay');
    }
    const {summary, ...result} = first;
    console.log(JSON.stringify({...result, valid: true, exactReplay: replay ? {
        directory: replay.directory, battles: replay.battles, uncompressedSha256: replay.uncompressedSha256
    } : null, limitations: ['Action and trace digests are checked for presence/format; full logs are not stored.',
        'Sampling results are not a requirement that all compositions win 50% of their fights.']}, null, 2));
})().catch(error => {
    for (const stream of OPEN_STREAMS) stream.destroy();
    console.error(error.stack || error);
    process.exitCode = 1;
});
