const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),assert=require('node:assert/strict');
const root=__dirname,pilot='/tmp/seeker-step1542-historical.77NJth',screen=path.join(root,'screen');
const prep=JSON.parse(fs.readFileSync(path.join(root,'preparation/manifest.json'))),policy=JSON.parse(fs.readFileSync(path.join(root,'frozen-policy.json')));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const canonical=v=>Array.isArray(v)?v.map(canonical):v&&typeof v==='object'?Object.fromEntries(Object.keys(v).sort().map(k=>[k,canonical(v[k])])):v;
let checks=0;const eq=(a,b,label)=>{assert.deepEqual(canonical(a),canonical(b),label);checks++;};const ok=(v,label)=>{assert.ok(v,label);checks++;};
const freeze=JSON.parse(fs.readFileSync(path.join(root,'runtime-freeze.json'))),oldFreeze=JSON.parse(fs.readFileSync(path.join(pilot,'runtime-freeze.json')));
eq(sha(fs.readFileSync(freeze.pilotRuntimeFreeze)),freeze.pilotRuntimeFreezeSHA256,'Pilot freeze unchanged');
for(const f of [...freeze.files,...oldFreeze.classFiles,...oldFreeze.sourceFiles])eq(sha(fs.readFileSync(f.path)),f.sha256,'All new/pilot frozen inputs classes sources unchanged');
const compressed=fs.readFileSync(path.join(screen,'battles.jsonl.gz')),raw=zlib.gunzipSync(compressed).toString('utf8').trim().split('\n'),rows=raw.map(JSON.parse);
eq(rows.length,2000,'Exactly authorized H executions');
const seeds=JSON.parse(fs.readFileSync(path.join(root,'seeds.json'))),seedMap=new Map(seeds.map(s=>[s.pair,s]));
const idx=new Map(),summary=[];
function logSlice(json){const key='"actionLog":';const start=json.indexOf(key)+key.length;ok(start>=key.length,'Full action log present');let quoted=false,escaped=false,depth=0;
  for(let i=start;i<json.length;i++){const c=json[i];if(quoted){if(escaped)escaped=false;else if(c==='\\')escaped=true;else if(c==='"')quoted=false;continue;}if(c==='"')quoted=true;else if(c==='{'||c==='[')depth++;else if(c==='}'||c===']'){depth--;if(depth===0)return json.slice(start,i+1);}}throw Error('Bad action log value');}
const strip=p=>{const{id,initiativeGauge,...rest}=p;return rest;};
for(let at=0;at<rows.length;at++){
  const r=rows[at],c=r.case;eq(c.phase.id,'H','Only phase H');ok([1,2].includes(c.scenarioId),'Only authorized scenarios');
  eq(c.level,25,'Only authorized level');eq(c.groupId,251,'Only authorized group');eq(r.level,25,'Level alias');eq(r.scenarioId,c.scenarioId,'Scenario alias');
  ok(Number.isInteger(r.pair)&&r.pair>=0&&r.pair<500,'Pair range');eq(r.reverse,r.mirror==='PLAYERS_FIRST','Shared mirror convention');
  const seed=seedMap.get(r.pair);eq(r.generationSeed,seed.generationSeed,'Frozen GEN seed');eq(r.battleSeed,seed.battleSeed,'Frozen BATTLE seed');eq(r.policySHA256,freeze.policySHA256,'Frozen policy');
  const key=`${c.scenarioId}/${r.pair}/${r.reverse}`;ok(!idx.has(key),'Unique execution');idx.set(key,r);
  const physical=prep.construction.find(x=>x.fixture.id===c.scenarioId);ok(Boolean(physical),'Frozen construction');
  eq(r.playersPrepared.map(strip),physical.playersPrepared.map(strip),'Exact unchanged player numeric values');
  eq(r.npcPrepared.map(strip),physical.npcPrepared.map(strip),'Exact unchanged NPC numeric values');
  eq(r.playersPrepared.length,c.scenarioId===1?3:4,'Player count');eq(r.npcPrepared.length,c.scenarioId===1?3:4,'NPC count');
  ok(r.rounds>0&&r.rounds<=2000&&Number.isInteger(r.rounds),'Round bound');ok(['WIN','LOSS','DRAW','TIMEOUT'].includes(r.outcome),'Valid outcome');
  if(r.outcome==='TIMEOUT')continue;
  eq(sha(Buffer.from(logSlice(raw[at]))),r.actionLogSHA256,'Full log serialized hash');eq(r.actionLog.events.length,r.actionCount,'Full event count');
  eq(r.actualEnemyPositions,Array(r.npcPrepared.length).fill('FRONT'),'Old NPC range1 geometry retained');
  const pa=r.playerStats.some(s=>s.remainHealth>0),ea=r.enemyStats.some(s=>s.remainHealth>0);eq(r.outcome,pa?'WIN':ea?'LOSS':'DRAW','Actual remaining HP outcome');ok(!(pa&&ea),'Only one surviving team');
  const init=r.initState.personagesById;eq(Object.keys(init).sort(),[...r.playersPrepared,...r.npcPrepared].map(p=>p.id).sort(),'Exact participant identities');
  for(const [player,ps,ss,total] of [[true,r.playersPrepared,r.playerStats,r.playersTotals],[false,r.npcPrepared,r.enemyStats,r.npcTotals]]){
    eq(ps.length,ss.length,'Individual stat count');let hp=0,damage=0,turns=0;
    for(let i=0;i<ps.length;i++){const p=ps[i],s=ss[i],a=init[p.id];ok(p.hp>0&&p.initiative>0&&Number.isFinite(p.power),'Living finite prepared body');
      eq(a.health,p.hp,'Init HP');eq(a.firstTeam,player?r.reverse:!r.reverse,'Init mirror');eq(a.initiativeGauge,p.initiativeGauge,'Init gauge');eq(a.initiative,p.initiative,'Init initiative');
      eq(a.range,p.range,'Init range');eq(a.attacksByRange,p.attacksByRange,'Init attacks');eq(a.defenses,p.defenses,'Init defenses');eq(a.damageTakenMultipliers,p.damageTakenMultipliers,'Init reductions');
      eq(a.skills,p.skills,'Init skills');eq(a.items,p.items,'Init item snapshots');eq(a.targetingTactic,p.targetingTactic,'Init tactic');eq(s.initialHealth,p.hp,'Stats initial HP');
      ok(s.remainHealth>=0&&s.remainHealth<=p.hp,'Stats remaining HP');hp+=s.remainHealth;damage+=s.normalDamageDealt+s.critDamageDealt+s.skillDamageDealt;turns+=s.turnsCount;}
    eq(total.remainingHp,hp,'Team HP sum');eq(total.damageDealt,damage,'Team damage sum');eq(total.turns,turns,'Team turns sum');ok(Number.isFinite(total.actualDamageTaken)&&total.actualDamageTaken>=0,'Actual damage taken finite');
  }
}
for(const r of rows){const twin=idx.get(`${r.scenarioId}/${r.pair}/${!r.reverse}`);ok(Boolean(twin),'Mirror exists');eq(r.playersPrepared,twin.playersPrepared,'Mirror same exact players');eq(r.npcPrepared,twin.npcPrepared,'Mirror same exact NPC');
  if(r.scenarioId===2){const b=idx.get(`1/${r.pair}/${r.reverse}`);ok(Boolean(b),'Append base exists');eq(r.playersPrepared.slice(0,3),b.playersPrepared,'Exact old player prefix UUID gauge numeric traits');}}
