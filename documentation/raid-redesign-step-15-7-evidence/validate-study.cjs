#!/usr/bin/env node
// Independent read-only verification. Run from any directory: node validate-study.cjs DIR [--plan-only].
const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const directory = path.resolve(process.argv[2] || '.');
const repository = path.resolve(__dirname, '../..');
const read = name => JSON.parse(fs.readFileSync(path.join(directory, name), 'utf8'));
const sha = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const canonical = value => JSON.stringify(value, (_, x) => x && !Array.isArray(x) && typeof x === 'object'
  ? Object.fromEntries(Object.keys(x).sort().map(k => [k, x[k]])) : x);
const same = (a, b, label) => assert.equal(canonical(a), canonical(b), label);
const near = (a, b, label) => assert.ok(Number.isFinite(a) && Number.isFinite(b)
  && Math.abs(a - b) <= 2e-12 * Math.max(1, Math.abs(a), Math.abs(b)), `${label}: ${a} != ${b}`);
const sum = values => [...values].sort((a, b) => a - b).reduce((a, b) => a + b, 0);
function distribution(values) {
  if (!values.length) return {count: 0};
  const ordered = [...values].sort((a, b) => a - b), q = p => ordered[Math.ceil(p * ordered.length) - 1];
  return {count: values.length, mean: values.reduce((a, b) => a + b, 0) / values.length,
    min: ordered[0], p50: q(.5), p90: q(.9), p95: q(.95), max: ordered.at(-1)};
}
function compare(actual, expected, label) {
  if (typeof expected === 'number') return near(actual, expected, label);
  if (expected === null || typeof expected !== 'object') return same(actual, expected, label);
  same(Object.keys(actual).sort(), Object.keys(expected).sort(), `${label} keys`);
  for (const key of Object.keys(expected)) compare(actual[key], expected[key], `${label}.${key}`);
}
const interval = (scores, delta = false) => {
  const mean = scores.reduce((a, b) => a + b, 0) / scores.length;
  const width = Math.sqrt(Math.log(40) * (delta ? 2 : .5) / scores.length);
  return {low: Math.max(delta ? -1 : 0, mean - width), high: Math.min(1, mean + width), unclippedHalfWidth: width};
};
const protocol = read('protocol.json'), samples = protocol.cases.map(c => c.sample);
assert.equal(protocol.metric, 'EQUIPMENT_POWER_SUM_V1');
assert.equal(protocol.caseCount, 359); assert.equal(protocol.attemptedBattles, 8620);
assert.equal(protocol.generationRoot, 20261115157000); assert.equal(protocol.battleRoot, 30261115157000);
assert.equal(protocol.tempoDamageMultiplier, 1.75);
const familyCounts = {}, groups = new Map(), expectedSlots = ['BODY','GLOVES','HELMET','MAIN_HAND','OFF_HAND','PANTS','SHOES'];
const expectedContexts = [], tiers = ['BEGINNER','MID','STRONG'];
const context = s => [s.family,s.cell.style,s.cell.tier,s.cell.size,s.cell.raidLevel,s.cell.effects,s.variant].join('|');
const expect = (family,style,tier,size,raidLevel,effects=false,variant='BASE') => expectedContexts.push(context({family,variant,cell:{style,tier,size,raidLevel,effects}}));
for (const t of tiers) for (const n of [1,2,3,7,8,15,30]) for (const l of [1,10,25,40,50]) expect('A_MAIN','MAIN',t,n,l);
for (const style of ['MOBILE','COMMON_FRONT_PRESSURE','PRESSURE','FLANK']) for (const t of tiers) for (const l of [1,25,50]) expect('B_SPECIALIZED',style,t,3,l);
for (const style of ['PIERCE_ARCANE','RAPIER_ARCANE','COMMAND_CUIRASS','RITUAL']) for (const t of tiers) for (const n of [1,3,15]) for (const l of [1,25,50]) expect('C_COUNTERPLAY',style,t,n,l);
for (const t of tiers) for (const n of [3,15]) for (const l of [1,25,50]) expect('D_EFFECTS','MAIN',t,n,l,true,'EFFECTS');
for (const t of tiers) for (const n of [3,7]) for (const l of [10,25]) for (const v of ['BASE','NORMAL_APPEND','WEAK_APPEND','ONE_ENHANCE','ONE_RARITY','UNEQUIP_ARMOR']) expect('E_CAUSAL',`CAUSAL_${v}`,t,n+(v.endsWith('_APPEND')?1:0),l,false,v);
for (const n of [1,3,7,15,30]) for (const l of [1,10,25,50]) expect('F_FULL_DEFAULTS','FULL_DEFAULTS','BEGINNER',n,l);
same(samples.map(context).sort(), expectedContexts.sort(), 'all preregistered matrix dimensions');
for (const [index, entry] of protocol.cases.entries()) {
  const s = entry.sample; assert.equal(s.caseId, index + 1); assert.equal(s.members.length, s.cell.size);
  assert.equal(s.pairs, s.family === 'E_CAUSAL' ? 20 : 10); assert.equal(entry.seeds.length, s.pairs);
  familyCounts[s.family] = (familyCounts[s.family] || 0) + 1;
  groups.set(s.seedGroup, [...(groups.get(s.seedGroup) || []), s]);
  for (const member of s.members) same(member.items.flatMap(i => i.object.slots).sort(), expectedSlots, 'occupied slots');
  for (const [pair, seed] of entry.seeds.entries()) same(seed, {pair,
    generationSeed: protocol.generationRoot + s.seedGroup * 1000 + pair,
    battleSeed: protocol.battleRoot + s.seedGroup * 1000 + pair}, 'registered seeds');
}
same(familyCounts, {A_MAIN:105,B_SPECIALIZED:36,C_COUNTERPLAY:108,D_EFFECTS:18,E_CAUSAL:72,F_FULL_DEFAULTS:20}, 'matrix');
assert.equal(samples.reduce((n, s) => n + s.pairs * 2, 0), 8620); assert.equal(groups.size, 281);
for (const group of groups.values()) {
  if (group.length === 1) continue;
  const base = group.find(s => s.variant === 'BASE'); assert.ok(base, 'shared seeds require a base');
  if (base.family === 'E_CAUSAL') {
    same(group.map(s => s.variant).sort(), ['BASE','NORMAL_APPEND','ONE_ENHANCE','ONE_RARITY','UNEQUIP_ARMOR','WEAK_APPEND'].sort(), 'causal variants');
    for (const s of group.filter(s => s.variant.endsWith('_APPEND'))) same(s.members.slice(0, -1), base.members, 'unchanged old members');
  } else { assert.equal(group.length, 2); assert.equal(group[1].family, 'D_EFFECTS'); same(base.members, group[1].members, 'effects equipment'); }
}
for (const [relative, digest] of Object.entries(protocol.publicSourceSha256)) assert.equal(sha(path.join(repository, relative)), digest, relative);
if (process.argv.includes('--plan-only')) { console.log(JSON.stringify({status:'PLAN_VALID', cases:359, pairs:4310, battles:8620, seedGroups:281})); process.exit(0); }
const summary = read('summary.json'); assert.equal(summary.protocolSha256, sha(path.join(directory, 'protocol.json')));
assert.equal(summary.caseCount, 359); assert.equal(summary.attemptedBattles, 8620); assert.equal(summary.cases.length, 359);
const results = samples.map(s => ({s, outcomes:{}, orders:{}, metrics:{}, statuses:{}, errors:[], pairs:new Map(), rows:0}));
const bump = (object, key) => object[key] = (object[key] || 0) + 1;
const mirrorKeys = ['members','sourceEffects','combatEffects','calibration','calibrationMetric','generationDiagnostics',
  'playerBasis','enemyBasis','playerEquipmentPowers','enemyEquipmentPowers','playerEquipmentPowerSum','enemyEquipmentPowerSum'];
