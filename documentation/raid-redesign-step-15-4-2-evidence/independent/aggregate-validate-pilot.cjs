'use strict';
// Independent complete raw arithmetic/statistical audit; stdout only, no battles/Java/subprocesses.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),zlib=require('node:zlib'),readline=require('node:readline'),assert=require('node:assert/strict');
const MOD=process.argv[2]||'/tmp/seeker-step15-4-2-causal.1HOgPK';
const HIST=process.argv[3]||'/tmp/seeker-step1542-historical.77NJth';
const read=(root,file)=>JSON.parse(fs.readFileSync(path.join(root,file),'utf8'));
const inputs=read(MOD,'frozen-preparation/inputs.json'),manifest=read(MOD,'frozen-preparation/manifest.json'),specs=read(MOD,'frozen-preparation/fixture-manifest.json'),hManifest=read(HIST,'preparation-frozen/manifest.json');
const ATT=['SLASH','BLUNT','PIERCE','MAGICAL'],DEF=['CLOTH','LEATHER','PLATE','ARCANE'],MATRIX={CLOTH:[.75,1.25,.9,1.1],LEATHER:[.9,1.1,1.25,.75],PLATE:[1.25,.75,1.1,.9],ARCANE:[1.1,.9,.75,1.25]},POINTS={COMMON:0,UNCOMMON:1,RARE:2,EPIC:3,LEGENDARY:4};
let assertions=0;const counts={rows:0,players:0,npcs:0,preparedFromGear:0,exactRngActors:0,fullLogs:0,fullLogEvents:0,focusedLogs:0,prefixes:0,mirrors:0,historicalCurrentPrepared:0};
function check(x,msg){assertions++;assert.ok(x,msg);}
function close(a,b,msg){check(Number.isFinite(a)&&Number.isFinite(b)&&Math.abs(a-b)<=3e-12*Math.max(1,Math.abs(a),Math.abs(b)),msg+': '+a+' / '+b);}
function canon(v,k){if(Array.isArray(v)){const a=v.map(x=>canon(x));return ['slots','availableOnSlots'].includes(k)?a.sort():a;}if(v&&typeof v==='object')return Object.fromEntries(Object.keys(v).sort().map(k=>[k,canon(v[k],k)]));return v;}
const digest=v=>crypto.createHash('sha256').update(JSON.stringify(canon(v))).digest('hex');
const hash=v=>crypto.createHash('sha256').update(v).digest('hex');
const sum=a=>a.reduce((s,x)=>s+x,0),stable=a=>sum(a.toSorted((a,b)=>a-b));
const q=l=>1+((l-10)*.06+.14),seed=(ns,g,p)=>crypto.createHash('sha256').update(ns+'/'+g+'/'+p).digest().readUIntBE(0,6);
// java.util.Random and RandomSupport.nextInt(origin,bound), checked against JDK25 src.zip.
class JavaRandom {
  constructor(seed) { this.state = (BigInt(seed) ^ 0x5deece66dn) & ((1n << 48n) - 1n); }
  bits(n) {
    this.state = (this.state * 0x5deece66dn + 11n) & ((1n << 48n) - 1n);
    return Number(this.state >> BigInt(48 - n));
  }
  long() {
    const high = BigInt.asIntN(32, BigInt(this.bits(32)));
    const low = BigInt.asIntN(32, BigInt(this.bits(32)));
    return BigInt.asIntN(64, (high << 32n) + low);
  }
  actor() {
    const high = (this.long() & 0xffffffffffff0fffn) | 0x4000n;
    const low = (this.long() & 0x3fffffffffffffffn) | 0x8000000000000000n;
    const h = BigInt.asUintN(64, high).toString(16).padStart(16, '0');
    const l = BigInt.asUintN(64, low).toString(16).padStart(16, '0');
    let gauge;
    for (;;) {
      const u = Math.floor(this.bits(32) / 2); gauge = u % 501;
      if (u + 500 - gauge <= 2147483647) break;
    }
    return { id: `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12)}-${l.slice(0, 4)}-${l.slice(4)}`, gauge };
  }
  unitSeed() { return Number(this.long() & ((1n << 48n) - 1n)); }
}


