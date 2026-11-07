#!/usr/bin/env node
'use strict';

// Node.js 24, built-ins only. This validates saved observations, not the battle engine.
// Usage: node validate-confirmation.cjs EXTRACTED_DIRECTORY [CURRENT_REFERENCE_TEAMS_JSON]
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');

const directory = process.argv[2];
assert.ok(directory, 'Supply the directory containing confirmation-*.');
const read = name => fs.readFileSync(path.join(directory, name));
const definitions = JSON.parse(zlib.gunzipSync(read('confirmation-cases.json.gz')));
const summary = JSON.parse(read('confirmation-summary.json'));
const canonical = value => JSON.stringify(value, (_, v) => v && !Array.isArray(v) && typeof v === 'object'
    ? Object.fromEntries(Object.entries(v).sort(([a], [b]) => a.localeCompare(b))) : v);
const same = (a, b, message) => assert.equal(canonical(a), canonical(b), message);
const integer = value => assert.ok(Number.isSafeInteger(value) && value >= 0, `Invalid integer: ${value}`);

// Frozen public slot/modifier/price rules used by these 52 conditions, not a full catalogue validator.
const slots = {};
for (const [slot, codes] of Object.entries({
    MAIN_HAND: ['mace', 'rapier'], OFF_HAND: ['dirk', 'tower_shield'],
    BODY: ['breastplate', 'cuirass', 'robe'], PANTS: ['leather_chausses', 'greaves', 'cloth_chausses'],
    SHOES: ['boots', 'sabatons', 'cloth_boots'], HELMET: ['leather_helm', 'great_helm', 'hood'],
    GLOVES: ['leather_gloves', 'gauntlets', 'cloth_gloves']
})) for (const code of codes) slots[code] = [slot];
for (const code of ['sledgehammer', 'command_staff', 'bow']) slots[code] = ['MAIN_HAND', 'OFF_HAND'];
const allowed = {
    COUNTER_ATTACK: ['BODY', 'HELMET'], THORNS: ['BODY', 'GLOVES'],
    DOUBLE_ATTACK: ['MAIN_HAND', 'OFF_HAND'], BERSERK: ['MAIN_HAND', 'OFF_HAND'],
    HIT_AND_RUN: ['SHOES', 'HELMET'], BLEEDING: ['MAIN_HAND', 'OFF_HAND'],
    KNOCKBACK: ['MAIN_HAND', 'OFF_HAND'], SELF_HEAL: ['BODY', 'PANTS'],
    PRECISE_STRIKE: ['MAIN_HAND', 'OFF_HAND'], RETREAT: ['PANTS', 'SHOES'],
    FEINT: ['SHOES', 'GLOVES'], GUARD: ['OFF_HAND', 'BODY'],
    PENETRATION: ['MAIN_HAND', 'OFF_HAND'], ACCUMULATION: ['MAIN_HAND', 'OFF_HAND'],
    TEMPO_BREAK: ['MAIN_HAND', 'OFF_HAND']
};
const attackSkills = new Set(['DOUBLE_ATTACK', 'BLEEDING', 'PRECISE_STRIKE', 'PENETRATION', 'ACCUMULATION']);
const defenseSkills = new Set(['COUNTER_ATTACK', 'THORNS', 'HIT_AND_RUN', 'SELF_HEAL', 'RETREAT', 'GUARD']);
const rarities = ['COMMON', 'UNCOMMON', 'RARE', 'EPIC', 'LEGENDARY'];
const rarityChainGold = [0, 150, 450, 975, 1725];
const counters = {participants: 0, items: 0, legalModifierAssignments: 0, numericSummaryValues: 0};
const byCode = new Map();
const groups = new Set();

assert.equal(summary.rootSeed, 2026110404);
assert.equal(summary.pairs, 500);
assert.equal(summary.conditions, 52);
assert.equal(summary.battles, 52000);
same([summary.catalog, summary.combat, summary.tempo], ['SCALING_V5', 'SCALING_SKILLS_V3', 'ACCELERATED_V1']);
assert.equal(definitions.length, 52);
assert.equal(summary.summaries.length, 52);

