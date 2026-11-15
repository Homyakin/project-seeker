const fs = require('node:fs');
const zlib = require('node:zlib');
const readline = require('node:readline');
const source = '/tmp/seeker-step15-3-fillers.HXdOad/independent-review/summary.json';
const rawSource = '/tmp/seeker-step153-screen.5Zg4N2/run/battles.jsonl.gz';
const types = ['SLASH','BLUNT','PIERCE','MAGICAL'];
const sortedSum = xs => xs.sort((a,b)=>a-b).reduce((a,b)=>a+b,0);
function parts(p) {
  const H = p.health / (types.reduce((s,t)=>s+p.damageTakenMultipliers[t],0)/4);
  const attack = Math.max(0,...p.attacksByRange.map(a=>types.reduce((s,t)=>s+(a.attack[t]||0),0)));
  return {H,D:attack*(1+Math.min(1,Math.max(0,p.critChance/100))*(p.critMultiplier-1))*(Math.min(p.initiative,1000)/1000)};
}
const models = {
  CURRENT_Q: xs => sortedSum(xs.map(x=>x.H))*sortedSum(xs.map(x=>x.D)),
  Q_PER_ACTUAL_BODY: xs => sortedSum(xs.map(x=>x.H))*sortedSum(xs.map(x=>x.D))/xs.length,
  SUM_SELF: xs => sortedSum(xs.map(x=>x.H*x.D)),
  MIN_SELF_PAIRS: xs => {const ps=xs.map(x=>x.H*x.D).sort((a,b)=>a-b);return ps.reduce((s,p,i)=>s+(2*(ps.length-i)-1)*p,0);},
  MIN_SELF_SQUARE_ROOT: xs => {const ps=xs.map(x=>x.H*x.D).sort((a,b)=>a-b);return Math.sqrt(ps.reduce((s,p,i)=>s+(2*(ps.length-i)-1)*p*p,0));},
  ROOT_SQUARED_CONTROL: xs => sortedSum(xs.map(x=>Math.sqrt(x.H*x.D)))**2
};
function npc(n,m) {
  const f=Math.floor(n/2),s=n-f;
  return Array.from({length:n},(_,i)=>{
    const front=i<f,w=n/(2*(front?f:s));
    const health=Math.max(1,Math.ceil((front?420:280)*(m*w)));
    const attack=Math.max(1,Math.ceil((front?70:130)*(m*w)));
    const defense=Math.max(1,Math.ceil((front?160:70)*m));
    const mu=[1.1,.9,.75,1.25].reduce((a,w)=>a+(1-defense*w/(defense*w+500)),0)/4;
    const H=health/mu,D=attack*(1+(front?.04:.08)*((front?1.5:1.7)-1))*((front?190:160)/1000);
    return {H,D,health,attack,defense};
  });
}
function find(target,n,metric) {
  let lo=0,hi=1;
  const score=m=>metric(npc(n,m));
  if(score(0)>=target)return {m:0,achieved:score(0)};
  while(score(hi)<target){lo=hi;hi*=2;if(hi>1e6)throw new Error('Diagnostic bound exceeded');}
  for(let k=0;k<64;k++){const mid=lo+(hi-lo)/2;if(mid<=lo||mid>=hi)break;if(score(mid)>=target)hi=mid;else lo=mid;}
  const m=target-score(lo)<score(hi)-target?lo:hi;
  return {m,achieved:score(m),npcs:npc(n,m).map(({health,attack,defense})=>({health,attack,defense}))};
}
const coefficient=(level,n,C,a=.7)=>(1+((level-10)*.06+.14))*(1+.05*Math.log(n)/Math.log(4))*C*(a+(1-a)*(3/n)**2);
(async()=>{
  const saved=JSON.parse(fs.readFileSync(source)), selected=new Map(), wanted=new Set([75,79,127,139,143,191]);
  const input=fs.createReadStream(rawSource).pipe(zlib.createGunzip());
  const lines=readline.createInterface({input,crlfDelay:Infinity});
  for await(const line of lines){
    const r=JSON.parse(line);if(!wanted.has(r.caseId))continue;
    const byId=r.initState.personagesById;
    selected.set(r.caseId,{cell:r.cell,players:r.playerBasis.map(p=>parts(byId[p.id])),
      enemies:r.enemyBasis.map(p=>parts(byId[p.id])),productionCalibration:r.calibration});
    wanted.delete(r.caseId);if(!wanted.size){lines.close();input.destroy();break;}
  }
  if(wanted.size)throw new Error('Missing main inputs');
  const anchor=selected.get(75), constants={}, normal=[],weak=[];
  for(const [name,metric]of Object.entries(models)){
    const C=metric(anchor.enemies)/(metric(anchor.players)*coefficient(25,3,1));constants[name]=C;
    for(const [tier,ids]of [['MID',[75,79]],['STRONG',[139,143]]]){
      const a=selected.get(ids[0]),b=selected.get(ids[1]);
      const ta=metric(a.players)*coefficient(25,3,C),tb=metric(b.players)*coefficient(25,4,C);
      normal.push({model:name,tier,playersAssessmentRatio:metric(b.players)/metric(a.players),
        targetRatio:tb/ta,base:find(ta,3,metric),append:find(tb,4,metric)});
    }
    for(const c of saved.results){
      const base=[c.strong],append=[c.strong,c.weak],n=2;
      const ta=metric(base)*coefficient(c.level,n,C),tb=metric(append)*coefficient(c.level,n,C);
      weak.push({model:name,style:c.style,level:c.level,C,assessmentBefore:metric(base),assessmentAfter:metric(append),
        targetRatio:tb/ta,Hrelative:c.weak.H/c.strong.H,Drelative:c.weak.D/c.strong.D,
        ownRelative:c.weak.H*c.weak.D/(c.strong.H*c.strong.D),base:find(ta,n,metric),append:find(tb,n,metric)});
    }
  }
  const calibrated={};
  for(const name of ['SUM_SELF','MIN_SELF_PAIRS','MIN_SELF_SQUARE_ROOT','Q_PER_ACTUAL_BODY']){
    const metric=models[name],solo=saved.results.find(x=>x.style==='MAIN');
    const r3=constants[name];
    const r2=metric(npc(2,solo.base.m))/(metric([solo.strong])*(1+((solo.level-10)*.06+.14))*(1+.05*Math.log(2)/Math.log(4)));
    const h2=r2/r3,a=(2.25-h2)/1.25;
    const twoAnchors={model:name,C:r3,r2,h2,a,normal:[],weak:[],sizeFactors:[]};
    for(const n of[2,3,4,7,8,15,30,128])twoAnchors.sizeFactors.push({M:n,
      old:.7+(1-.7)*(3/n)**2,new:a+(1-a)*(3/n)**2,
      ratio:(a+(1-a)*(3/n)**2)/(.7+(1-.7)*(3/n)**2)});
    for(const [tier,ids]of [['MID',[75,79]],['STRONG',[139,143]]]){
      const x=selected.get(ids[0]),y=selected.get(ids[1]);
      const ta=metric(x.players)*coefficient(25,3,r3,a),tb=metric(y.players)*coefficient(25,4,r3,a);
      twoAnchors.normal.push({tier,targetRatio:tb/ta,base:find(ta,3,metric),append:find(tb,4,metric)});
    }
    for(const c of saved.results){
      const ta=metric([c.strong])*coefficient(c.level,2,r3,a),tb=metric([c.strong,c.weak])*coefficient(c.level,2,r3,a);
      twoAnchors.weak.push({style:c.style,targetRatio:tb/ta,base:find(ta,2,metric),append:find(tb,2,metric)});
    }
    calibrated[name]=twoAnchors;
  }
  const sizeScreen=[];
  for(const name of ['SUM_SELF','MIN_SELF_PAIRS','MIN_SELF_SQUARE_ROOT']){
    const cal=calibrated[name],metric=models[name];
    for(const [tier,prefixId,soloId]of [['MID',127,79],['STRONG',191,143]])for(const level of[25,50]){
      const reference=selected.get(prefixId).players,soloPlayer=selected.get(soloId).players[3];
      let previous=null;
      for(const n of[...Array.from({length:30},(_,i)=>i+1),50]){
        const players=n===1?[soloPlayer]:Array.from({length:n},(_,i)=>i<reference.length?reference[i]:reference[1+(i-1)%3]);
        const M=Math.max(2,n),target=metric(players)*coefficient(level,M,cal.C,cal.a),fit=find(target,M,metric);
        const f=Math.floor(M/2);
        const row={model:name,tier,level,N:n,M,extendedPastReferenceLimit:n>30,
          inputPrefixFromPrevious:n>2&&n<=30,assessment:metric(players),target,m:fit.m,
          achieved:fit.achieved,mRatioToPrevious:previous&&n===previous.N+1?fit.m/previous.m:null,
          guardian:fit.npcs[0],slinger:fit.npcs.at(-1),frontTotalHealth:fit.npcs.slice(0,f).reduce((s,p)=>s+p.health,0),
          backTotalHealth:fit.npcs.slice(f).reduce((s,p)=>s+p.health,0),
          frontCount:f,backCount:M-f,
          frontTotalAttack:fit.npcs.slice(0,f).reduce((s,p)=>s+p.attack,0),
          backTotalAttack:fit.npcs.slice(f).reduce((s,p)=>s+p.attack,0)};
        sizeScreen.push(row);previous=row;
      }
    }
  }
  console.log(JSON.stringify({purpose:'PURE_ALGEBRA_FROM_SAVED_153_INPUTS_NO_BATTLES_NOT_PRODUCTION_SEARCH_REPLAY',
    anchor:{source:rawSource,caseId:75,cell:anchor.cell,productionCalibration:anchor.productionCalibration},
    constants,normal,weak,twoAnchors:calibrated.SUM_SELF,calibrated,sizeScreen,limitations:['Diagnostic scalar factory and bounded binary search, not Java production execution',
      'No outcomes calculated; constants anchored to achieved characteristics of production15.2 MID N3 L25',
      'Alternative assessment changes NPC valuation as well as player valuation',
      'ROOT_SQUARED is an analytical control, not a proposed approved formula']},null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
