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
    private static final long GENERATION_ROOT=2_032_110_815_400L;
    private static final long BATTLE_ROOT=3_032_110_815_400L;
    private static double newC;
    private static double twoAnchorA;
    private enum Model { Q_CURRENT, B_TWO_ANCHOR }
    private static final List<Model> SCREEN_MODELS=List.of(Model.B_TWO_ANCHOR);
    private record Case(int id,int groupId,String style,Tier tier,int originalSize,int level,String variant,List<InputMember> members) { }
    private record Prepared(List<BattlePersonage> players,List<BattlePersonage> enemies,PveCharacteristicScaling.Result calibration,double playerScore) { }

    public static void main(String[] args)throws Exception {
        if(args.length!=2||!List.of("prepare","screen").contains(args[0]))throw new IllegalArgumentException("prepare|screen NEW_OUTPUT_DIRECTORY");
        final var output=Path.of(args[1]);Files.createDirectory(output);
        final int pairs=Integer.getInteger("formula.pairs",20);
        if(pairs<1||pairs>1000)throw new IllegalArgumentException("Pairs must be1..1000");
        final var anchor=anchors();
        final var bridge=validateBridge();
        final var cases=cases();
        final var manifest=new LinkedHashMap<String,Object>();
        manifest.put("purpose","OUTSIDE_PRODUCTION_CANDIDATE_COMPARISON_NOT_BALANCE_ACCEPTANCE");
        manifest.put("factory","Current private MyconidColonyGenerator.teamColony via reflection; no copied factory, class override or production edit");
        manifest.put("formula","B_TWO_ANCHOR=sum_i,j(min(p_i,p_j)), p_i=EHP_i*normalRate_i; same exact neutral components; ascending self-products then sum (2*(N-i)-1)*p_i, zero-based i");
        manifest.put("ignored","Skills, dodge, actual matchup, opportunity to attack at best range, actual lifetime");
        manifest.put("anchorPolicy","C and a from exact actual 15.2 MID MAIN N3 L25 and STRONG MAIN N1 L5 NPC factories; no outcome fitting; same g/h/role weights and bases");
        manifest.put("anchors",anchor);manifest.put("bridgeValidation",bridge);
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
                final var prepared=RandomUtils.withSeed(generationSeed,()->prepare(cell.members(),model,cell.level()));
                final var ps=prepared.players();final var enemies=prepared.enemies();
                final var row=new LinkedHashMap<String,Object>();
                row.put("case",cell);row.put("model",model);row.put("pair",pair);row.put("reverse",reverse);
                row.put("generationSeed",generationSeed);row.put("battleSeed",battleSeed);row.put("calibration",prepared.calibration());
                row.put("C",model==Model.Q_CURRENT?OLD_C:newC);row.put("a",model==Model.Q_CURRENT?OLD_A:twoAnchorA);
                row.put("playerScore",prepared.playerScore());row.put("enemyScore",assess(model,enemies));
                row.put("playerParts",parts(ps));row.put("enemyParts",parts(enemies));
                try {
                    final var result=RandomUtils.withSeed(battleSeed,()->new Battle(battleSeed).process(
                        reverse?ps:enemies,reverse?enemies:ps,2000,TEMPO));
                    final boolean pa=ps.stream().anyMatch(BattlePersonage::isAlive),ea=enemies.stream().anyMatch(BattlePersonage::isAlive);
                    final String outcome=pa?"WIN":ea?"LOSS":"DRAW";
                    counts.merge(outcome,1,Integer::sum);pairScore+=pa?0.5:ea?0:0.25;
                    row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());
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

    private static PveCharacteristicScaling.Result calibrate(Model model,List<BattlePersonage> ps,int count,double powerBonus,double a) {
        final double target=assess(model,ps)*powerBonus*g(count)*(model==Model.Q_CURRENT?OLD_C:newC)*h(count,a);
        return PveCharacteristicScaling.search(target,maxMultiplier(count),
            m->RandomUtils.withSeed(0,()->assess(model,npcs(count,m))));
    }

    private static List<BattlePersonage> players(List<InputMember> members) {
        return members.stream().map(member->{
            final var p=BattlePersonage.forCombat(member.items(),member.position(),PersonageEffects.EMPTY,Optional.of(member.build()),VERSION);
            p.setTargetingTactic(member.tactic());if(!p.isAlive())throw new IllegalStateException("Dead input");return p;}).toList();
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
        for(final var tier:List.of(Tier.MID,Tier.STRONG))for(final int level:List.of(20,25)) {
            final var base=inputs("MAIN",tier,3,level);++group;
            add(result,group,"MAIN",tier,3,level,"BASE",base);
            add(result,group,"MAIN",tier,3,level,"NORMAL_APPEND",append(base,inputs("MAIN",tier,4,level).get(3)));
            add(result,group,"MAIN",tier,3,level,"WEAK_APPEND",append(base,weak));
        }
        for(final var style:List.of("MAIN","PIERCE_ARCANE","RITUAL")) {
            final int level=style.equals("MAIN")?5:style.equals("PIERCE_ARCANE")?15:25;
            final var base=inputs(style,Tier.STRONG,1,level);++group;
            add(result,group,style,Tier.STRONG,1,level,"BASE",base);add(result,group,style,Tier.STRONG,1,level,"WEAK_APPEND",append(base,weak));
        }
        final var eight=inputs("MAIN",Tier.MID,8,30);++group;
        add(result,group,"MAIN",Tier.MID,8,30,"BASE",eight);add(result,group,"MAIN",Tier.MID,8,30,"WEAK_APPEND",append(eight,weak));
        for(final var tier:List.of(Tier.MID,Tier.STRONG))for(final int n:List.of(15,30))for(final int level:List.of(30,40,50))
            add(result,++group,"MAIN",tier,n,level,"BASE",inputs("MAIN",tier,n,level));
        for(final var style:List.of("STRONG_PLUS_WEAK","WEAK_ONLY"))for(final int level:List.of(30,50)) {
            final var members=new ArrayList<InputMember>();members.add(style.equals("WEAK_ONLY")?weak:inputs("MAIN",Tier.STRONG,1,level).getFirst());
            while(members.size()<30)members.add(weak);add(result,++group,style,Tier.STRONG,30,level,"BASE",List.copyOf(members));
        }
        if(result.size()!=36)throw new IllegalStateException("Screen changed");return result;
    }
    private static void add(List<Case> rows,int group,String style,Tier tier,int original,int level,String variant,List<InputMember> members) {
        rows.add(new Case(rows.size()+1,group,style,tier,original,level,variant,List.copyOf(members)));
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
