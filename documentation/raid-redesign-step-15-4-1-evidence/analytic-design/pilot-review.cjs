const fs=require('node:fs'),zlib=require('node:zlib'),readline=require('node:readline');
const root='/tmp/seeker-step15-4-separate-scaling.cZmkPh/screen';
const summary=JSON.parse(fs.readFileSync(`${root}/summary.json`,'utf8'));
const analysis=JSON.parse(fs.readFileSync(`${root}/analysis.json`,'utf8'));
const manifest=JSON.parse(fs.readFileSync(`${root}/manifest.json`,'utf8'));
const js=JSON.parse(fs.readFileSync(`${__dirname}/assessment.json`,'utf8'));
const types=['SLASH','BLUNT','PIERCE','MAGICAL'];
const sum=xs=>xs.slice().sort((a,b)=>a-b).reduce((a,b)=>a+b,0);
const mean=xs=>sum(xs)/xs.length;
const close=(a,b)=>Math.abs(a-b)<=Math.max(1,Math.abs(a),Math.abs(b))*1e-12;
const parts=p=>{
  const mu=sum(types.map(t=>p.damageTakenMultipliers[t]))/4;
  const attack=Math.max(0,...p.attacksByRange.map(x=>sum(types.map(t=>x.attack[t]||0))));
  return {H:p.health/mu,D:attack*(1+Math.min(1,Math.max(0,p.critChance/100))*(p.critMultiplier-1))*(Math.min(p.initiative,1000)/1000)};
};
const byKey=new Map(summary.cases.map(x=>[`${x.model}/${x.case.id}`,x]));
const cases=summary.cases.map(x=>{
  if(x.pairs!==30||x.pairScores.length!==30)throw Error('pairs');
  const win=x.outcomes.WIN||0,draw=x.outcomes.DRAW||0,loss=x.outcomes.LOSS||0;
  if(win+draw+loss!==60||!close(mean(x.pairScores),(win+.5*draw)/60)||!close(x.winrate,mean(x.pairScores)))throw Error('scores');
  return {id:x.case.id,group:x.case.groupId,model:x.model,style:x.case.style,tier:x.case.tier,N:x.case.members.length,
    originalSize:x.case.originalSize,L:x.case.level,variant:x.case.variant,wins:win,draws:draw,losses:loss,
    winratePP:100*x.winrate,meanRounds:x.meanRounds};
});
const comparisons=analysis.causal.map(x=>{
  const base=byKey.get(`${x.model}/${x.baseCaseId}`),after=byKey.get(`${x.model}/${x.caseId}`);
  const deltas=after.pairScores.map((p,i)=>p-base.pairScores[i]),delta=100*mean(deltas);
  if(!close(delta,x.deltaPP))throw Error('delta');
  return {model:x.model,style:x.style,tier:x.tier,originalSize:x.originalSize,L:x.level,variant:x.variant,
    basePP:100*base.winrate,afterPP:100*after.winrate,deltaPP:delta};
});
const counterCases=cases.filter(x=>x.style==='COUNTER');
const counterContrasts=[];
for(const model of ['SAME','CROSS'])for(const [title,a,b]of [
  ['PIERCE minus SLASH, ARCANE',38,40],['PIERCE minus SLASH, LEATHER',39,41],
  ['ARCANE minus LEATHER, PIERCE',38,39],['ARCANE minus LEATHER, SLASH',40,41]]){
  const left=byKey.get(`${model}/${a}`),right=byKey.get(`${model}/${b}`);
  counterContrasts.push({model,title,deltaPP:100*mean(left.pairScores.map((p,i)=>p-right.pairScores[i]))});
}
(async()=>{
  const numerical=[],input=fs.createReadStream(`${root}/battles.jsonl.gz`).pipe(zlib.createGunzip());
  const lines=readline.createInterface({input,crlfDelay:Infinity});
  for await(const line of lines){
    if(!line.includes('"pair":0,'))continue;
    const r=JSON.parse(line);if(r.reverse)continue;
    const actors=r.initState.personagesById;
    const ps=r.playerParts.map(x=>parts(actors[x.id])),H=sum(ps.map(x=>x.H)),D=sum(ps.map(x=>x.D));
    if(!close(H,r.playerMetrics.H)||!close(D,r.playerMetrics.D))throw Error('neutral inputs');
    const scale=Math.sqrt(1+((r.case.level-10)*.06+.14))/js.anchor.s25;
    const targetH=(js.anchor.enemies.H*scale)*(r.model==='SAME'?H/js.anchor.players.H:D/js.anchor.players.D);
    const targetD=(js.anchor.enemies.D*scale)*(r.model==='SAME'?D/js.anchor.players.D:H/js.anchor.players.H);
    const M=r.enemyParts.length,F=Math.floor(M/2);
    const psNPC=Array.from({length:M},(_,i)=>js.profiles[i<F?'G':'S']);
    const naturalH=sum(psNPC.map(x=>x.baseHP/x.mu)),naturalD=sum(psNPC.map(x=>x.baseATK*x.kappa));
    const mH=targetH/naturalH,mD=targetD/naturalD;
    const v=r.calibration;
    for(const [a,b]of [[targetH,v.targetH],[targetD,v.targetD],[mH,v.hpMultiplier],[mD,v.attackMultiplier]])if(!close(a,b))throw Error('actual/JS numeric target');
    numerical.push({id:r.case.id,model:r.model,M,targetH,targetD,mH,mD,
      targetHExact:Object.is(targetH,v.targetH),targetDExact:Object.is(targetD,v.targetD),
      mHExact:Object.is(mH,v.hpMultiplier),mDExact:Object.is(mD,v.attackMultiplier),
      targetHRelativeError:Math.abs(targetH-v.targetH)/Math.max(1,Math.abs(targetH)),
      targetDRelativeError:Math.abs(targetD-v.targetD)/Math.max(1,Math.abs(targetD))});
    if(numerical.length===100){lines.close();input.destroy();break;}
  }
  if(numerical.length!==100)throw Error('missing actual cases');
  console.log(JSON.stringify({purpose:'INDEPENDENT_30PAIR_PILOT_RECOMPUTATION_NOT_ACCEPTANCE',root,
    counts:{conditions:50,models:2,pairsPerModelCondition:30,battles:6000},
    cases,comparisons,counterCases,counterContrasts,
    numericActualVsJS:{samplePolicy:'pair0/playersFirst per all100model-condition combinations; full6000raw validated by another reviewer',
      count:numerical.length,allTargetsExact:numerical.every(x=>x.targetHExact&&x.targetDExact),allMultipliersExact:numerical.every(x=>x.mHExact&&x.mDExact),
      maximumTargetRelativeError:Math.max(...numerical.flatMap(x=>[x.targetHRelativeError,x.targetDRelativeError])),rows:numerical},
    validationReport:JSON.parse(fs.readFileSync(`${root}/validation.json`,'utf8')),
    anchorConstantsExact:Object.keys(js.constants).every(m=>Object.is(js.constants[m].kH,manifest.anchors.kConceptual[`${m}_H`])&&Object.is(js.constants[m].kD,manifest.anchors.kConceptual[`${m}_D`])),
    limitations:['30pairs, no formal simultaneous20pp decision','Counter contrasts descriptive; not a jointly covered additional family',
      'SAME/CROSS identical countercase fights are correlated copies, not twice independent evidence','No outcome fitting or new battles']},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