function mus(defenses){return ATT.map((_,i)=>{let d=0;for(const t of DEF)d+=(defenses[t]||0)*MATRIX[t][i];return 1-d/(d+500);});}
function grow(b,item){return item.enhanceLevel===0||b===0?b:item.object.progressionVersion==='V1'?b+Math.floor((item.enhanceLevel*b+30)/60):Math.floor(b*(1+item.enhanceLevel*5/100)+.5);}
function prepared(member,version,force){
 let hp=0,crit=0,dodge=0,critMultiplier=1.2,speed=0,threat=0,impact=80,maxRange=1;
 const defenses={},parts=[],skills={},items=[];
 const slots=new Set();
 for(const item of member.items){const o=item.object;
  for(const slot of o.slots){check(!slots.has(slot),'unique equipment slot');slots.add(slot);}
  hp+=grow(o.health,item);crit+=o.critChance;dodge+=o.dodgeChance;critMultiplier+=o.critMultiplier;speed+=o.speed;threat+=o.baseThreat;impact+=o.impact||0;
  if(o.defense)defenses[o.defense.defenseType]=(defenses[o.defense.defenseType]||0)+grow(o.defense.defense,item);
  for(const a of o.attacks){parts.push({...a,attack:grow(a.attack,item)});maxRange=Math.max(maxRange,a.maxRange);}
  if(item.modifier&&POINTS[item.rarity]>0)skills[item.modifier.activeEnum]=(skills[item.modifier.activeEnum]||0)+POINTS[item.rarity]*o.slots.length;
  if(o.code!==null)items.push({code:o.code,rarity:item.rarity,skill:item.modifier?item.modifier.activeEnum:null});
 }
 check(slots.size===7&&hp>0,'full live equipment');
 const attacks=Array.from({length:maxRange},(_,j)=>{const r=j+1,attack={};for(const a of parts)if(a.minRange<=r&&r<=a.maxRange)attack[a.attackType]=(attack[a.attackType]||0)+a.attack;return {range:r,attack};});
 return {hp,crit,dodge,critMultiplier,speed,threat,impact:Math.min(100,impact),range:maxRange,defenses,attacks,
  skills:Object.keys(skills).sort().map(code=>({code,points:skills[code],formulaVersion:version})),items,position:member.position,tactic:member.tactic,scalingRules:force||version!=='LEGACY_SKILLS_V1'};
}
function lp(t){
 const a=Math.max(1,sum(Object.values(t.attacks[0]?.attack||{}))),mu=sum(ATT.map((_,i)=>mus(t.defenses)[i]))/4;
 const effective=Math.max(1,a*(1+t.crit/100*(t.critMultiplier-1))),hit=Math.max(1,1-t.dodge/100),expected=effective*mu*hit;
 let attack=0,hpBonus=0;
 for(const s of t.skills){if(s.formulaVersion&&s.formulaVersion!=='LEGACY_SKILLS_V1')continue;
  const k=s.points>=8?30:s.points>=6?25:s.points>=4?20:s.points>=2?15:10;
  if(s.code==='DOUBLE_ATTACK')attack+=k*.5*(1-t.dodge/100);
  else if(s.code==='PRECISE_STRIKE')attack+=k*.5*(t.dodge/100);
  else if(s.code==='BERSERK'){const total=t.hp/expected,until=t.hp*.7/expected;attack+=(a*(k/100))*(total-until)/total;}
  else check(s.code==='KNOCKBACK'||s.code==='GUARD','only accounted legacy fixture skills');
 }
 return Math.max(1,t.hp+hpBonus)*Math.max(1,effective+attack)/mu/hit*(Math.max(1,t.speed)/1000);
}
function hd(t){const mu=sum(mus(t.defenses))/4,a=Math.max(0,...t.attacks.map(x=>sum(ATT.map(k=>x.attack[k]||0))));
 return {H:t.hp/mu,D:a*(1+Math.max(0,Math.min(1,t.crit/100))*(t.critMultiplier-1))*Math.min(t.speed,1000)/1000};}
