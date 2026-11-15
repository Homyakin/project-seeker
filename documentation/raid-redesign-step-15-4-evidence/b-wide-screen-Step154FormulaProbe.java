package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling.WeightedBase;
import ru.homyakin.seeker.game.event.pve.PveTeamAssessment;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

/** Outside-production candidate score using the actual private NPC factory, not a copied generator. */
public final class Step154FormulaProbe {
    private static final SkillFormulaVersion VERSION=SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final BattleTempoVersion TEMPO=BattleTempoVersion.ACCELERATED_V1;
    private static final MyconidColonyGenerator GENERATOR=new MyconidColonyGenerator();
    private static final Class<?> FORMATION=formationType();
    private static final Constructor<?> FORMATION_CONSTRUCTOR=formationConstructor();
    private static final Method FACTORY=factory();
    private static final Method MAXIMUM_ATTACK=maximumAttack();
    private static final double OLD_C=constant("TEAM_CALIBRATION_CORRECTION");
    private static final double OLD_A=constant("GROUP_CORRECTION_FLOOR");
    private static final double GROUP_COEF=constant("GROUP_SCALE_COEF");
    private static final long GENERATION_ROOT=2_042_110_815_400L;
    private static final long BATTLE_ROOT=3_042_110_815_400L;
    private static double newC;
    private static double twoAnchorA;
    private enum Model { Q_CURRENT, B_TWO_ANCHOR }
    private static final List<Model> SCREEN_MODELS=List.of(Model.B_TWO_ANCHOR);
    private record Case(int id,int groupId,String style,Tier tier,int originalSize,int level,String variant,boolean effects,List<InputMember> members) { }
    private record Prepared(List<BattlePersonage> players,List<BattlePersonage> enemies,PveCharacteristicScaling.Result calibration,double playerScore) { }

    public static void main(String[] args)throws Exception {
        if(args.length!=2||!List.of("prepare","screen").contains(args[0]))throw new IllegalArgumentException("prepare|screen NEW_OUTPUT_DIRECTORY");
        final var output=Path.of(args[1]);Files.createDirectory(output);
        final int pairs=Integer.getInteger("formula.pairs",10);
        if(pairs<1||pairs>1000)throw new IllegalArgumentException("Pairs must be1..1000");
        final var anchor=anchors();
        final var bridge=validateBridge();
        final var cases=cases();
        final var effectBridge=validateEffects();
        final var manifest=new LinkedHashMap<String,Object>();
        manifest.put("purpose","OUTSIDE_PRODUCTION_CANDIDATE_COMPARISON_NOT_BALANCE_ACCEPTANCE");
        manifest.put("factory","Current private MyconidColonyGenerator.teamColony via reflection; no copied factory, class override or production edit");
        manifest.put("formula","B_TWO_ANCHOR=sum_i,j(min(p_i,p_j)), p_i=EHP_i*normalRate_i; same exact neutral components; ascending self-products then sum (2*(N-i)-1)*p_i, zero-based i");
        manifest.put("ignored","Skills, dodge, actual matchup, opportunity to attack at best range, actual lifetime");
        manifest.put("anchorPolicy","C and a from exact actual 15.2 MID MAIN N3 L25 and STRONG MAIN N1 L5 NPC factories; no outcome fitting; same g/h/role weights and bases");
        manifest.put("anchors",anchor);manifest.put("bridgeValidation",bridge);
        manifest.put("effectsBridgeValidation",effectBridge);
        manifest.put("fixedScreen","519conditions approved before battles;306MAIN+60causal+6effects+45lowSolo+48specialized3+54highSpecialized");
        manifest.put("models",SCREEN_MODELS);manifest.put("pairs",pairs);
        manifest.put("generationRoot",GENERATION_ROOT);manifest.put("battleRoot",BATTLE_ROOT);
        manifest.put("seedOffset",Long.getLong("formula.seedOffset",0L));
        manifest.put("seedPolicy","root+offset+groupId*1000+pair; model/variant absent; one mirrored pair; fresh objects and seeded trial isolation");
        manifest.put("versions",List.of("SCALING_V5",VERSION.name(),TEMPO.name()));
        manifest.put("confidencePolicy","Pointwise Hoeffding on independent mirrored-pair scores; no shared50/50 goal");
        manifest.put("roundLimit",2000);manifest.put("conditionsPerModel",cases.size());
        Files.writeString(output.resolve("manifest.json"),json(manifest),StandardOpenOption.CREATE_NEW);
        Files.writeString(output.resolve("inputs.json"),json(cases),StandardOpenOption.CREATE_NEW);
        if(args[0].equals("prepare"))return;
        final var summaries=new ArrayList<Map<String,Object>>();
        try(var raw=writer(output.resolve("battles.jsonl.gz"))) {
            for(final var cell:cases)for(final var model:SCREEN_MODELS) {
                summaries.add(runCell(cell,model,pairs,raw));
                System.out.println("done="+summaries.size()+" "+model+" "+cell.style()+" N"+cell.members().size()+" L"+cell.level()+" "+cell.variant());
            }
        }
        Files.writeString(output.resolve("summary.json"),json(Map.of("manifest",manifest,"cases",summaries)),StandardOpenOption.CREATE_NEW);
    }

