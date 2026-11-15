const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),readline=require('node:readline'),assert=require('node:assert/strict');
const args=process.argv.slice(2),plan=args.includes('--plan'),preflight=args.includes('--preflight'),pos=args.filter(x=>!['--plan','--preflight'].includes(x));
if(pos.length!==2||plan&&preflight)throw Error('Usage: node confirmation-result-checker.cjs ARCHIVE_OR_RUN_ROOT RUN_FOLDER [--plan|--preflight] (stdout only; no file writes)');
const root=path.resolve(pos[0]),run=path.join(root,pos[1]),frozen=path.join(root,'frozen-preparation');
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
const policyPath=path.join(frozen,'confirmation-policy.json'),policy=JSON.parse(fs.readFileSync(policyPath));
let checks=0;const eq=(a,b,label)=>{assert.deepEqual(a,b,label);checks++;},ok=(x,label)=>{assert.ok(x,label);checks++;};
eq(sha(policyPath),'3876690b6ceb542c1bf2a3112d2b6e194ae922c028f706bd2b08686c11e515cf','Exact prespecified confirmation policy');
const inputs=JSON.parse(fs.readFileSync(path.join(frozen,'inputs.json'))),inputMap=new Map(inputs.map(c=>[c.id,c]));
const allocations=new Map();for(const a of policy.allocations)for(const rule of a.rules)for(const caseId of a.caseIds){const key=`${rule}/${caseId}`;ok(!allocations.has(key),'No duplicate allocation');ok(inputMap.has(caseId),'Frozen input exists');allocations.set(key,{rule,caseId,pairs:a.pairs});}
eq(allocations.size,37,'37 prespecified model conditions');eq(new Set([...allocations.values()].map(x=>x.caseId)).size,34,'34 original conditions');
eq([...allocations.values()].reduce((s,a)=>s+2*a.pairs,0),80000,'Exactly80000 authorized executions');
function primarySpec(text){const m=/^(BLEND_T|BASELINE)(\d+)-(?:(BLEND_T|BASELINE))?(\d+)$/.exec(text);ok(Boolean(m),'Known primary contrast syntax');return{name:text,leftRule:m[1],leftId:+m[2],rightRule:m[3]||m[1],rightId:+m[4],kind:m[3]==='BASELINE'?'IMPROVEMENT':'APPEND'};}
function secondarySpecs(){return[
  ...policy.secondaryFamily.stripping.map(name=>({name,kind:'STRIPPING'})),
  ...policy.secondaryFamily.counterSimple.map(name=>({name,kind:'COUNTER_SIMPLE'})),
  ...policy.secondaryFamily.counterExtreme.map(name=>({name,kind:'COUNTER_EXTREME'})),
  ...policy.secondaryFamily.weakInsteadOfOrdinary.map(name=>({name,kind:'WEAK_INSTEAD_OF_ORDINARY'}))
].map(x=>{const [leftId,rightId]=x.name.split('-').map(Number);return{...x,leftRule:'BLEND_T',rightRule:'BLEND_T',leftId,rightId};});}
const primary=policy.primaryFamily.comparisons.map(primarySpec),secondary=secondarySpecs();eq(primary.length,11,'Primary K11');eq(secondary.length,20,'Secondary K20');
function planned(spec,family){const l=allocations.get(`${spec.leftRule}/${spec.leftId}`),r=allocations.get(`${spec.rightRule}/${spec.rightId}`);ok(l&&r,'Both contrast allocations');eq(l.pairs,r.pairs,'Frozen equal pair count');
  const lc=inputMap.get(l.caseId),rc=inputMap.get(r.caseId);eq(lc.groupId,rc.groupId,'Prespecified shared seed group');
  return{...spec,pairs:l.pairs,groupId:lc.groupId,K:family.K,alpha:family.alpha,halfWidth:Math.sqrt(2*Math.log(2*family.K/family.alpha)/l.pairs),
    criterion:spec.kind==='APPEND'?'lower > -0.20':spec.kind==='IMPROVEMENT'||spec.kind==='COUNTER_EXTREME'?'lower > 0':spec.kind==='STRIPPING'?'upper < +0.20':'REPORT_ONLY'};}
const plannedPrimary=primary.map(s=>planned(s,policy.primaryFamily)),plannedSecondary=secondary.map(s=>planned(s,policy.secondaryFamily));
if(plan){console.log(JSON.stringify({mode:'PRESPECIFIED_PLAN_ONLY_NO_OUTCOMES_READ',policySHA256:sha(policyPath),checks,modelConditions:allocations.size,executions:80000,
  primary:plannedPrimary,secondary:plannedSecondary,coverage:'Two separate simultaneous95percent families, not a joint95percent statement',timeoutPolicy:'Any affected comparison is INCOMPLETE and cannot confirm a criterion; TIMEOUT is not LOSS'},null,2));process.exit(0);}
