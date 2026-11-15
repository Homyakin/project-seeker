const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),assert=require('node:assert/strict');
const root=__dirname;
const paths={H:path.join(root,'screen/battles.jsonl.gz'),C9:'/tmp/seeker-step15-4-2-confirm.TpC9K3/screen/battles.jsonl.gz'};
const policyPath=path.join(root,'frozen-policy.json'),policy=JSON.parse(fs.readFileSync(policyPath));
const sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const read=p=>zlib.gunzipSync(fs.readFileSync(p)).toString('utf8').trim().split('\n').map(JSON.parse);
const rows=Object.fromEntries(Object.entries(paths).map(([phase,p])=>[phase,read(p)]));
const seed=(ns,pair)=>crypto.createHash('sha256').update(`${ns}/251/${pair}`).digest().readUIntBE(0,6);
const indexes={},counts={},raws={};
for(const [phase,rs]of Object.entries(rows)){
  assert.equal(rs.length,2000);indexes[phase]=new Map();counts[phase]={};raws[phase]={path:paths[phase],sha256:sha(fs.readFileSync(paths[phase]))};
  for(const r of rs){assert.equal(r.case.phase.id,phase);assert.equal(r.case.level,25);assert.equal(r.case.groupId,251);assert.ok([1,2].includes(r.case.scenarioId));
    assert.ok(Number.isInteger(r.pair)&&r.pair>=0&&r.pair<500);assert.equal(typeof r.reverse,'boolean');
    assert.equal(r.generationSeed,seed(policy.generationNamespace,r.pair));assert.equal(r.battleSeed,seed(policy.battleNamespace,r.pair));
    const key=`${r.case.scenarioId}/${r.pair}/${r.reverse}`;assert.ok(!indexes[phase].has(key));indexes[phase].set(key,r);
    const c=counts[phase][r.case.scenarioId]??={executions:0,WIN:0,LOSS:0,DRAW:0,TIMEOUT:0,playersFirstWins:0,npcFirstWins:0};
    c.executions++;assert.ok(Object.hasOwn(c,r.outcome));c[r.outcome]++;if(r.outcome==='WIN'){if(r.reverse)c.playersFirstWins++;else c.npcFirstWins++;}
  }
}
const score=r=>r.outcome==='WIN'?1:r.outcome==='DRAW'?.5:r.outcome==='LOSS'?0:null;
const pairs=[];
for(let pair=0;pair<500;pair++){
  const all=[];for(const phase of ['H','C9'])for(const scenarioId of [1,2])for(const reverse of [false,true]){const r=indexes[phase].get(`${scenarioId}/${pair}/${reverse}`);assert.ok(r);all.push(r);}
  if(all.some(r=>r.outcome==='TIMEOUT')){pairs.push({pair,complete:false});continue;}
  const values={};for(const phase of ['H','C9']){const v=scenarioId=>[false,true].reduce((sum,reverse)=>sum+score(indexes[phase].get(`${scenarioId}/${pair}/${reverse}`)),0)/2;
    values[phase]={base:v(1),append:v(2)};values[phase].delta=values[phase].append-values[phase].base;}
  pairs.push({pair,complete:true,H:values.H,C9:values.C9,DID:values.C9.delta-values.H.delta});
}
const complete=pairs.filter(p=>p.complete),n=complete.length,mean=get=>complete.reduce((sum,p)=>sum+get(p),0)/n;
const alpha=policy.confidence.alpha,K=policy.confidence.familySize,ln=Math.log(2*K/alpha),appendHalf=Math.sqrt(2*ln/n),didHalf=Math.sqrt(8*ln/n);
const contrast=(name,value,half,bound)=>({name,estimate:value,halfWidth:half,simultaneousFamily95:[Math.max(-bound,value-half),Math.min(bound,value+half)]});
const contrasts=[contrast('deltaH',mean(p=>p.H.delta),appendHalf,1),contrast('deltaC9',mean(p=>p.C9.delta),appendHalf,1),contrast('DID',mean(p=>p.DID),didHalf,2)];
const pilotH=read('/tmp/seeker-step1542-historical.77NJth/screen/battles.jsonl.gz');
const pilotC0=read('/tmp/seeker-step15-4-2-causal.1HOgPK/screen/battles.jsonl.gz').filter(r=>r.case.phase.id==='C0');
const c0index=new Map(pilotC0.map(r=>[`${r.case.level}/${r.case.scenarioId}/${r.pair}/${r.reverse}`,r]));let pilotMatches=0;
for(const h of pilotH){const c=c0index.get(`${h.case.level}/${h.case.scenarioId}/${h.pair}/${h.reverse}`);assert.ok(c);assert.equal(h.outcome,c.outcome);assert.equal(h.rounds,c.rounds);pilotMatches++;}
const report={verdict:'PASS',purpose:'INDEPENDENT_RAW_COUNT_AND_PREDECLARED_K3_RECOMPUTATION_NO_BATTLE_EXECUTION',policySHA256:sha(fs.readFileSync(policyPath)),
  pilotExcludedFromConfirmation:true,raws,completePairs:n,incompletePairs:500-n,counts,
  rates:{H:{base:mean(p=>p.H.base),append:mean(p=>p.H.append)},C9:{base:mean(p=>p.C9.base),append:mean(p=>p.C9.append)}},
  confidence:{familySize:K,alpha,appendHalfWidth:appendHalf,didHalfWidth:didHalf},contrasts,
  pilotCoherentHistoricalVersusCurrentC0:{rows:pilotMatches,outcomesAndRoundsAllExact:true,scope:'Only frozen projection,30 pair pilot, not a claim that current LEGACY is generally identical to the old engine'},pairs};
fs.writeFileSync(path.join(root,'independent-comparison.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify({...report,pairs:undefined},null,2));
