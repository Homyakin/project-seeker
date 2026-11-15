const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),readline=require('node:readline'),assert=require('node:assert/strict');
const root=__dirname,repo='/home/user/projects/project-seeker';
const sources={confirmedWeak:'/tmp/seeker-step15-3-fillers.HXdOad/independent-review/summary.json',preparedRaw:'/tmp/seeker-step153-screen.5Zg4N2/run/battles.jsonl.gz'};
const types=['SLASH','BLUNT','PIERCE','MAGICAL'];
const sum=xs=>[...xs].sort((a,b)=>a-b).reduce((a,b)=>a+b,0);
const Q=xs=>sum(xs.map(x=>x.H))*sum(xs.map(x=>x.D));
const B=xs=>[...xs].map(x=>x.H*x.D).sort((a,b)=>a-b).reduce((v,p,i)=>v+(2*(xs.length-i)-1)*p,0);
const T=xs=>Math.max(0,...xs.map(x=>x.H*x.D).sort((a,b)=>b-a).map((p,i)=>(i+1)*(i+1)*p));
const metrics={CURRENT_Q:Q,PLAYER_BLEND:xs=>(Q(xs)+Math.min(Q(xs),B(xs)))/2,PLAYER_B:xs=>Math.min(Q(xs),B(xs)),
  PLAYER_BEST_UNIFORM_BLEND:xs=>(Q(xs)+Math.min(Q(xs),T(xs)))/2};
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
function parts(p){const mu=types.reduce((s,t)=>s+p.damageTakenMultipliers[t],0)/4,H=p.health/mu;
  const maxAttack=Math.max(0,...p.attacksByRange.map(a=>sum(types.map(t=>a.attack[t]||0))));
  const criticalFactor=1+Math.min(1,Math.max(0,p.critChance/100))*(p.critMultiplier-1),D=maxAttack*criticalFactor*(Math.min(1000,p.initiative)/1000);
  return {H,D,p:H*D,hp:p.health,maxAttack,criticalFactor,speed:p.initiative,skills:p.skills};}
const coefficient=(L,M)=>(1+((L-10)*.06+.14))*(1+.05*Math.log(M)/Math.log(4))*.5900075417594784*(.7+.3*(3/M)**2);
function describe(xs){const q=Q(xs),b=B(xs),t=T(xs);return {N:xs.length,Q:q,B:b,T:t,BtoQ:q?b/q:1,TtoQ:q?t/q:1,blendToQ:q?(q+b)/(2*q):1,bestUniformBlendToQ:q?(q+t)/(2*q):1,parts:xs};}
function pair(label,base,added,level){return {label,level,base:describe(base),append:describe(added),models:Object.entries(metrics).map(([model,f])=>{
  const m0=Math.max(2,base.length),m1=Math.max(2,added.length),a=f(base),b=f(added),ta=a*coefficient(level,m0),tb=b*coefficient(level,m1);
  return {model,assessmentRatio:b/a,targetRatio:tb/ta,baseTarget:ta,appendTarget:tb,
    baseToProduction:f(base)/Q(base),appendToProduction:f(added)/Q(added)};})};}