    private static Map<String,Object> anchors() {
        final var main=inputs("MAIN",Tier.MID,3,25);
        final var ps=RandomUtils.withSeed(1540,()->players(main));
        final var actual=RandomUtils.withSeed(1541,()->GENERATOR.generateCalibrated(ps,bonus(25),VERSION));
        final double referenceMultiplier=actual.calibration().multiplier();
        final double r3=bScore(actual.enemies())/(bScore(ps)*bonus(25)*g(3));
        newC=r3;
        final var solo=RandomUtils.withSeed(1542,()->players(inputs("MAIN",Tier.STRONG,1,5)));
        final var soloActual=RandomUtils.withSeed(1543,()->GENERATOR.generateCalibrated(solo,bonus(5),VERSION));
        final double r2=bScore(soloActual.enemies())/(bScore(solo)*bonus(5)*g(2));
        final double h2=r2/r3;
        twoAnchorA=(2.25-h2)/1.25;
        final var result=new LinkedHashMap<String,Object>();
        if(newC!=0.47754531235323505||twoAnchorA!=0.6046927918979718)throw new IllegalStateException("Frozen B constants changed");
        result.put("oldC",OLD_C);result.put("screenA",twoAnchorA);result.put("Cnew",newC);result.put("r2",r2);result.put("r3",r3);
        result.put("referenceMultiplierMID3L25",referenceMultiplier);result.put("referenceMultiplierSTRONG1L5",soloActual.calibration().multiplier());
        result.put("midPlayersParts",parts(ps));result.put("midNpcParts",parts(actual.enemies()));
        result.put("soloPlayersParts",parts(solo));result.put("soloNpcParts",parts(soloActual.enemies()));
        result.put("twoAnchorScreenParameters",Map.of("C",r3,"a",twoAnchorA,"h2",h2,"h3",1.0,
            "hInfinity",twoAnchorA,"rangeMinimumMAtLeast2",Math.min(h2,twoAnchorA),"rangeMaximumMAtLeast2",Math.max(h2,twoAnchorA),
            "allMPositive",h2>0&&twoAnchorA>0,"aLessThan1_8",twoAnchorA<1.8));
        final var candidate=calibrate(Model.B_TWO_ANCHOR,ps,3,bonus(25),twoAnchorA);
        final var soloCandidate=calibrate(Model.B_TWO_ANCHOR,solo,2,bonus(5),twoAnchorA);
        result.put("midAnchorFactoryCharacteristicsExact",traits(actual.enemies()).equals(traits(npcs(3,candidate.multiplier()))));
        result.put("soloAnchorFactoryCharacteristicsExact",traits(soloActual.enemies()).equals(traits(npcs(2,soloCandidate.multiplier()))));
        if(!traits(actual.enemies()).equals(traits(npcs(3,candidate.multiplier())))||!traits(soloActual.enemies()).equals(traits(npcs(2,soloCandidate.multiplier()))))throw new IllegalStateException("B_TWO_ANCHOR does not preserve exact NPC anchor traits");
        return result;
    }

    private static Map<String,Object> validateBridge() {
        int checked=0;
        for(final int n:List.of(1,3,8))for(final int level:List.of(5,25)) {
            final var input=inputs("MAIN",Tier.MID,n,level);
            final var reference=RandomUtils.withSeed(154_000+n*1000L+level,()->{
                final var ps=players(input);return GENERATOR.generateCalibrated(ps,bonus(level),VERSION);});
            final var candidate=RandomUtils.withSeed(154_000+n*1000L+level,()->prepare(input,Model.Q_CURRENT,level));
            if(!reference.calibration().equals(candidate.calibration()))throw new IllegalStateException("Q_CURRENT calibration bridge differs N"+n+" L"+level);
            for(int i=0;i<reference.enemies().size();++i) {
                final var a=reference.enemies().get(i);final var b=candidate.enemies().get(i);
                if(!a.id().equals(b.id())||a.initiativeGauge()!=b.initiativeGauge()||!traits(List.of(a)).equals(traits(List.of(b))))
                    throw new IllegalStateException("Q_CURRENT factory bridge differs N"+n+" L"+level);
            }
            ++checked;
        }
        return Map.of("status","EXACT_ACTUAL_CURRENT_BASELINE_BRIDGE","conditions",checked,"compared","calibration,traits,UUID,gauge");
    }

