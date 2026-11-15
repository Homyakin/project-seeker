const fs = require('node:fs');
const crypto = require('node:crypto');
const assert = require('node:assert/strict');
const path = require('node:path');
const out = __dirname;
const commonDir = '/tmp/seeker-step15-4-2-causal.1HOgPK/preparation';
const commonPath = path.join(commonDir, 'fixture-manifest.json');
const historicalPath = path.join(out, 'preparation-frozen/manifest.json');
const common = JSON.parse(fs.readFileSync(commonPath));
const historical = JSON.parse(fs.readFileSync(historicalPath));
const currentPrepared = JSON.parse(fs.readFileSync(path.join(commonDir, 'prepared-pair0.json'))).filter(x => x.phase === 'C0');
const seeds = JSON.parse(fs.readFileSync(path.join(commonDir, 'seeds.json')));
let checks=0;
const canonical = value => Array.isArray(value) ? value.map(canonical) : value && typeof value==='object'
  ? Object.fromEntries(Object.keys(value).sort().map(key=>[key,canonical(value[key])])) : value;
const eq = (a,b,label) => { assert.deepEqual(canonical(a),canonical(b),label);checks++; };
const hspec = member => ({position:member.position,tactic:member.tactic,
  items:member.items.map(i=>({code:i.code,rarity:i.rarity,enhanceLevel:i.enhanceLevel,modifier:i.active}))});
const cspec = member => ({position:member.position,tactic:member.tactic,items:member.items});
for(const fixture of historical.fixtures){
  const c=common.fixtureSpecs.find(x=>x.gear==='OLD'&&x.scenarioId===fixture.id);
  assert.ok(c);checks++;
  eq(fixture.members.map(hspec),c.members.map(cspec),`Exact item order, rarity, enhancement, modifier, position, tactic scenario ${fixture.id}`);
}
function seed(namespace,groupId,pair){
  const bytes=crypto.createHash('sha256').update(`${namespace}/${groupId}/${pair}`).digest();
  return bytes.readUIntBE(0,6);
}
for(const s of seeds){
  eq(seed(historical.namespaces.generation,s.groupId,s.pair),s.generationSeed,'generation SHA48');
  eq(seed(historical.namespaces.battle,s.groupId,s.pair),s.battleSeed,'battle SHA48');
}
function htraits(p){return {
  id:p.id,hp:p.hp,legacyPower:p.power,range:p.range,position:p.startPosition,speed:p.initiative,gauge:p.initiativeGauge,
  crit:p.critChance,dodge:p.dodgeChance,critMultiplier:p.critMultiplier,defenses:p.defenses,attacks:p.attacksByRange,
  reductions:p.damageTakenMultipliers,tactic:p.targetingTactic,threat:p.totalThreat,items:p.items,skills:p.skills
};}
function ctraits(p){const {impact,scalingRules,...rest}=p;return {...rest,skills:p.skills.map(({formulaVersion,...skill})=>skill)};}
const rows=[];
for(const h of historical.construction){
  const c=currentPrepared.find(c=>c.scenarioId===h.fixture.id&&c.level===h.level);
  assert.ok(c);checks++;
  const sharedSeeds=seeds.find(s=>s.groupId===10*h.level+(h.fixture.group==='T'?1:2)&&s.pair===0);
  eq(h.generationSeed,sharedSeeds.generationSeed,'prepared GEN');
  eq(h.battleSeed,sharedSeeds.battleSeed,'prepared BATTLE');
  eq(h.playersPrepared.map(htraits),c.players.map(ctraits),`Historical versus current C0 actual player values UUID gauge L${h.level} scenario ${h.fixture.id}`);
  eq(h.npcPrepared.map(htraits),c.enemies.map(ctraits),`Historical versus current C0 actual NPC values UUID gauge L${h.level} scenario ${h.fixture.id}`);
  rows.push({scenarioId:h.fixture.id,level:h.level,players:h.playersPrepared.length,enemies:h.npcPrepared.length,
    playerPower:h.playersPrepared.reduce((sum,p)=>sum+p.power,0),enemyPower:h.npcPrepared.reduce((sum,p)=>sum+p.power,0),exactPreparedTraits:true});
}
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const report={verdict:'PASS',checks,commonFixtureManifest:commonPath,commonFixtureSHA256:sha(commonPath),historicalManifest:historicalPath,
  historicalManifestSHA256:sha(historicalPath),helperSHA256:sha(path.join(out,'HistoricalCausalProbe.java')),
  scope:'Exact OLD fixture descriptions and order; all120 seed records; all8 pair0 C0 versus coherent historical player/NPC prepared traits including UUID/gauge. Excludes only names and nonhistorical metadata impact/scalingRules/formulaVersion.',rows};
fs.writeFileSync(path.join(out,'common-preparation-validation.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report,null,2));