const prepared=JSON.parse(fs.readFileSync(path.join(frozen,'prepared-pair0.json'))),prepIndex=new Map(prepared.map(p=>[`${p.rule}/${p.case.id}`,p]));eq(prepared.length,37,'37 prepared models');
const types=['SLASH','BLUNT','PIERCE','MAGICAL'],sum=xs=>[...xs].sort((a,b)=>a-b).reduce((s,x)=>s+x,0);
function evaluate(p){const cs=p.players.map(x=>{const H=x.hp/(types.reduce((s,t)=>s+x.reductions[t],0)/4),a=Math.max(0,...x.attacks.map(v=>types.reduce((s,t)=>s+(v.attack[t]||0),0)));
  const D=a*(1+Math.max(0,Math.min(1,x.crit/100))*(x.critMultiplier-1))*(Math.min(1000,x.speed)/1000);return{id:x.id,H,D,p:H*D};});eq(cs,p.contributions,'Exact independently calculated neutral contributions');
  const Q=sum(cs.map(x=>x.H))*sum(cs.map(x=>x.D)),pi=cs.map(x=>x.p).sort((a,b)=>a-b);let rawB=0,rawT=pi[pi.length-1],bestK=1;
  for(let i=0;i<pi.length;i++){rawB+=(2*(pi.length-i)-1)*pi[i];const k=i+1,t=k*k*pi[pi.length-k];if(t>rawT){rawT=t;bestK=k;}}
  const boundedB=Math.min(rawB,Q),boundedT=Math.min(rawT,Q),uniform=cs.every(x=>x.H===cs[0].H&&x.D===cs[0].D);
  const F=Q===0?0:p.rule==='BASELINE'||uniform?Q:Q/2+boundedT/2;
  return{Q,rawB,rawT,bestK,boundedB,boundedT,F,discount:Q===0?1:Math.max(.5,Math.min(1,F/Q)),exactSoloOrIdentical:uniform};}
for(const [key,p]of prepIndex){ok(allocations.has(key),'Only allocated prepared model');eq(evaluate(p),p.playerEvaluation,'Frozen BLEND_T or baseline exact');
  eq(p.target.requestedTarget,p.target.baselineTarget*p.target.discount,'Exact unchanged target then discount');const t=p.target;
  eq(t.baselineTarget,((((t.assessment*t.levelPowerBonus)*t.groupScaling)*t.encounterCorrection)*t.sizeCorrection),'Original target multiplication order');}
if(preflight){console.log(JSON.stringify({mode:'INDEPENDENT_NUMERIC_PREFLIGHT_NO_OUTCOMES_READ',policySHA256:sha(policyPath),checks,preparedModelConditions:prepared.length,
  neutralAndQTFexact:true,originalTargetThenDiscountExact:true,primary:plannedPrimary,secondary:plannedSecondary},null,2));process.exit(0);}
