'use strict';
// Read-only source checks; no Java/battles/current target reads or subprocesses.
const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto'),assert=require('node:assert/strict');
const ROOT=process.argv[2]||'/home/user/projects/project-seeker';
const OLD=process.argv[3]||'/tmp/seeker-step1542-historical.77NJth';
const read=p=>fs.readFileSync(p,'utf8'),sha=b=>crypto.createHash('sha256').update(b).digest('hex');
const basis=JSON.parse(read(path.join(OLD,'source-basis.json')));
const git=JSON.parse(read(path.join(__dirname,'historical-git-sha256.json')));
let assertions=0;const check=(x,m)=>{assertions++;assert.ok(x,m);};
check(basis.commit===git.commit&&basis.commit==='6150d562546cca22bed139c4d0fa23456e4d801e','approved historical commit');
check(basis.checks.length===git.orderedSha256.length&&basis.checks.length===86,'complete public source/resource list');
for(let i=0;i<basis.checks.length;i++){
 const x=basis.checks[i],file=path.join(OLD,'basis-6150',x.path);
 check(sha(fs.readFileSync(file))===x.sha256,'historical file unchanged '+x.path);
 check(x.sha256===git.orderedSha256[i],'historical file exact git blob '+x.path);
}
const jars=read(path.join(OLD,'dependencies.classpath')).trim().split(':');
check(jars.length===79&&jars.every(x=>x.includes('/.m2/')&&x.endsWith('.jar')&&!x.includes('/target/')&&fs.existsSync(x)),'jar-only historic dependency classpath without current target');
const stub=read(path.join(OLD,'linkage-only/ru/homyakin/seeker/locale/LocalizationCoverage.java'));
check(stub.includes('throw new UnsupportedOperationException(')&&!stub.includes('return '),'unrelated UI stub aborts, supplies no combat value');
const JAVA='game/src/main/java/ru/homyakin/seeker/';
const current=p=>read(path.join(ROOT,JAVA,p)),old=p=>read(path.join(OLD,'basis-6150',JAVA,p));
const person=current('game/battle/BattlePersonage.java'),context=current('game/battle/BattleContext.java'),
 battle=current('game/battle/Battle.java'),turn=current('game/battle/VersionedBattleTurn.java'),
 generator=current('game/event/raid/generator/MyconidColonyGenerator.java'),
 assess=current('game/event/pve/PveTeamAssessment.java');
for(const p of ['game/battle/targeting/TargetingTactic.java','game/battle/targeting/TargetingMath.java','game/battle/targeting/TargetingTacticCoefficients.java'])
 check(current(p)===old(p),'targeting source itself unchanged '+p);
check(context.includes('firstTeam.stream().anyMatch(BattlePersonage::usesScalingCombatRules)')&&context.includes('secondTeam.stream().anyMatch(BattlePersonage::usesScalingCombatRules)'),'one force flag activates team-global common engine');
check(person.includes('if (context.usesScalingSkillOrder())')&&person.includes('return VersionedBattleTurn.process(this, context, log, round);'),'all actors route to common turn once global flag active');
check(battle.includes('? random.shuffle("turn-order:" + rounds, movers)')&&battle.includes(': RandomUtils.shuffle(movers)'),'global flag also selects turn-order random policy');
for(const token of ['selectInitialTarget(self, enemyTeam, context)','context.random().pickWeighted(','self.versionedCriticalSucceeds(context.random())',
 'finalTarget.versionedDodgeSucceeds(context.random(), self)','updateAttemptThreat(','context.moveTowardEnemy(self)',
 'self.expireLegacyRangeBonusesForVersionedTurn()','processPeriodicDamage(self, context, log, round)',
 'self.applyLegacyTurnStartSkill(ActiveEnum.BERSERK','self.applyLegacyTurnEndSkill(ActiveEnum.SELF_HEAL',
 'applyLegacySkill(ActiveEnum.PRECISE_STRIKE','applyLegacySkill(ActiveEnum.DOUBLE_ATTACK'])
 check(turn.includes(token),'common engine covers '+token);
