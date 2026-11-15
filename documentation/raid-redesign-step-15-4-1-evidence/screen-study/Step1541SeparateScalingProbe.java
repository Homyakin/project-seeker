package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Cell;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.Mode;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.models.*;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;

/** Step15.4.1 outside-production fixed two-model screen. Never edits or overrides runtime classes. */
public final class Step1541SeparateScalingProbe {
    private static final SkillFormulaVersion V3=SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final BattleTempoVersion TEMPO=BattleTempoVersion.ACCELERATED_V1;
    private static final MyconidColonyGenerator GENERATOR=new MyconidColonyGenerator();
    private static final String GEN="MYCONID-15.4.1-SPLIT-SCREEN-GEN-20261108-v1";
    private static final String BATTLE="MYCONID-15.4.1-SPLIT-SCREEN-BATTLE-20261108-v1";
    private static final Field ATTACK_PARTS=field(BattlePersonage.class,"scalingBaseAttackParts");
    private static final Map<Integer,Metrics> NATURAL=new TreeMap<>();
    private static double referenceDefMultiplier;
    private static Metrics referencePlayers,referenceNpc;
    private enum Model { SAME,CROSS }
    private enum Role { GUARDIAN,SLINGER }
    private record Metrics(double H,double D) { }
    private record Case(int id,int groupId,String style,Tier tier,int originalSize,int level,String variant,boolean effects,List<InputMember> members) { }
    private record Calibration(double levelScale,double hRatio,double dRatio,double targetH,double targetD,double hpMultiplier,double attackMultiplier,
                               double achievedH,double achievedD,double healthRoundingBound,double damageRoundingBound,String policy) { }
    private record Prepared(List<BattlePersonage> players,List<BattlePersonage> enemies,List<Long> unitSeeds,Metrics playerMetrics,Calibration calibration) { }

