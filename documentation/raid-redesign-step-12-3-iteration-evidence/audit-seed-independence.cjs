'use strict';
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const directory = path.resolve(process.argv[2] || '.');
const excluded = new Set(['ritual-counter-confirmation', 'progression-confirmation']);
function hash(text) {
    let n = 0;
    for (let i = 0; i < text.length; i++) n = (Math.imul(n, 31) + text.charCodeAt(i)) | 0;
    return BigInt(n);
}
function mix(base, index) {
    const u = n => BigInt.asUintN(64, n);
    let n = u(base + 0x9e3779b97f4a7c15n * (BigInt(index) + 1n));
    n = u((n ^ n >> 30n) * 0xbf58476d1ce4e5b9n);
    n = u((n ^ n >> 27n) * 0x94d049bb133111ebn);
    return BigInt.asIntN(64, n ^ n >> 31n).toString();
}
function category(name) {
    return excluded.has(name) ? 'excluded-preliminary' : name.includes('confirmation') ? 'accepted-confirmation' : 'screen';
}
async function load(name) {
    const prefix = path.join(directory, name), text = fs.readFileSync(prefix + '-summary.json', 'utf8');
    const summary = JSON.parse(text), root = BigInt(text.match(/"rootSeed"\s*:\s*(-?\d+)/)[1]);
    const definitions = JSON.parse(zlib.gunzipSync(fs.readFileSync(prefix + '-cases.json.gz')));
    const byCase = new Map(), groups = new Map(), seeds = new Set();
    for (const { case: c } of definitions) {
        assert(!byCase.has(c.code)); byCase.set(c.code, c.seedGroup);
        if (!groups.has(c.seedGroup)) {
            const base = BigInt.asIntN(64, root + hash(c.seedGroup));
            groups.set(c.seedGroup, { name: c.seedGroup, hash: hash(c.seedGroup).toString(), base: base.toString(),
                cases: 0, expected: new Set(Array.from({ length: summary.pairs }, (_, i) => mix(base, i))), actual: new Set() });
        }
        groups.get(c.seedGroup).cases++;
    }
    let records = 0;
    const stream = fs.createReadStream(prefix + '-pairs.jsonl.gz').pipe(zlib.createGunzip());
    for await (const line of readline.createInterface({ input: stream, crlfDelay: Infinity })) {
        if (!line.trim()) continue;
        const row = JSON.parse(line), group = groups.get(byCase.get(row.case)); assert(group, 'Unknown raw case');
        const exact = line.match(/"seed"\s*:\s*(-?\d+)(?=\s*[,}])/)[1];
        assert.equal(exact, mix(BigInt(group.base), row.pair), 'Raw seed differs from Java long schedule');
        group.actual.add(exact); seeds.add(exact); records++;
    }
    assert.equal(records, summary.battles);
    for (const group of groups.values()) assert.deepEqual(group.actual, group.expected, 'Missing generated seeds');
    return { name, root: root.toString(), category: category(name), pairs: summary.pairs, records, groups, seeds };
}
(async () => {
    const names = fs.readdirSync(directory).filter(n => n.endsWith('-summary.json')).map(n => n.slice(0, -13)).sort();
    const studies = [];
    for (const name of names) studies.push(await load(name));
    const overlaps = [];
    for (let i = 0; i < studies.length; i++) for (let j = i + 1; j < studies.length; j++) {
        const left = studies[i], right = studies[j], shared = [...left.seeds].filter(s => right.seeds.has(s));
        if (!shared.length) continue;
        const blocks = [];
        for (const a of left.groups.values()) for (const b of right.groups.values()) {
            const common = [...a.actual].filter(s => b.actual.has(s));
            if (common.length) blocks.push({ leftGroup: a.name, rightGroup: b.name, leftBase: a.base,
                rightBase: b.base, equalBase: a.base === b.base, sharedActualSeeds: common.length,
                firstSharedExactSeed: common[0] });
        }
        overlaps.push({ left: left.name, leftCategory: left.category, right: right.name,
            rightCategory: right.category, sharedActualSeeds: shared.length, blocks });
    }
    const acceptedVsScreen = overlaps.filter(o => [o.leftCategory, o.rightCategory].includes('accepted-confirmation')
        && [o.leftCategory, o.rightCategory].includes('screen'));
    const acceptedVsAccepted = overlaps.filter(o => o.leftCategory === 'accepted-confirmation' && o.rightCategory === 'accepted-confirmation');
    const required = ['core-confirmation', 'protected-confirmation', 'size-confirmation',
        'progression-confirmation-independent', 'ritual-counter-confirmation-independent'];
    for (const name of required) assert(studies.some(s => s.name === name), 'Missing final confirmation: ' + name);
    const report = {
        ok: !acceptedVsScreen.length && !acceptedVsAccepted.length,
        scope: 'Exact signed Java long root+JavaHash(seedGroup) blocks plus all actual raw integer seed lexemes; each matches regenerated SplitMix64 schedule',
        intentionallyExcludedPreliminary: [...excluded], acceptedVsScreen, acceptedVsAccepted,
        studies: studies.map(s => ({ name: s.name, rootSeed: s.root, category: s.category, pairs: s.pairs,
            records: s.records, uniqueActualSeeds: s.seeds.size, groups: [...s.groups.values()].map(g => ({
                seedGroup: g.name, javaHash: g.hash, signedLongBase: g.base, cases: g.cases,
                actualAndExpectedSeedCount: g.actual.size, firstExactSeed: mix(BigInt(g.base), 0),
                lastExactSeed: mix(BigInt(g.base), s.pairs - 1) })) })),
        allCrossStudyOverlaps: overlaps,
        limitations: 'Within-study pairing is intentional; reported cross-screen overlaps are not independent confirmations. Zero observed overlap establishes these enumerated studies only, not arbitrary future Java hashes or seeds'
    };
    console.log(JSON.stringify(report, null, 2));
    if (!report.ok) process.exitCode = 1;
})().catch(error => { console.log(JSON.stringify({ ok: false, error: error.message })); process.exitCode = 1; });
