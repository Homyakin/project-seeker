const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),readline=require('node:readline'),assert=require('node:assert/strict');
const args=process.argv.slice(2);
if(args.length!==2||!['pilot','screen'].includes(args[1]))throw Error('Usage: node pilot-result-checker.cjs ARCHIVE_OR_RUN_ROOT pilot|screen (stdout only; no files written)');
const run=path.resolve(args[0]);
const frozen=path.join(run,'frozen-preparation'),screen=path.join(run,args[1]);
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const policy=JSON.parse(fs.readFileSync(path.join(frozen,'screen-policy.json'))),inputs=JSON.parse(fs.readFileSync(path.join(frozen,'inputs.json'))),prepared=JSON.parse(fs.readFileSync(path.join(frozen,'prepared-pair0.json'))),saved=JSON.parse(fs.readFileSync(path.join(screen,'summary.json')));
let checks=0;const eq=(a,b,label)=>{assert.deepEqual(a,b,label);checks++;},ok=(x,label)=>{assert.ok(x,label);checks++;};
eq(sha(path.join(frozen,'screen-policy.json')),'e41947b273b6db0defa748fcd0a8edbcbd1dec482570a5049aa1f0b7e9c858cf','Frozen policy');
eq(sha(path.join(frozen,'manifest.json')),policy.manifestSHA256,'Frozen manifest');eq(sha(path.join(frozen,'Step1543HeterogeneityProbe.java')),policy.helperSHA256,'Frozen helper');
const types=['SLASH','BLUNT','PIERCE','MAGICAL'],sum=xs=>[...xs].sort((a,b)=>a-b).reduce((s,x)=>s+x,0);
function evaluate(row){const cs=row.players.map(p=>{const mu=types.reduce((s,t)=>s+p.reductions[t],0)/4,H=p.hp/mu;
  const attack=Math.max(0,...p.attacks.map(a=>types.reduce((s,t)=>s+(a.attack[t]||0),0)));
  const D=attack*(1+Math.max(0,Math.min(1,p.crit/100))*(p.critMultiplier-1))*(Math.min(1000,p.speed)/1000);
  return{id:p.id,H,D,p:H*D};});
  eq(cs,row.contributions,'Independent prepared neutral parts exact');
  const Q=sum(cs.map(x=>x.H))*sum(cs.map(x=>x.D)),pi=cs.map(x=>x.p).sort((a,b)=>a-b);let rawB=0,rawT=pi[pi.length-1],bestK=1;
  for(let i=0;i<pi.length;i++){rawB+=(2*(pi.length-i)-1)*pi[i];const k=i+1,t=k*k*pi[pi.length-k];if(t>rawT){rawT=t;bestK=k;}}
  const uniform=cs.every(p=>p.H===cs[0].H&&p.D===cs[0].D),boundedB=Math.min(rawB,Q),boundedT=Math.min(rawT,Q);
  const F=Q===0?0:row.rule==='BASELINE'||uniform?Q:Q/2+(row.rule==='BLEND_B'?boundedB:boundedT)/2;
  return{Q,rawB,rawT,bestK,boundedB,boundedT,F,discount:Q===0?1:Math.max(.5,Math.min(1,F/Q)),exactSoloOrIdentical:uniform};
}
const prepIndex=new Map();for(const p of prepared){eq(evaluate(p),p.playerEvaluation,'Independent Q/B/T/F/d prepared calculation exact');
  eq(p.target.requestedTarget,p.target.baselineTarget*p.target.discount,'Target multiplication order exact');
  const target=p.target;eq(target.baselineTarget,((((target.assessment*target.levelPowerBonus)*target.groupScaling)*target.encounterCorrection)*target.sizeCorrection),'Original target order exact');
  prepIndex.set(`${p.case.id}/${p.rule}`,p);}