const hashes = crypto.createHash('sha256'), compressed = fs.createReadStream(path.join(directory, 'battles.jsonl.gz'));
compressed.on('data', chunk => hashes.update(chunk));
(async () => {
  let rowCount = 0;
  for await (const line of readline.createInterface({input:compressed.pipe(zlib.createGunzip()), crlfDelay:Infinity})) {
    assert.ok(line.length); const r = JSON.parse(line), x = results[r.caseId - 1]; assert.ok(x, 'known case'); const s = x.s;
    for (const key of ['caseId','family','variant','change','seedGroup','cell']) same(r[key], s[key], key);
    assert.ok(Number.isInteger(r.pair) && r.pair >= 0 && r.pair < s.pairs && typeof r.reverse === 'boolean');
    const seed = protocol.cases[r.caseId - 1].seeds[r.pair];
    assert.equal(r.generationSeed, seed.generationSeed); assert.equal(r.battleSeed, seed.battleSeed);
    same(r.members.map(m => ({build:m.build,position:m.position,tactic:m.tactic,items:m.sourceItems})), s.members, 'registered members');
    for (const m of r.members) same(m.sourceItems, m.combatItems, 'raid permanent items');
    assert.equal(r.catalog, 'SCALING_V5'); assert.equal(r.playerSkills, 'SCALING_SKILLS_V3'); assert.equal(r.npcSkills, r.playerSkills);
    assert.equal(r.tempo, 'ACCELERATED_V1'); assert.equal(r.calibrationMetric, protocol.metric);
    assert.ok(!('playerPower' in r) && !('enemyPower' in r), 'legacy metrics are explicitly renamed');
    const d = r.generationDiagnostics, p = d.parameters, c = r.calibration, count = Math.max(2, s.cell.size), front = Math.floor(count / 2);
    assert.equal(d.assessmentVersion, protocol.metric); assert.equal(p.enemyCount, count); assert.equal(p.frontCount, front); assert.equal(p.backCount, count-front);
    assert.equal(r.playerEquipmentPowers.length, s.cell.size); assert.equal(r.enemyEquipmentPowers.length, count);
    for (const power of [...r.playerEquipmentPowers,...r.enemyEquipmentPowers]) assert.ok(Number.isFinite(power) && power >= 0);
    near(sum(r.playerEquipmentPowers), r.playerEquipmentPowerSum, 'player sum'); near(r.playerEquipmentPowerSum, d.participantAssessment, 'assessment');
    near(sum(r.enemyEquipmentPowers), r.enemyEquipmentPowerSum, 'enemy sum'); near(r.enemyEquipmentPowerSum, c.achievedPower, 'achieved');
    near(p.powerBonus, 1 + ((s.cell.raidLevel - 10) * .06 + .14), 'level'); near(p.groupSizeScaling, 1 + .05 * Math.log(count) / Math.log(4), 'group');
    assert.equal(p.encounterCorrection, .77); near(c.targetPower, ((d.participantAssessment * p.powerBonus) * p.groupSizeScaling) * .77, 'target');
    assert.ok(Number.isFinite(c.multiplier) && c.multiplier >= 0 && c.evaluations > 0);
    assert.ok(['MATCHED','CLOSEST_STEP','MINIMUM_EXCEEDS_TARGET'].includes(c.status));
    assert.equal(r.enemyBasis.length, count);
    r.enemyBasis.forEach((enemy, i) => {
      const g = i < front, scaled = base => Math.max(1, Math.ceil(base * c.multiplier));
      assert.equal(enemy.health, scaled(g ? 420 : 280)); assert.equal(enemy.attack, scaled(g ? 70 : 130)); assert.equal(enemy.defense, scaled(g ? 160 : 70));
      assert.equal(enemy.declaredPosition, g ? 'FRONT' : 'BACK'); assert.equal(enemy.range, g ? 1 : 3); same(enemy.attackTypes, ['MAGICAL'], 'NPC attack type');
      same(enemy.skills, [{code:g ? 'KNOCKBACK' : 'PRECISE_STRIKE',points:4,formulaVersion:'SCALING_SKILLS_V3'}], 'NPC skills');
      const mu = [1.1,.9,.75,1.25].map(w => 1 - enemy.defense*w/(enemy.defense*w+500)).reduce((a,b)=>a+b,0)/4;
      const power = enemy.health / ((1-(g?.03:.05))*mu) * ((g?.19:.16)*(1+(g?.04:.08)*((1.2+(g?.3:.5))-1))*enemy.attack);
      near(power, r.enemyEquipmentPowers[i], 'independent NPC power');
    });
    const pair = x.pairs.get(r.pair) || []; assert.ok(!pair.some(v => v.reverse === r.reverse), 'duplicate mirror');
    const inputHash = crypto.createHash('sha256').update(canonical(Object.fromEntries(mirrorKeys.map(k=>[k,r[k]])))).digest('hex');
    if (pair.length) assert.equal(inputHash,pair[0].inputHash,'identical mirrored inputs');
    pair.push({reverse:r.reverse,outcome:r.outcome,inputHash}); x.pairs.set(r.pair,pair);
    assert.ok(['WIN','LOSS','DRAW','TIMEOUT'].includes(r.outcome)); bump(x.outcomes,r.outcome);
    const order = r.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'; bump(x.orders[order] ||= {}, r.outcome); bump(x.statuses,c.status);
    if (c.targetPower > 0) x.errors.push(Math.abs(c.achievedPower/c.targetPower-1));
    if (r.outcome !== 'TIMEOUT') {
      assert.equal(r.initState.tempoVersion, r.tempo); assert.ok(r.rounds >= 0 && r.rounds <= r.roundLimit);
      assert.match(r.actionSha256,/^[0-9a-f]{64}$/); assert.match(r.traceSha256,/^[0-9a-f]{64}$/);
      r.enemyBasis.forEach((e,i) => assert.equal(r.initState.lines[r.initState.personagesById[e.id].lineIndex].position, i < front ? 'FRONT' : 'BACK'));
      for (const key of ['rounds','actionCount','normalAttempts','moves','forcedMoves']) (x.metrics[key] ||= []).push(r[key]);
      for (const side of ['playerTotals','enemyTotals']) for (const key of ['survivors','initialHealth','remainHealth','normalAttacksHit','normalDamage','critDamage','skillDamage','turns']) (x.metrics[`${side}.${key}`] ||= []).push(r[side][key]);
    }
    x.rows++; rowCount++;
  }
  assert.equal(rowCount,8620); assert.equal(hashes.digest('hex'),summary.rawSha256);
  for (const [index,x] of results.entries()) {
    const s=x.s, scores=[]; assert.equal(x.rows,s.pairs*2); assert.equal(x.pairs.size,s.pairs);
    for(let i=0;i<s.pairs;i++) { const pair=x.pairs.get(i); assert.equal(pair.length,2); scores.push(pair.some(r=>r.outcome==='TIMEOUT')?null:pair.reduce((n,r)=>n+(r.outcome==='WIN'?.5:r.outcome==='DRAW'?.25:0),0)); }
    const complete=scores.filter(v=>v!==null), expected={caseId:s.caseId,family:s.family,variant:s.variant,change:s.change,cell:s.cell,seedGroup:s.seedGroup,
      outcomes:x.outcomes,outcomesByOrder:x.orders,attemptedBattles:x.rows,completedBattles:x.rows-(x.outcomes.TIMEOUT||0),completePairs:complete.length,
      incompletePairs:s.pairs-complete.length,pairScoresByIndex:scores,playerWinsPerAttempt:(x.outcomes.WIN||0)/x.rows,pairedScore:distribution(complete),
      completedBattleMetrics:Object.fromEntries(Object.entries(x.metrics).map(([k,v])=>[k,distribution(v)])),calibrationStatuses:x.statuses,calibrationAbsoluteRelativeError:distribution(x.errors)};
    if(complete.length) expected.pairedScore95Hoeffding=interval(complete); compare(summary.cases[index],expected,`case${s.caseId}`);
  }
  for(const c of summary.comparisons) { const a=summary.cases[c.baseCaseId-1], b=summary.cases[c.variantCaseId-1]; assert.equal(a.seedGroup,b.seedGroup);
    const deltas=a.pairScoresByIndex.flatMap((v,i)=>v===null||b.pairScoresByIndex[i]===null?[]:[b.pairScoresByIndex[i]-v]);
    compare(c.pairedDeltas,deltas,'paired deltas'); compare(c.distribution,distribution(deltas),'delta distribution'); if(deltas.length) compare(c.pairedDelta95Hoeffding,interval(deltas,true),'delta interval'); }
  assert.equal(summary.comparisons.length,78);
  console.log(JSON.stringify({status:'VALID',caseCount:359,attemptedBattles:rowCount,verifiedComparisons:78,independentNpcFormula:true}));
})().catch(error=>{ console.error(error.stack); process.exitCode=1; });