check(person.includes('if (formulaVersion.isScaling())')&&person.includes('scalingSkillPoints.put(entry.getKey(), entry.getValue());')
 &&person.includes('itemSkills.add(skill);'),'scaling skill book separate from legacy rating objects');
const powerMethod=t=>t.slice(t.indexOf('    public double '+(t.includes('    public double legacyPower()')?'legacyPower':'power')+'()'),t.lastIndexOf('    private record Target('));
const normalizedOld=powerMethod(old('game/battle/BattlePersonage.java')).replace('public double power()','public double legacyPower()')
 .replaceAll('powerMaxHealth','legacyPowerMaxHealth').replaceAll('powerSlotOneAttackSum','legacyPowerSlotOneAttackSum').replaceAll('REQUIRED_SPEED','CombatRules.INITIATIVE_THRESHOLD').replaceAll(/\s+/g,'');
check(normalizedOld===powerMethod(person).replaceAll(/\s+/g,''),'old power/current legacyPower identical formula after declared identifier renames');
check(person.includes('for (var skill : itemSkills)')&&person.includes('skill instanceof AttackPowerSkill'),'only legacy rating objects priced');
check(generator.includes('TEAM_CALIBRATION_CORRECTION = 0.5900075417594784')&&generator.includes('GROUP_CORRECTION_FLOOR = 0.7'),'actual production Q coefficients frozen');
check(generator.includes('PveTeamAssessment.assess(personages)')&&generator.includes('formation.frontWeight()')&&generator.includes('formation.backWeight()'),'production Q and weighted final factory active for V3');
check(assess.includes('stableSum(effectiveHealth) * stableSum(damageRates)'),'team Q contains cross actor terms');
check(assess.includes('reductionSum += reduction;'),'production Q mu order follows enum, not separate-probe sorted mu');
check(generator.includes('LEGACY_POWER_CALIBRATION_CORRECTION = 0.77')&&generator.includes('MathUtils.log(2.2, myconidCount)'),'old controller coefficients retained in explicit legacy path');
const results={status:'PASS',purpose:'STATIC_SOURCE_CAUSAL_SCOPE_AUDIT_NO_JAVA_OR_BATTLES',assertions,historicalGitFiles:basis.checks.length,
 historicalClasspathJars:jars.length,sourceHashes:Object.fromEntries([
 ['BattlePersonage',person],['BattleContext',context],['Battle',battle],['VersionedBattleTurn',turn],['Myconid',generator],['PveTeamAssessment',assess]
 ].map(([k,v])=>[k,sha(v)])),
 conclusions:[
 'C2 force common flag switches the global turn execution package, not merely reaction order or player skill values.',
 'The package includes candidate selection, normal attack access, crit/dodge/damage random routing, threat accounting, legacy hook routing, periodic/cooldown/movement handling and turn-order random policy.',
 'C3 player V3 removes old offensive rating contributions from target even before combat; new skill execution and induced calibration are not isolated by a single switch.',
 'C4 NPC V3 removes old NPC rating contribution, potentially raising searched m for fixed target; knockback was already rated zero, precise old contribution is small, so no magnitude prediction follows.',
 'C9 production Q is a package of metric/weights/correction/search isolation, not a pure metric switch.',
 'Exact MAIN versus common projection changes both GUARD and battle_staff/staff+orb; it is not a pure GUARD effect.'
 ],limitations:[
 'Static token checks verify routing/source invariants, not runtime branch coverage or a proof of winrate causation.',
 'Observed damage/actions/initial geometry are still required; no battle output is inspected here.',
 'Historical commit6150 includes declared RNG/iteration stabilization versus pre-instrumentation source.'
 ]};
process.stdout.write(JSON.stringify(results,null,2)+'\n');