async function main(){
  const sourcePaths=['game/src/main/java/ru/homyakin/seeker/game/event/pve/PveTeamAssessment.java','game/src/main/java/ru/homyakin/seeker/game/event/pve/PveCharacteristicScaling.java','game/src/main/java/ru/homyakin/seeker/game/event/raid/generator/MyconidColonyGenerator.java'];
  const beforeSources=sourcePaths.map(p=>({path:p,sha256:sha(path.join(repo,p))}));
  const inputSHA256=Object.fromEntries(Object.entries(sources).map(([key,p])=>[key,sha(p)]));
  const weak=JSON.parse(fs.readFileSync(sources.confirmedWeak)).results;
  const wanted=new Set([75,79,127,139,143,191]),selected=new Map();
  const input=fs.createReadStream(sources.preparedRaw).pipe(zlib.createGunzip()),lines=readline.createInterface({input,crlfDelay:Infinity});
  for await(const line of lines){const r=JSON.parse(line);if(!wanted.has(r.caseId))continue;const init=r.initState.personagesById;
    selected.set(r.caseId,{cell:r.cell,players:r.playerBasis.map(p=>parts(init[p.id])),calibration:r.calibration});wanted.delete(r.caseId);
    if(!wanted.size){lines.close();input.destroy();break;}}
  assert.equal(wanted.size,0);
  const weakCases=weak.map(c=>pair(`${c.style}_STRONG_SOLO_PLUS_WEAK`,[c.strong],[c.strong,c.weak],c.level));
  const ordinaryCases=[['MID',75,79],['STRONG',139,143]].map(([tier,a,b])=>pair(`MAIN_${tier}_N3_TO_N4`,selected.get(a).players,selected.get(b).players,25));
  const fillers=[];
  for(const c of weak)for(const k of [0,1,2,3,6,14,29]){
    const xs=[c.strong,...Array.from({length:k},()=>c.weak)],n=Math.max(2,xs.length);
    fillers.push({style:c.style,k,N:xs.length,...describe(xs),models:Object.entries(metrics).map(([model,f])=>({model,relativeToStrong:f(xs)/(c.strong.H*c.strong.D),target:f(xs)*coefficient(c.level,n)}))});
  }
  const ordinarySizes=[];
  for(const [tier,id]of [['MID',127],['STRONG',191]])for(const n of [3,4,7,8,15,30]){
    const ps=selected.get(id).players,team=Array.from({length:n},(_,i)=>ps[i]??ps[1+(i-1)%3]);
    ordinarySizes.push({tier,N:n,...describe(team)});
  }
  let checks=0;
  const almost=(a,b)=>{assert.ok(Math.abs(a-b)<=1e-10*Math.max(1,Math.abs(a),Math.abs(b)));checks++;};
  for(let i=0;i<1000;i++){
    const n=1+i%10,xs=Array.from({length:n},(_,j)=>({H:1+(i*13+j*19)%700,D:((i*23+j*11)%200)/10}));
    const q=Q(xs),b=B(xs),t=T(xs);assert.ok(t>=0&&t<=b+1e-10*Math.max(1,b)&&b<=q+1e-10*Math.max(1,q));checks++;
    for(const f of Object.values(metrics)){
      almost(f(xs),f([...xs].reverse()));
      for(const key of ['H','D']){const bigger=xs.map((x,j)=>j===i%n?{...x,[key]:x[key]+1}:x);assert.ok(f(bigger)+1e-9>=f(xs));checks++;}
      const extra=[...xs,{H:3,D:4}];assert.ok(f(extra)+1e-9>=f(xs));checks++;
    }
  }
  for(const n of [1,2,3,7,30])for(const f of Object.values(metrics))almost(f(Array.from({length:n},()=>({H:17,D:3}))),n*n*17*3);
  const roleControl={equalSelfDifferentRoles:describe([{H:100,D:1},{H:1,D:100}]),zeroDpsTank:pair('PURE_TANK_APPEND',[{H:10,D:10}],[{H:10,D:10},{H:100,D:0}],25)};
  for(const p of beforeSources)assert.equal(sha(path.join(repo,p.path)),p.sha256,'Production source unchanged');
  const report={purpose:'PURE_PLAYER_TARGET_ALGEBRA_ONLY_NO_JAVA_NO_BATTLES_NO_PRODUCTION_CHANGE',sources,inputSHA256,beforeSources,checks,
    selectedForParentControlledPilot:['PLAYER_BLEND','PLAYER_BEST_UNIFORM_BLEND'],unselectedAnalyticControls:['CURRENT_Q','PLAYER_B'],
    selectedInputBasis:[...selected].map(([caseId,x])=>({caseId,cell:x.cell,playerCount:x.players.length})),
    parameters:'Production C=.5900075417594784,a=.7,g log4,q unchanged; NPC Q, weights and search unchanged. Only player target multiplier d changes. No re-anchoring.',weakCases,ordinaryCases,fillers,ordinarySizes,roleControl,
    caveats:['B/Q is not strength-only heterogeneity: it also discounts complementary H/D roles, including equal p.',
      'Numeric target ratios do not establish win-rate safety or a sufficient correction; actual factory and frozen battle controls are required.',
      'PLAYER_B ignores a zero-DPS tank in the technical target; PLAYER_BLEND retains half its health cross contribution.',
      'No displayed equipment power, item roles, skill pricing, counter compensation or outcome fitting introduced.']};
  fs.writeFileSync(path.join(root,'assessment.json'),JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify({checks,weak:weakCases.map(c=>({label:c.label,models:c.models})),ordinary:ordinaryCases.map(c=>({label:c.label,baseDiscount:c.base.BtoQ,appendDiscount:c.append.BtoQ,models:c.models})),roleControl},null,2));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
