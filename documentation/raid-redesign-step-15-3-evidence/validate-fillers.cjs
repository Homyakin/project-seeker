const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const readline = require('node:readline');
const assert = require('node:assert/strict');
const crypto = require('node:crypto');
const allStyles = ['MAIN', 'PIERCE_ARCANE', 'RAPIER_ARCANE', 'RITUAL', 'WEAK_ONLY'];
function normalize(x, key) {
  if (Array.isArray(x)) {
    const a = x.map(v => normalize(v));
    return key === 'slots' || key === 'availableOnSlots' ? a.sort() : a;
  }
  if (x && typeof x === 'object') return Object.fromEntries(Object.keys(x).sort().map(k => [k, normalize(x[k], k)]));
  return x;
}
const equal = (a, b) => assert.deepEqual(normalize(a), normalize(b));
function close(a, b) {
  assert(Number.isFinite(a) && Number.isFinite(b));
  assert(Math.abs(a-b) <= Math.max(1, Math.abs(a), Math.abs(b)) * 1e-12, `${a} != ${b}`);
}
function sum(xs) { return xs.sort((a,b) => a-b).reduce((a,b) => a+b, 0); }
function q(actors) {
  const health = [], offense = [];
  for (const p of actors) {
    health.push(p.health / (Object.values(p.damageTakenMultipliers).reduce((a,b) => a+b, 0) / 4));
    const attack = Math.max(0, ...p.attacksByRange.map(a => Object.values(a.attack).reduce((x,y) => x+y, 0)));
    offense.push(attack * (1 + Math.max(0,Math.min(100,p.critChance))/100*(p.critMultiplier-1))
      * Math.min(p.initiative,1000)/1000);
  }
  return sum(health) * sum(offense);
}
(async () => {
  const dir = process.argv[2];
  const manifest = JSON.parse(fs.readFileSync(path.join(dir, 'manifest.json')));
  const inputs = JSON.parse(fs.readFileSync(path.join(dir, 'inputs.json')));
  const summary = JSON.parse(fs.readFileSync(path.join(dir, 'summary.json')));
  const seen = new Set(), groups = new Map(); let rows = 0;
  const rl = readline.createInterface({input: fs.createReadStream(path.join(dir, 'battles.jsonl.gz')).pipe(zlib.createGunzip())});
  for await (const line of rl) {
    const r = JSON.parse(line); ++rows;
    const {style, pair, reverse} = r, level = r.cell.raidLevel, n = r.cell.size;
    const key = `${style}/${level}/${n}/${pair}/${reverse}`;
    assert(!seen.has(key), 'Duplicate raw battle'); seen.add(key);
    assert(manifest.styles.includes(style) && manifest.levels.includes(level) && manifest.sizes.includes(n));
    assert(Number.isInteger(pair) && pair>=0 && pair<manifest.pairs);
    const seedStep = manifest.seedOffset + allStyles.indexOf(style)*1000000 + level*10000 + pair;
    equal(r.generationSeed, manifest.generationRoot + seedStep); equal(r.battleSeed, manifest.battleRoot + seedStep);
    equal(r.catalog, 'SCALING_V5'); equal(r.playerSkills, 'SCALING_SKILLS_V3'); equal(r.npcSkills, 'SCALING_SKILLS_V3');
    equal(r.tempo, 'ACCELERATED_V1'); equal(r.calibrationMetric, 'NEUTRAL_TEAM_V1');
    assert(r.members.length===n && r.playerBasis.length===n);
    const expected = inputs[style].inputsBySize[n]; assert(expected.length===n);
    equal(expected[0], inputs[style].fixedFirstMember);
    for (let i=0; i<n; i++) {
      equal(r.members[i].build, expected[i].build); equal(r.members[i].position, expected[i].position);
      equal(r.members[i].tactic, expected[i].tactic);
      equal(r.members[i].sourceItems, expected[i].items); equal(r.members[i].combatItems, expected[i].items);
      assert(r.playerBasis[i].health>0);
      if (style==='WEAK_ONLY' || i>0) {
        equal(r.playerBasis[i].health,840); equal(r.playerBasis[i].skills,[]);
        const occupied=[];
        for (const item of r.members[i].sourceItems) {
          equal(item.enhanceLevel,0); equal(item.rarity,'COMMON'); equal(item.modifier,null);
          occupied.push(...item.object.slots);
        }
        assert(occupied.length===7 && new Set(occupied).size===7 && r.members[i].sourceItems.length===7);
      }
    }
    equal(r.playerBasis[0].skills, inputs[style].firstSkills);
    const count=Math.max(2,n), front=Math.floor(count/2);
    assert(r.enemyBasis.length===count);
    equal(r.actualEnemyPositions, Array.from({length:count},(_,i)=>i<front?'FRONT':'BACK'));
    for (const e of r.enemyBasis) {
      assert(e.health>0); equal(e.skills.length,1); equal(e.skills[0].points,4); equal(e.skills[0].formulaVersion,'SCALING_SKILLS_V3');
    }
    const d=r.generationDiagnostics, p=d.parameters;
    equal(d.assessmentVersion,'NEUTRAL_TEAM_V1'); equal(p.enemyCount,count); equal(p.frontCount,front);
    close(p.sizeCorrection,.7+.3*(3/count)**2);
    const target=d.participantAssessment*p.powerBonus*p.groupSizeScaling*p.encounterCorrection*p.sizeCorrection;
    close(r.calibration.targetPower,target);
    const init=r.initState.personagesById;
    close(q(r.playerBasis.map(p=>init[p.id])),d.participantAssessment);
    close(q(r.enemyBasis.map(p=>init[p.id])),r.calibration.achievedPower);
    assert(['WIN','LOSS','DRAW','TIMEOUT'].includes(r.outcome));
    const groupKey=`${style}/${level}/${n}`;
    if (!groups.has(groupKey)) groups.set(groupKey,{style,level,n,pairs:new Map(),outcomes:{WIN:0,LOSS:0,DRAW:0,TIMEOUT:0},roundSum:0,complete:0});
    const g=groups.get(groupKey); g.outcomes[r.outcome]++;
    if (!g.pairs.has(pair)) g.pairs.set(pair,[]); g.pairs.get(pair).push(r.outcome);
    if(r.outcome!=='TIMEOUT'){g.roundSum+=r.rounds;g.complete++;}
  }
  assert(rows===manifest.styles.length*manifest.levels.length*manifest.sizes.length*manifest.pairs*2);
  assert(groups.size===summary.cases.length);
  for(const c of summary.cases){
    const g=groups.get(`${c.style}/${c.level}/${c.size}`);assert(g);
    equal(c.wins,g.outcomes.WIN);equal(c.losses,g.outcomes.LOSS);equal(c.draws,g.outcomes.DRAW);equal(c.timeouts,g.outcomes.TIMEOUT);
    equal(c.pairs,manifest.pairs);
    const scores=[];
    for(let i=0;i<c.pairs;i++){
      const pair=g.pairs.get(i);assert(pair.length===2);
      scores.push(pair.includes('TIMEOUT')?null:pair.reduce((a,o)=>a+(o==='WIN'?.5:o==='DRAW'?.25:0),0));
    }
    equal(c.pairScores,scores);
    const complete=scores.filter(x=>x!==null),mean=complete.reduce((a,b)=>a+b,0)/complete.length;
    close(c.pairScoreMean,mean);equal(c.completePairs,complete.length);
    close(c.winrate,c.wins/(2*c.pairs));close(c.meanRounds,g.roundSum/g.complete);
    const half=Math.sqrt(Math.log(40)/(2*complete.length));
    equal(c.pointwisePairScore95Hoeffding,[Math.max(0,mean-half),Math.min(1,mean+half)]);
  }
  for(const d of summary.pairedAppendDifferences){
    const base=summary.cases.find(c=>c.style===d.style&&c.level===d.level&&c.size===1).pairScores;
    const added=summary.cases.find(c=>c.style===d.style&&c.level===d.level&&c.size===d.size).pairScores;
    const delta=added.flatMap((x,i)=>x===null||base[i]===null?[]:[x-base[i]]);
    equal(d.pairDifferences,delta);equal(d.completePairs,delta.length);
    const mean=delta.reduce((a,b)=>a+b,0)/delta.length,half=Math.sqrt(2*Math.log(40)/delta.length);
    close(d.meanDifference,mean);equal(d.pointwisePairedDifference95Hoeffding,[Math.max(-1,mean-half),Math.min(1,mean+half)]);
  }
  const hash=crypto.createHash('sha256').update(fs.readFileSync(path.join(dir,'battles.jsonl.gz'))).digest('hex');
  process.stdout.write(JSON.stringify({status:'VALID',conditions:groups.size,battles:rows,rawSha256:hash,
    timeouts:summary.cases.reduce((a,c)=>a+c.timeouts,0),draws:summary.cases.reduce((a,c)=>a+c.draws,0)})+'\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
