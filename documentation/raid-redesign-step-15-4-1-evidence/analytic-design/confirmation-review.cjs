const fs=require('node:fs'),crypto=require('node:crypto');
const policyPath='/home/user/projects/project-seeker/documentation/raid-redesign-step-15-4-1-evidence/confirmation-policy.json';
const policyText=fs.readFileSync(policyPath,'utf8'),policy=JSON.parse(policyText);
const policySha=crypto.createHash('sha256').update(policyText).digest('hex');
if(policySha!=='e2bd20c81664fa504bb88c7deb78e5e2d61c56fa51e2559d9013adc12bdaeac9')throw Error('Policy differs from independently read pre-confirmation version');
const root=process.argv[2];
if(!root)throw Error('Expected completed confirmation screen directory');
const summary=JSON.parse(fs.readFileSync(`${root}/summary.json`,'utf8'));
const manifest=JSON.parse(fs.readFileSync(`${root}/manifest.json`,'utf8'));
const validation=JSON.parse(fs.readFileSync(`${root}/validation.json`,'utf8'));
const independentPath='/tmp/seeker-split-confirm-independent.UTPQFj/validation.json';
const independent=JSON.parse(fs.readFileSync(independentPath,'utf8'));
const mean=xs=>xs.reduce((a,b)=>a+b,0)/xs.length;
const close=(a,b)=>Math.abs(a-b)<=1e-9;
if(validation.status!=='PASS')throw Error('Raw validation not PASS');
if(independent.status!=='PASS'||independent.rows!==24000||independent.rawGzipSha256!==validation.rawGzipSha256||independent.policySha256!==policySha)throw Error('Independent raw validation mismatch');
if(manifest.generationNamespace!==policy.generationNamespace||manifest.battleNamespace!==policy.battleNamespace)throw Error('Seed namespace mismatch');
if(summary.cases.length!==24)throw Error('Expected24model conditions');
const byKey=new Map(summary.cases.map(x=>[`${x.model}/${x.case.id}`,x]));
if(byKey.size!==24)throw Error('Duplicate model/condition');
let independentScoreChecks=0;
for(const x of independent.states){
  const original=byKey.get(`${x.model}/${x.id}`);
  if(!original||x.rows!==1000||x.pairScores.length!==500||!x.pairScores.every((v,i)=>Object.is(v,original.pairScores[i]))||!close(x.winrate,original.winrate)||!close(x.meanRounds,original.meanRounds))throw Error('Independent raw-state summary mismatch');
  independentScoreChecks+=x.pairScores.length;
}
if(independentScoreChecks!==12000)throw Error('Independent pair-state coverage');
const cases=summary.cases.map(x=>{
  if(!policy.models.includes(x.model)||x.case.id<1||x.case.id>12||x.pairs!==500||x.pairScores.length!==500||!x.pairScores.every(v=>[0,.25,.5,.75,1].includes(v)))throw Error('Fixed case/pair policy');
  const expectedGroup=x.case.id<=8?1:x.case.id<=10?2:3;
  if(x.case.groupId!==expectedGroup)throw Error('Unexpected group allocation');
  const expectedN=x.case.id<=10?(x.case.id%2===1?3:4):(x.case.id===11?1:2);
  if(x.case.members.length!==expectedN||x.case.originalSize!==(x.case.id<=10?3:1)||x.case.level!==25||x.case.tier!==(x.case.id<=10?'MID':'STRONG'))throw Error('Wrong size/tier/level case');
  if(Object.keys(x.outcomes).some(k=>!['WIN','LOSS','DRAW'].includes(k)))throw Error('Unhandled unresolved outcome');
  const wins=x.outcomes.WIN||0,losses=x.outcomes.LOSS||0,draws=x.outcomes.DRAW||0;
  if(wins+losses+draws!==1000||!close(mean(x.pairScores),(wins+.5*draws)/1000)||!close(mean(x.pairScores),x.winrate))throw Error('Outcomes/pair summary mismatch');
  return {model:x.model,id:x.case.id,frozenLabel:policy.caseOrder[x.case.id-1],group:x.case.groupId,style:x.case.style,tier:x.case.tier,
    variant:x.case.variant,N:x.case.members.length,L:x.case.level,wins,losses,draws,winratePP:100*x.winrate,meanRounds:x.meanRounds};
});
const families={};
for(const [name,f]of Object.entries(policy.families)){
  if(f.orientation!=='second minus first mirrored-pair score'||f.comparisonsIncludingBothModels!==12)throw Error('Family policy unexpectedly changed');
  const half=100*Math.sqrt(2*Math.log(2*f.comparisonsIncludingBothModels/f.alpha)/500);
  const comparisons=[];
  for(const model of policy.models)for(const [from,to]of f.comparisonsPerModel){
    const base=byKey.get(`${model}/${from}`),after=byKey.get(`${model}/${to}`);
    const deltas=after.pairScores.map((p,i)=>p-base.pairScores[i]);
    const delta=100*mean(deltas),ci=[Math.max(-100,delta-half),Math.min(100,delta+half)];
    comparisons.push({model,from,to,fromLabel:policy.caseOrder[from-1],toLabel:policy.caseOrder[to-1],
      basePP:100*base.winrate,afterPP:100*after.winrate,deltaPP:delta,family95PP:ci,
      negativeConfirmed:ci[1]<0,positiveConfirmed:ci[0]>0,
      ordinaryAppendHarmOver20Confirmed:name==='append'&&to!==12&&ci[1]<-20,
      weakAppend:name==='append'&&to===12});
  }
  if(comparisons.length!==12)throw Error('Family size');
  families[name]={comparisons:comparisons,halfWidthPP:half,coverage:'95percent within this separately fixed12comparison family, not joint over both families'};
}
const producerAnalysis=JSON.parse(fs.readFileSync(`${root}/analysis.json`,'utf8'));
let comparisonChecks=0;
for(const [name,f]of Object.entries(families))for(const c of f.comparisons){
  const other=producerAnalysis.families[name].comparisons.find(x=>x.model===c.model&&x.firstCaseId===c.from&&x.secondCaseId===c.to);
  const independentOther=independent.families[name].results.find(x=>x.model===c.model&&x.first===c.from&&x.second===c.to);
  if(!other||!independentOther||!close(other.deltaPP,c.deltaPP)||!close(100*independentOther.pairedMeanDifference,c.deltaPP)||
    !c.family95PP.every((v,i)=>close(v,other.simultaneous95FamilyHoeffdingDeltaPP[i])&&close(v,100*independentOther.simultaneousInterval[i])))throw Error('Independent family interval mismatch');
  comparisonChecks++;
}
console.log(JSON.stringify({purpose:'INDEPENDENT_FIXED_POLICY_CONFIRMATION_SUMMARY_RECOMPUTATION_NO_NEW_BATTLES',
  root,policyPath,policySha256:policySha,counts:{conditions:12,models:2,pairsPerModelCondition:500,battles:24000},
  cases,families,rawValidationReport:validation,
  independentRawAgreement:{path:independentPath,status:independent.status,rows:independent.rows,
    pairScoresComparedExactly:independentScoreChecks,causalIntervalsCompared:comparisonChecks,rawGzipSha256:independent.rawGzipSha256,
    assertionCount:independent.assertions,methods:independent.independentMethods,limitations:independent.limitations},
  limitations:['Pair scores and outcome aggregates independently recomputed; full raw structural validation is separately owned',
    'No pool with exploration and no posthoc comparisons or confidence families','Identical SAME/CROSS baseline countercase executions are not independent replications',
    'A weak negative effect is not automatically permitted by an ordinary20pp threshold','No production acceptance and no universal claim over untested teams']},null,2));
