const fs=require('node:fs');
const roots={B:'/tmp/seeker-step15-4-b-confirm.9rPIEP/screen',B_NORM:'/tmp/seeker-step15-4-composition-confirm.O9o40T/screen'};
const half=100*Math.sqrt(2*Math.log(2*9/.05)/500);
const mean=xs=>xs.reduce((a,b)=>a+b,0)/xs.length;
const close=(a,b)=>Math.abs(a-b)<1e-9;
const studies={};
for(const [name,root]of Object.entries(roots)){
  const summary=JSON.parse(fs.readFileSync(`${root}/summary.json`,'utf8'));
  const analysis=JSON.parse(fs.readFileSync(`${root}/analysis.json`,'utf8'));
  const byId=new Map(summary.cases.map(x=>[x.case.id,x]));
  const cases=summary.cases.map(x=>{
    if(x.pairs!==500||x.pairScores.length!==500||!x.pairScores.every(p=>[0,.5,1].includes(p)))throw Error('pairs');
    const wins=x.outcomes.WIN||0,losses=x.outcomes.LOSS||0,draws=x.outcomes.DRAW||0;
    if(wins+losses+draws!==1000||!close(mean(x.pairScores),(wins+draws*.5)/1000)||!close(mean(x.pairScores),x.winrate))throw Error('summary scores');
    return {id:x.case.id,group:x.case.groupId,style:x.case.style,tier:x.case.tier,originalSize:x.case.originalSize,
      N:x.case.members.length,level:x.case.level,variant:x.case.variant,effects:x.case.effects||false,
      diagnosticFixedNpc:analysis.cases.find(c=>c.id===x.case.id)?.diagnosticFixedNpc||false,
      wins,losses,draws,winratePP:100*x.winrate,meanRounds:x.meanRounds};
  });
  const comparisons=analysis.comparisons.map(x=>{
    const a=byId.get(x.baseCaseId),b=byId.get(x.id);
    const deltas=b.pairScores.map((p,i)=>p-a.pairScores[i]);
    const delta=100*mean(deltas),ci=[Math.max(-100,delta-half),Math.min(100,delta+half)];
    if(!close(delta,x.deltaPP)||!deltas.every((d,i)=>close(d,x.pairedDeltas[i]))||!ci.every((d,i)=>close(d,x.simultaneous9Family95HoeffdingPP[i])))throw Error('comparison');
    return {id:x.id,baseCaseId:x.baseCaseId,style:x.style,tier:x.tier,originalSize:x.originalSize,level:x.level,
      variant:x.variant,diagnosticFixedNpc:x.diagnosticFixedNpc||false,basePP:100*a.winrate,appendPP:100*b.winrate,
      deltaPP:delta,family95PP:ci,ordinaryHarmOver20Confirmed:!x.diagnosticFixedNpc&&x.variant==='NORMAL_APPEND'&&ci[1]<-20,
      negativeConfirmed:ci[1]<0,positiveConfirmed:ci[0]>0};
  });
  if(comparisons.length!==9)throw Error('family count');
  const validationPath=`${root}/validation.json`;
  studies[name]={root,conditions:cases.length,pairsPerCondition:500,fights:cases.length*1000,
    cases,comparisons,validation:fs.existsSync(validationPath)?JSON.parse(fs.readFileSync(validationPath,'utf8')):null};
}
console.log(JSON.stringify({purpose:'INDEPENDENT_SUMMARY_AND_PAIRED_FAMILY_INTERVAL_RECOMPUTATION_NO_BATTLES',
  halfWidth9FamilyPP:half,studies,
  limitations:['Uses saved pairScores; separate raw validation is performed by another reviewer',
    'Each fixed study has its own9-comparison95percent family; not a joint95percent guarantee over both studies',
    'Actual and fixedNPC groups have different group seeds, so effects must not be subtracted as a paired difference',
    'All counts are independent mirrored pairs, not twice as many independent battles']},null,2));