    public static void main(String[] args)throws Exception {
        if(args.length!=2||!List.of("prepare","screen").contains(args[0]))throw new IllegalArgumentException("prepare|screen NEW_OUTPUT_DIRECTORY");
        if(Integer.getInteger("split.pairs",30)!=30)throw new IllegalArgumentException("Approved fixed screen is exactly30pairs");
        final var output=Path.of(args[1]);Files.createDirectory(output);
        final var anchors=anchors();
        for(int n=2;n<=30;++n) {final int count=n;NATURAL.put(n,RandomUtils.withSeed(0,()->metrics(colony(count,1,1).enemies())));}
        final var cases=cases();
        final var bridges=bridges(cases);
        final var manifest=new LinkedHashMap<String,Object>();
        manifest.put("purpose","STEP15.4.1_FIXED_TWO_MODEL_EXPLORATION_NOT_BALANCE_ACCEPTANCE");
        manifest.put("models",List.of(Model.values()));manifest.put("conditions",50);manifest.put("pairs",30);manifest.put("battles",6000);
        manifest.put("formulaSAME","targetH=HrefNPC*(sqrt(qL)/sqrt(q25))*(Hplayers/HrefPlayers); targetD=DrefNPC*(sqrt(qL)/sqrt(q25))*(Dplayers/DrefPlayers)");
        manifest.put("formulaCROSS","targetH=HrefNPC*(sqrt(qL)/sqrt(q25))*(Dplayers/DrefPlayers); targetD=DrefNPC*(sqrt(qL)/sqrt(q25))*(Hplayers/HrefPlayers)");
        manifest.put("qL","1+((L-10)*.06+.14), unchanged existing level bonus");manifest.put("defensePolicy","FIXED_G707_S309_FROM_ACTUAL15.2_ANCHOR; no level/size/type adaptation");
        manifest.put("roundingPolicy","Closedform targetH/naturalH and targetD/naturalD, no row weights; min1ceil naturalHP/ATK once; defense retained from actual factory at referenceDefMultiplier");
        manifest.put("removed","No shared HP/ATK/DEF multiplier, no M/(2F/S) weights, no g/h/a/r or outcome fitting");
        manifest.put("neutralPolicy","H=sum(maxHP/mean4damageTakenMultiplier); D=sum(maxProfileAttack*clampedCritExpectation*cappedFrequency); sorted reductions and team sums; skills/dodge/range access ignored in assessment but active in real combat");
        manifest.put("factoryPolicy","Actual guardian(double,V3)/slinger(double,V3) with weight1; reconstruct immutable synthetic item from actual template, change only HP/attack parts, then forCombat on the same isolated unitseed. No writes to BattlePersonage final fields.");
        manifest.put("generationNamespace",GEN);manifest.put("battleNamespace",BATTLE);manifest.put("seedPolicy","SHA256(namespace+'/'+groupId+'/'+pair), first48BE; no model/variant. GEN constructs players then draws one48bit unitseed per NPC; template/rebuild replay that unitseed. SAME/CROSS and countertwins share NPC IDs/gauges; BASE/APPEND only player prefix promised.");
        manifest.put("anchors",anchors);manifest.put("bridges",bridges);manifest.put("naturalByCount",NATURAL);
        manifest.put("versions",List.of("SCALING_V5",V3.name(),TEMPO.name()));manifest.put("roundLimit",2000);
        manifest.put("limitations","Exploration30pairs, not50/50 target or universal monotonicity; mirrors/model/variant cohorts correlated; fixed DEF is an experimental design, not a production decision");
        final var seeds=new ArrayList<Map<String,Object>>();
        for(final int group:cases.stream().map(Case::groupId).distinct().toList())for(int pair=0;pair<30;++pair)seeds.add(Map.of("groupId",group,"pair",pair,"generationSeed",seed(GEN,group,pair),"battleSeed",seed(BATTLE,group,pair)));
        write(output,"manifest.json",manifest);write(output,"inputs.json",cases);write(output,"seeds.json",seeds);
        if(args[0].equals("prepare"))return;
        final var summaries=new ArrayList<Map<String,Object>>();
        try(var raw=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(output.resolve("battles.jsonl.gz"),StandardOpenOption.CREATE_NEW)),StandardCharsets.UTF_8))) {
            for(final var c:cases)for(final var model:Model.values()) {summaries.add(run(c,model,raw));System.out.println("done="+summaries.size()+" "+model+" case"+c.id()+" "+c.style()+" N"+c.members().size()+" L"+c.level()+" "+c.variant());}
        }
        write(output,"summary.json",Map.of("manifest",manifest,"cases",summaries));
    }

    private static Map<String,Object> anchors() {
        final var input=inputs("MAIN",Tier.MID,3,25);
        final var ps=RandomUtils.withSeed(154100,()->players(input,PersonageEffects.EMPTY));
        final var actual=RandomUtils.withSeed(154101,()->GENERATOR.generateCalibrated(ps,q(25),V3));
        referenceDefMultiplier=actual.calibration().multiplier();referencePlayers=metrics(ps);referenceNpc=metrics(actual.enemies());
        if(referenceDefMultiplier!=4.414285714285715||actual.enemies().getFirst().defenses().get(DefenseType.ARCANE)!=707||actual.enemies().get(1).defenses().get(DefenseType.ARCANE)!=309)throw new IllegalStateException("Actual anchor changed");
        final double s=Math.sqrt(q(25));
        return Map.of("referenceDefMultiplier",referenceDefMultiplier,"referencePlayers",referencePlayers,"referenceNPC",referenceNpc,
            "q25",q(25),"sqrtQ25",s,"kConceptual",Map.of("SAME_H",referenceNpc.H()/(s*referencePlayers.H()),"SAME_D",referenceNpc.D()/(s*referencePlayers.D()),
            "CROSS_H",referenceNpc.H()/(s*referencePlayers.D()),"CROSS_D",referenceNpc.D()/(s*referencePlayers.H())),
            "actualNPCParts",parts(actual.enemies()),"playerParts",parts(ps),"anchorPolicy","Preserve aggregate neutral H/D after integer rounding; removal of role weights intentionally changes individual NPC HP/ATK");
    }

    private record Colony(List<BattlePersonage> enemies,List<Long> seeds) { }
    private static Colony colony(int count,double hp,double attack) {
        final var result=new ArrayList<BattlePersonage>();final var seeds=new ArrayList<Long>();final var seen=new HashSet<Long>();
        for(int i=0;i<count;++i) {
            final long unit=RandomUtils.getInInterval(0L,(1L<<48)-1);if(!seen.add(unit))throw new IllegalStateException("Duplicate unitseed");
            seeds.add(unit);result.add(rebuild(i<count/2?Role.GUARDIAN:Role.SLINGER,hp,attack,referenceDefMultiplier,unit));
        }
        return new Colony(List.copyOf(result),List.copyOf(seeds));
    }
    private static Prepared prepare(Case c,Model model) {
        final var ps=players(c.members(),effects(c));final var p=metrics(ps);final int count=Math.max(2,ps.size());final var natural=NATURAL.get(count);
        final double level=Math.sqrt(q(c.level()))/Math.sqrt(q(25)),h=p.H()/referencePlayers.H(),d=p.D()/referencePlayers.D();
        final double targetH=referenceNpc.H()*level*(model==Model.SAME?h:d),targetD=referenceNpc.D()*level*(model==Model.SAME?d:h);
        final double hp=targetH/natural.H(),attack=targetD/natural.D();final var built=colony(count,hp,attack);final var achieved=metrics(built.enemies());
        final var all=new HashSet<UUID>();for(final var person:ps)if(!all.add(person.id()))throw new IllegalStateException("Duplicate player UUID");
        for(final var person:built.enemies())if(!all.add(person.id()))throw new IllegalStateException("Duplicate NPC/player UUID");
        final double boundH=stable(parts(built.enemies()).stream().mapToDouble(x->1/(double)x.get("mu")).toArray());
        final double boundD=stable(parts(built.enemies()).stream().mapToDouble(x->(double)x.get("criticalFactor")*(double)x.get("frequency")).toArray());
        if(achieved.H()+1e-8<targetH||achieved.H()>targetH+boundH+1e-7||achieved.D()+1e-8<targetD||achieved.D()>targetD+boundD+1e-7)throw new IllegalStateException("Closedform rounding bound broken");
        return new Prepared(ps,built.enemies(),built.seeds(),p,new Calibration(level,h,d,targetH,targetD,hp,attack,achieved.H(),achieved.D(),boundH,boundD,"MIN1_CEIL_CLOSED_FORM"));
    }

    private static BattlePersonage actual(Role role,double multiplier,long seed) {
        return RandomUtils.withSeed(seed,()->{try {final var m=MyconidColonyGenerator.class.getDeclaredMethod(role==Role.GUARDIAN?"guardian":"slinger",double.class,SkillFormulaVersion.class);m.setAccessible(true);return (BattlePersonage)m.invoke(GENERATOR,multiplier,V3);}catch(Exception e){throw new IllegalStateException(e);}});
    }
    @SuppressWarnings("unchecked") private static List<ItemAttack> rawAttacks(BattlePersonage p) {
        try{return (List<ItemAttack>)ATTACK_PARTS.get(p);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static BattlePersonage rebuild(Role role,double hp,double attack,double defense,long unitSeed) {
        final var natural=actual(role,1,unitSeed);final var template=actual(role,defense,unitSeed);
        if(template.skillSnapshots().size()!=1||template.skillSnapshots().getFirst().points()!=4||template.skillSnapshots().getFirst().formulaVersion()!=V3||template.defenses().size()!=1||template.impactStrength()!=ImpactStrength.BASE_VALUE)throw new IllegalStateException("Synthetic factory contract changed");
        final var attacks=rawAttacks(natural).stream().map(a->new ItemAttack(a.attackType(),a.minRange(),a.maxRange(),PveCharacteristicScaling.multiply(a.attack(),attack))).toList();
        final var item=new Item(new ItemObject(null,Set.of(PersonageSlot.MAIN_HAND),attacks,
            Optional.of(new ItemDefense(DefenseType.ARCANE,template.defenses().get(DefenseType.ARCANE))),PveCharacteristicScaling.multiply(natural.maxHealth(),hp),
            template.critChance(),template.dodgeChance(),template.critMultiplier()-CombatRules.BASE_CRIT_MULTIPLIER,template.initiative(),template.totalThreat(),0,ItemProgressionVersion.LEGACY,null),
            Optional.of(new Modifier(template.skillSnapshots().getFirst().code())),ItemRarity.LEGENDARY,0);
        return RandomUtils.withSeed(unitSeed,()->{final var p=BattlePersonage.forCombat(List.of(item),template.startPosition(),PersonageEffects.EMPTY,template.name(),V3);p.setTargetingTactic(template.targetingTactic());return p;});
    }
    private static List<BattlePersonage> players(List<InputMember> members,PersonageEffects effects) {
        return members.stream().map(m->{final var p=BattlePersonage.forCombat(m.items(),m.position(),effects,Optional.of(m.build()),V3);p.setTargetingTactic(m.tactic());if(!p.isAlive())throw new IllegalStateException("Dead input");return p;}).toList();
    }
    private static PersonageEffects effects(Case c) {
        try {final var method=PveAdaptationStudy.class.getDeclaredMethod("combatEffects",Cell.class);method.setAccessible(true);return (PersonageEffects)method.invoke(null,new Cell(Mode.RAID,"MYCONID_COLONY","MAIN",c.tier(),c.members().size(),c.level(),c.effects()));}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static Metrics metrics(List<BattlePersonage> team) {
        final var p=parts(team);return new Metrics(stable(p.stream().mapToDouble(x->(double)x.get("H")).toArray()),stable(p.stream().mapToDouble(x->(double)x.get("D")).toArray()));
    }
    private static List<Map<String,Object>> parts(List<BattlePersonage> team) {
        return team.stream().map(p->{
            final double mu=stable(p.defenseReductions().values().stream().mapToDouble(Double::doubleValue).toArray())/AttackType.values().length;
            final double attack=p.attacksByRange().stream().mapToDouble(a->stable(a.attack().values().stream().mapToDouble(Integer::doubleValue).toArray())).max().orElse(0);
            final double critical=1+Math.clamp(p.critChance()/100.0,0.0,1.0)*(p.critMultiplier()-1),frequency=CombatRules.cappedTurnFrequency(p.initiative());
            return Map.<String,Object>of("id",p.id(),"hp",p.maxHealth(),"mu",mu,"H",p.maxHealth()/mu,"maximumAttack",attack,"criticalFactor",critical,"frequency",frequency,"D",attack*critical*frequency,"skills",p.skillSnapshots());
        }).toList();
    }
    private static double stable(double[] values){Arrays.sort(values);double result=0;for(final double x:values)result+=x;return result;}
    private static double q(int level){return 1+((level-10)*.06+.14);}

    private static Map<String,Object> run(Case c,Model model,BufferedWriter raw)throws Exception {
        final var scores=new ArrayList<Double>();final var counts=new TreeMap<String,Integer>();long rounds=0;int completed=0;
        for(int pair=0;pair<30;++pair) {
            final long gen=seed(GEN,c.groupId(),pair),battle=seed(BATTLE,c.groupId(),pair);double score=0;boolean complete=true;
            for(final boolean reverse:List.of(false,true)) {
                final var p=RandomUtils.withSeed(gen,()->prepare(c,model));
                final var row=new LinkedHashMap<String,Object>();row.put("case",c);row.put("model",model);row.put("pair",pair);row.put("reverse",reverse);
                row.put("generationSeed",gen);row.put("battleSeed",battle);row.put("unitSeeds",p.unitSeeds());row.put("playerMetrics",p.playerMetrics());row.put("calibration",p.calibration());
                row.put("playerParts",parts(p.players()));row.put("enemyParts",parts(p.enemies()));row.put("sourceEffects",effects(c).effects());row.put("combatEffects",effects(c).effects());
                try {
                    final var result=RandomUtils.withSeed(battle,()->new Battle(battle).process(reverse?p.players():p.enemies(),reverse?p.enemies():p.players(),2000,TEMPO));
                    final boolean pa=p.players().stream().anyMatch(BattlePersonage::isAlive),ea=p.enemies().stream().anyMatch(BattlePersonage::isAlive);final String outcome=pa?"WIN":ea?"LOSS":"DRAW";
                    counts.merge(outcome,1,Integer::sum);score+=pa?.5:ea?0:.25;rounds+=result.rounds();++completed;
                    row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());row.put("actionSha256",digest(result.actionLog()));row.put("traceSha256",digest(result.actionLog().traceEvents()));
                    row.put("actualEnemyPositions",p.enemies().stream().map(e->result.initState().lines().get(result.initState().personagesById().get(e.id()).lineIndex()).position()).toList());
                    row.put("actionCount",result.actionLog().events().size());row.put("normalAttempts",result.actionLog().traceEvents().stream().filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance).count());
                    row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
                    row.put("playerStats",p.players().stream().map(BattlePersonage::battlePersonageStats).toList());row.put("enemyStats",p.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
                }catch(IllegalStateException e){if(e.getMessage()==null||!e.getMessage().startsWith("Battle did not finish within"))throw e;complete=false;counts.merge("TIMEOUT",1,Integer::sum);row.put("outcome","TIMEOUT");row.put("rounds",2000);}
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));raw.newLine();
            }
            scores.add(complete?score:null);
        }
        final var finished=scores.stream().filter(java.util.Objects::nonNull).toList();final double mean=finished.stream().mapToDouble(Double::doubleValue).average().orElse(0),half=finished.isEmpty()?1:Math.sqrt(Math.log(40)/(2*finished.size()));
        return Map.of("case",c,"model",model,"pairs",30,"pairScores",scores,"outcomes",counts,"winrate",counts.getOrDefault("WIN",0)/60.0,"pairMean",mean,"pointwise95Hoeffding",List.of(Math.max(0,mean-half),Math.min(1,mean+half)),"meanRounds",completed==0?0:rounds/(double)completed);
    }

    private static Map<String,Object> bridges(List<Case> cases) {
        int rebuildCases=0;
        for(final var role:Role.values())for(final double m:List.of(.25,1.0,2.328571428571429,4.414285714285715,8.0))for(final long seed:List.of(1L,1541L,281474976710655L)) {
            final var a=actual(role,m,seed);final var b=rebuild(role,m,m,m,seed);if(!traits(List.of(a)).equals(traits(List.of(b))))throw new IllegalStateException("Factory rebuild differs "+role+" "+m);++rebuildCases;
        }
        for(final var role:Role.values()) {
            final var natural=actual(role,1,123);final var rebuilt=rebuild(role,2,3,referenceDefMultiplier,123);
            if(rebuilt.maxHealth()!=natural.maxHealth()*2||rebuilt.attackAtRange(1).get(AttackType.MAGICAL)!=natural.attackAtRange(1).get(AttackType.MAGICAL)*3||!rebuilt.skillSnapshots().equals(natural.skillSnapshots())||!rebuilt.id().equals(natural.id())||rebuilt.initiativeGauge()!=natural.initiativeGauge())throw new IllegalStateException("Independent axes/RNG failed");
        }
        final var anchor=cases.getFirst();final var same=RandomUtils.withSeed(154199,()->prepare(anchor,Model.SAME));final var cross=RandomUtils.withSeed(154199,()->prepare(anchor,Model.CROSS));
        if(!same.calibration().equals(cross.calibration())||!traits(same.enemies()).equals(traits(cross.enemies())))throw new IllegalStateException("SAME/CROSS anchor differs");
        for(final var model:Model.values()) {
            Prepared first=null;
            for(final var c:cases)if(c.variant().startsWith("COUNTER_")) {
                final var p=RandomUtils.withSeed(154120,()->prepare(c,model));
                if(!p.playerMetrics().equals(referencePlayers))throw new IllegalStateException("Countertwins no longer match reference neutral inputs");
                if(first==null)first=p;else if(!first.playerMetrics().equals(p.playerMetrics())||!first.calibration().equals(p.calibration())||!traits(first.enemies()).equals(traits(p.enemies())))throw new IllegalStateException("Type counter invariance failed");
            }
        }
        final var actualTail=RandomUtils.withSeed(154130,()->{final int first=RandomUtils.getWithMax(1000000);rebuild(Role.GUARDIAN,2,3,4,123);return List.of(first,RandomUtils.getWithMax(1000000));});
        final var expectedTail=RandomUtils.withSeed(154130,()->List.of(RandomUtils.getWithMax(1000000),RandomUtils.getWithMax(1000000)));
        if(!actualTail.equals(expectedTail))throw new IllegalStateException("Factory rebuild leaked RNG");
        int prefixes=0;
        for(final var c:cases)if(c.variant().endsWith("APPEND")) {
            final var base=cases.stream().filter(x->x.groupId()==c.groupId()&&x.variant().equals("BASE")).findFirst().orElseThrow();
            final var b=RandomUtils.withSeed(154140,()->prepare(base,Model.SAME));final var p=RandomUtils.withSeed(154140,()->prepare(c,Model.SAME));
            if(!traits(b.players()).equals(traits(p.players().subList(0,b.players().size()))))throw new IllegalStateException("Player prefix changed");++prefixes;
        }
        final var fx=cases.stream().filter(Case::effects).findFirst().orElseThrow();final var cell=new Cell(Mode.RAID,"MYCONID_COLONY","MAIN",fx.tier(),fx.members().size(),fx.level(),true);
        final var reference=RandomUtils.withSeed(154150,()->PveAdaptationStudy.prepare(cell));final var rebuilt=RandomUtils.withSeed(154150,()->prepare(fx,Model.SAME));
        if(!traits(reference.players()).equals(traits(rebuilt.players())))throw new IllegalStateException("Actual effects bridge differs");
        int levelChecks=0;final var checkedSizes=new HashSet<Integer>();
        for(final var c:cases)if(c.style().equals("MAIN")&&c.variant().equals("BASE")&&Set.of(1,3,6,30).contains(c.originalSize())&&checkedSizes.add(c.originalSize()))for(final var model:Model.values()) {
            Prepared previous=null;
            for(int level=1;level<=50;++level) {
                final var changed=new Case(c.id(),c.groupId(),c.style(),c.tier(),c.originalSize(),level,c.variant(),c.effects(),c.members());
                final var current=RandomUtils.withSeed(154160,()->prepare(changed,model));
                if(previous!=null)for(int i=0;i<current.enemies().size();++i) {
                    final var a=previous.enemies().get(i);final var b=current.enemies().get(i);
                    if(b.maxHealth()<a.maxHealth()||b.attackAtRange(1).get(AttackType.MAGICAL)<a.attackAtRange(1).get(AttackType.MAGICAL)||!a.defenses().equals(b.defenses()))throw new IllegalStateException("Level monotonicity/fixed DEF failed");
                }
                previous=current;++levelChecks;
            }
        }
        return Map.of("status","PASS","actualUnweightedFactoryRebuilds",rebuildCases,"independentAxes",2,"sameCrossAnchorExact",true,"counterType2x2ExactForBothModels",true,"outerRngTailExact",true,"playerPrefixesExact",prefixes,"actualPreparedEffectsExact",true,"levelMonotonicConstructionChecks",levelChecks);
    }
    private static List<Object> traits(List<BattlePersonage> team) {
        return team.stream().map(p->(Object)List.of(p.id(),p.initiativeGauge(),p.maxHealth(),p.attacksByRange(),p.defenses(),p.defenseReductions(),p.critChance(),p.dodgeChance(),p.critMultiplier(),p.initiative(),p.totalThreat(),p.impactStrength(),p.startPosition(),p.range(),p.targetingTactic(),p.skillSnapshots(),p.itemSnapshots(),p.legacyPower())).toList();
    }

    private static List<Case> cases()throws Exception {
        final var w=MyconidAdaptationStudy.class.getDeclaredMethod("weakWithDefaults");w.setAccessible(true);final var weak=(InputMember)w.invoke(null);
        if(RandomUtils.withSeed(0,()->players(List.of(weak),PersonageEffects.EMPTY)).getFirst().maxHealth()!=840)throw new IllegalStateException("Weak fixture wrong");
        final var r=new ArrayList<Case>();int group=0;
        for(final var tier:List.of(Tier.MID,Tier.STRONG))normal(r,++group,tier,3,25,weak,false);
        for(final int level:List.of(25,40))for(final var tier:List.of(Tier.MID,Tier.STRONG))normal(r,++group,tier,6,level,weak,tier==Tier.MID&&level==25);
        normal(r,++group,Tier.MID,12,50,weak,true);
        for(final var style:List.of("MAIN","PIERCE_ARCANE","RITUAL"))for(final int level:style.equals("MAIN")?List.of(1,5,10):style.equals("PIERCE_ARCANE")?List.of(5,15,25):List.of(10,25,40)) {
            final var base=inputs(style,Tier.STRONG,1,level);++group;add(r,group,style,Tier.STRONG,1,level,"BASE",false,base);add(r,group,style,Tier.STRONG,1,level,"WEAK_APPEND",false,append(base,weak));
        }
        add(r,++group,"RITUAL",Tier.BEGINNER,15,50,"BASE",false,inputs("RITUAL",Tier.BEGINNER,15,50));
        for(final int level:List.of(30,50))add(r,++group,"MAIN",Tier.MID,30,level,"BASE",false,inputs("MAIN",Tier.MID,30,level));
        ++group;for(final var attack:List.of(AttackType.PIERCE,AttackType.SLASH))for(final var defense:List.of(DefenseType.ARCANE,DefenseType.LEATHER))add(r,group,"COUNTER",Tier.MID,3,25,"COUNTER_"+attack+"_"+defense,false,retag(inputs("MAIN",Tier.MID,3,25),attack,defense));
        for(final int level:List.of(5,30,50)) {final int n=level==5?1:30;add(r,++group,"WEAK_ONLY",Tier.BEGINNER,n,level,"BASE",false,java.util.Collections.nCopies(n,weak));}
        for(final int level:List.of(30,50)) {final var members=new ArrayList<>(inputs("MAIN",Tier.STRONG,1,level));while(members.size()<30)members.add(weak);add(r,++group,"STRONG_PLUS_WEAK",Tier.STRONG,30,level,"BASE",false,members);}
        normal(r,++group,Tier.MID,8,25,weak,true);
        final var base=r.getFirst();add(r,base.groupId(),"MAIN",Tier.MID,3,25,"EFFECTS",true,base.members());
        if(r.size()!=50||group!=26)throw new IllegalStateException("Approved50case screen changed: "+r.size());return List.copyOf(r);
    }
    private static void normal(List<Case> r,int group,Tier tier,int n,int level,InputMember weak,boolean addWeak) {
        final var base=inputs("MAIN",tier,n,level);add(r,group,"MAIN",tier,n,level,"BASE",false,base);add(r,group,"MAIN",tier,n,level,"NORMAL_APPEND",false,append(base,inputs("MAIN",tier,n+1,level).get(n)));if(addWeak)add(r,group,"MAIN",tier,n,level,"WEAK_APPEND",false,append(base,weak));
    }
    private static void add(List<Case> r,int g,String style,Tier tier,int n,int level,String variant,boolean effects,List<InputMember> members){r.add(new Case(r.size()+1,g,style,tier,n,level,variant,effects,List.copyOf(members)));}
    private static List<InputMember> append(List<InputMember> base,InputMember extra){final var result=new ArrayList<>(base);result.add(extra);return List.copyOf(result);}
    private static List<InputMember> inputs(String style,Tier tier,int n,int level){return PveAdaptationStudy.inputFor(new Cell(Mode.RAID,"MYCONID_COLONY",style,tier,n,level,false));}
    private static List<InputMember> retag(List<InputMember> members,AttackType attack,DefenseType defense) {
        return members.stream().map(m->new InputMember(m.build(),m.position(),m.tactic(),m.items().stream().map(i->{final var o=i.object();return new Item(new ItemObject(o.code(),o.slots(),o.attacks().stream().map(a->new ItemAttack(attack,a.minRange(),a.maxRange(),a.attack())).toList(),o.defense().map(d->new ItemDefense(defense,d.defense())),o.health(),o.critChance(),o.dodgeChance(),o.critMultiplier(),o.speed(),o.baseThreat(),o.impact(),o.progressionVersion(),o.locales()),i.modifier(),i.rarity(),i.enhanceLevel());}).toList())).toList();
    }
    private static Field field(Class<?> owner,String name){try{final var f=owner.getDeclaredField(name);f.setAccessible(true);return f;}catch(Exception e){throw new IllegalStateException(e);}}
    private static long seed(String namespace,int group,int pair){try{final var bytes=MessageDigest.getInstance("SHA-256").digest((namespace+"/"+group+"/"+pair).getBytes(StandardCharsets.UTF_8));long value=0;for(int i=0;i<6;++i)value=(value<<8)|(bytes[i]&255L);return value;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String digest(Object value)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PveAdaptationStudy.JSON.writeValueAsBytes(value)));}
    private static void write(Path dir,String name,Object value)throws Exception{Files.writeString(dir.resolve(name),PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(value),StandardOpenOption.CREATE_NEW);}
}
