const fs=require('node:fs');
const zlib=require('node:zlib');
const readline=require('node:readline');
const rawSource='/tmp/seeker-step153-screen.5Zg4N2/run/battles.jsonl.gz';
const weakSource='/tmp/seeker-step15-3-fillers.HXdOad/independent-review/summary.json';
const types=['SLASH','BLUNT','PIERCE','MAGICAL'];
const finite=(x,positive=false)=>{
  if(!Number.isFinite(x)||(positive?x<=0:x<0))throw Error('Invalid finite nonnegative/positive value');
  return x;
};
const sum=xs=>xs.slice().sort((a,b)=>a-b).reduce((a,b)=>finite(a+finite(b)),0);
function parts(p){
  const mu=sum(types.map(t=>finite(p.damageTakenMultipliers[t],true)))/4;
  const H=finite(finite(p.health)/finite(mu,true));
  const A=Math.max(0,...p.attacksByRange.map(x=>sum(types.map(t=>finite(x.attack[t]||0)))));
  const c=1+Math.min(1,Math.max(0,p.critChance/100))*(finite(p.critMultiplier)-1);
  const f=Math.min(finite(p.initiative),1000)/1000;
  const D=A===0||c===0||f===0?0:finite(A*finite(c)*f);
  return {H,D,mu};
}
function totals(xs){if(!Array.isArray(xs)||!xs.length)throw Error('Empty prepared team');return {H:sum(xs.map(x=>finite(x.H))),D:sum(xs.map(x=>finite(x.D)))};}
const q=L=>1+((L-10)*.06+.14);
const profiles={
  G:{baseHP:420,baseATK:70,defense:707,criticalFactor:1.02,frequency:.19},
  S:{baseHP:280,baseATK:130,defense:309,criticalFactor:1.056,frequency:.16}
};
for(const x of Object.values(profiles)){
  x.mu=sum([1.1,.9,.75,1.25].map(w=>1-x.defense*w/(x.defense*w+500)))/4;
  x.kappa=x.criticalFactor*x.frequency;
}
const limit=Math.floor(2147483647/16);
const multiply=(b,m)=>{const x=Math.ceil(b*finite(m));if(!Number.isFinite(x)||x>limit)throw Error('NPC integer limit');return Math.max(1,x);};
function factory(M,mH,mD){
  const F=Math.floor(M/2);
  return Array.from({length:M},(_,i)=>{
    const p=profiles[i<F?'G':'S'],hp=multiply(p.baseHP,mH),attack=multiply(p.baseATK,mD);
    return {kind:i<F?'G':'S',hp,attack,defense:p.defense,H:hp/p.mu,D:attack*p.kappa};
  });
}
let anchorBasis;
function calibrate(players,L,model,constants){
  const T=totals(players),M=Math.max(2,players.length),s=Math.sqrt(finite(q(L),true));
  const levelRatio=finite(s/anchorBasis.s25,true);
  const targetH=finite((anchorBasis.E.H*levelRatio)*(model==='SAME'?T.H/anchorBasis.P.H:T.D/anchorBasis.P.D));
  const targetD=finite((anchorBasis.E.D*levelRatio)*(model==='SAME'?T.D/anchorBasis.P.D:T.H/anchorBasis.P.H));
  const F=Math.floor(M/2),bases=Array.from({length:M},(_,i)=>profiles[i<F?'G':'S']);
  const denomH=finite(sum(bases.map(x=>x.baseHP/x.mu)),true),denomD=finite(sum(bases.map(x=>x.baseATK*x.kappa)),true);
  const mH=finite(targetH/denomH),mD=finite(targetD/denomD),npcs=factory(M,mH,mD),achieved=totals(npcs);
  const boundH=sum(bases.map(x=>1/x.mu)),boundD=sum(bases.map(x=>x.kappa));
  const diagnostic=(target,actual,m,bound)=>({m,achieved:actual,status:target===0?'MINIMUM_EXCEEDS_TARGET':'CLOSED_FORM_CEIL',
    overage:actual-target,mathematicalPositiveTargetOverageBound:bound,
    floatingComparisonTolerance:Math.max(1,target,actual)*1e-12});
  const Hfit=diagnostic(targetH,achieved.H,mH,boundH),Dfit=diagnostic(targetD,achieved.D,mD,boundD);
  const front=totals(npcs.slice(0,F)),back=totals(npcs.slice(F));
  return {model,N:players.length,M,L,players:T,targetH,targetD,Hfit,Dfit,
    denominators:{H:denomH,D:denomD},achieved,front,back,npcs};
}
function permutations(xs){if(xs.length<2)return [xs];return xs.flatMap((x,i)=>permutations(xs.filter((_,j)=>j!==i)).map(rest=>[x,...rest]));}
(async()=>{
  const selected=new Map(),wanted=new Set([75,79,127,139,143,191]);
  const input=fs.createReadStream(rawSource).pipe(zlib.createGunzip());
  const lines=readline.createInterface({input,crlfDelay:Infinity});
  for await(const line of lines){
    const r=JSON.parse(line);if(!wanted.has(r.caseId))continue;
    const actors=r.initState.personagesById;
    selected.set(r.caseId,{players:r.playerBasis.map(x=>parts(actors[x.id])),enemies:r.enemyBasis.map(x=>parts(actors[x.id]))});
    wanted.delete(r.caseId);if(!wanted.size){lines.close();input.destroy();break;}
  }
  if(wanted.size)throw Error('Missing archived inputs');
  const anchor=selected.get(75),P=totals(anchor.players),E=totals(anchor.enemies),s25=Math.sqrt(q(25));
  anchorBasis={P,E,s25};
  const constants={SAME:{kH:E.H/(s25*P.H),kD:E.D/(s25*P.D)},CROSS:{kH:E.H/(s25*P.D),kD:E.D/(s25*P.H)}};
  const weak=JSON.parse(fs.readFileSync(weakSource,'utf8')).results;
  const comparisons=[];
  for(const model of ['SAME','CROSS']){
    for(const x of weak){
      const before=calibrate([x.strong],x.level,model,constants),after=calibrate([x.strong,x.weak],x.level,model,constants);
      comparisons.push({model,case:`${x.style}_SOLO_WEAK`,before,after,targetHRatio:after.targetH/before.targetH,targetDRatio:after.targetD/before.targetD});
    }
    for(const [tier,source]of [['MID',127],['STRONG',191]])for(const L of [25,40,50])for(const N of [3,6,12,29]){
      const before=calibrate(selected.get(source).players.slice(0,N),L,model,constants);
      const after=calibrate(selected.get(source).players.slice(0,N+1),L,model,constants);
      comparisons.push({model,case:`MAIN_${tier}_${N}_NORMAL_L${L}`,before,after,targetHRatio:after.targetH/before.targetH,targetDRatio:after.targetD/before.targetD});
    }
  }
  const tests=[];
  const check=(name,ok)=>{if(!ok)throw Error(`Test failed: ${name}`);tests.push({name,pass:true});};
  for(const model of ['SAME','CROSS']){
    const a=calibrate(anchor.players,25,model,constants);
    check(`${model}:anchor exact target H`,Object.is(a.targetH,E.H));
    check(`${model}:anchor exact target D`,Object.is(a.targetD,E.D));
    for(const order of permutations(anchor.players)){
      const t=totals(order);check(`${model}:participant order`,Object.is(t.H,P.H)&&Object.is(t.D,P.D));
    }
    let lastH=-1,lastD=-1;
    for(const m of [0,.001,.01,.1,1,2,4,16,128]){
      const tH=totals(factory(7,m,1)),tD=totals(factory(7,1,m));
      check(`${model}:NPC axis monotonic`,tH.H>=lastH&&tD.D>=lastD);lastH=tH.H;lastD=tD.D;
      check(`${model}:HP does not amplify D`,Object.is(tH.D,totals(factory(7,1,1)).D));
      check(`${model}:ATK does not amplify H`,Object.is(tD.H,totals(factory(7,1,1)).H));
    }
    const zeroD=calibrate([{H:1000,D:0}],25,model,constants);
    check(`${model}:zero input axis yields minimum flag`,model==='SAME'?zeroD.Dfit.status==='MINIMUM_EXCEEDS_TARGET':zeroD.Hfit.status==='MINIMUM_EXCEEDS_TARGET');
  }
  for(const ds of permutations([.11,.37,.77,1.13])){
    const p={health:731,initiative:1300,critChance:100,critMultiplier:1.8,damageTakenMultipliers:Object.fromEntries(types.map((t,i)=>[t,ds[i]])),attacksByRange:[{range:1,attack:{SLASH:17,BLUNT:31,PIERCE:7,MAGICAL:13}},{range:3,attack:{SLASH:1}}]};
    const ref=parts({...p,damageTakenMultipliers:Object.fromEntries(types.map((t,i)=>[t,[.11,.37,.77,1.13][i]]))});
    check('type-label permutation sorted mu',Object.is(parts(p).H,ref.H));
    check('range-profile order',Object.is(parts({...p,attacksByRange:p.attacksByRange.slice().reverse()}).D,ref.D));
    for(const attacks of permutations([17,31,7,13]))check('attack-type permutation same range',Object.is(parts({...p,attacksByRange:[{range:1,attack:Object.fromEntries(types.map((t,i)=>[t,attacks[i]]))}]}).D,ref.D));
    check('speed capped1000',Object.is(parts({...p,initiative:1000}).D,ref.D));
  }
  check('negative rejection',(()=>{try{totals([{H:-1,D:1}]);return false;}catch{return true;}})());
  check('nonfinite rejection',(()=>{try{totals([{H:Infinity,D:1}]);return false;}catch{return true;}})());
  check('empty rejection',(()=>{try{totals([]);return false;}catch{return true;}})());
  check('zero multiplier minimum one',factory(2,0,0).every(x=>x.hp===1&&x.attack===1));
  check('large characteristic rejected',(()=>{try{factory(2,1e9,1);return false;}catch{return true;}})());
  const critToy={health:100,initiative:1000,critChance:0,critMultiplier:1.8,damageTakenMultipliers:Object.fromEntries(types.map(t=>[t,1])),attacksByRange:[{range:1,attack:{SLASH:100}},{range:3,attack:{MAGICAL:80}}]};
  check('crit0',parts(critToy).D===100);
  check('crit100',parts({...critToy,critChance:100}).D===180);
  check('crit clamp low',parts({...critToy,critChance:-100}).D===100);
  check('crit clamp high',parts({...critToy,critChance:200}).D===180);
  check('do not combine different ranges',parts(critToy).D===100);
  check('zero attack',parts({...critToy,attacksByRange:[]}).D===0);
  check('zero speed',parts({...critToy,initiative:0}).D===0);
  for(const c of comparisons)for(const v of [c.before,c.after])for(const axis of ['H','D']){
    const fit=v[`${axis}fit`],eps=fit.floatingComparisonTolerance;
    check(`${v.model}:positive-target overage bound`,fit.overage>=-eps&&fit.overage<fit.mathematicalPositiveTargetOverageBound+eps);
  }
  for(const model of ['SAME','CROSS']){
    const base=calibrate(anchor.players,25,model,constants),extended=calibrate(anchor.players.concat([{H:0,D:0}]),25,model,constants);
    check(`${model}:zero body no target increase`,Object.is(base.targetH,extended.targetH)&&Object.is(base.targetD,extended.targetD));
    const h2=calibrate(anchor.players.map(x=>({...x,H:2*x.H})),25,model,constants),d2=calibrate(anchor.players.map(x=>({...x,D:2*x.D})),25,model,constants);
    check(`${model}:homogeneity H`,model==='SAME'?Object.is(h2.targetH,2*base.targetH)&&Object.is(h2.targetD,base.targetD):Object.is(h2.targetD,2*base.targetD)&&Object.is(h2.targetH,base.targetH));
    check(`${model}:homogeneity D`,model==='SAME'?Object.is(d2.targetD,2*base.targetD)&&Object.is(d2.targetH,base.targetH):Object.is(d2.targetH,2*base.targetH)&&Object.is(d2.targetD,base.targetD));
  }
  for(const c of comparisons)for(const v of [c.before,c.after]){
    const expected=((E.H*E.D)*(q(v.L)/q(25)))*((v.players.H/P.H)*(v.players.D/P.D));
    const actual=v.targetH*v.targetD;
    check(`${v.model}:same scalar product within floating tolerance`,Math.abs(actual-expected)<=Math.max(1,Math.abs(expected))*1e-12);
  }
  const anchorCandidates=['SAME','CROSS'].map(model=>calibrate(anchor.players,25,model,constants));
  console.log(JSON.stringify({purpose:'READ_ONLY_PURE_TWO_AXIS_DESIGN_NO_BATTLES_NO_JAVA',sources:{rawSource,weakSource},
    numericalPolicy:'sorted finite mu, attacks and participant contributions; anchor-normalized ratio targets; independent closed-form HP/ATK factors; ceil/minimum1 with bounded overage; Java factory confirmation still required',
    profiles,anchor:{level:25,N:3,players:P,enemies:E,s25},constants,
    productConstant:constants.SAME.kH*constants.SAME.kD,
    anchorCandidates,comparisons,tests:{count:tests.length,allPass:true,checks:tests},
    limitations:['No wins estimated; fixed DEF and no M/2 weights intentionally change original NPC traits',
      'Weak-input H/D loaded from archived enum-order decomposition may differ by ulps from new sorted-mu extraction',
      'Helper factory arithmetic is not an actual Java/NPC construction test',
      'One anchor is used, no g,h,a,r or solo-preserving additional factors']},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
