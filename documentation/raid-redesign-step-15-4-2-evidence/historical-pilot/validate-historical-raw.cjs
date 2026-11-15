const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),assert=require('node:assert/strict');
const root=__dirname,screen=path.join(root,'screen');
const manifest=JSON.parse(fs.readFileSync(path.join(root,'preparation-frozen/manifest.json')));
const freeze=JSON.parse(fs.readFileSync(path.join(root,'runtime-freeze.json')));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const eq=(a,b,msg)=>{assert.deepEqual(a,b,msg);checks++;};
const ok=(v,msg)=>{assert.ok(v,msg);checks++;};
let checks=0;
for(const entry of [...freeze.classFiles,...freeze.sourceFiles])eq(sha(fs.readFileSync(entry.path)),entry.sha256,'Frozen runtime/source unchanged');
const bytes=fs.readFileSync(path.join(screen,'battles.jsonl.gz'));
const lines=zlib.gunzipSync(bytes).toString('utf8').trim().split('\n');
eq(lines.length,480,'Exactly authorized historical executions');
const rows=lines.map(JSON.parse),index=new Map(),summaries=[];
const seed=(namespace,groupId,pair)=>crypto.createHash('sha256').update(`${namespace}/${groupId}/${pair}`).digest().readUIntBE(0,6);
function sliceValue(json,key){
  const start=json.indexOf(`"${key}":`)+key.length+3;
  ok(start>key.length+2,'Raw object key exists');
  let string=false,escape=false,depth=0;
  for(let end=start;end<json.length;end++){
    const c=json[end];
    if(string){if(escape)escape=false;else if(c==='\\')escape=true;else if(c==='"')string=false;continue;}
    if(c==='"')string=true;
    else if(c==='{'||c==='[')depth++;
    else if(c==='}'||c===']'){depth--;if(depth===0)return json.slice(start,end+1);}
  }
  throw Error('Unterminated raw value');
}
for(let at=0;at<rows.length;at++){
  const r=rows[at],c=r.case,fixture=manifest.fixtures.find(f=>f.id===c.scenarioId);
  ok(Boolean(fixture),'Known scenario');eq(c.phase.id,'H','Historical phase');eq(r.fixtureId,c.scenarioId,'Scenario aliases');
  ok(manifest.levels.includes(c.level),'Known level');eq(r.level,c.level,'Level aliases');
  const groupId=10*c.level+(fixture.group==='T'?1:2);eq(c.groupId,groupId,'Group calculation');
  ok(Number.isInteger(r.pair)&&r.pair>=0&&r.pair<30,'Authorized pair');
  eq(r.reverse,r.mirror==='PLAYERS_FIRST','Same modern reverse convention');
  eq(r.generationSeed,seed(manifest.namespaces.generation,groupId,r.pair),'SHA48 GEN');
  eq(r.battleSeed,seed(manifest.namespaces.battle,groupId,r.pair),'SHA48 BATTLE');
  const key=`${c.level}/${c.scenarioId}/${r.pair}/${r.reverse}`;
  ok(!index.has(key),'Unique case/pair/mirror');index.set(key,r);
  eq(r.playersPrepared.length,fixture.members.length,'Player count');eq(r.npcPrepared.length,Math.max(2,fixture.members.length),'Historical NPC count');
  ok(['WIN','LOSS','DRAW','TIMEOUT'].includes(r.outcome),'Known outcome');
  ok(Number.isInteger(r.rounds)&&r.rounds>0&&r.rounds<=2000,'Round bound');
  for(const p of [...r.playersPrepared,...r.npcPrepared]){
    ok(p.hp>0&&p.initiative>0&&Number.isFinite(p.power)&&p.power>0,'Live finite prepared participant');
    eq(p.lineIndex,-1,'Prepared before placement');
  }
  if(r.outcome==='TIMEOUT')continue;
  eq(r.actualEnemyPositions,Array(r.npcPrepared.length).fill('FRONT'),'Historical range1 BACK NPC truly brought forward');
  eq(sha(Buffer.from(sliceValue(lines[at],'actionLog'))),r.actionLogSHA256,'Full log hash matches unmodified serialized object');
  eq(r.actionCount,r.actionLog.events.length,'Full event count');
  eq(r.playerStats.length,r.playersPrepared.length,'Player stats count');eq(r.enemyStats.length,r.npcPrepared.length,'NPC stats count');
  const pa=r.playerStats.some(s=>s.remainHealth>0),ea=r.enemyStats.some(s=>s.remainHealth>0);
  eq(r.outcome,pa?'WIN':ea?'LOSS':'DRAW','Outcome from actual remaining HP');
  ok(!(pa&&ea),'Completed battle cannot leave both teams alive');
  const init=r.initState.personagesById;
  const all=[...r.playersPrepared,...r.npcPrepared];
  eq(Object.keys(init).sort(),all.map(p=>p.id).sort(),'Init contains exactly the same prepared bodies');
  for(const [team,prepared,stats,totals] of [
    ['player',r.playersPrepared,r.playerStats,r.playersTotals],['npc',r.npcPrepared,r.enemyStats,r.npcTotals]]){
    let damage=0,turns=0,hp=0;
    for(let i=0;i<prepared.length;i++){
      const p=prepared[i],actual=init[p.id],s=stats[i];
      eq(actual.health,p.hp,'Actual init HP');eq(actual.firstTeam,team==='player'?r.reverse:!r.reverse,'Actual mirror placement');
      eq(actual.initiative,p.initiative,'Actual init speed');eq(actual.initiativeGauge,p.initiativeGauge,'Actual init gauge');
      eq(actual.range,p.range,'Actual init range');eq(actual.defenses,p.defenses,'Actual init defenses');
      eq(actual.attacksByRange,p.attacksByRange,'Actual init attacks');eq(actual.skills,p.skills,'Actual init skills');
      eq(actual.targetingTactic,p.targetingTactic,'Actual init tactic');eq(actual.damageTakenMultipliers,p.damageTakenMultipliers,'Actual init reductions');
      eq(s.initialHealth,p.hp,'Stats initial health');ok(s.remainHealth>=0&&s.remainHealth<=p.hp,'Stats HP bound');
      damage+=s.normalDamageDealt+s.critDamageDealt+s.skillDamageDealt;turns+=s.turnsCount;hp+=s.remainHealth;
    }
    eq(totals.damageDealt,damage,'Exact team damage sum');eq(totals.turns,turns,'Exact team turns sum');eq(totals.remainingHp,hp,'Exact team remaining HP sum');
    ok(Number.isFinite(totals.actualDamageTaken)&&totals.actualDamageTaken>=0,'Finite actual damage taken');
  }
}
for(const r of rows){
  const twin=index.get(`${r.level}/${r.fixtureId}/${r.pair}/${!r.reverse}`);ok(Boolean(twin),'Both mirrors');
  eq(r.playersPrepared,twin.playersPrepared,'Mirrors same prepared players');eq(r.npcPrepared,twin.npcPrepared,'Mirrors same prepared NPC');
  if(r.fixtureId%2===0){const base=index.get(`${r.level}/${r.fixtureId-1}/${r.pair}/${r.reverse}`);ok(Boolean(base),'Append base exists');
    eq(r.playersPrepared.slice(0,base.playersPrepared.length),base.playersPrepared,'Exact living player prefix, UUID/gauge unchanged');}
}
const saved=JSON.parse(fs.readFileSync(path.join(screen,'summary.json')));
for(const level of manifest.levels)for(const fixture of manifest.fixtures){
  const sample=rows.filter(r=>r.level===level&&r.fixtureId===fixture.id),outcomes={WIN:0,LOSS:0,DRAW:0,TIMEOUT:0};
  sample.forEach(r=>outcomes[r.outcome]++);eq(sample.length,60,'60 executions per cell');
  const scores=Array.from({length:30},(_,pair)=>sample.filter(r=>r.pair===pair).reduce((s,r)=>s+(r.outcome==='WIN'?.5:r.outcome==='DRAW'?.25:0),0));
  const s=saved.cases.find(x=>x.level===level&&x.fixtureId===fixture.id);ok(Boolean(s),'Saved summary cell');
  eq(s.wins,outcomes.WIN,'Summary wins');eq(s.losses,outcomes.LOSS,'Summary losses');eq(s.draws,outcomes.DRAW,'Summary draws');eq(s.timeouts,outcomes.TIMEOUT,'Summary timeouts');
  eq(s.winRate,outcomes.WIN/60,'Summary win rate');
  const meanRounds=sample.reduce((sum,r)=>sum+r.rounds,0)/60;eq(s.meanRounds,meanRounds,'Summary mean rounds');
  summaries.push({scenarioId:fixture.id,level,outcomes,winRate:outcomes.WIN/60,pairScores:scores,meanRounds,
    sideWins:{playersFirst:sample.filter(r=>r.reverse&&r.outcome==='WIN').length,npcFirst:sample.filter(r=>!r.reverse&&r.outcome==='WIN').length}});
}
const contrasts=[];
for(const level of manifest.levels)for(const [base,append] of [[1,2],[3,4]]){
  const b=summaries.find(s=>s.level===level&&s.scenarioId===base),a=summaries.find(s=>s.level===level&&s.scenarioId===append);
  const differences=a.pairScores.map((score,i)=>score-b.pairScores[i]);
  const delta=differences.reduce((s,x)=>s+x,0)/30;
  const half=Math.sqrt(2*Math.log(40)/30);
  contrasts.push({level,base,append,delta,individual95HoeffdingHalfWidth:half,individual95Hoeffding:[Math.max(-1,delta-half),Math.min(1,delta+half)],
    pairDifferences:differences,warning:'Only30 mirrored pairs; pointwise descriptive pilot interval, not simultaneous acceptance evidence. Saturated zero or one endpoints do not prove absence of harm.'});
}
const report={verdict:'PASS',checks,rows:rows.length,rawSHA256:sha(bytes),runtimeUnchanged:true,fullActionLogs:true,summaries,contrasts};
fs.writeFileSync(path.join(screen,'validation.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({verdict:report.verdict,checks,rows:rows.length,rawSHA256:report.rawSHA256,contrasts:contrasts.map(({pairDifferences,...rest})=>rest)},null,2));