    private static Map<String,Object> runCell(Case cell,Model model,int pairs,BufferedWriter raw)throws Exception {
        final var scores=new ArrayList<Double>();final var counts=new TreeMap<String,Integer>();
        long rounds=0;int completed=0;
        for(int pair=0;pair<pairs;++pair) {
            final long key=Long.getLong("formula.seedOffset",0L)+cell.groupId()*1000L+pair;
            final long generationSeed=GENERATION_ROOT+key,battleSeed=BATTLE_ROOT+key;
            double pairScore=0;boolean complete=true;
            for(final boolean reverse:List.of(false,true)) {
                final var prepared=RandomUtils.withSeed(generationSeed,()->prepare(cell,model));
                final var ps=prepared.players();final var enemies=prepared.enemies();
                final var row=new LinkedHashMap<String,Object>();
                row.put("case",cell);row.put("model",model);row.put("pair",pair);row.put("reverse",reverse);
                row.put("generationSeed",generationSeed);row.put("battleSeed",battleSeed);row.put("calibration",prepared.calibration());
                row.put("C",model==Model.Q_CURRENT?OLD_C:newC);row.put("a",model==Model.Q_CURRENT?OLD_A:twoAnchorA);
                row.put("playerScore",prepared.playerScore());row.put("enemyScore",assess(model,enemies));
                row.put("playerParts",parts(ps));row.put("enemyParts",parts(enemies));
                row.put("sourceEffects",effects(cell).effects());row.put("combatEffects",effects(cell).effects());
                try {
                    final var result=RandomUtils.withSeed(battleSeed,()->new Battle(battleSeed).process(
                        reverse?ps:enemies,reverse?enemies:ps,2000,TEMPO));
                    final boolean pa=ps.stream().anyMatch(BattlePersonage::isAlive),ea=enemies.stream().anyMatch(BattlePersonage::isAlive);
                    final String outcome=pa?"WIN":ea?"LOSS":"DRAW";
                    counts.merge(outcome,1,Integer::sum);pairScore+=pa?0.5:ea?0:0.25;
                    row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());
                    row.put("actualEnemyPositions",enemies.stream().map(enemy->result.initState().lines().get(result.initState().personagesById().get(enemy.id()).lineIndex()).position()).toList());
                    row.put("actionCount",result.actionLog().events().size());
                    row.put("normalAttempts",result.actionLog().traceEvents().stream().filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance).count());
                    row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());
                    row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
                    row.put("actionSha256",digest(result.actionLog()));row.put("traceSha256",digest(result.actionLog().traceEvents()));
                    row.put("playerStats",ps.stream().map(BattlePersonage::battlePersonageStats).toList());
                    row.put("enemyStats",enemies.stream().map(BattlePersonage::battlePersonageStats).toList());
                    rounds+=result.rounds();++completed;
                }catch(IllegalStateException e) {
                    if(e.getMessage()==null||!e.getMessage().startsWith("Battle did not finish within"))throw e;
                    complete=false;counts.merge("TIMEOUT",1,Integer::sum);row.put("outcome","TIMEOUT");row.put("rounds",2000);
                }
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));raw.newLine();
            }
            scores.add(complete?pairScore:null);
        }
        final var finished=scores.stream().filter(java.util.Objects::nonNull).toList();
        final double mean=finished.stream().mapToDouble(Double::doubleValue).average().orElse(0);
        final double half=finished.isEmpty()?1:Math.sqrt(Math.log(40)/(2*finished.size()));
        return Map.of("case",cell,"model",model,"pairs",pairs,"pairScores",scores,"outcomes",counts,
            "winrate",counts.getOrDefault("WIN",0)/(2.0*pairs),"pairMean",mean,
            "pointwise95Hoeffding",List.of(Math.max(0,mean-half),Math.min(1,mean+half)),"meanRounds",completed==0?0:rounds/(double)completed);
    }

    private static Prepared prepare(List<InputMember> members,Model model,int level) {
        final var ps=players(members);final int count=Math.max(2,ps.size());
        final var calibration=calibrate(model,ps,count,bonus(level),model==Model.Q_CURRENT?OLD_A:twoAnchorA);
        return new Prepared(ps,npcs(count,calibration.multiplier()),calibration,assess(model,ps));
    }
    private static Prepared prepare(Case cell,Model model) {
        final var ps=players(cell.members(),effects(cell));final int count=Math.max(2,ps.size());
        final var calibration=calibrate(model,ps,count,bonus(cell.level()),model==Model.Q_CURRENT?OLD_A:twoAnchorA);
        return new Prepared(ps,npcs(count,calibration.multiplier()),calibration,assess(model,ps));
    }

    private static PveCharacteristicScaling.Result calibrate(Model model,List<BattlePersonage> ps,int count,double powerBonus,double a) {
        final double target=assess(model,ps)*powerBonus*g(count)*(model==Model.Q_CURRENT?OLD_C:newC)*h(count,a);
        return PveCharacteristicScaling.search(target,maxMultiplier(count),
            m->RandomUtils.withSeed(0,()->assess(model,npcs(count,m))));
    }

    private static List<BattlePersonage> players(List<InputMember> members) {
        return players(members,PersonageEffects.EMPTY);
    }
    private static List<BattlePersonage> players(List<InputMember> members,PersonageEffects effects) {
        return members.stream().map(member->{
            final var p=BattlePersonage.forCombat(member.items(),member.position(),effects,Optional.of(member.build()),VERSION);
            p.setTargetingTactic(member.tactic());if(!p.isAlive())throw new IllegalStateException("Dead input");return p;}).toList();
    }
    private static PersonageEffects effects(Case cell) {
        try {
            final var method=PveAdaptationStudy.class.getDeclaredMethod("combatEffects",Cell.class);method.setAccessible(true);
            return (PersonageEffects)method.invoke(null,new Cell(Mode.RAID,"MYCONID_COLONY",cell.style(),cell.tier(),cell.members().size(),cell.level(),cell.effects()));
        }catch(Exception e){throw new IllegalStateException("Actual combat effects failed",e);}
    }
    private static Map<String,Object> validateEffects() {
        int checked=0;
        for(final int n:List.of(3,15))for(final int level:List.of(25,30,40)) {
            final var referenceCell=new Cell(Mode.RAID,"MYCONID_COLONY","MAIN",Tier.MID,n,level,true);
            final var cs=new Case(0,0,"MAIN",Tier.MID,n,level,"EFFECTS",true,PveAdaptationStudy.inputFor(referenceCell));
            final var fx=effects(cs);
            final var expected=new PersonageEffects(Map.of(
                ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType.MENU_ITEM_EFFECT,
                new ru.homyakin.seeker.game.personage.models.effect.PersonageEffect(new ru.homyakin.seeker.game.effect.Effect.Multiplier(20,ru.homyakin.seeker.game.effect.EffectCharacteristic.ATTACK),null),
                ru.homyakin.seeker.game.personage.models.effect.PersonageEffectType.WORKER_OF_DAY_EFFECT,
                new ru.homyakin.seeker.game.personage.models.effect.PersonageEffect(new ru.homyakin.seeker.game.effect.Effect.Multiplier(15,ru.homyakin.seeker.game.effect.EffectCharacteristic.HEALTH),null)));
            if(!fx.equals(expected))throw new IllegalStateException("Effects are not actual permanent20%attack15%health fixture");
            final long seed=154_400+n*1000L+level;
            final var reference=RandomUtils.withSeed(seed,()->PveAdaptationStudy.prepare(referenceCell));
            final var candidate=RandomUtils.withSeed(seed,()->prepare(cs,Model.B_TWO_ANCHOR));
            if(!traits(reference.players()).equals(traits(candidate.players())))throw new IllegalStateException("Effects player traits differ");
            for(int i=0;i<n;++i)if(!reference.players().get(i).id().equals(candidate.players().get(i).id())||reference.players().get(i).initiativeGauge()!=candidate.players().get(i).initiativeGauge())throw new IllegalStateException("Effects player RNG differs");
            ++checked;
        }
        return Map.of("status","EXACT_ACTUAL_PREPARED_EFFECTFUL_PLAYERS","conditions",checked,"effects","Actual private combatEffects;20percentAttack15percentHP;nullExpiry is the original permanent fixture, not a manual substitute");
    }

    @SuppressWarnings("unchecked") private static List<BattlePersonage> npcs(int count,double m) {
        try{return (List<BattlePersonage>)FACTORY.invoke(GENERATOR,FORMATION_CONSTRUCTOR.newInstance(count),m,VERSION);}
        catch(Exception e){throw new IllegalStateException("Actual NPC factory failed",e);}
    }
    private static double maxMultiplier(int count) {
        final int f=count/2,s=count-f;final double wf=count/(2.0*f),ws=count/(2.0*s);
        return PveCharacteristicScaling.maxWeightedMultiplier(
            new WeightedBase((int)constant("BASE_GUARDIAN_ATTACK"),wf),new WeightedBase((int)constant("BASE_GUARDIAN_HEALTH"),wf),
            new WeightedBase((int)constant("BASE_GUARDIAN_DEFENSE"),1),new WeightedBase((int)constant("BASE_SLINGER_ATTACK"),ws),
            new WeightedBase((int)constant("BASE_SLINGER_HEALTH"),ws),new WeightedBase((int)constant("BASE_SLINGER_DEFENSE"),1));
    }

    private static List<Map<String,Object>> parts(List<BattlePersonage> team) {
        return team.stream().map(p->{
            double mu=0;for(final var type:AttackType.values())mu+=p.defenseReductions().get(type);
            final double health=p.maxHealth()/(mu/AttackType.values().length);
            final double crit=1+Math.clamp(p.critChance()/100.0,0.0,1.0)*(p.critMultiplier()-1);
            final double attack;
            try{attack=(double)MAXIMUM_ATTACK.invoke(null,p);}catch(Exception e){throw new IllegalStateException(e);}
            final double frequency=CombatRules.cappedTurnFrequency(p.initiative());
            final double rate=attack==0||crit==0||frequency==0?0:attack*crit*frequency;
            if(!Double.isFinite(health*rate))throw new IllegalStateException("Nonfinite P component");
            return Map.<String,Object>of("id",p.id(),"hp",p.maxHealth(),"mu",mu/AttackType.values().length,"effectiveHealth",health,
                "maximumAttack",attack,"criticalFactor",crit,"frequency",frequency,"normalRate",rate,"selfProduct",health*rate,"skills",p.skillSnapshots());
        }).toList();
    }
    private static double bScore(List<BattlePersonage> team) {
        final var values=parts(team).stream().map(p->(Double)p.get("selfProduct")).sorted().toList();double total=0;
        for(int i=0;i<values.size();++i)total+=(2*(values.size()-i)-1)*values.get(i);
        return total;
    }
    private static double assess(Model model,List<BattlePersonage> team){return model==Model.Q_CURRENT?PveTeamAssessment.assess(team):bScore(team);}
    private static double bonus(int level){return 1+((level-10)*0.06+0.14);}
    private static double g(int count){return 1+MathUtils.log(4,count)*GROUP_COEF;}
    private static double h(int count,double a){return a+(1-a)*Math.pow(3.0/count,2);}

    private static List<Case> cases()throws Exception {
        final var weakFactory=MyconidAdaptationStudy.class.getDeclaredMethod("weakWithDefaults");weakFactory.setAccessible(true);
        final var weak=(InputMember)weakFactory.invoke(null);
        if(RandomUtils.withSeed(0,()->players(List.of(weak))).getFirst().maxHealth()!=840)throw new IllegalStateException("Wrong weak input");
        final var result=new ArrayList<Case>();int group=0;
        final var sizes=new ArrayList<Integer>();for(int n=2;n<=16;++n)sizes.add(n);sizes.addAll(List.of(29,30));
        final var tiers=List.of(Tier.BEGINNER,Tier.MID,Tier.STRONG);
        for(final var tier:tiers)for(final int n:sizes)for(final int level:List.of(10,20,25,30,40,50))
            add(result,++group,"MAIN",tier,n,level,"BASE",inputs("MAIN",tier,n,level));
        if(result.size()!=306)throw new IllegalStateException("MAIN screen changed");
        final var mainCases=List.copyOf(result);
        for(final var tier:List.of(Tier.MID,Tier.STRONG))for(final int n:List.of(2,4,6,8,14))for(final int level:List.of(20,25,30)) {
            final var base=mainCases.stream().filter(c->c.tier()==tier&&c.originalSize()==n&&c.level()==level).findFirst().orElseThrow();
            add(result,base.groupId(),"MAIN",tier,n,level,"NORMAL_APPEND",append(base.members(),inputs("MAIN",tier,n+1,level).get(n)));
            add(result,base.groupId(),"MAIN",tier,n,level,"WEAK_APPEND",append(base.members(),weak));
        }
        for(final int n:List.of(3,15))for(final int level:List.of(25,30,40)) {
            final var base=mainCases.stream().filter(c->c.tier()==Tier.MID&&c.originalSize()==n&&c.level()==level).findFirst().orElseThrow();
            result.add(new Case(result.size()+1,base.groupId(),"MAIN",Tier.MID,n,level,"EFFECTS",true,base.members()));
        }
        for(final var style:List.of("PIERCE_ARCANE","RAPIER_ARCANE"))for(final var tier:tiers)for(final int level:List.of(1,5,10))
            add(result,++group,style,tier,1,level,"LOW_SOLO",inputs(style,tier,1,level));
        for(final var tier:tiers)for(final int level:List.of(1,5,10,20,25))
            add(result,++group,"MAIN",tier,1,level,"LOW_SOLO",inputs("MAIN",tier,1,level));
        for(final var tier:tiers)for(final int level:List.of(1,5,15,25))
            add(result,++group,"RITUAL",tier,1,level,"LOW_SOLO",inputs("RITUAL",tier,1,level));
        for(final var style:List.of("FLANK","PRESSURE","RITUAL","PIERCE_ARCANE"))for(final var tier:tiers)for(final int level:List.of(10,25,40,50))
            add(result,++group,style,tier,3,level,"SPECIALIZED",inputs(style,tier,3,level));
        for(final var style:List.of("RITUAL","PIERCE_ARCANE","RAPIER_ARCANE"))for(final int n:List.of(7,15))for(final int level:List.of(30,40,50))for(final var tier:tiers)
            add(result,++group,style,tier,n,level,"HIGH_SPECIALIZED",inputs(style,tier,n,level));
        if(result.size()!=519)throw new IllegalStateException("Screen changed");
        for(final var c:result)if(c.members().size()!=c.originalSize()+(c.variant().endsWith("APPEND")?1:0))throw new IllegalStateException("Wrong concrete input size "+c.id());
        return result;
    }
    private static void add(List<Case> rows,int group,String style,Tier tier,int original,int level,String variant,List<InputMember> members) {
        rows.add(new Case(rows.size()+1,group,style,tier,original,level,variant,false,List.copyOf(members)));
    }
    private static List<InputMember> append(List<InputMember> base,InputMember added){final var result=new ArrayList<>(base);result.add(added);return List.copyOf(result);}
    private static List<InputMember> inputs(String style,Tier tier,int size,int level){return PveAdaptationStudy.inputFor(new Cell(Mode.RAID,"MYCONID_COLONY",style,tier,size,level,false));}

    private static List<Object> traits(List<BattlePersonage> team) {
        return team.stream().map(p->(Object)List.of(p.maxHealth(),p.attacksByRange(),p.defenses(),p.critChance(),p.dodgeChance(),p.critMultiplier(),
            p.initiative(),p.totalThreat(),p.startPosition(),p.range(),p.skillSnapshots())).toList();
    }
    private static Class<?> formationType(){try{return Class.forName(MyconidColonyGenerator.class.getName()+"$Formation");}catch(Exception e){throw new IllegalStateException(e);}}
    private static Constructor<?> formationConstructor(){try{final var c=FORMATION.getDeclaredConstructor(int.class);c.setAccessible(true);return c;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Method factory(){try{final var m=MyconidColonyGenerator.class.getDeclaredMethod("teamColony",FORMATION,double.class,SkillFormulaVersion.class);m.setAccessible(true);return m;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Method maximumAttack(){try{final var m=PveTeamAssessment.class.getDeclaredMethod("maximumAttack",BattlePersonage.class);m.setAccessible(true);return m;}catch(Exception e){throw new IllegalStateException(e);}}
    private static double constant(String name){try{final var f=MyconidColonyGenerator.class.getDeclaredField(name);f.setAccessible(true);return ((Number)f.get(null)).doubleValue();}catch(Exception e){throw new IllegalStateException(e);}}
    private static String digest(Object value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PveAdaptationStudy.JSON.writeValueAsBytes(value)));}
    private static BufferedWriter writer(Path p)throws Exception{return new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(p,StandardOpenOption.CREATE_NEW)),StandardCharsets.UTF_8));}
    private static String json(Object value)throws Exception{return PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value);}
}
