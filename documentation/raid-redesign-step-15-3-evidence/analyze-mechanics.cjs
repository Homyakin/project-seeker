const fs=require('node:fs'),path=require('node:path'),zlib=require('node:zlib'),readline=require('node:readline'),assert=require('node:assert/strict');
const root=process.argv[2]||'/tmp/seeker-step15-3-fillers.HXdOad';
function mean(xs){return xs.reduce((a,b)=>a+b,0)/xs.length;}
function parts(p){
  const mu=Object.values(p.damageTakenMultipliers).reduce((a,b)=>a+b,0)/4;
  const attack=Math.max(...p.attacksByRange.map(r=>Object.values(r.attack).reduce((a,b)=>a+b,0)));
  const crit=1+Math.max(0,Math.min(100,p.critChance))/100*(p.critMultiplier-1);
  const frequency=Math.min(p.initiative,1000)/1000;
  return {health:p.health,meanDamageTakenMultiplier:mu,H:p.health/mu,maxAttack:attack,criticalExpectation:crit,
    speed:p.initiative,frequency,D:attack*crit*frequency,skills:p.skills,attacksByRange:p.attacksByRange};
}
function staticData(r){
  const init=r.initState.personagesById;
  const players=r.playerBasis.map(p=>parts(init[p.id]));
  const npcs=r.enemyBasis.map(p=>{const s=init[p.id];return {role:p.declaredPosition==='FRONT'?'GUARDIAN':'SLINGER',
    health:s.health,attack:Object.values(s.attacksByRange[0].attack).reduce((a,b)=>a+b,0),defenses:s.defenses,
    speed:s.initiative,skills:s.skills};});
  return {players,npcs,playerQ:r.generationDiagnostics.participantAssessment,
    target:r.calibration.targetPower,achieved:r.calibration.achievedPower,m:r.calibration.multiplier,
    parameters:r.generationDiagnostics.parameters,strongToEnemies:r.enemyBasis.map(p=>Math.abs(
      init[r.playerBasis[0].id].lineIndex-init[p.id].lineIndex))};
}
(async()=>{
  const results=[];
  for(const [folder,style] of [['confirm-main','MAIN'],['confirm-pierce','PIERCE_ARCANE'],['confirm-ritual','RITUAL']]){
    const dir=path.join(root,folder),s=JSON.parse(fs.readFileSync(path.join(dir,'summary.json'))),groups=new Map();
    const rl=readline.createInterface({input:fs.createReadStream(path.join(dir,'battles.jsonl.gz')).pipe(zlib.createGunzip())});
    for await(const line of rl){
      const r=JSON.parse(line);if(![1,2].includes(r.cell.size))continue;
      const size=r.cell.size;if(!groups.has(size))groups.set(size,{static:staticData(r),rows:[],distances:new Set()});
      const g=groups.get(size),now=staticData(r);
      assert.deepEqual(now,g.static);g.distances.add(JSON.stringify(now.strongToEnemies));
      const p=r.playerTotals,e=r.enemyTotals;
      g.rows.push({win:r.outcome==='WIN'?1:0,rounds:r.rounds,playerRecordedDamage:p.normalDamage+p.critDamage+p.skillDamage,
        enemyRecordedDamage:e.normalDamage+e.critDamage+e.skillDamage,playerTurns:p.turns,enemyTurns:e.turns,
        playerSurvivors:p.survivors,enemySurvivors:e.survivors,playerRemainHealth:p.remainHealth,enemyRemainHealth:e.remainHealth,
        forcedMoves:r.forcedMoves,allNormalAttempts:r.normalAttempts});
    }
    const a=groups.get(1),b=groups.get(2),strong=a.static.players[0],weak=b.static.players[1];
    assert.deepEqual(strong,b.static.players[0]);
    const hSdW=strong.H*weak.D,hWdS=weak.H*strong.D,hWdW=weak.H*weak.D;
    const ca=s.cases.find(c=>c.size===1),cb=s.cases.find(c=>c.size===2),diff=s.pairedAppendDifferences.find(d=>d.size===2);
    const metrics=g=>Object.fromEntries(Object.keys(g.rows[0]).map(k=>[k,mean(g.rows.map(r=>r[k]))]));
    const simultaneousHalf=Math.sqrt(2*Math.log(2*3/.05)/diff.completePairs);
    results.push({style,level:ca.level,completePairs:diff.completePairs,baseWins:ca.wins,appendWins:cb.wins,
      baseWinrate:ca.winrate,appendWinrate:cb.winrate,difference:diff.meanDifference,
      pointwisePairedDifference95Hoeffding:diff.pointwisePairedDifference95Hoeffding,strong,weak,
      simultaneousThreeConfirmations95BonferroniHoeffding:{familySize:3,halfWidth:simultaneousHalf,
        interval:[Math.max(-1,diff.meanDifference-simultaneousHalf),Math.min(1,diff.meanDifference+simultaneousHalf)],
        scope:'Only three fixed N1-to-N2 confirmations on fresh offset 1000000000; not the 270-cell discovery screen or other append comparisons'},
      crossTerms:{strongSelf:strong.H*strong.D,strongHealthWeakDamage:hSdW,weakHealthStrongDamage:hWdS,weakSelf:hWdW,
        sumAdditionalQ:hSdW+hWdS+hWdW,weakOwnProductShareOfAdditionalQ:hWdW/(hSdW+hWdS+hWdW)},
      base:a.static,append:b.static,
      ratios:{playerQ:b.static.playerQ/a.static.playerQ,target:b.static.target/a.static.target,
        multiplier:b.static.m/a.static.m,neutralHealth:(strong.H+weak.H)/strong.H,
        neutralDamageRate:(strong.D+weak.D)/strong.D},actualBattleMeans:{base:metrics(a),append:metrics(b)},
      limitation:'Raw contains team totals, initial snapshots and action hashes, not individual final stats/full logs. No attribution of actual damage or turns specifically to weak member is possible.'});
  }
  process.stdout.write(JSON.stringify({purpose:'INDEPENDENT_READ_ONLY_MECHANICS_FROM_FRESH_CONFIRMED_RAW_NO_NEW_BATTLES',
    rawRoot:root,productionUnchanged:true,results},null,2)+'\n');
})().catch(e=>{process.stderr.write(e.stack+'\n');process.exitCode=1;});