function Q(ts){const p=ts.map(hd);return stable(p.map(x=>x.H))*stable(p.map(x=>x.D));}
function npc(g,m,version,range,w=1){
 const hp=Math.max(1,Math.ceil((g?420:280)*(m*w))),atk=Math.max(1,Math.ceil((g?70:130)*(m*w))),def=Math.max(1,Math.ceil((g?160:70)*m));
 return {hp,attacks:Array.from({length:g?1:range},(_,i)=>({range:i+1,attack:{MAGICAL:atk}})),defenses:{ARCANE:def},
 crit:g?4:8,dodge:g?3:5,critMultiplier:g?1.5:1.7,speed:g?190:160,threat:g?35:15,impact:80,range:g?1:range,position:g?'FRONT':'BACK',tactic:'THREAT',
 skills:[{code:g?'KNOCKBACK':'PRECISE_STRIKE',points:4,formulaVersion:version}],items:[]};
}
function search32(n,target,version,range){let evals=0;
 const value=m=>{evals++;return lp(npc(true,m,version,range))*Math.floor(n/2)+lp(npc(false,m,version,range))*(n-Math.floor(n/2));};
 let low=0,high=1;if(value(0)>=target)return {m:0,evals};
 while(value(high)<target){low=high;high*=2;check(high<100000,'bounded diagnostic search');}
 for(let i=0;i<32;i++){const mid=(low+high)/2;if(value(mid)>=target)high=mid;else low=mid;}
 const ld=Math.abs(value(low)-target),hd=Math.abs(value(high)-target);return {m:ld<hd?low:high,evals};
}
function hTraits(t){return {id:t.id,gauge:t.initiativeGauge,hp:t.maxHp,attacks:t.attacksByRange,defenses:t.defenses,reductions:t.damageTakenMultipliers,crit:t.critChance,dodge:t.dodgeChance,critMultiplier:t.critMultiplier,speed:t.initiative,threat:t.totalThreat,position:t.startPosition,range:t.range,tactic:t.targetingTactic,skills:t.skills.map(s=>({...s,formulaVersion:'LEGACY_SKILLS_V1'})),items:t.items,legacyPower:t.power,impact:80,scalingRules:false};}
function compareTrait(a,b,label){for(const k of ['hp','crit','dodge','speed','threat','impact','range','position','tactic'])check(a[k]===b[k],label+' '+k);close(a.critMultiplier,b.critMultiplier,label+' critMultiplier');
 for(const k of ['attacks','defenses','items'])check(digest(a[k])===digest(b[k]),label+' '+k);
 check(digest(a.skills.toSorted((a,b)=>a.code.localeCompare(b.code)))===digest(b.skills.toSorted((a,b)=>a.code.localeCompare(b.code))),label+' skills');
 mus(a.defenses).forEach((v,i)=>close(a.reductions[ATT[i]],v,label+' defense matrix'));
 close(a.legacyPower,lp(b),label+' independently recomputed legacyPower');
}
function geometry(positions,ranges,np,nr,reverse){const lines=a=>['BACK',...(a.some(x=>x==='MID'||x==='FRONT')?['MID']:[]),...(a.includes('FRONT')?['FRONT']:[])],pl=lines(positions),nl=lines(np),first=reverse?pl:nl,second=(reverse?nl:pl).toReversed(),total=first.length+second.length;
 const at=(p,r,isFirst)=>{let x=isFirst?first.indexOf(p):first.length+second.indexOf(p),near=isFirst?first.length:first.length-1;if(Math.abs(x-near)>r)x=isFirst?Math.min(first.length-1,Math.max(0,near-r)):Math.max(first.length,Math.min(total-1,near+r));return x;};
 return {p:positions.map((x,i)=>at(x,ranges[i],reverse)),n:np.map((x,i)=>at(x,nr[i],!reverse))};}