const metadata=c=>({id:c.id,groupId:c.groupId,family:c.family,style:c.style,tier:c.tier,level:c.level,variant:c.variant,N:c.members.length});
const index=new Map(),cells=new Map();let rowCount=0;
const derive=(namespace,groupId,pair)=>crypto.createHash('sha256').update(`${namespace}/${groupId}/${pair}`).digest().readUIntBE(0,6);
async function main(){
  const rawPath=path.join(run,'battles.jsonl.gz'),reader=readline.createInterface({input:fs.createReadStream(rawPath).pipe(zlib.createGunzip()),crlfDelay:Infinity});
  for await(const line of reader){const r=JSON.parse(line),key=`${r.rule}/${r.case.id}`,allocation=allocations.get(key),p=prepIndex.get(key);ok(allocation&&p,'Only authorized model/condition');
    eq(metadata(r.case),metadata(inputMap.get(r.case.id)),'Frozen condition identity');ok(Number.isInteger(r.pair)&&r.pair>=0&&r.pair<allocation.pairs,'Frozen sample bound');eq(typeof r.reverse,'boolean','Mirror flag');
    eq(r.generationSeed,derive(policy.generationNamespace,r.case.groupId,r.pair),'Fresh SHA48 generation seed');eq(r.battleSeed,derive(policy.battleNamespace,r.case.groupId,r.pair),'Fresh SHA48 battle seed');
    eq(r.npcSeed,derive(policy.npcNamespace,r.case.groupId,r.pair),'Recorded unused NPC seed');eq(r.npcSeedUsed,false,'No fixed NPC conditions');
    eq(r.ruleAssessment,p.playerEvaluation,'Raw player evaluation unchanged across sample');eq(r.target,p.target,'Raw target unchanged across sample');eq(r.calibrationAssessment,p.calibrationEvaluation,'Raw calibration evaluation');
    ok(['WIN','LOSS','DRAW','TIMEOUT'].includes(r.outcome),'Explicit valid outcome');if(r.outcome!=='TIMEOUT')eq(r.outcome,r.playerStats.some(x=>x.remainHealth>0)?'WIN':r.enemyStats.some(x=>x.remainHealth>0)?'LOSS':'DRAW','Actual HP outcome');
    const rowKey=`${key}/${r.pair}/${r.reverse}`;ok(!index.has(rowKey),'No duplicate execution');const small={pair:r.pair,reverse:r.reverse,outcome:r.outcome,rounds:r.rounds};index.set(rowKey,small);
    const c=cells.get(key)||{case:metadata(r.case),rule:r.rule,pairs:allocation.pairs,outcomes:{WIN:0,LOSS:0,DRAW:0,TIMEOUT:0},rows:[]};c.outcomes[r.outcome]++;c.rows.push(small);cells.set(key,c);rowCount++;
  }
  eq(rowCount,80000,'Full authorized execution total');eq(cells.size,37,'All authorized model conditions');
  const saved=JSON.parse(fs.readFileSync(path.join(run,'summary.json'))),summaries=new Map();
  for(const [key,c]of cells){eq(c.rows.length,2*c.pairs,'Exact execution count per cell');const pairScores=[];
    for(let pair=0;pair<c.pairs;pair++){const a=index.get(`${key}/${pair}/false`),b=index.get(`${key}/${pair}/true`);ok(a&&b,'Two fresh mirrors per pair');
      const score=r=>r.outcome==='WIN'?1:r.outcome==='DRAW'?.5:r.outcome==='LOSS'?0:null;pairScores.push(a.outcome==='TIMEOUT'||b.outcome==='TIMEOUT'?null:(score(a)+score(b))/2);}
    const completeRows=c.rows.filter(r=>r.outcome!=='TIMEOUT'),summary={case:c.case,rule:c.rule,pairs:c.pairs,outcomes:c.outcomes,pairScores,winrate:c.outcomes.WIN/(2*c.pairs),
      meanRounds:completeRows.length?completeRows.reduce((s,r)=>s+r.rounds,0)/completeRows.length:0,sideWins:{playersFirst:c.rows.filter(r=>r.reverse&&r.outcome==='WIN').length,npcFirst:c.rows.filter(r=>!r.reverse&&r.outcome==='WIN').length}};
    const old=saved.cases.find(s=>s.case.id===c.case.id&&s.rule===c.rule);ok(Boolean(old),'Saved summary exists');eq(old.pairs,c.pairs,'Saved sample size');eq(old.winrate,summary.winrate,'Independent win rate');eq(old.pairScores,pairScores,'Independent paired outcomes');eq(old.meanRounds,summary.meanRounds,'Independent mean rounds');summaries.set(key,summary);
  }
  function compute(spec){const l=summaries.get(`${spec.leftRule}/${spec.leftId}`),r=summaries.get(`${spec.rightRule}/${spec.rightId}`),ds=l.pairScores.map((x,i)=>x===null||r.pairScores[i]===null?null:x-r.pairScores[i]),complete=ds.filter(x=>x!==null),estimate=complete.length?complete.reduce((s,x)=>s+x,0)/complete.length:null;
    const incomplete=ds.length-complete.length,interval=incomplete?null:[Math.max(-1,estimate-spec.halfWidth),Math.min(1,estimate+spec.halfWidth)];let criterionStatus='REPORT_ONLY';
    if(incomplete)criterionStatus='INCOMPLETE';else if(spec.criterion==='lower > -0.20')criterionStatus=interval[0]>-.20?'PASS':interval[1]<=-.20?'NEGATIVE':'INCONCLUSIVE';
    else if(spec.criterion==='lower > 0')criterionStatus=interval[0]>0?'PASS':interval[1]<=0?'NEGATIVE':'INCONCLUSIVE';
    else if(spec.criterion==='upper < +0.20')criterionStatus=interval[1]<.20?'PASS':interval[0]>=.20?'NEGATIVE':'INCONCLUSIVE';
    return{...spec,leftCase:l.case,rightCase:r.case,leftWinrate:l.winrate,rightWinrate:r.winrate,estimate,completePairs:complete.length,incompletePairs:incomplete,simultaneousFamily95:interval,criterionStatus,
      pairedDifferenceSHA256:crypto.createHash('sha256').update(JSON.stringify(ds)).digest('hex')};}
  const primaryResults=plannedPrimary.map(compute),secondaryResults=plannedSecondary.map(compute);
  const report={purpose:'INDEPENDENT_FRESH_RAW_RECOMPUTATION_OF_ONLY_PRESPECIFIED_K11_AND_K20_FAMILIES_NO_BATTLES_NO_FILE_WRITES',policySHA256:sha(policyPath),rawSHA256:sha(rawPath),checks,rowCount,modelConditions:cells.size,
    pilotExcluded:true,coverage:'Primary K11 and secondary K20 each simultaneous95percent separately. No joint95percent claim across both families or game content.',
    primary:{K:11,alpha:.05,allProjectCriteriaMet:primaryResults.every(x=>x.criterionStatus==='PASS'),comparisons:primaryResults},
    secondary:{K:20,alpha:.05,comparisons:secondaryResults},cells:[...summaries.values()],
    limitations:policy.knownPilotCosts,implementationAuthorized:false};
  console.log(JSON.stringify(report,null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
