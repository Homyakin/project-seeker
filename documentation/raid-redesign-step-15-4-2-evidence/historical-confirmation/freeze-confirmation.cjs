const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const root=__dirname,pilot='/tmp/seeker-step1542-historical.77NJth';
const policyPath='/home/user/projects/project-seeker/documentation/raid-redesign-step-15-4-2-evidence/confirmation-policy.json';
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const policySHA256=sha(policyPath);assert.equal(policySHA256,'b4c8ca144c8b454e6a123405390ea985f2f041ed9669a77a441233a51e714ebf');
const policy=JSON.parse(fs.readFileSync(policyPath));
const prepPath=path.join(root,'preparation/manifest.json'),prep=JSON.parse(fs.readFileSync(prepPath));
assert.equal(prep.policySHA256,policySHA256);assert.equal(prep.pairs,500);assert.equal(prep.level,25);assert.equal(prep.groupId,251);
const oldPrep=JSON.parse(fs.readFileSync(path.join(pilot,'preparation-frozen/manifest.json')));
assert.deepEqual(prep.fixtures,oldPrep.fixtures.slice(0,2));
const oldFreeze=JSON.parse(fs.readFileSync(path.join(pilot,'runtime-freeze.json')));
for(const e of [...oldFreeze.classFiles,...oldFreeze.sourceFiles])assert.equal(sha(e.path),e.sha256,'Pilot/runtime immutable');
const fixturePath='/tmp/seeker-step15-4-2-causal.1HOgPK/preparation/fixture-manifest.json';
assert.equal(sha(fixturePath),policy.fixtureManifestSHA256);
const seed=(namespace,group,pair)=>crypto.createHash('sha256').update(`${namespace}/${group}/${pair}`).digest().readUIntBE(0,6);
const oldSeeds=new Set();for(const l of [10,25])for(const g of [1,2])for(let pair=0;pair<30;pair++)for(const ns of Object.values(oldPrep.namespaces))oldSeeds.add(seed(ns,10*l+g,pair));
const seen=new Set(),seeds=[];
for(let pair=0;pair<500;pair++){
  const generationSeed=seed(prep.namespaces.generation,251,pair),battleSeed=seed(prep.namespaces.battle,251,pair);
  for(const s of [generationSeed,battleSeed]){assert.ok(!oldSeeds.has(s),'Fresh roots disjoint from pilot');assert.ok(!seen.has(s),'Fresh roots unique');seen.add(s);}
  seeds.push({groupId:251,pair,generationSeed,battleSeed});
}
for(const row of prep.construction){
  const old=oldPrep.construction.find(c=>c.fixture.id===row.fixture.id&&c.level===25);
  const strip=p=>{const {id,initiativeGauge,...rest}=p;return rest;};
  assert.deepEqual(row.playersPrepared.map(strip),old.playersPrepared.map(strip),'Same physical player preparation, except fresh UUID/gauge');
  assert.deepEqual(row.npcPrepared.map(strip),old.npcPrepared.map(strip),'Same physical NPC preparation, except fresh UUID/gauge');
  assert.equal(row.generationSeed,seeds[0].generationSeed);assert.equal(row.battleSeed,seeds[0].battleSeed);
}
fs.copyFileSync(policyPath,path.join(root,'frozen-policy.json'));
fs.copyFileSync(fixturePath,path.join(root,'frozen-common-fixture.json'));
fs.writeFileSync(path.join(root,'seeds.json'),JSON.stringify(seeds,null,2)+'\n');
const files=['HistoricalCausalConfirmProbe.java','classes/HistoricalCausalConfirmProbe.class','preparation/manifest.json','seeds.json','frozen-policy.json','frozen-common-fixture.json'].map(p=>({path:path.join(root,p),sha256:sha(path.join(root,p))}));
fs.writeFileSync(path.join(root,'runtime-freeze.json'),JSON.stringify({purpose:'BEFORE_FIRST_FRESH_CONFIRMATION_OUTCOME',policySHA256,files,pilotRuntimeFreeze:path.join(pilot,'runtime-freeze.json'),pilotRuntimeFreezeSHA256:sha(path.join(pilot,'runtime-freeze.json')),executionLimit:2000,conditions:[{scenarioId:1,level:25},{scenarioId:2,level:25}],pairCount:500,seedRoots:1000,freshRootsDisjoint:true,fixtureUnchanged:true},null,2)+'\n');
console.log(JSON.stringify({ready:true,policySHA256,fixtureUnchanged:true,oldRuntimeUnchanged:true,constructionOnly:true,freshSeedRoots:1000,executionLimit:2000},null,2));