const meta=c=>({id:c.id,groupId:c.groupId,family:c.family,style:c.style,tier:c.tier,level:c.level,variant:c.variant,N:c.members.length});
const cells=new Map(),rowIndex=new Map();let rows=0;
async function main(){
  const stream=fs.createReadStream(path.join(screen,'battles.jsonl.gz')).pipe(zlib.createGunzip()),reader=readline.createInterface({input:stream,crlfDelay:Infinity});
  for await(const line of reader){const r=JSON.parse(line),c=r.case,key=`${c.id}/${r.rule}`,p=prepIndex.get(key);ok(Boolean(p),'Frozen numeric condition');
    eq(r.ruleAssessment,p.playerEvaluation,'All raw player evaluations equal fixed numeric preparation');
    eq(r.target,p.target,'All raw requested targets equal preparation');
    eq(r.calibrationAssessment,p.calibrationEvaluation,'Calibration assessments remain explicit including fixed NPC');
    eq(r.outcome,r.playerStats.some(s=>s.remainHealth>0)?'WIN':r.enemyStats.some(s=>s.remainHealth>0)?'LOSS':'DRAW','Independent actual HP outcome');
    ok(r.pair>=0&&r.pair<25&&Number.isInteger(r.pair),'25 pair bound');eq(typeof r.reverse,'boolean','Mirror boolean');
    const rk=`${key}/${r.pair}/${r.reverse}`;ok(!rowIndex.has(rk),'Unique execution');
    const short={caseId:c.id,rule:r.rule,pair:r.pair,reverse:r.reverse,outcome:r.outcome,rounds:r.rounds,actionSha256:r.actionSha256,traceSha256:r.traceSha256};rowIndex.set(rk,short);
    const cell=cells.get(key)||{case:meta(c),rule:r.rule,rows:[],outcomes:{WIN:0,LOSS:0,DRAW:0,TIMEOUT:0},evaluation:p.playerEvaluation,target:p.target,calibration:p.calibration};
    cell.rows.push(short);ok(Object.hasOwn(cell.outcomes,r.outcome),'Known outcome');cell.outcomes[r.outcome]++;cells.set(key,cell);rows++;
  }
  eq(rows,11200,'Authorized total only');eq(cells.size,224,'Authorized model condition count');
  const summaries=[];
  for(const [key,c]of cells){eq(c.rows.length,50,'50 executions per condition');const pairScores=[];
    for(let pair=0;pair<25;pair++){const rs=c.rows.filter(r=>r.pair===pair);eq(rs.length,2,'Two mirrors');eq(rs.map(r=>r.reverse).sort(),[false,true],'Both sides');
      pairScores.push(rs.some(r=>r.outcome==='TIMEOUT')?null:rs.reduce((s,r)=>s+(r.outcome==='WIN'?.5:r.outcome==='DRAW'?.25:0),0));}
    const summary={case:c.case,rule:c.rule,outcomes:c.outcomes,winrate:c.outcomes.WIN/50,pairScores,meanRounds:c.rows.reduce((s,r)=>s+r.rounds,0)/50,
      sideWins:{playersFirst:c.rows.filter(r=>r.reverse&&r.outcome==='WIN').length,npcFirst:c.rows.filter(r=>!r.reverse&&r.outcome==='WIN').length},discount:c.evaluation.discount,bestK:c.evaluation.bestK,m:c.calibration.multiplier};
    const old=saved.cases.find(s=>s.case.id===c.case.id&&s.rule===c.rule);ok(Boolean(old),'Saved summary condition');eq(summary.winrate,old.winrate,'Independent raw win rate');eq(pairScores,old.pairScores,'Independent raw pair scores');eq(summary.meanRounds,old.meanRounds,'Independent raw mean rounds');summaries.push(summary);
  }
  let immutableModelExecutions=0;
  for(const [key,c]of cells)if(c.rule==='BASELINE'&&c.evaluation.exactSoloOrIdentical&&c.case.family!=='FIXED_NPC')for(const rule of ['BLEND_B','BLEND_T'])for(const r of c.rows){
    const other=rowIndex.get(`${c.case.id}/${rule}/${r.pair}/${r.reverse}`);ok(Boolean(other),'Unchanged path model');
    eq([other.outcome,other.rounds,other.actionSha256,other.traceSha256],[r.outcome,r.rounds,r.actionSha256,r.traceSha256],'Unchanged solo/uniform full event and trace hashes');immutableModelExecutions++;}
  const by=(id,rule)=>summaries.find(s=>s.case.id===id&&s.rule===rule);
  const append=(a,b,rule)=>{const x=by(a,rule),y=by(b,rule),ds=y.pairScores.map((score,i)=>score===null||x.pairScores[i]===null?null:score-x.pairScores[i]);
    const valid=ds.filter(x=>x!==null),delta=valid.reduce((s,x)=>s+x,0)/valid.length,half=Math.sqrt(2*Math.log(40)/valid.length);
    return{rule,base:x.case,append:y.case,baseWinrate:x.winrate,appendWinrate:y.winrate,delta,descriptivePointwise95Hoeffding:[Math.max(-1,delta-half),Math.min(1,delta+half)],pairDifferences:ds};};
  const rules=['BASELINE','BLEND_B','BLEND_T'];
  const weakCore=rules.flatMap(rule=>[[13,14],[19,20],[25,26]].map(([a,b])=>append(a,b,rule)));
  const ordinary=rules.flatMap(rule=>[[1,2],[4,5],[7,8],[10,11]].map(([a,b])=>append(a,b,rule)));
  const ordinaryWeak=rules.flatMap(rule=>[[1,3],[4,6],[7,9],[10,12]].map(([a,b])=>append(a,b,rule)));
  const n8=rules.map(rule=>append(63,64,rule)),mixed=rules.map(rule=>append(73,74,rule));
  const stripped=rules.flatMap(rule=>[[14,65,66],[20,67,68],[26,69,70]].map(([full,weapon,body])=>({rule,style:by(full,rule).case.style,full:by(full,rule).winrate,weaponRemoved:by(weapon,rule).winrate,bodyRemoved:by(body,rule).winrate})));
  const counters=rules.flatMap(rule=>[49,50,51,52,53,54,55,56].map(id=>({rule,...by(id,rule)})));
  const groups=Object.fromEntries([...new Set(inputs.map(c=>c.family))].map(family=>[family,summaries.filter(s=>s.case.family===family)]));
  const report={purpose:'POST_OUTCOME_INDEPENDENT_NUMERIC_AND_RAW_RESULT_REVIEW_NO_BATTLES_NOT_RETROACTIVE_PRE_OUTCOME_ACK',checks,rows,modelConditions:cells.size,
    policySHA256:sha(path.join(frozen,'screen-policy.json')),rawSHA256:sha(path.join(screen,'battles.jsonl.gz')),preparedModelCalculationsExact:224,immutableModelExecutions,
    outcomes:Object.fromEntries(['WIN','LOSS','DRAW','TIMEOUT'].map(outcome=>[outcome,summaries.reduce((s,x)=>s+x.outcomes[outcome],0)])),
    selectionCriterion:'Pre-outcome policy order; descriptive pilot only, not all50/50 or universal monotonicity',
    worstWeakCoreByRule:Object.fromEntries(rules.map(rule=>[rule,Math.min(...weakCore.filter(x=>x.rule===rule).map(x=>x.delta))])),weakCore,ordinary,ordinaryWeak,n8,mixed,stripped,counters,groups,
    recommendation:'BLEND_T is the sole candidate recommended for a separately frozen fresh confirmation; BLEND_B leaves the RITUAL core failure. Not a production decision.'};
  console.log(JSON.stringify(report,null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});