function checkTeam(gears, equipment, level) {
    assert.equal(gears.length, equipment.length);
    let gold = 0;
    for (let index = 0; index < gears.length; index++) {
        const gear = gears[index], member = equipment[index], occupied = new Set();
        same(gear.build, member.build, 'Manifest differs from requested build');
        assert.equal(gear.catalog, 'SCALING_V5');
        assert.equal(member.catalog, gear.catalog);
        same(member.items.map(item => item.code), gear.build.itemCodes);
        assert.equal(new Set(gear.build.itemCodes).size, gear.build.itemCodes.length);
        assert.ok(['FRONT', 'MID', 'BACK'].includes(gear.build.position));
        for (const code of Object.keys(gear.build.assignments)) assert.ok(gear.build.itemCodes.includes(code));
        for (const item of member.items) {
            const occupiedByItem = slots[item.code];
            assert.ok(occupiedByItem, `Unrecognised study item ${item.code}`);
            for (const slot of occupiedByItem) { assert.ok(!occupied.has(slot), 'Overlapping slots'); occupied.add(slot); }
            const rank = rarities.indexOf(item.rarity), assignment = gear.build.assignments[item.code];
            assert.ok(rank >= 0);
            assert.equal(item.level, level);
            assert.equal(item.rarity, assignment?.rarity ?? 'COMMON');
            assert.equal(item.skill, assignment?.skill ?? 'NONE');
            assert.equal(item.points, assignment ? rank * occupiedByItem.length : 0);
            gold += 100 * occupiedByItem.length + rarityChainGold[rank];
            for (const field of ['health', 'speed', 'points', 'level']) integer(item[field]);
            assert.ok(item.attacks.length || item.defense || item.health > 0);
            for (const attack of item.attacks) {
                for (const field of ['attack', 'minRange', 'maxRange']) integer(attack[field]);
                assert.ok(attack.minRange >= 1 && attack.maxRange >= attack.minRange);
                assert.ok(['SLASH', 'PIERCE', 'BLUNT', 'MAGICAL'].includes(attack.attackType));
            }
            if (item.defense) {
                integer(item.defense.value); assert.ok(item.defense.value > 0);
                assert.ok(['LEATHER', 'PLATE', 'CLOTH', 'ARCANE'].includes(item.defense.type));
            }
            if (assignment) {
                assert.ok(allowed[item.skill]?.some(slot => occupiedByItem.includes(slot)), 'Illegal modifier slot');
                if (attackSkills.has(item.skill)) assert.ok(item.attacks.length, 'Attack modifier without attack');
                if (defenseSkills.has(item.skill)) assert.ok(item.defense, 'Defense modifier without defense');
                counters.legalModifierAssignments++;
            }
            counters.items++;
        }
        assert.equal(occupied.size, 7, 'Incomplete equipment');
        counters.participants++;
    }
    return gold;
}

for (const definition of definitions) {
    const c = definition.case;
    assert.ok(!byCode.has(c.code), `Duplicate definition ${c.code}`);
    assert.ok([0, 6].includes(c.level));
    assert.ok([7, 15].includes(c.left.length));
    assert.equal(c.left.length, c.right.length);
    assert.equal(checkTeam(c.left, definition.leftEquipment, c.level), definition.leftNominalGold);
    assert.equal(checkTeam(c.right, definition.rightEquipment, c.level), definition.rightNominalGold);
    assert.equal(definition.leftNominalGold, definition.rightNominalGold, 'Unequal nominal gold');
    groups.add(c.group);
    byCode.set(c.code, {definition, seen: new Set(), outcomes: {WIN: 0, LOSS: 0}, rounds: [], leftMax: [], rightMax: []});
}

let responseConditions = 0;
for (const {definition: d} of byCode.values()) {
    if (!d.case.code.startsWith('RESPONSE_')) continue;
    const oldCode = d.case.code.replace(/^RESPONSE_/, '').replace('_COMMAND_LEATHER', '');
    const old = byCode.get(oldCode)?.definition;
    assert.ok(old, `Missing corresponding old counter ${oldCode}`);
    same(d.case.left, old.case.left, 'Changed Raider in response condition');
    same(d.leftEquipment, old.leftEquipment);
    same(d.case.right[0], old.case.right[0], 'Changed Guardian');
    same(d.rightEquipment[0], old.rightEquipment[0]);
    for (let index = 1; index < d.case.right.length; index++) {
        const next = structuredClone(d.case.right[index]);
        assert.equal(next.build.code, 'RESPONSE_COMMAND_LEATHER');
        assert.equal(next.build.itemCodes[0], 'command_staff');
        next.build.code = old.case.right[index].build.code;
        next.build.itemCodes[0] = 'sledgehammer';
        next.build.assignments.sledgehammer = next.build.assignments.command_staff;
        delete next.build.assignments.command_staff;
        same(next, old.case.right[index], 'Response changed more than weapon');
        same(d.rightEquipment[index].items.slice(1), old.rightEquipment[index].items.slice(1), 'Changed armor');
        const weapon = d.rightEquipment[index].items[0], oldWeapon = old.rightEquipment[index].items[0];
        for (const field of ['rarity', 'level', 'points', 'skill']) assert.equal(weapon[field], oldWeapon[field]);
    }
    responseConditions++;
}
assert.equal(responseConditions, 16);

