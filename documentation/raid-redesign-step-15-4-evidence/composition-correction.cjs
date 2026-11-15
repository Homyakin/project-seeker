const fs = require('node:fs');
const saved = JSON.parse(fs.readFileSync(`${__dirname}/assessment.json`, 'utf8'));
const original = saved.calibrated.MIN_SELF_PAIRS;
const mReference = saved.anchor.productionCalibration.multiplier;
const B = xs => xs.map(p => p.H*p.D).sort((a,b)=>a-b)
  .reduce((s,p,i,ps)=>s+(2*(ps.length-i)-1)*p,0);
function npc(M,m) {
  const F=Math.floor(M/2),S=M-F;
  return Array.from({length:M},(_,i)=>{
    const front=i<F,w=M/(2*(front?F:S));
    const health=Math.max(1,Math.ceil((front?420:280)*(m*w)));
    const attack=Math.max(1,Math.ceil((front?70:130)*(m*w)));
    const defense=Math.max(1,Math.ceil((front?160:70)*m));
    const mu=[1.1,.9,.75,1.25].reduce((s,w)=>s+(1-defense*w/(defense*w+500)),0)/4;
    return {health,attack,defense,H:health/mu,
      D:attack*(1+(front?.04:.08)*((front?1.5:1.7)-1))*((front?190:160)/1000)};
  });
}
const B3Reference=B(npc(3,mReference));
const r=M=>B(npc(M,mReference))/((M/3)**2*B3Reference);
const h=(M,a)=>a+(1-a)*(3/M)**2;
const a=(2.25-original.h2/r(2))/1.25;
const q=L=>1+((L-10)*.06+.14);
const g=M=>1+.05*Math.log(M)/Math.log(4);
function find(M,target) {
  const score=m=>B(npc(M,m));
  let lo=0,hi=1;
  if(score(0)>=target) return {m:0,achieved:score(0),npcs:npc(M,0)};
  while(score(hi)<target){lo=hi;hi*=2;if(hi>1e6)throw Error('bound');}
  for(let k=0;k<64;k++){
    const mid=lo+(hi-lo)/2;
    if(mid<=lo||mid>=hi)break;
    if(score(mid)>=target)hi=mid;else lo=mid;
  }
  const m=target-score(lo)<score(hi)-target?lo:hi;
  return {m,achieved:score(m),npcs:npc(M,m)};
}
function summarize(M,fit) {
  const F=Math.floor(M/2),front=fit.npcs.slice(0,F),back=fit.npcs.slice(F);
  const sum=(xs,k)=>xs.reduce((s,x)=>s+x[k],0);
  return {m:fit.m,achieved:fit.achieved,frontCount:F,backCount:M-F,
    guardian:front[0],slinger:back[0],frontTotalHealth:sum(front,'health'),
    frontTotalAttack:sum(front,'attack'),backTotalHealth:sum(back,'health'),backTotalAttack:sum(back,'attack')};
}
const sizeScreen=[];
for(const tier of ['MID','STRONG'])for(const L of [25,40,50])for(let N=2;N<=30;N++){
  const basis=saved.sizeScreen.find(x=>x.model==='MIN_SELF_PAIRS'&&x.tier===tier&&x.level===50&&x.N===N);
  const oldFactor=h(N,original.a),newFactor=h(N,a)*r(N);
  const oldTarget=basis.assessment*q(L)*g(N)*original.C*oldFactor;
  const newTarget=basis.assessment*q(L)*g(N)*original.C*newFactor;
  sizeScreen.push({tier,level:L,N,M:N,playersAssessment:basis.assessment,
    original:{factor:oldFactor,target:oldTarget,...summarize(N,find(N,oldTarget))},
    corrected:{factor:newFactor,target:newTarget,...summarize(N,find(N,newTarget))}});
}
const transitions=[];
for(const tier of ['MID','STRONG'])for(const L of [25,40,50])for(const [from,to] of [[2,3],[3,4],[4,5],[6,7],[8,9],[12,13]]){
  const before=sizeScreen.find(x=>x.tier===tier&&x.level===L&&x.N===from);
  const after=sizeScreen.find(x=>x.tier===tier&&x.level===L&&x.N===to);
  transitions.push({tier,level:L,from,to,original:{mBefore:before.original.m,mAfter:after.original.m,ratio:after.original.m/before.original.m},
    corrected:{mBefore:before.corrected.m,mAfter:after.corrected.m,ratio:after.corrected.m/before.corrected.m}});
}
const result={purpose:'PURE_COMPOSITION_CORRECTION_NO_BATTLES_NO_PRODUCTION_CHANGES',
  source:`${__dirname}/assessment.json`,formula:'h_corrected(M)=(a+(1-a)*(3/M)^2)*r(M)',
  rFormula:'B(npc(M,mReference))/((M/3)^2*B(npc(3,mReference)))',
  constants:{C:original.C,mReference,B3Reference,r2:r(2),oldA:original.a,a,
    oldH2:original.h2,newH2:h(2,a),correctedH2:h(2,a)*r(2),anchorDifference:h(2,a)*r(2)-original.h2},
  factors:Array.from({length:49},(_,i)=>i+2).concat([128]).map(M=>({M,r:r(M),h:h(M,a),correctedH:h(M,a)*r(M),oldH:h(M,original.a)})),
  transitions,sizeScreen,
  limitations:['Numerical diagnostic factory, not Java production search replay',
    'All player assessments are fixed from actual15.3 MAIN inputs, N2..30 prefixes',
    'No outcome inference; the formula corrects scalar composition at one fixed NPC reference point only',
    'mReference is inherited from original MID MAIN N3 L25 anchor, not a new fitted parameter']};
if(process.argv[2]==='table'){
  console.log('| Снаряжение | Уровень | Переход | B, прежнее m | Поправка состава, m | Изменение нового m |');
  console.log('| --- | ---: | --- | ---: | ---: | ---: |');
  for(const x of transitions.filter(x=>(x.from===3&&x.level===25)||([6,12].includes(x.from)&&[40,50].includes(x.level))))
    console.log(`| ${x.tier} | ${x.level} | ${x.from}→${x.to} | ${x.original.mBefore.toFixed(6)} → ${x.original.mAfter.toFixed(6)} | ${x.corrected.mBefore.toFixed(6)} → ${x.corrected.mAfter.toFixed(6)} | ${((x.corrected.ratio-1)*100).toFixed(2)}% |`);
}else console.log(JSON.stringify(result,null,2));