check(inputs.length===84&&manifest.battles===5040&&manifest.pairs===30&&manifest.combinedApprovedBattles===5520,'approved fixed pilot sizes');
check(hManifest.pairs===30&&digest(hManifest.levels)===digest([10,25]),'historical fixed pilot sizes');
const phases=['C0','C1','C2','C3','C4','C5','C6','C7','C8','C9','C10'];
for(const c of inputs){const p=c.phase,k=Number(p.id.slice(1));check(phases.includes(p.id)&&[10,25].includes(c.level),'approved phase/level');
 check(p.gear===(k===0?'OLD':'V5')&&p.force===(k>=2)&&p.playerSkills===(k>=3?'SCALING_SKILLS_V3':'LEGACY_SKILLS_V1')&&p.npcSkills===(k>=4?'SCALING_SKILLS_V3':'LEGACY_SKILLS_V1'),'exact phase skill/force/gear ladder');
 check(p.tempo===(k>=5&&k!==8?'ACCELERATED_V1':'BASELINE_V1')&&p.slingerRange===(k>=6?3:1)&&p.allLog4===(k===7||k>=9)&&p.actualQ===(k>=9)&&p.exactMain===(k===10),'exact phase tempo/range/g/Q ladder');
 check((k===10?[5,6]:[1,2,3,4]).includes(c.scenarioId),'approved exact/common fixture scope');
 check(c.groupId===10*c.level+([3,4].includes(c.scenarioId)?2:1),'phase-independent cohort group');
}
const inputByKey=new Map(inputs.map(c=>[c.phase.id+'/'+c.scenarioId+'/'+c.level,c]));
const states=new Map(),rowRefs=new Map(),prefixRefs=new Map(),hPreparedRefs=new Map(),rawFiles=[];
function state(phase,scenario,level){const key=phase+'/'+scenario+'/'+level;if(!states.has(key))states.set(key,{phase,scenario,level,rows:0,wins:0,losses:0,draws:0,rounds:0,pairScores:Array(30).fill(0),seen:new Set(),members:null,geometry:null,focus:{logs:0,damageTargets:{},attemptTargets:{},survivalByRole:{},moves:0,forcedMoves:0},representative:null});return states.get(key);}
function statSum(x){return {recordedTurns:x.turnsCount,normalAttempts:x.normalAttackCount+x.critsCount+x.missesCount,normalDamage:x.normalDamageDealt,criticalDamage:x.critDamageDealt,skillDamage:x.skillDamageDealt,damage:x.normalDamageDealt+x.critDamageDealt+x.skillDamageDealt,remainingHp:x.remainHealth,died:x.remainHealth===0?1:0};}
function replayEvents(row,players,enemies,stats,st){const hp=Object.fromEntries([...players,...enemies].map(x=>[x.id,x.hp])),max={...hp},positions=Object.fromEntries(Object.values(row.initState.personagesById).map(x=>[x.id,x.lineIndex])),events=row.actionLog.events;
 const roles=Object.fromEntries([...players.map((x,i)=>[x.id,'P'+i]),...enemies.map((x,i)=>[x.id,i<Math.floor(enemies.length/2)?'GUARDIAN':'SLINGER'])]);
 const actual=Object.fromEntries(Object.keys(hp).map(id=>[id,{normalDamage:0,criticalDamage:0,skillDamage:0,attempts:0,taken:0,death:null}]));
 let moves=0,forced=0;
 for(const e of events){check(Number.isInteger(e.round)&&e.round>=1&&e.round<=row.rounds,'event round domain');
  if(e.type==='MovedTowardEnemy'||e.type==='PersonageForcedMove'){check(e.personageId in hp,'move known actor');positions[e.personageId]=e.newLineIndex;if(e.type==='MovedTowardEnemy')moves++;else forced++;}
  if(e.type==='AttackDodged'){check(e.attackerId in hp&&e.targetId in hp,'dodge actors');actual[e.attackerId].attempts++;if(row.pair<2){const k=roles[e.attackerId]+'->'+roles[e.targetId]+'@'+Math.abs(positions[e.attackerId]-positions[e.targetId]);st.focus.attemptTargets[k]=(st.focus.attemptTargets[k]||0)+1;}}
  if(['DamageReceived','SkillDamage','EffectDamage','ScalingSkillDamage'].includes(e.type)){
   const src=e.attackerId||e.sourceId;check(e.targetId in hp&&src in hp,'damage known actors');
   check(Number.isInteger(e.damageTaken)&&e.damageTaken>=0&&e.damageTaken<=hp[e.targetId],'damage capped to remaining HP');
   hp[e.targetId]-=e.damageTaken;check(hp[e.targetId]===e.remainingHealth,'event remaining HP arithmetic');actual[e.targetId].taken+=e.damageTaken;
   const kind=e.type==='DamageReceived'?(e.roll.crit?'criticalDamage':'normalDamage'):'skillDamage';actual[src][kind]+=e.damageTaken;
   if(e.type==='DamageReceived')actual[src].attempts++;
   if(hp[e.targetId]===0&&actual[e.targetId].death===null)actual[e.targetId].death=e.round;
   if(row.pair<2){const k=roles[src]+'->'+roles[e.targetId];st.focus.damageTargets[k]=(st.focus.damageTargets[k]||0)+e.damageTaken;
    if(e.type==='DamageReceived'){const a=k+'@'+Math.abs(positions[src]-positions[e.targetId]);st.focus.attemptTargets[a]=(st.focus.attemptTargets[a]||0)+1;}}
  }
  if(e.type==='PersonageHealed'){check(e.personageId in hp&&e.amount>=0,'healing known actor');hp[e.personageId]=Math.min(max[e.personageId],hp[e.personageId]+e.amount);check(hp[e.personageId]===e.remainingHealth,'healing HP arithmetic');}
 }
 [...players,...enemies].forEach((a,i)=>{const s=statSum(stats[i]),v=actual[a.id];check(hp[a.id]===s.remainingHp,'full log final HP');for(const k of ['normalDamage','criticalDamage','skillDamage'])check(v[k]===s[k],'full event damage matches individual stat '+k);check(v.attempts===s.normalAttempts,'full event normal attempts match individual stat');
  if(row.pair<2){const role=roles[a.id],entry=st.focus.survivalByRole[role]||(st.focus.survivalByRole[role]={observations:0,observedRounds:0,censored:0});entry.observations++;entry.observedRounds+=v.death===null?row.rounds:v.death;entry.censored+=v.death===null?1:0;}});
 check(events.length===row.actionCount&&moves===row.moves&&forced===row.forcedMoves,'full event/move counts');
 const saved=row.actionSha256||row.actionLogSHA256;check(hash(JSON.stringify(row.actionLog))===saved,'whole serialized event log SHA');
 if(row.pair<2){st.focus.logs++;st.focus.moves+=moves;st.focus.forcedMoves+=forced;counts.focusedLogs++;}
 counts.fullLogs++;counts.fullLogEvents+=events.length;
}
async function stream(root,historical){
 const file=path.join(root,'screen/battles.jsonl.gz'),compressed=crypto.createHash('sha256'),plain=crypto.createHash('sha256');let rows=0,bytes=0;
 const f=fs.createReadStream(file);f.on('data',x=>compressed.update(x));const g=f.pipe(zlib.createGunzip());g.on('data',x=>{plain.update(x);bytes+=x.length;});
 const lines=readline.createInterface({input:g,crlfDelay:Infinity});
 for await(const line of lines){const row=JSON.parse(line),phase=historical?'H':row.phase,scenario=historical?row.fixtureId:row.scenarioId,level=row.level,c=inputByKey.get((historical?'C0':phase)+'/'+scenario+'/'+level),p=c.phase,st=state(phase,scenario,level);
  check(c&&row.case.scenarioId===scenario&&row.case.level===level&&row.case.groupId===c.groupId,'raw case and frozen metadata');
  if(!historical)check(digest(row.case)===digest(c),'whole modern frozen input retained');
  check(Number.isInteger(row.pair)&&row.pair>=0&&row.pair<30&&typeof row.reverse==='boolean','pair/reverse domain');
  const index=row.pair+'/'+row.reverse;check(!st.seen.has(index),'unique pair/orientation');st.seen.add(index);
  check(row.generationSeed===seed(manifest.generationNamespace,c.groupId,row.pair)&&row.battleSeed===seed(manifest.battleNamespace,c.groupId,row.pair),'SHA48 phase-independent GEN/BATTLE seed');
  check(row.outcome!=='TIMEOUT'&&row.initState&&Number.isInteger(row.rounds)&&row.rounds>0&&row.rounds<=2000,'completed battle, no timeout');
  const players=historical?row.playersPrepared.map(hTraits):row.playerTraits,enemies=historical?row.npcPrepared.map(hTraits):row.enemyTraits,n=Math.max(2,c.members.length),F=Math.floor(n/2);
  check(players.length===c.members.length&&enemies.length===n,'same count model and player input');
  const expectedP=c.members.map(m=>prepared(m,historical?'LEGACY_SKILLS_V1':p.playerSkills,historical?false:p.force));
  const rng=new JavaRandom(row.generationSeed);
  players.forEach((a,i)=>{compareTrait(a,expectedP[i],'prepared player');const r=rng.actor();check(a.id===r.id&&a.gauge===r.gauge,'exact player GEN UUID/gauge');counts.preparedFromGear++;counts.exactRngActors++;});
  const actualPlayerPower=sum(players.map(lp)),actualEnemyPower=sum(enemies.map(lp)),isQ=!historical&&p.actualQ,group=1+Math.log(n)/Math.log((!historical&&p.allLog4)||n>7?4:2.2)*.05;
  let target,m,evals=0;
  if(isQ){const h=.7+.3*Math.pow(3/n,2);target=Q(players)*q(level)*group*.5900075417594784*h;m=row.calibration.multiplier;close(row.calibration.result.achievedPower,Q(enemies),'actual production achievedQ');}
  else{target=actualPlayerPower*q(level)*group*.77;const s=search32(n,target,historical?'LEGACY_SKILLS_V1':p.npcSkills,historical?1:p.slingerRange);m=s.m;evals=s.evals;for(let i=0;i<evals*2;i++)rng.actor();}
  enemies.forEach((a,i)=>{const w=isQ?n/(2*(i<F?F:n-F)):1,b=npc(i<F,m,historical?'LEGACY_SKILLS_V1':p.npcSkills,historical?1:p.slingerRange,w);compareTrait(a,b,'NPC actual factory stats');const r=rng.actor();check(a.id===r.id&&a.gauge===r.gauge,'exact NPC GEN UUID/gauge including original trial consumption');counts.exactRngActors++;});
  if(!historical){for(const [key,v]of Object.entries({targetPower:target,multiplier:m,groupScaling:group,q:q(level),actualPlayerLegacyPower:actualPlayerPower,actualEnemyLegacyPower:actualEnemyPower}))close(row.calibration[key],v,'calibration '+key);
   check(row.diagnostics.globalScalingSkillOrder===(p.force||p.playerSkills!=='LEGACY_SKILLS_V1'||p.npcSkills!=='LEGACY_SKILLS_V1'),'global mode diagnostic');
   check(digest(row.diagnostics.actualPlayerScalingRules)===digest(players.map(x=>x.scalingRules))&&digest(row.diagnostics.actualEnemyScalingRules)===digest(enemies.map(x=>x.scalingRules)),'per-actor mode diagnostics');
  }
  const byId=row.initState.personagesById;check(Object.keys(byId).length===players.length+enemies.length,'all exact initial actors');
  const geom=geometry(players.map(x=>x.position),players.map(x=>x.range),enemies.map(x=>x.position),enemies.map(x=>x.range),row.reverse);
  [...players,...enemies].forEach((a,i)=>{const x=byId[a.id];check(x&&x.health===a.hp&&x.initiativeGauge===a.gauge&&x.initiative===a.speed,'init HP/gauge/actor traits');check(x.firstTeam===(i<players.length?row.reverse:!row.reverse),'init side');
   check(x.lineIndex===(i<players.length?geom.p[i]:geom.n[i-players.length]),'independent initial geometry');check(digest(x.attacksByRange)===digest(a.attacks)&&digest(x.defenses)===digest(a.defenses),'init actual attack/defense');});
  const stats=[...row.playerStats,...row.enemyStats];check(stats.length===players.length+enemies.length,'individual stats full array');
  stats.forEach((s,i)=>{check(Number.isInteger(s.remainHealth)&&s.remainHealth>=0&&s.remainHealth<=[...players,...enemies][i].hp&&s.initialHealth===[...players,...enemies][i].hp,'individual final HP valid');for(const k of ['normalAttackCount','critsCount','missesCount','turnsCount','normalDamageDealt','critDamageDealt','skillDamageDealt'])check(Number.isInteger(s[k])&&s[k]>=0,'individual counter domain');});
  const pa=row.playerStats.some(x=>x.remainHealth>0),na=row.enemyStats.some(x=>x.remainHealth>0);check(!(pa&&na),'completed sides not both alive');check(row.outcome===(pa?'WIN':na?'LOSS':'DRAW'),'outcome independently from final HP');
  const prepDigest=digest({players:players.map(({id,gauge,hp,attacks,defenses,crit,dodge,critMultiplier,speed,threat,position,range,tactic,skills,items,legacyPower})=>({id,gauge,hp,attacks,defenses,crit,dodge,critMultiplier,speed,threat,position,range,tactic,skills,items,legacyPower})),enemies:enemies.map(({id,gauge,hp,attacks,defenses,crit,dodge,critMultiplier,speed,threat,position,range,tactic,skills,items,legacyPower})=>({id,gauge,hp,attacks,defenses,crit,dodge,critMultiplier,speed,threat,position,range,tactic,skills,items,legacyPower}))});
  const mirrorKey=phase+'/'+scenario+'/'+level+'/'+row.pair;if(rowRefs.has(mirrorKey)){check(rowRefs.get(mirrorKey)===prepDigest,'fresh mirrored prepared actors identical');rowRefs.delete(mirrorKey);counts.mirrors++;}else rowRefs.set(mirrorKey,prepDigest);
  const prefixKey=phase+'/'+level+'/'+([1,2,5,6].includes(scenario)?'T':'S')+'/'+row.pair+'/'+row.reverse,baseLength=[1,2,5,6].includes(scenario)?3:1;
  const pref=digest(players.slice(0,baseLength));if([1,3,5].includes(scenario))prefixRefs.set(prefixKey,pref);else{check(prefixRefs.get(prefixKey)===pref,'unchanged initial player prefix');counts.prefixes++;}
  if(historical)hPreparedRefs.set(scenario+'/'+level+'/'+row.pair+'/'+row.reverse,prepDigest);else if(phase==='C0'){check(hPreparedRefs.get(scenario+'/'+level+'/'+row.pair+'/'+row.reverse)===prepDigest,'true H/current C0 prepared exact across all seeds');counts.historicalCurrentPrepared++;}
  if(!st.members)st.members=[...players,...enemies].map((a,i)=>({role:i<players.length?'P'+i:i-players.length<F?'GUARDIAN':'SLINGER',recordedTurns:0,normalAttempts:0,normalDamage:0,criticalDamage:0,skillDamage:0,damage:0,remainingHp:0,died:0}));
  stats.forEach((s,i)=>{const vals=statSum(s);for(const k of Object.keys(vals))st.members[i][k]+=vals[k];});
  if(row.actionLog)replayEvents(row,players,enemies,stats,st);else check(row.pair>=2&&!historical,'promised focused full log retained');
  if(row.pair===0&&!row.reverse){st.geometry={playerLines:geom.p,enemyLines:geom.n,distances:geom.p.map(x=>geom.n.map(y=>Math.abs(x-y))),playerRanges:players.map(x=>x.range)};
   st.representative={target,multiplier:m,playerLegacyPower:actualPlayerPower,enemyLegacyPower:actualEnemyPower,playerQ:Q(players),enemyQ:Q(enemies),players:players.map(x=>({hp:x.hp,attacks:x.attacks,speed:x.speed,legacyPower:x.legacyPower})),enemies:enemies.map(x=>({hp:x.hp,attacks:x.attacks,defenses:x.defenses}))};}
  st.rows++;st.rounds+=row.rounds;if(pa){st.wins++;st.pairScores[row.pair]+=.5;}else if(na)st.losses++;else{st.draws++;st.pairScores[row.pair]+=.25;}
  counts.rows++;counts.players+=players.length;counts.npcs+=enemies.length;rows++;
 }
 check(rows===(historical?480:5040),'complete immutable raw stream');
 rawFiles.push({historical,rows,gzipSha256:compressed.digest('hex'),plainSha256:plain.digest('hex'),plainBytes:bytes});
}
function vec(phase,scenario,level){const s=states.get(phase+'/'+scenario+'/'+level);check(s&&s.rows===60,'complete cell');return s.pairScores;}
function describe(v,half){const mean=sum(v)/v.length;return {mean,min:Math.min(...v),max:Math.max(...v),pointwise95Hoeffding:[Math.max(-half.support,mean-half.width),Math.min(half.support,mean+half.width)],interpretation:'Descriptive30-pair pilot; no simultaneous family/acceptance/unique cause claim.'};}
async function main(){
 await stream(HIST,true);await stream(MOD,false);check(counts.rows===5520&&states.size===92&&rowRefs.size===0,'all approved executions/cells/mirrors');
 const out=[];
 for(const st of states.values()){const n=st.rows;check(n===60&&st.seen.size===60,'all30fresh pairs complete');st.winrate=st.wins/n;st.pairMean=sum(st.pairScores)/30;st.meanRounds=st.rounds/n;st.members=st.members.map(x=>Object.fromEntries(Object.entries(x).map(([k,v])=>[k,k==='role'?v:v/n])));delete st.seen;out.push(st);}
 const modern=read(MOD,'screen/summary.json');check(modern.cases.length===84,'all producer summaries');
 for(const saved of modern.cases){const c=saved.case,s=states.get(c.phase.id+'/'+c.scenarioId+'/'+c.level);check(digest(saved.pairScores)===digest(s.pairScores),'raw pair scores equal saved summary');close(saved.winrate,s.winrate,'saved winrate');close(saved.pairMean,s.pairMean,'saved mean');close(saved.meanRounds,s.meanRounds,'saved rounds');for(const [k,key]of [['WIN','wins'],['LOSS','losses'],['DRAW','draws']])check((saved.outcomes[k]||0)===s[key],'saved outcome totals');}
 const old=read(HIST,'screen/summary.json');check(old.rows===480&&old.cases.length===8,'historic summary count');for(const x of old.cases){const s=states.get('H/'+x.fixtureId+'/'+x.level);check(x.wins===s.wins&&x.losses===s.losses&&x.draws===s.draws&&x.timeouts===0,'historic saved outcomes');close(x.winRate,s.winrate,'historic rate');close(x.meanRounds,s.meanRounds,'historic rounds');}
 const append=[],av=new Map();for(const phase of ['H',...phases])for(const level of [10,25])for(const join of phase==='C10'?['T']:['T','S']){const base=phase==='C10'?5:join==='T'?1:3,added=base+1,a=vec(phase,base,level),b=vec(phase,added,level),v=b.map((x,i)=>x-a[i]);av.set(phase+'/'+join+'/'+level,v);append.push({phase,join,level,base,added,baselineMean:sum(a)/30,addedMean:sum(b)/30,...describe(v,{support:1,width:Math.sqrt(2*Math.log(40)/30)}),pairDifferences:v});}
 const did=[];for(const [from,to]of [['H','C0'],['C0','C1'],['C1','C2'],['C2','C3'],['C3','C4'],['C4','C5'],['C5','C6'],['C6','C7'],['C4','C8'],['C7','C9'],['C9','C10'],['H','C9']])for(const level of [10,25])for(const join of to==='C10'?['T']:['T','S']){const a=av.get(from+'/'+join+'/'+level),b=av.get(to+'/'+join+'/'+level),v=b.map((x,i)=>x-a[i]);did.push({from,to,join,level,...describe(v,{support:2,width:Math.sqrt(8*Math.log(40)/30)}),pairDifferences:v});}
 const interactions=[];for(const level of [10,25])for(const scenario of [1,2,3,4]){const a=vec('C4',scenario,level),b=vec('C5',scenario,level),c=vec('C8',scenario,level),d=vec('C6',scenario,level),v=a.map((x,i)=>(d[i]-b[i])-(c[i]-x));interactions.push({level,scenario,rangeEffectAtTempo1:sum(c.map((x,i)=>x-a[i]))/30,rangeEffectAtTempo175:sum(d.map((x,i)=>x-b[i]))/30,interaction:sum(v)/30,pairInteractions:v,interpretation:'Conditional tempo/range interaction, descriptive only.'});}
 process.stdout.write(JSON.stringify({status:'PASS',purpose:'INDEPENDENT_FULL5520_PILOT_AUDIT_NO_NEW_BATTLES',assertions,counts,rawFiles,states:out,append,did,interactions,
 validatorSha256:hash(fs.readFileSync(__filename)),limitations:['n30 pilot is descriptive; not pooled with fresh confirmation.','recordedTurns includes queued/dead invocations; not strictly executed actions.','normalAttempts is normalAttackCount+critsCount+missesCount, not event count or trace count.','Full BattleEvent logs validated where retained; traceEvents array absent despite historical modern frozen manifest wording.','All phase switches are conditional packages; C9/C10 not pure metric/Guard switches.','No battle engine replay, new formulas, production,target or DB changes.']})+'\n');
}
main().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});