function javaHash(value) {
    let hash = 0;
    for (let index = 0; index < value.length; index++) hash = (Math.imul(31, hash) + value.charCodeAt(index)) | 0;
    return BigInt(hash);
}
function seed(group, pair) {
    const u64 = value => BigInt.asUintN(64, value);
    let x = u64(BigInt(summary.rootSeed) + javaHash(group) + 0x9e3779b97f4a7c15n * BigInt(pair + 1));
    x = u64((x ^ (x >> 30n)) * 0xbf58476d1ce4e5b9n);
    x = u64((x ^ (x >> 27n)) * 0x94d049bb133111ebn);
    return BigInt.asIntN(64, x ^ (x >> 31n));
}
const uniqueSeeds = new Set();
function actors(team, members, size) {
    assert.equal(members.length, size);
    let turns = 0, attempts = 0, survivors = 0, longest = 0;
    members.forEach((member, index) => {
        assert.equal(member.index, index);
        for (const field of ['ownTurns', 'normalAttempts', 'maxConsecutiveNonAttackOwnTurns']) integer(member[field]);
        assert.equal(typeof member.alive, 'boolean');
        assert.ok(member.normalAttempts <= member.ownTurns);
        assert.ok(member.maxConsecutiveNonAttackOwnTurns <= member.ownTurns - member.normalAttempts);
        assert.equal(member.maxConsecutiveNonAttackOwnTurns === 0, member.ownTurns === member.normalAttempts);
        turns += member.ownTurns; attempts += member.normalAttempts; survivors += Number(member.alive);
        longest = Math.max(longest, member.maxConsecutiveNonAttackOwnTurns);
    });
    assert.equal(turns, team.actions, 'Actor turns differ from team actions');
    assert.equal(attempts, team.attempts, 'Actor attempts differ from team attempts');
    assert.equal(survivors, team.survivors, 'Actor survivors differ from team survivors');
    assert.equal(turns - attempts, team.movementTurns, 'Nonattack actions differ from movement turns');
    return longest;
}

async function main() {
    let records = 0;
    const lines = readline.createInterface({input: fs.createReadStream(path.join(directory, 'confirmation-raw.jsonl.gz'))
        .pipe(zlib.createGunzip()), crlfDelay: Infinity});
    for await (const line of lines) {
        assert.ok(line.length, 'Blank raw record');
        // JSON.parse would round Java long values. Read its integer token before parsing other fields.
        const seedTokens = [...line.matchAll(/"seed"\s*:\s*(-?\d+)/g)];
        assert.equal(seedTokens.length, 1);
        const exactSeed = BigInt(seedTokens[0][1]), raw = JSON.parse(line), state = byCode.get(raw.case);
        assert.ok(state, `Unknown raw condition ${raw.case}`);
        integer(raw.pair); assert.ok(raw.pair < 500); assert.equal(typeof raw.reverse, 'boolean');
        const key = raw.pair * 2 + Number(raw.reverse);
        assert.ok(!state.seen.has(key), 'Repeated pair/side order'); state.seen.add(key);
        assert.equal(exactSeed, seed(state.definition.case.group, raw.pair), 'Exact 64-bit seed mismatch');
        uniqueSeeds.add(exactSeed.toString());
        assert.ok(['WIN', 'LOSS'].includes(raw.outcome), 'Draw/timeout/unrecognised outcome');
        integer(raw.rounds); assert.ok(raw.rounds > 0 && raw.rounds < 10000, 'Timeout');
        assert.equal(raw.moveViolations, 0);
        const leftMax = actors(raw.left, raw.leftActors, state.definition.case.left.length);
        const rightMax = actors(raw.right, raw.rightActors, state.definition.case.right.length);
        assert.ok(raw.outcome === 'WIN' ? raw.left.survivors > 0 && raw.right.survivors === 0
            : raw.right.survivors > 0 && raw.left.survivors === 0, 'Outcome contradicts survivors');
        state.outcomes[raw.outcome]++;
        state.rounds.push(raw.rounds); state.leftMax.push(leftMax); state.rightMax.push(rightMax);
        records++;
    }
    assert.equal(records, 52000);
    assert.equal(groups.size, 20);
    assert.equal(uniqueSeeds.size, 10000);
    const stats = values => {
        const sorted = [...values].sort((a, b) => a - b), n = sorted.length;
        return {median: (sorted[(n - 1) >> 1] + sorted[n >> 1]) / 2,
            p95: sorted[Math.ceil(n * .95) - 1], max: sorted[n - 1], mean: values.reduce((a, b) => a + b, 0) / n};
    };
    const summaryCodes = new Set();
    for (const saved of summary.summaries) {
        assert.ok(!summaryCodes.has(saved.case)); summaryCodes.add(saved.case);
        const state = byCode.get(saved.case); assert.ok(state);
        assert.equal(state.seen.size, 1000); assert.equal(saved.battles, 1000);
        assert.equal(saved.size, state.definition.case.left.length);
        assert.equal(saved.level, state.definition.case.level);
        assert.equal(saved.family, state.definition.case.family);
        assert.equal(saved.moveViolations, 0);
        for (const outcome of ['WIN', 'LOSS']) assert.equal(saved.outcomes[outcome] ?? 0, state.outcomes[outcome]);
        assert.ok(Object.keys(saved.outcomes).every(key => ['WIN', 'LOSS'].includes(key)));
        for (const [name, values] of [['rounds', state.rounds], ['leftMaxNonAttackOwnTurns', state.leftMax],
            ['rightMaxNonAttackOwnTurns', state.rightMax]]) {
            same(saved[name], stats(values), `Summary mismatch ${saved.case}/${name}`);
            counters.numericSummaryValues += 4;
        }
    }
    assert.equal(counters.numericSummaryValues, 624);
    const referenceCases = process.argv[3] ? validateReferences(process.argv[3]) : null;
    console.log(JSON.stringify({valid: true, records, conditions: definitions.length, pairsPerCondition: 500,
        orders: 2, uniqueSeedGroups: groups.size, exactUniqueJavaLongSeeds: uniqueSeeds.size, ...counters,
        equalNominalGoldConditions: 52, responseConditionsDifferOnlyByWeapon: responseConditions,
        movementViolations: 0, timeouts: 0, currentAdaptiveReferenceCases: referenceCases,
        scope: 'Saved observation consistency, exact seeds, structural equipment legality and nominal prices; not an engine rerun or global/PvE acceptance'}, null, 2));
}