const saved=JSON.parse(fs.readFileSync(path.join(screen,'summary.json')));
for(const scenarioId of [1,2]){const sample=rows.filter(r=>r.scenarioId===scenarioId),outcomes={WIN:0,LOSS:0,DRAW:0,TIMEOUT:0};sample.forEach(r=>outcomes[r.outcome]++);eq(sample.length,1000,'1000 executions per condition');
  const pairScores=Array.from({length:500},(_,pair)=>{const pairRows=sample.filter(r=>r.pair===pair);eq(pairRows.length,2,'Two mirrors per pair');return pairRows.some(r=>r.outcome==='TIMEOUT')?null:pairRows.reduce((sum,r)=>sum+(r.outcome==='WIN'?.5:r.outcome==='DRAW'?.25:0),0);});
  const s=saved.cases.find(s=>s.scenarioId===scenarioId);eq(s.wins,outcomes.WIN,'Saved wins');eq(s.losses,outcomes.LOSS,'Saved losses');eq(s.draws,outcomes.DRAW,'Saved draws');eq(s.timeouts,outcomes.TIMEOUT,'Saved timeouts');eq(s.pairScores,pairScores,'Saved complete pair scores');eq(s.winRate,outcomes.WIN/1000,'Saved win rate');
  const meanRounds=sample.reduce((sum,r)=>sum+r.rounds,0)/1000;eq(s.meanRounds,meanRounds,'Saved mean rounds');
  summary.push({scenarioId,level:25,outcomes,winRate:outcomes.WIN/1000,meanRounds,pairScores,
    sideWins:{playersFirst:sample.filter(r=>r.reverse&&r.outcome==='WIN').length,npcFirst:sample.filter(r=>!r.reverse&&r.outcome==='WIN').length}});
}
const differences=summary[1].pairScores.map((x,i)=>x===null||summary[0].pairScores[i]===null?null:x-summary[0].pairScores[i]);
const complete=differences.filter(x=>x!==null),deltaH=complete.reduce((sum,x)=>sum+x,0)/complete.length;
const half=Math.sqrt(2*Math.log(2*policy.confidence.familySize/policy.confidence.alpha)/complete.length);
const report={verdict:'PASS',checks,rows:rows.length,rawSHA256:sha(compressed),policySHA256:freeze.policySHA256,allFrozenFilesUnchanged:true,fullActionLogs:true,
  summary,deltaH:{mean:deltaH,completePairs:complete.length,halfWidth:half,simultaneousPlannedFamily95:[Math.max(-1,deltaH-half),Math.min(1,deltaH+half)],
    familySize:3,pilotExcluded:true,note:'H component only; parent computes all three predeclared H/C9/DID contrasts on shared complete pair indices. No claim harm exceeds20pp from this interval.'}};
fs.writeFileSync(path.join(screen,'validation.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({verdict:report.verdict,checks,rows:rows.length,rawSHA256:report.rawSHA256,deltaH:report.deltaH},null,2));
