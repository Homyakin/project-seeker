'use strict';
// Independent read-only15.4.3 frozen-data audit. No JVM, battle execution, subprocesses or file writes.
// Usage: node validate-heterogeneity.cjs STUDY_ROOT preflight [SCREEN_POLICY_JSON]
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const ROOT=path.resolve(process.argv[2]||'.'),MODE=process.argv[3]||'preflight',POLICY=process.argv[4]||path.join(ROOT,'screen-policy.json');
const read=(r,f)=>JSON.parse(fs.readFileSync(path.join(r,f),'utf8')),hash=x=>crypto.createHash('sha256').update(x).digest('hex');
let assertions=0;function check(x,m){assertions++;assert.ok(x,m);}function close(a,b,m){check(Number.isFinite(a)&&Number.isFinite(b)&&Math.abs(a-b)<=3e-12*Math.max(1,Math.abs(a),Math.abs(b)),m+': '+a+' / '+b);}
function canon(v,k){if(Array.isArray(v)){const a=v.map(x=>canon(x));return ['slots','availableOnSlots'].includes(k)?a.sort():a;}if(v&&typeof v==='object')return Object.fromEntries(Object.keys(v).sort().map(k=>[k,canon(v[k],k)]));return v;}const digest=v=>hash(JSON.stringify(canon(v))),eq=(a,b,m)=>check(digest(a)===digest(b),m);
const ATT=['SLASH','BLUNT','PIERCE','MAGICAL'],MATRIX={CLOTH:[.75,1.25,.9,1.1],LEATHER:[.9,1.1,1.25,.75],PLATE:[1.25,.75,1.1,.9],ARCANE:[1.1,.9,.75,1.25]};
const sum=a=>a.reduce((s,x)=>s+x,0),stable=a=>sum(a.toSorted((a,b)=>a-b));
const seed=(ns,g,p)=>crypto.createHash('sha256').update(ns+'/'+g+'/'+p).digest().readUIntBE(0,6);
const frozen=path.join(ROOT,'frozen-preparation'),manifest=read(frozen,'manifest.json'),inputs=read(frozen,'inputs.json'),samples=read(frozen,'prepared-pair0.json'),seeds=read(frozen,'seeds.json'),byId=new Map(inputs.map(c=>[c.id,c]));
function components(a){
 for(const x of [a.hp,a.critMultiplier,a.speed])check(Number.isFinite(x)&&x>=0,'finite actor inputs');
 let mu=0;for(let i=0;i<ATT.length;i++){const t=ATT[i],eff=sum(Object.entries(a.defenses).map(([type,x])=>x*MATRIX[type][i])),own=1-eff/(eff+500);close(a.reductions[t],own,'neutral reduction from armor');mu+=a.reductions[t];}
 const H=a.hp/(mu/4),at=Math.max(0,...a.attacks.map(p=>sum(ATT.map(t=>p.attack[t]||0)))),D=at*(1+Math.max(0,Math.min(1,a.crit/100))*(a.critMultiplier-1))*Math.min(a.speed,1000)/1000;
 check(Number.isFinite(H)&&H>=0&&Number.isFinite(D)&&D>=0,'finite H/D');return {id:a.id,H,D,p:H*D};
}
function evaluation(actors,rule){
 const cs=actors.map(components),Q=stable(cs.map(x=>x.H))*stable(cs.map(x=>x.D)),pi=cs.map(x=>x.p).toSorted((a,b)=>a-b);
 let rawB=0,rawT=pi.at(-1),bestK=1;for(let i=0;i<pi.length;i++){rawB+=(2*(pi.length-i)-1)*pi[i];const k=i+1,v=(k*k)*pi[pi.length-k];if(v>rawT){rawT=v;bestK=k;}}
 const quadratic=sum(pi.flatMap(x=>pi.map(y=>Math.min(x,y))));close(rawB,quadratic,'B independently from all ordered pairs');
 check(rawT<=rawB*(1+1e-12)&&rawB<=Q*(1+1e-12),'T<=B<=Q');const boundedB=Math.min(rawB,Q),boundedT=Math.min(rawT,Q),exactSoloOrIdentical=cs.every(x=>x.H===cs[0].H&&x.D===cs[0].D);
 const F=Q===0?0:rule==='BASELINE'||exactSoloOrIdentical?Q:Q/2+(rule==='BLEND_B'?boundedB:boundedT)/2,discount=Q===0?1:Math.max(.5,Math.min(1,F/Q));
 return {cs,Q,rawB,rawT,bestK,boundedB,boundedT,F,discount,exactSoloOrIdentical};
}
function compareEvaluation(saved,own,label){for(const k of ['Q','rawB','rawT','boundedB','boundedT','F','discount'])close(saved[k],own[k],label+'.'+k);check(saved.exactSoloOrIdentical===own.exactSoloOrIdentical,label+' uniform');check(Number.isInteger(saved.bestK)&&saved.bestK>=1&&saved.bestK<=own.cs.length,label+' bestK domain');}
function numeric(row){
 const c=row.case,rule=row.rule,ps=row.players||row.playerTraits,ns=row.enemies||row.enemyTraits,ev=evaluation(ps,rule),fixed=c.family==='FIXED_NPC',basis=fixed?ps.slice(0,1):ps,cv=evaluation(basis,fixed?'BASELINE':rule),t=row.target,m=row.calibration.multiplier,N=Math.max(2,basis.length),F=Math.floor(N/2),S=N-F,wF=N/(2*F),wS=N/(2*S);
 eq(c,byId.get(c.id),'whole frozen case');check(['BASELINE','BLEND_B','BLEND_T'].includes(rule)&&(!fixed||rule==='BASELINE'),'allowed rule');
 compareEvaluation(row.playerEvaluation||row.ruleAssessment,ev,'players');compareEvaluation(row.calibrationEvaluation||row.calibrationAssessment,cv,'calibration basis');
 check(ps.length===c.members.length&&ns.length===N,'actual counts');check(new Set([...ps,...ns].map(a=>a.id)).size===ps.length+ns.length,'unique actor UUIDs');
 check(row.contributions.length===ps.length,'contribution count');row.contributions.forEach((x,i)=>{check(x.id===ps[i].id,'contribution ID');for(const k of ['H','D','p'])close(x[k],ev.cs[i][k],'actor contribution '+k);});
 const q=1+((c.level-10)*.06+.14),g=1+Math.log(N)/Math.log(4)*.05,h=.7+(1-.7)*(3/N)**2,C=.5900075417594784,baseline=cv.Q*q*g*C*h;
 for(const [k,v]of Object.entries({assessment:cv.Q,levelPowerBonus:q,count:N,frontCount:F,backCount:S,frontWeight:wF,backWeight:wS,groupScaling:g,sizeCorrection:h,encounterCorrection:C,baselineTarget:baseline,discount:cv.discount,requestedTarget:baseline*cv.discount}))close(t[k],v,'target '+k);
 close(row.calibration.targetPower,t.requestedTarget,'search target');check(Number.isFinite(m)&&m>=0,'finite calibration m');
 ns.forEach((a,i)=>{const front=i<F,w=front?wF:wS,hp=Math.max(1,Math.ceil((front?420:280)*(m*w))),atk=Math.max(1,Math.ceil((front?70:130)*(m*w))),def=Math.max(1,Math.ceil((front?160:70)*m));
 check(a.hp===hp,'exact weighted HP');eq(a.defenses,{ARCANE:def},'exact unweighted DEF');eq(a.attacks,Array.from({length:front?1:3},(_,j)=>({range:j+1,attack:{MAGICAL:atk}})),'exact weighted/ranged attack');
 check(a.speed===(front?190:160)&&a.crit===(front?4:8)&&a.dodge===(front?3:5)&&a.critMultiplier===(front?1.5:1.7),'fixed NPC numerics');check(a.position===(front?'FRONT':'BACK')&&a.range===(front?1:3),'NPC formation/range');
 eq(a.skills,[{code:front?'KNOCKBACK':'PRECISE_STRIKE',points:4,formulaVersion:'SCALING_SKILLS_V3'}],'actual V3 NPC skills');check(a.scalingRules===true,'V3 combat rules');});
 const ne=evaluation(ns,'BASELINE');close(row.calibration.achievedPower,ne.Q,'actual NPC Q');check(row.calibration.evaluations>0,'finite completed search');
 ps.forEach((a,i)=>{const member=c.members[i];check(a.position===member.position&&a.tactic===member.tactic,'preserved declared player arrangement');check(a.hp>0&&a.scalingRules===true,'live V3 prepared player');
 const items=member.items.map(x=>({code:x.object.code,rarity:x.rarity,skill:x.modifier?.activeEnum||null}));eq(a.items,items,'snapshot items follow frozen full input');for(const x of a.skills)check(x.formulaVersion==='SCALING_SKILLS_V3','player V3 skill');});
 return {ev,cv,ps,ns};
}
function preflight(){
 const freeze=[];for(const line of fs.readFileSync(path.join(ROOT,'prebattle-freeze.sha256'),'utf8').trim().split(/\r?\n/)){const m=/^([0-9a-f]{64})  (.+)$/.exec(line);check(m&&!path.isAbsolute(m[2])&&!m[2].split('/').includes('..'),'relative freeze path');check(hash(fs.readFileSync(path.join(ROOT,m[2])))===m[1],'prebattle frozen bytes '+m[2]);freeze.push({file:m[2],sha256:m[1]});}
 const policy=JSON.parse(fs.readFileSync(POLICY,'utf8'));check(hash(fs.readFileSync(POLICY))==='e41947b273b6db0defa748fcd0a8edbcbd1dec482570a5049aa1f0b7e9c858cf','root policy frozen SHA');
 check(hash(fs.readFileSync(path.join(ROOT,'Step1543HeterogeneityProbe.java')))===policy.helperSHA256&&hash(fs.readFileSync(path.join(frozen,'manifest.json')))===policy.manifestSHA256,'policy fixes helper/manifest');
 check(inputs.length===76&&byId.size===76&&samples.length===224&&seeds.length===475,'approved inventory');
 check(manifest.battles===11200&&manifest.pairs===25&&manifest.modelConditions===224,'approved bounded count');
 inputs.forEach((c,i)=>{check(c.id===i+1&&c.members.length>=1&&c.members.length<=30&&c.level>=1&&c.level<=50,'frozen input domain');for(const p of c.members){const slots=p.items.flatMap(x=>x.object.slots);check(slots.length===7&&new Set(slots).size===7,'all slots present and disjoint');}});
 const n8=inputs.filter(c=>c.family==='CONFIRMED_N8');check(n8.length===2&&n8[0].members.length===8&&n8[1].members.length===9&&n8.every(c=>c.level===30),'confirmed group risk included');
 const weak=read(path.join(ROOT,'source-inputs'),'fillers-main-confirmation-inputs.json').MAIN.inputsBySize['2'][1],ordinary=read(path.join(ROOT,'source-inputs'),'ordinary-selected-inputs.json').cases;
 for(const c of inputs){if(['STRONG_FILLERS','STRONG_FILLERS_HIGH'].includes(c.family)){const sourceName={MAIN:'main',PIERCE_ARCANE:'pierce',RITUAL:'ritual'}[c.style],strong=read(path.join(ROOT,'source-inputs'),'fillers-'+sourceName+'-confirmation-inputs.json')[c.style].fixedFirstMember;eq(c.members[0],strong,'actual archived strong solo');c.members.slice(1).forEach(x=>eq(x,weak,'same actual weak filler'));}
 if(c.family==='WEAK_ONLY')c.members.forEach(x=>eq(x,weak,'actual weak-only actor'));
 if(c.family==='ORDINARY'){const ref=ordinary[c.tier];eq(c.members.slice(0,3),ref.base,'actual archived ordinary base');if(c.variant==='NORMAL_APPEND')eq(c.members[3],ref.ordinaryBruiser,'archived ordinary added B');if(c.variant==='WEAK_APPEND')eq(c.members[3],weak,'actual weak append');}
 if(c.variant.endsWith('APPEND')){const base=inputs.find(x=>x.family===c.family&&x.groupId===c.groupId&&x.style===c.style&&x.variant==='BASE');check(!!base,'append has base');eq(c.members.slice(0,base.members.length),base.members,'unchanged input prefix');}}
 const sm=new Map();for(const r of samples){const key=r.case.id+'/'+r.rule;check(!sm.has(key),'one sample/model');numeric(r);sm.set(key,r);}
 for(const c of inputs){const rules=c.family==='FIXED_NPC'?['BASELINE']:['BASELINE','BLEND_B','BLEND_T'];for(const rule of rules)check(sm.has(c.id+'/'+rule),'every approved model sample');const ref=sm.get(c.id+'/BASELINE');for(const rule of rules){const x=sm.get(c.id+'/'+rule);eq(x.players,ref.players,'same players across target rules');eq(x.enemies.map(a=>[a.id,a.gauge]),ref.enemies.map(a=>[a.id,a.gauge]),'same NPC random stream across rules');}}
 const a=sm.get('71/BASELINE'),b=sm.get('72/BASELINE');eq(a.enemies,b.enemies,'fixed NPC exact full traits UUID gauge');eq(a.calibration,b.calibration,'fixed NPC exact calibration');eq(a.players,b.players.slice(0,1),'fixed diagnostic strong unchanged');
 const seen=new Set();for(const x of seeds){check(x.groupId>=1&&x.groupId<=19&&x.pair>=0&&x.pair<25,'seed domain');const k=x.groupId+'/'+x.pair;check(!seen.has(k),'unique seed record');seen.add(k);for(const [field,ns]of [['generationSeed',manifest.generationNamespace],['battleSeed',manifest.battleNamespace],['npcSeed',manifest.npcNamespace]])check(x[field]===seed(ns,x.groupId,x.pair),'independent SHA48 root');}
 return {freeze,policySha256:hash(fs.readFileSync(POLICY)),conditions:inputs.length,modelConditions:samples.length,seedRecords:seeds.length,approvedExecutions:11200,fullHelperReadSeparately:true};
}
function totalStats(stats){return stats.reduce((a,x)=>{a.damage+=x.normalDamageDealt+x.critDamageDealt+x.skillDamageDealt;a.normalAttempts+=x.normalAttackCount+x.critsCount+x.missesCount;a.recordedTurns+=x.turnsCount;a.remainingHp+=x.remainHealth;a.deaths+=x.remainHealth===0?1:0;return a;},{damage:0,normalAttempts:0,recordedTurns:0,remainingHp:0,deaths:0});}
function withoutRandom(a){const x={...a};delete x.id;delete x.gauge;return x;}
function eventAudit(r,ps,ns){
 const all=[...ps,...ns],stats=[...r.playerStats,...r.enemyStats],hp=Object.fromEntries(all.map(a=>[a.id,a.hp])),max={...hp},actual=Object.fromEntries(all.map(a=>[a.id,{normalDamage:0,critDamage:0,skillDamage:0,attempts:0}]));
 const roles=Object.fromEntries([...ps.map((x,i)=>[x.id,'P'+i]),...ns.map((x,i)=>[x.id,i<Math.floor(ns.length/2)?'G':'S'])]),targets={};
 let moves=0,forced=0;for(const e of r.actionLog.events){check(Number.isInteger(e.round)&&e.round>=1&&e.round<=r.rounds,'event round');if(e.type==='MovedTowardEnemy')moves++;if(e.type==='PersonageForcedMove')forced++;
 if(e.type==='AttackDodged'){check(e.attackerId in actual&&e.targetId in actual,'dodge IDs');actual[e.attackerId].attempts++;}
 if(['DamageReceived','EffectDamage','SkillDamage','ScalingSkillDamage'].includes(e.type)){const id=e.attackerId||e.sourceId;check(id in actual&&e.targetId in actual,'damage IDs');check(Number.isInteger(e.damageTaken)&&e.damageTaken>=0&&e.damageTaken<=hp[e.targetId],'damage cap');hp[e.targetId]-=e.damageTaken;check(hp[e.targetId]===e.remainingHealth,'event HP arithmetic');
 const field=e.type==='DamageReceived'?(e.roll.crit?'critDamage':'normalDamage'):'skillDamage';actual[id][field]+=e.damageTaken;if(e.type==='DamageReceived')actual[id].attempts++;const k=roles[id]+'->'+roles[e.targetId];targets[k]=(targets[k]||0)+e.damageTaken;}
 if(e.type==='PersonageHealed'){check(e.personageId in hp&&e.amount>=0,'heal ID/amount');hp[e.personageId]=Math.min(max[e.personageId],hp[e.personageId]+e.amount);check(hp[e.personageId]===e.remainingHealth,'heal arithmetic');}}
 all.forEach((a,i)=>{const x=stats[i],v=actual[a.id];check(hp[a.id]===x.remainHealth,'journal final HP');check(v.normalDamage===x.normalDamageDealt&&v.critDamage===x.critDamageDealt&&v.skillDamage===x.skillDamageDealt,'journal individual damage');check(v.attempts===x.normalAttackCount+x.critsCount+x.missesCount,'journal individual normal attempts');});
 check(r.actionLog.events.length===r.actionCount&&moves===r.moves&&forced===r.forcedMoves,'retained event counters');check(hash(JSON.stringify(r.actionLog))===r.actionSha256,'whole action-log SHA');
 check(Array.isArray(r.traceEvents)&&hash(JSON.stringify(r.traceEvents))===r.traceSha256,'whole separate trace SHA');const attempts=r.traceEvents.filter(e=>e.attemptId!==undefined);check(attempts.length===r.normalAttempts,'retained trace attempt count');
 return {events:r.actionLog.events.length,traceEvents:r.traceEvents.length,targets};
}
async function auditRaw(){
 const zlib=require('node:zlib'),readline=require('node:readline'),run=path.join(ROOT,fs.existsSync(path.join(ROOT,'pilot','battles.jsonl.gz'))?'pilot':'screen'),prepared=new Map(samples.map(x=>[x.case.id+'/'+x.rule,x]));
 eq(read(run,'manifest.json'),manifest,'run manifest frozen');eq(read(run,'inputs.json'),inputs,'run inputs frozen');eq(read(run,'seeds.json'),seeds,'run seed list frozen');eq(read(run,'prepared-pair0.json'),samples,'run samples frozen');
 const states=new Map(),mirrors=new Map(),playerModels=new Map(),unchanged=new Map(),counterNPC=new Map(),fixedNPC=new Map(),prefixes=new Map(),continuity=new Map(),stripped=new Map();
 const counts={rows:0,cells:0,mirrors:0,prefixes:0,modelIdentities:0,unchangedBattles:0,counterNPCIdentities:0,fixedNPCIdentities:0,fullLogs:0,fullLogEvents:0,fullTraceEvents:0};
 const comp=crypto.createHash('sha256'),plain=crypto.createHash('sha256');let plainBytes=0;const file=fs.createReadStream(path.join(run,'battles.jsonl.gz'));file.on('data',b=>comp.update(b));const gun=file.pipe(zlib.createGunzip());gun.on('data',b=>{plain.update(b);plainBytes+=b.length;});
 for await(const line of readline.createInterface({input:gun,crlfDelay:Infinity})){
  const r=JSON.parse(line),c=byId.get(r.case.id),key=r.case.id+'/'+r.rule;check(c&&prepared.has(key),'approved raw cell');const p=prepared.get(key),a=numeric(r),fixed=c.family==='FIXED_NPC';
  check(Number.isInteger(r.pair)&&r.pair>=0&&r.pair<25&&typeof r.reverse==='boolean'&&r.mirror===r.reverse,'pair/mirror domain');
  for(const [field,ns]of [['generationSeed',manifest.generationNamespace],['battleSeed',manifest.battleNamespace],['npcSeed',manifest.npcNamespace]])check(r[field]===seed(ns,c.groupId,r.pair),'raw independent SHA48 root');
  check(r.npcSeedUsed===fixed,'third NPC stream only diagnostic');close(r.actualPlayerAssessment,a.ev.Q,'actual player Q');
  eq(r.target,p.target,'target exact across seeds');eq(r.calibration,p.calibration,'isolated actual search exact across seeds');eq(r.diagnostics,p.diagnostics,'unchanged actual calibration diagnostics');
  eq(a.ps.map(withoutRandom),p.players.map(withoutRandom),'all frozen prepared player numerics/items/skills');
  eq(a.ns.map(withoutRandom),p.enemies.map(withoutRandom),'all frozen NPC numerics/skills');
  let st=states.get(key);if(!st){st={case:c.id,rule:r.rule,family:c.family,style:c.style,tier:c.tier,level:c.level,variant:c.variant,N:c.members.length,rows:0,outcomes:{WIN:0,LOSS:0,DRAW:0,TIMEOUT:0},pairScores:Array(25).fill(0),seen:new Set(),rounds:0,events:0,players:totalStats([]),enemies:totalStats([]),firstDamage:0,lastDamage:0,focus:{logs:0,targets:{}},calibration:{m:r.calibration.multiplier,Q:r.actualPlayerAssessment,F:r.ruleAssessment.F,discount:r.ruleAssessment.discount,target:r.target.requestedTarget,npcG:a.ns[0].hp+'/'+a.ns[0].attacks[0].attack.MAGICAL+'/'+a.ns[0].defenses.ARCANE,npcS:a.ns.at(-1).hp+'/'+a.ns.at(-1).attacks[0].attack.MAGICAL+'/'+a.ns.at(-1).defenses.ARCANE}};states.set(key,st);}
  const orient=r.pair+'/'+r.reverse;check(!st.seen.has(orient),'unique orientation');st.seen.add(orient);
  const mk=key+'/'+r.pair,md=digest([r.playerTraits,r.enemyTraits,r.target,r.calibration]);if(!r.reverse){check(!mirrors.has(mk),'mirror first unique');mirrors.set(mk,md);}else{check(mirrors.get(mk)===md,'fresh mirror prepared identity');mirrors.delete(mk);counts.mirrors++;}
  const pm=c.id+'/'+r.pair,pp=digest(r.playerTraits),nr=digest(r.enemyTraits.map(x=>[x.id,x.gauge]));if(r.rule==='BASELINE'&&!r.reverse)playerModels.set(pm,[pp,nr]);else if(!fixed){eq(playerModels.get(pm),[pp,nr],'player and NPC UUID/gauge unchanged across rules');counts.modelIdentities++;}
  if(c.family==='COUNTER_TWINS'){const k=r.rule+'/'+c.variant+'/'+r.pair,dd=digest([r.enemyTraits,r.target]);if(counterNPC.has(k)){check(counterNPC.get(k)===dd,'counter type twins exact enemy/target equality');counts.counterNPCIdentities++;}else counterNPC.set(k,dd);}
  if(fixed){const k=r.pair+'/'+r.reverse,dd=digest([r.enemyTraits,r.target,r.calibration]);if(c.variant==='BASE')fixedNPC.set(k,dd);else{check(fixedNPC.get(k)===dd,'fixed diagnostic full enemy identity');counts.fixedNPCIdentities++;}}
  const group=c.family+'/'+c.groupId+'/'+c.style+'/'+r.rule+'/'+r.pair+'/'+r.reverse;if(c.variant==='BASE')prefixes.set(group,r.playerTraits);else if(['NORMAL_APPEND','WEAK_APPEND'].includes(c.variant)||c.variant.startsWith('FILLERS_N')||c.variant.startsWith('WEAK_N')){const base=prefixes.get(group);check(!!base,'append raw base already retained');eq(r.playerTraits.slice(0,base.length),base,'actual prepared prefix unchanged');counts.prefixes++;}
  if(c.family==='STRONG_FILLERS'&&c.variant==='FILLERS_N2')stripped.set(c.groupId+'/'+r.rule+'/'+r.pair+'/'+r.reverse,r.playerTraits);
  if(c.family==='STRIPPED'){const base=stripped.get(c.groupId+'/'+r.rule+'/'+r.pair+'/'+r.reverse);check(!!base,'strip has full+weak comparison');eq(r.playerTraits.slice(1),base.slice(1),'strip keeps weak unchanged');eq(r.playerTraits.map(x=>[x.id,x.gauge]),base.map(x=>[x.id,x.gauge]),'strip preserves actor random state');}
  if(c.family==='SWORD_CONTINUITY'){const k=r.rule+'/'+r.pair+'/'+r.reverse;if(c.variant==='SWORD_PLUS0')continuity.set(k,{p:r.playerTraits[0],target:r.target.requestedTarget});else{const b=continuity.get(k);eq(b.p,r.playerTraits[0],'sword upgrade keeps strong unchanged');check(r.target.requestedTarget>=b.target*(1-1e-14),'actual one upgrade target monotone');}}
  const all=[...a.ps,...a.ns],stats=[...r.playerStats,...r.enemyStats];check(stats.length===all.length,'individual final-stat cardinality');check(r.initState.tempoVersion==='ACCELERATED_V1','unchanged1.75 tempo');check(Object.keys(r.initState.personagesById).length===all.length,'all initial actors');
  all.forEach((x,i)=>{const y=r.initState.personagesById[x.id],ss=stats[i],player=i<a.ps.length;check(y&&y.id===x.id&&y.health===x.hp&&y.initiative===x.speed&&y.initiativeGauge===x.gauge,'actual initial trait/UUID/gauge');check(y.firstTeam===(player?r.reverse:!r.reverse),'mirrored sides');
   eq(y.attacksByRange,x.attacks,'initial profiles');eq(y.defenses,x.defenses,'initial defenses');eq(y.damageTakenMultipliers,x.reductions,'initial reductions');
   check(y.range===x.range&&y.targetingTactic===x.tactic,'initial range/tactic');const ln=r.initState.lines[y.lineIndex];check(ln&&ln.personageIds.includes(x.id),'initial line contains actor');
   check(ss.initialHealth===x.hp&&Number.isInteger(ss.remainHealth)&&ss.remainHealth>=0&&ss.remainHealth<=x.hp,'individual final HP');for(const f of ['normalDamageDealt','normalAttackCount','critDamageDealt','critsCount','missesCount','turnsCount','skillDamageDealt','skillDamageCount'])check(Number.isSafeInteger(ss[f])&&ss[f]>=0,'finite individual counter '+f);
  });
  const pa=r.playerStats.some(x=>x.remainHealth>0),ea=r.enemyStats.some(x=>x.remainHealth>0);check(!(pa&&ea),'completed sides');check(r.outcome===(pa?'WIN':ea?'LOSS':'DRAW')&&Number.isInteger(r.rounds)&&r.rounds>0&&r.rounds<=manifest.roundLimit,'outcome from HP/no timeout');
  const ps=totalStats(r.playerStats),es=totalStats(r.enemyStats);check(ps.normalAttempts+es.normalAttempts===r.normalAttempts,'trace normal attempts equal individual ordinary stats, not event count');
  check(Number.isSafeInteger(r.actionCount)&&r.actionCount>0,'finite recorded events');if(r.pair===0){const l=eventAudit(r,a.ps,a.ns);counts.fullLogs++;counts.fullLogEvents+=l.events;counts.fullTraceEvents+=l.traceEvents;st.focus.logs++;for(const[k,v]of Object.entries(l.targets))st.focus.targets[k]=(st.focus.targets[k]||0)+v;}else check(r.actionLog===undefined&&r.traceEvents===undefined,'retention scope exactly pair0');
  const sameKey=c.id+'/'+r.pair+'/'+r.reverse,sameDigest=digest([r.enemyTraits,r.outcome,r.rounds,r.actionSha256,r.traceSha256,r.playerStats,r.enemyStats]);if(r.rule==='BASELINE')unchanged.set(sameKey,sameDigest);else if(r.target.discount===1){check(unchanged.get(sameKey)===sameDigest,'exact unchanged solo/uniform individual outcome/full journal hashes/stats');counts.unchangedBattles++;}
  st.rows++;st.outcomes[r.outcome]++;st.rounds+=r.rounds;st.events+=r.actionCount;st.pairScores[r.pair]+=r.outcome==='WIN'?.5:r.outcome==='DRAW'?.25:0;
  for(const k of Object.keys(ps)){st.players[k]+=ps[k];st.enemies[k]+=es[k];}st.firstDamage+=r.playerStats[0].normalDamageDealt+r.playerStats[0].critDamageDealt+r.playerStats[0].skillDamageDealt;const last=r.playerStats.at(-1);st.lastDamage+=last.normalDamageDealt+last.critDamageDealt+last.skillDamageDealt;counts.rows++;
 }
 check(counts.rows===11200&&states.size===224&&mirrors.size===0,'complete bounded raw');counts.cells=states.size;
 const summary=read(run,'summary.json');eq(summary.manifest,manifest,'summary manifest');check(summary.cases.length===224,'all saved cell summaries');
 for(const x of summary.cases){const st=states.get(x.case.id+'/'+x.rule);eq(x.case,byId.get(x.case.id),'saved whole frozen case');check(st.rows===50&&st.seen.size===50,'all25pairs complete');eq(x.pairScores,st.pairScores,'saved pair-score vector');for(const k of Object.keys(st.outcomes))check((x.outcomes[k]||0)===st.outcomes[k],'saved outcomes');close(x.winrate,st.outcomes.WIN/50,'saved winrate');close(x.pairMean,sum(st.pairScores)/25,'saved pair mean');close(x.meanRounds,st.rounds/50,'saved rounds');close(x.meanEvents,st.events/50,'saved event count');}
 const comparisons=[];for(const st of states.values()){const c=byId.get(st.case);let base=inputs.find(x=>x.family===c.family&&x.groupId===c.groupId&&x.style===c.style&&x.variant==='BASE');if(c.family==='STRIPPED')base=inputs.find(x=>x.family==='STRONG_FILLERS'&&x.groupId===c.groupId&&x.variant==='FILLERS_N2');if(c.variant==='SWORD_PLUS1')base=inputs.find(x=>x.family==='SWORD_CONTINUITY'&&x.variant==='SWORD_PLUS0');if(!base||base.id===c.id)continue;const bs=states.get(base.id+'/'+st.rule),v=st.pairScores.map((x,i)=>x-bs.pairScores[i]);comparisons.push({family:c.family,style:c.style,tier:c.tier,level:c.level,rule:st.rule,base:base.id,added:c.id,variant:c.variant,baseWinrate:bs.outcomes.WIN/50,addedWinrate:st.outcomes.WIN/50,delta:sum(v)/25,pairDifferences:v,interpretation:'Descriptive frozen25-pair comparison only; not simultaneous acceptance.'});}
 const modelContrasts=[];for(const st of states.values())if(st.rule!=='BASELINE'){const b=states.get(st.case+'/BASELINE'),v=st.pairScores.map((x,i)=>x-b.pairScores[i]);modelContrasts.push({case:st.case,rule:st.rule,deltaToProduction:sum(v)/25,pairDifferences:v});}
 const out=[];for(const st of states.values()){delete st.seen;st.winrate=st.outcomes.WIN/st.rows;st.meanRounds=st.rounds/st.rows;st.meanEvents=st.events/st.rows;for(const k of Object.keys(st.players)){st.players[k]/=st.rows;st.enemies[k]/=st.rows;}st.firstDamage/=st.rows;st.lastDamage/=st.rows;out.push(st);}
 return {counts,raw:{gzipSha256:comp.digest('hex'),plainSha256:plain.digest('hex'),plainBytes},states:out,comparisons,modelContrasts};
}
async function main(){check(['preflight','raw'].includes(MODE),'mode preflight|raw');const frozenCheck=preflight(),raw=MODE==='raw'?await auditRaw():null;process.stdout.write(JSON.stringify({status:'PASS',purpose:MODE==='raw'?'INDEPENDENT11200_RAW_AND_PREBATTLE_AUDIT_NO_NEW_BATTLES':'INDEPENDENT_PREBATTLE_FROZEN_ACK_NO_OUTCOMES_OR_NEW_BATTLES',assertions,frozen:frozenCheck,...(raw||{}),limitations:['No battle-engine replay or compiled-runtime reproduction here.','25 mirrored pairs/cell are descriptive only, not global acceptance.','Full logs/trace arrays checked only retained pair0, two logs per cell.','turnsCount means recorded invocations, not strictly executed attacks; ordinary attempts from individual normal+crit+miss counters.']})+'\n');}
main().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});