function validateReferences(file) {
    const exported = JSON.parse(fs.readFileSync(file)), cases = exported.adaptiveCounterplay.cases;
    assert.equal(exported.combat, 'SCALING_SKILLS_V3');
    assert.equal(exported.adaptiveCounterplay.tempo, 'ACCELERATED_V1');
    assert.equal(cases.length, 16);
    const variants = {ORDINARY_BLEEDING: 'BASE_BLEEDING', SIX_UNCOMMON: 'BASE_SIX_UNCOMMON',
        HEAVY_BLEEDING: 'HEAVY_BLEEDING', HEAVY_DOUBLE_BLEEDING: 'HEAVY_DOUBLE_BLEED'};
    const checked = new Set(), grow = (base, level) => base + Math.floor((base * level + 30) / 60);
    for (const current of cases) {
        const match = current.code.match(/^ADAPTIVE_(.+)_N(7|15)_L(0|6)$/); assert.ok(match);
        const code = `RESPONSE_${variants[match[1]]}_COMMAND_LEATHER_N${match[2]}_L${match[3]}`;
        assert.ok(!checked.has(code)); checked.add(code);
        const confirmed = byCode.get(code)?.definition; assert.ok(confirmed);
        assert.equal(current.enhanceLevel, confirmed.case.level); assert.equal(current.size, confirmed.case.left.length);
        assert.equal(current.raiderNominalGold, confirmed.leftNominalGold);
        assert.equal(current.responseNominalGold, confirmed.rightNominalGold);
        for (const [field, evidence] of [['raiders', confirmed.leftEquipment], ['response', confirmed.rightEquipment]]) {
            assert.equal(current[field].length, evidence.length);
            current[field].forEach((member, index) => {
                const saved = evidence[index];
                assert.equal(member.catalog, saved.catalog); assert.equal(member.position, saved.build.position);
                assert.equal(member.tactic, saved.build.tactic);
                const items = member.items.map(item => ({code: item.object.code, rarity: item.rarity,
                    level: item.enhanceLevel, points: item.modifier ? rarities.indexOf(item.rarity) * item.object.slots.length : 0,
                    skill: item.modifier?.activeEnum ?? 'NONE', health: grow(item.object.health, item.enhanceLevel),
                    speed: item.object.speed, attacks: item.object.attacks.map(attack => ({...attack,
                        attack: grow(attack.attack, item.enhanceLevel)})), defense: item.object.defense ? {
                        type: item.object.defense.defenseType, value: grow(item.object.defense.defense, item.enhanceLevel)
                    } : null}));
                same(items, saved.items, `Current reference differs from confirmation ${code}/${field}/${index}`);
            });
        }
    }
    return checked.size;
}

main().catch(error => { console.error(error.stack); process.exitCode = 1; });
