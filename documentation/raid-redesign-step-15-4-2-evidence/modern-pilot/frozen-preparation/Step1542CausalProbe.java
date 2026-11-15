package ru.homyakin.seeker.game.battle;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.raid.generator.HeadLegacyMyconidOracle;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.catalog.*;
import ru.homyakin.seeker.game.item.models.*;
import ru.homyakin.seeker.game.item.modifier.ModifierCompatibility;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.ResourceUtils;

/** Fixed15.4.2 causal pilot. Current engine, no production mutation or new formula. */
public final class Step1542CausalProbe {
    private static final SkillFormulaVersion LEGACY=SkillFormulaVersion.LEGACY_SKILLS_V1,V3=SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final String GEN="MYCONID-15.4.2-CAUSAL-PILOT-GEN-20261108-v1",BATTLE="MYCONID-15.4.2-CAUSAL-PILOT-BATTLE-20261108-v1";
    private static final int PAIRS=30,LIMIT=2000;
    private static final Constructor<BattlePersonage> CONSTRUCTOR=constructor();
    private static final LoadedEquipmentCatalog NEW=EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5);
    private static final Path OLD_DATA=Path.of(System.getProperty("causal.oldData","/tmp/seeker-step15-4-2-causal.1HOgPK/old-data"));
    private static final Map<String,ItemObject> OLD_OBJECTS=oldObjects();
    private static final Map<ActiveEnum,Modifier> OLD_MODIFIERS=oldModifiers();
    private static final Map<String,ItemObject> NEW_OBJECTS=NEW.itemObjects().stream().collect(Collectors.toUnmodifiableMap(ItemObject::code,Function.identity()));
    private static final Map<ActiveEnum,Modifier> NEW_MODIFIERS=NEW.modifiers().stream().collect(Collectors.toUnmodifiableMap(Modifier::activeEnum,Function.identity()));
    private record Phase(String id,String gear,SkillFormulaVersion playerSkills,SkillFormulaVersion npcSkills,boolean force,BattleTempoVersion tempo,int slingerRange,boolean allLog4,boolean actualQ,boolean exactMain) { }
    private record Scenario(int id,String name,String tier,int originalSize,boolean appended,List<InputMember> oldMembers,List<InputMember> newMembers) { }
    private record Cell(int id,Phase phase,int scenarioId,int level,int groupId,List<InputMember> members) { }
    private record Prepared(List<BattlePersonage> players,List<BattlePersonage> enemies,Map<String,Object> calibration,Map<String,Object> diagnostics) { }

    public static void main(String[] args)throws Exception {
        if(args.length!=2||!List.of("prepare","screen").contains(args[0]))throw new IllegalArgumentException("prepare|screen NEW_DIRECTORY");
        final var root=Path.of(args[1]);Files.createDirectory(root);
        final var scenarios=scenarios();final var phases=phases();final var cells=cells(phases,scenarios);
        final var bridge=bridges(scenarios,phases,cells);
        final var seeds=new ArrayList<Map<String,Object>>();
        for(final int group:cells.stream().map(Cell::groupId).distinct().toList())for(int pair=0;pair<PAIRS;++pair)seeds.add(Map.of("groupId",group,"pair",pair,"generationSeed",seed(GEN,group,pair),"battleSeed",seed(BATTLE,group,pair)));
        final var manifest=new LinkedHashMap<String,Object>();
        manifest.put("purpose","FIXED15.4.2_CAUSAL_PILOT_NOT_NEW_FORMULA_OR_ACCEPTANCE");manifest.put("conditions",84);manifest.put("pairs",PAIRS);manifest.put("battles",5040);manifest.put("expectedIndependentHistoricalBattles",480);manifest.put("combinedApprovedBattles",5520);
        manifest.put("phases",phases);manifest.put("levels",List.of(10,25));manifest.put("generationNamespace",GEN);manifest.put("battleNamespace",BATTLE);
        manifest.put("seedPolicy","First48BE SHA256(namespace+'/'+groupId+'/'+pair); groupId=10*level+(T3=1,S1=2); no phase/variant. Exact current MAIN uses T3 cohort.120distinctseedrecords.");
        manifest.put("controllerPolicy","HEAD32step SUMactualLegacyPower*q*g*.77, unweighted roles; no PveCharacteristicScaling.search and no V3Q except declared aggregate referencesC9/C10");
        manifest.put("scope","C0 is CURRENT engine with old gear/skills, not the true historical engine; C2 changes the complete newer turn execution/combat-rules mode, NOT only initiative ordering; V3skill switch also removes its old skillPowerRating from legacyPower; actual/proxy reported.");
        manifest.put("effects","NONE");manifest.put("qL","1+((L-10)*.06+.14)");manifest.put("roundLimit",LIMIT);manifest.put("bridge",bridge);manifest.put("phaseComparisons","Sequential changes are conditional, not independent additive causes; C4/C5/C6/C8 is the tempo×range2x2; C7 onlyg; C9/C10 aggregate references, not single-factor transitions");
        manifest.put("fullActionLogPolicy","Full actionLog including traceEvents for pair0/1 in every condition and bothmirrors; hashes/counts/individualstats for all5040battles");
        write(root,"fixture-manifest.json",Map.of("scenarios",scenarios,"fixtureSpecs",specs(scenarios),"projection","G onlyGUARDmodifierremoved,RAREshieldretained; MstaffRAREDOUBLE2+COMMONorb; allothercanonical MID+3/STRONG+6 carriers unchanged; oldweak720HP,newweak840HP"));
        write(root,"manifest.json",manifest);write(root,"inputs.json",cells);write(root,"seeds.json",seeds);
        final var prepared=new ArrayList<Map<String,Object>>();
        for(final var c:cells) {
            final var p=RandomUtils.withSeed(seed(GEN,c.groupId(),0),()->prepare(c));
            prepared.add(Map.of("caseId",c.id(),"phase",c.phase().id(),"scenarioId",c.scenarioId(),"level",c.level(),"players",traits(p.players()),"enemies",traits(p.enemies()),"calibration",p.calibration(),"diagnostics",p.diagnostics()));
        }
        write(root,"prepared-pair0.json",prepared);
        if(args[0].equals("prepare"))return;
        final var summary=new ArrayList<Map<String,Object>>();
        try(var raw=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(root.resolve("battles.jsonl.gz"),StandardOpenOption.CREATE_NEW)),StandardCharsets.UTF_8))) {
            for(final var c:cells) {summary.add(run(c,raw));System.out.println("done="+summary.size()+" "+c.phase().id()+" scenario"+c.scenarioId()+" L"+c.level());}
        }
        write(root,"summary.json",Map.of("manifest",manifest,"cases",summary));
    }
    private static List<Phase> phases() {
        final var b=BattleTempoVersion.BASELINE_V1;final var a=BattleTempoVersion.ACCELERATED_V1;
        return List.of(new Phase("C0","OLD",LEGACY,LEGACY,false,b,1,false,false,false),new Phase("C1","V5",LEGACY,LEGACY,false,b,1,false,false,false),
            new Phase("C2","V5",LEGACY,LEGACY,true,b,1,false,false,false),new Phase("C3","V5",V3,LEGACY,true,b,1,false,false,false),
            new Phase("C4","V5",V3,V3,true,b,1,false,false,false),new Phase("C5","V5",V3,V3,true,a,1,false,false,false),
            new Phase("C6","V5",V3,V3,true,a,3,false,false,false),new Phase("C7","V5",V3,V3,true,a,3,true,false,false),
            new Phase("C8","V5",V3,V3,true,b,3,false,false,false),new Phase("C9","V5",V3,V3,true,a,3,true,true,false),
            new Phase("C10","V5",V3,V3,true,a,3,true,true,true));
    }
    private static List<Scenario> scenarios() {
        final var current3=PveAdaptationStudy.inputFor(new PveAdaptationStudy.Cell(PveAdaptationStudy.Mode.RAID,"MYCONID_COLONY","MAIN",Tier.MID,3,25,false));
        final var current4=PveAdaptationStudy.inputFor(new PveAdaptationStudy.Cell(PveAdaptationStudy.Mode.RAID,"MYCONID_COLONY","MAIN",Tier.MID,4,25,false));
        final var strong=PveAdaptationStudy.inputFor(new PveAdaptationStudy.Cell(PveAdaptationStudy.Mode.RAID,"MYCONID_COLONY","MAIN",Tier.STRONG,1,25,false));
        final var projected3=current3.stream().map(Step1542CausalProbe::project).toList();final var projected4=append(projected3,current4.get(3));
        final var newWeak=weak(false);final var oldWeak=weak(true);
        return List.of(new Scenario(1,"T3","MID",3,false,old(projected3),projected3),new Scenario(2,"T3_PLUS_BRUISER","MID",3,true,old(projected4),projected4),
            new Scenario(3,"S1","STRONG",1,false,old(strong),strong),new Scenario(4,"S1_PLUS_WEAK","STRONG",1,true,append(old(strong),oldWeak),append(strong,newWeak)),
            new Scenario(5,"EXACT_MAIN3","MID",3,false,List.of(),current3),new Scenario(6,"EXACT_MAIN3_PLUS_BRUISER","MID",3,true,List.of(),append(current3,current4.get(3))));
    }
    private static InputMember project(InputMember m) {
        final var items=new ArrayList<Item>();
        for(final var i:m.items()) {
            if(i.object().code().equals("battle_staff")) {items.add(new Item(NEW_OBJECTS.get("staff"),Optional.of(NEW_MODIFIERS.get(ActiveEnum.DOUBLE_ATTACK)),ItemRarity.RARE,i.enhanceLevel()));items.add(new Item(NEW_OBJECTS.get("orb"),Optional.empty(),ItemRarity.COMMON,i.enhanceLevel()));}
            else if(i.modifier().map(x->x.activeEnum()==ActiveEnum.GUARD).orElse(false))items.add(new Item(i.object(),Optional.empty(),i.rarity(),i.enhanceLevel()));
            else items.add(i);
        }
        return new InputMember(m.build(),m.position(),m.tactic(),List.copyOf(items));
    }
    private static List<InputMember> old(List<InputMember> ms) {
        return ms.stream().map(m->new InputMember(m.build(),m.position(),m.tactic(),m.items().stream().map(i->{
            final var object=Objects.requireNonNull(OLD_OBJECTS.get(i.object().code()),"Nonshared item "+i.object().code());
            return new Item(object,i.modifier().map(x->Objects.requireNonNull(OLD_MODIFIERS.get(x.activeEnum()))),i.rarity(),i.enhanceLevel());}).toList())).toList();
    }
    private static InputMember weak(boolean old) {
        final var sword=(old?OLD_OBJECTS:NEW_OBJECTS).get("sword");final var items=new ArrayList<Item>();items.add(new Item(sword,Optional.empty(),ItemRarity.COMMON,0));
        final var occupied=EnumSet.copyOf(sword.slots());
        if(old)for(final var slot:PersonageSlot.values())DefaultItems.defaultItemForSlot(slot,occupied).ifPresent(i->{items.add(i);occupied.addAll(i.object().slots());});
        else for(final var object:NEW.defaultItems())if(object.slots().stream().noneMatch(occupied::contains)){items.add(Item.fromObject(object));occupied.addAll(object.slots());}
        if(items.size()!=7||!occupied.equals(EnumSet.allOf(PersonageSlot.class)))throw new IllegalStateException("Weak slots");
        return new InputMember("WEAK_WITH_DEFAULTS",Position.FRONT,TargetingTactic.THREAT,List.copyOf(items));
    }
    private static List<Cell> cells(List<Phase> phases,List<Scenario> scenarios) {
        final var out=new ArrayList<Cell>();
        for(final var phase:phases)for(final int level:List.of(10,25))for(final var s:scenarios)if(phase.exactMain()==(s.id()>=5)) {
            out.add(new Cell(out.size()+1,phase,s.id(),level,10*level+(s.id()==3||s.id()==4?2:1),phase.gear().equals("OLD")?s.oldMembers():s.newMembers()));
        }
        if(out.size()!=84)throw new IllegalStateException("Approved84conditions");return List.copyOf(out);
    }
    private static Prepared prepare(Cell c) {
        final var players=new ArrayList<BattlePersonage>();
        for(final var input:c.members()) {
            final var p=c.phase().actualQ()?BattlePersonage.forCombat(input.items(),input.position(),PersonageEffects.EMPTY,Optional.of(input.build()),c.phase().playerSkills()):construct(input.items(),input.position(),Optional.of(input.build()),c.phase().playerSkills(),c.phase().force());
            p.setTargetingTactic(input.tactic());players.add(p);
        }
        final List<BattlePersonage> enemies;final var cal=new LinkedHashMap<String,Object>();final var diag=new LinkedHashMap<String,Object>();
        if(c.phase().actualQ()) {
            final var actual=new MyconidColonyGenerator().generateCalibrated(players,q(c.level()),V3);enemies=actual.enemies();
            cal.put("metric","ACTUAL_PRODUCTION15.2_Q");cal.put("result",actual.calibration());diag.put("generationDiagnostics",actual.diagnostics());
            cal.put("multiplier",actual.calibration().multiplier());cal.put("targetPower",actual.calibration().targetPower());
            cal.put("groupScaling",actual.diagnostics().orElseThrow().parameters().get("groupSizeScaling"));
            cal.put("encounterCorrection",actual.diagnostics().orElseThrow().parameters().get("encounterCorrection"));
        }else {
            final var actual=new LegacyPhaseMyconidController(c.phase().npcSkills(),c.phase().force(),c.phase().slingerRange(),c.phase().allLog4()).generate(players,q(c.level()));enemies=actual.enemies();
            cal.put("metric","HEAD32_SUM_ACTUAL_LEGACY_POWER");cal.put("multiplier",actual.multiplier());cal.put("targetPower",actual.targetPower());cal.put("groupScaling",actual.groupScaling());cal.put("encounterCorrection",.77);
        }
        final double playerPower=power(players),enemyPower=power(enemies);cal.put("q",q(c.level()));cal.put("actualPlayerLegacyPower",playerPower);cal.put("actualEnemyLegacyPower",enemyPower);
        diag.put("actualPlayerScalingRules",players.stream().map(BattlePersonage::usesScalingCombatRules).toList());diag.put("actualEnemyScalingRules",enemies.stream().map(BattlePersonage::usesScalingCombatRules).toList());
        diag.put("globalScalingSkillOrder",java.util.stream.Stream.concat(players.stream(),enemies.stream()).anyMatch(BattlePersonage::usesScalingCombatRules));
        final boolean unavailable=c.members().stream().flatMap(x->x.items().stream()).flatMap(i->i.modifier().stream()).anyMatch(x->Set.of(ActiveEnum.GUARD,ActiveEnum.ACCUMULATION,ActiveEnum.PENETRATION,ActiveEnum.TEMPO_BREAK).contains(x.activeEnum()));
        if(unavailable)diag.put("legacyPlayerProxy","UNAVAILABLE_FOR_EXACT_NEW_GUARD_INPUT");
        else diag.put("legacyPlayerProxy",RandomUtils.withSeed(0,()->power(c.members().stream().map(i->construct(i.items(),i.position(),Optional.empty(),LEGACY,false)).toList())));
        diag.put("enemyRanges",enemies.stream().map(BattlePersonage::range).toList());
        if(!c.phase().actualQ()) {
            final double m=(double)cal.get("multiplier");final int n=enemies.size();
            diag.put("legacyEnemyProxy",RandomUtils.withSeed(0,()->LegacyPhaseMyconidController.factory(true,m,LEGACY,false,c.phase().slingerRange()).legacyPower()*(n/2)+LegacyPhaseMyconidController.factory(false,m,LEGACY,false,c.phase().slingerRange()).legacyPower()*(n-n/2)));
        }
        final var ids=new java.util.HashSet<java.util.UUID>();for(final var p:java.util.stream.Stream.concat(players.stream(),enemies.stream()).toList())if(!ids.add(p.id()))throw new IllegalStateException("Duplicate UUID");
        return new Prepared(List.copyOf(players),List.copyOf(enemies),cal,diag);
    }
    static BattlePersonage construct(List<Item> items,Position position,Optional<String> name,SkillFormulaVersion version,boolean force) {
        try{return CONSTRUCTOR.newInstance(items,position,Map.of(),PersonageEffects.EMPTY,null,name,version,Map.of(),force);}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static Constructor<BattlePersonage> constructor() {
        try{final var c=BattlePersonage.class.getDeclaredConstructor(List.class,Position.class,Map.class,PersonageEffects.class,LocalDateTime.class,Optional.class,SkillFormulaVersion.class,Map.class,boolean.class);c.setAccessible(true);return c;}catch(Exception e){throw new IllegalStateException(e);}
    }
    private static double power(List<BattlePersonage> ps){return ps.stream().mapToDouble(BattlePersonage::legacyPower).sum();}
    private static Map<String,Object> run(Cell c,BufferedWriter raw)throws Exception {
        final var outcomes=new TreeMap<String,Integer>();final var scores=new ArrayList<Double>();long rounds=0,actions=0;int completed=0;
        for(int pair=0;pair<PAIRS;++pair) {
            final long gen=seed(GEN,c.groupId(),pair),battle=seed(BATTLE,c.groupId(),pair);double score=0;boolean complete=true;
            for(final boolean reverse:List.of(false,true)) {
                final var p=RandomUtils.withSeed(gen,()->prepare(c));final var row=new LinkedHashMap<String,Object>();
                row.put("case",c);row.put("phase",c.phase().id());row.put("scenarioId",c.scenarioId());row.put("level",c.level());row.put("pair",pair);row.put("reverse",reverse);row.put("mirror",reverse);row.put("generationSeed",gen);row.put("battleSeed",battle);row.put("calibration",p.calibration());row.put("diagnostics",p.diagnostics());row.put("playerTraits",traits(p.players()));row.put("enemyTraits",traits(p.enemies()));
                try {
                    final var result=RandomUtils.withSeed(battle,()->new Battle(battle).process(reverse?p.players():p.enemies(),reverse?p.enemies():p.players(),LIMIT,c.phase().tempo()));
                    final boolean pa=p.players().stream().anyMatch(BattlePersonage::isAlive),ea=p.enemies().stream().anyMatch(BattlePersonage::isAlive);final String outcome=pa?"WIN":ea?"LOSS":"DRAW";
                    outcomes.merge(outcome,1,Integer::sum);score+=pa?.5:ea?0:.25;rounds+=result.rounds();actions+=result.actionLog().events().size();++completed;
                    row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());row.put("actionSha256",digest(result.actionLog()));row.put("traceSha256",digest(result.actionLog().traceEvents()));
                    if(pair<2)row.put("actionLog",result.actionLog());
                    row.put("actualEnemyPositions",positions(p.enemies(),result));row.put("actualPlayerPositions",positions(p.players(),result));
                    row.put("actionCount",result.actionLog().events().size());row.put("normalAttempts",result.actionLog().traceEvents().stream().filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance).count());
                    row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
                }catch(IllegalStateException e){if(e.getMessage()==null||!e.getMessage().startsWith("Battle did not finish within"))throw e;complete=false;outcomes.merge("TIMEOUT",1,Integer::sum);row.put("outcome","TIMEOUT");row.put("rounds",LIMIT);}
                row.put("playerStats",p.players().stream().map(BattlePersonage::battlePersonageStats).toList());row.put("enemyStats",p.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
                raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));raw.newLine();
            }
            scores.add(complete?score:null);
        }
        final var done=scores.stream().filter(Objects::nonNull).toList();final double mean=done.stream().mapToDouble(Double::doubleValue).average().orElse(0),half=done.isEmpty()?1:Math.sqrt(Math.log(40)/(2*done.size()));
        return Map.of("case",c,"pairs",PAIRS,"pairScores",scores,"outcomes",outcomes,"winrate",outcomes.getOrDefault("WIN",0)/60.0,"pairMean",mean,"pointwise95Hoeffding",List.of(Math.max(0,mean-half),Math.min(1,mean+half)),"meanRounds",completed==0?0:rounds/(double)completed,"meanActions",completed==0?0:actions/(double)completed);
    }
    private static List<Position> positions(List<BattlePersonage> ps,BattleResult result){return ps.stream().map(p->result.initState().lines().get(result.initState().personagesById().get(p.id()).lineIndex()).position()).toList();}
    private static Map<String,Object> bridges(List<Scenario> scenarios,List<Phase> phases,List<Cell> cells) {
        if(OLD_OBJECTS.size()!=40||OLD_MODIFIERS.size()!=11||!NEW_OBJECTS.keySet().containsAll(OLD_OBJECTS.keySet()))throw new IllegalStateException("Shared40 catalog");
        int checks=0;
        for(final var c:cells)if(c.phase().id().equals("C0"))for(final long seed:List.of(1L,1542L,281474976710655L)) {
            final var expected=RandomUtils.withSeed(seed,()->{final var players=new ArrayList<BattlePersonage>();for(final var i:c.members()){final var p=new BattlePersonage(i.items(),i.position(),Optional.of(i.build()));p.setTargetingTactic(i.tactic());players.add(p);}final var enemies=new HeadLegacyMyconidOracle().generate(players,q(c.level()));return List.of(traits(players),traits(enemies),RandomUtils.getWithMax(1000000));});
            final var actual=RandomUtils.withSeed(seed,()->{final var p=prepare(c);return List.of(traits(p.players()),traits(p.enemies()),RandomUtils.getWithMax(1000000));});
            if(!expected.equals(actual))throw new IllegalStateException("HEAD32oracle differs "+c.id());++checks;
        }
        int fixtureChecks=0;
        for(final var s:scenarios)for(final var ms:List.of(s.oldMembers(),s.newMembers()))for(final var member:ms) {
            final var occupied=EnumSet.noneOf(PersonageSlot.class);
            for(final var i:member.items()) {for(final var slot:i.object().slots())if(!occupied.add(slot))throw new IllegalStateException("Duplicate equipment slot");
                if(i.modifier().isPresent())for(final var slot:i.object().slots())if(!ModifierCompatibility.isCompatible(i.object(),i.modifier().get(),slot))throw new IllegalStateException("Illegal modifier");}
            if(!occupied.equals(EnumSet.allOf(PersonageSlot.class)))throw new IllegalStateException("Missing equipment slot");++fixtureChecks;
        }
        for(final var s:scenarios)if(s.appended()) {
            final var base=scenarios.get(s.id()-2);
            if(!s.newMembers().subList(0,base.newMembers().size()).equals(base.newMembers())||!s.oldMembers().isEmpty()&&!s.oldMembers().subList(0,base.oldMembers().size()).equals(base.oldMembers()))throw new IllegalStateException("Input prefix");
        }
        final int oldHp=construct(List.of(weak(true).items().toArray(Item[]::new)),Position.FRONT,Optional.empty(),LEGACY,false).maxHealth();
        final int newHp=construct(weak(false).items(),Position.FRONT,Optional.empty(),V3,true).maxHealth();
        if(oldHp!=720||newHp!=840)throw new IllegalStateException("Weak defaults differ");
        for(final var phase:phases)if(!phase.actualQ()) {
            final var p=RandomUtils.withSeed(11,()->LegacyPhaseMyconidController.factory(false,2,phase.npcSkills(),phase.force(),phase.slingerRange()));
            if(p.range()!=phase.slingerRange()||p.skillSnapshots().size()!=1||p.skillSnapshots().getFirst().points()!=4||p.skillSnapshots().getFirst().formulaVersion()!=phase.npcSkills()||p.usesScalingCombatRules()!=(phase.force()||phase.npcSkills().isScaling()))throw new IllegalStateException("Independent factory flags");
        }
        for(final int n:List.of(7,8))for(final boolean all:List.of(false,true)) {
            final double g=1+ru.homyakin.seeker.utils.MathUtils.log(!all&&n<=7?2.2:4,n)*.05;if(!Double.isFinite(g)||g<=1)throw new IllegalStateException("g branch");
        }
        return Map.of("status","PASS","head32OracleFullTraitsAndRngTail",checks,"legalFullEquipmentMembers",fixtureChecks,"oldWeakHP",oldHp,"newWeakHP",newHp,"independentNpcSkillRangeForceChecks",9,"gBranchConstructionChecks",4,"noBattlesInPreparation",true);
    }
    private static List<Map<String,Object>> traits(List<BattlePersonage> ps) {
        return ps.stream().map(p->{final var r=new LinkedHashMap<String,Object>();r.put("id",p.id());r.put("gauge",p.initiativeGauge());r.put("hp",p.maxHealth());r.put("attacks",p.attacksByRange());r.put("defenses",p.defenses());r.put("reductions",p.defenseReductions());r.put("crit",p.critChance());r.put("dodge",p.dodgeChance());r.put("critMultiplier",p.critMultiplier());r.put("speed",p.initiative());r.put("threat",p.totalThreat());r.put("impact",p.impactStrength());r.put("position",p.startPosition());r.put("range",p.range());r.put("tactic",p.targetingTactic());r.put("skills",p.skillSnapshots());r.put("items",p.itemSnapshots());r.put("legacyPower",p.legacyPower());r.put("scalingRules",p.usesScalingCombatRules());return(Map<String,Object>)r;}).toList();
    }
    private static List<Map<String,Object>> specs(List<Scenario> ss) {
        final var out=new ArrayList<Map<String,Object>>();
        for(final var s:ss)if(s.id()<=4)for(final String gear:List.of("OLD","V5")) {
            final var members=(gear.equals("OLD")?s.oldMembers():s.newMembers()).stream().map(m->Map.of("build",m.build(),"position",m.position(),"tactic",m.tactic(),"items",m.items().stream().map(i->{final var r=new LinkedHashMap<String,Object>();r.put("code",i.object().code());r.put("rarity",i.rarity());r.put("enhanceLevel",i.enhanceLevel());r.put("modifier",i.modifier().map(Modifier::activeEnum).orElse(null));return r;}).toList())).toList();
            out.add(Map.of("scenarioId",s.id(),"name",s.name(),"gear",gear,"members",members));
        }
        return out;
    }
    private static List<InputMember> append(List<InputMember> base,InputMember added){final var r=new ArrayList<>(base);r.add(added);return List.copyOf(r);}
    private static Map<String,ItemObject> oldObjects(){try(var in=Files.newInputStream(OLD_DATA.resolve("item_objects_catalog.toml"))){return ItemObjectsToml.load(in).itemObjects().stream().collect(Collectors.toUnmodifiableMap(ItemObject::code,Function.identity()));}catch(Exception e){throw new IllegalStateException(e);}}
    private static Map<ActiveEnum,Modifier> oldModifiers(){try(var in=Files.newInputStream(OLD_DATA.resolve("item_modifiers_catalog.toml"))){return ItemModifiersToml.load(in).modifiers().stream().collect(Collectors.toUnmodifiableMap(Modifier::activeEnum,Function.identity()));}catch(Exception e){throw new IllegalStateException(e);}}
    private static double q(int l){return 1+((l-10)*.06+.14);}
    private static long seed(String ns,int group,int pair){try{final var b=MessageDigest.getInstance("SHA-256").digest((ns+"/"+group+"/"+pair).getBytes(StandardCharsets.UTF_8));long n=0;for(int i=0;i<6;++i)n=n<<8|(b[i]&255L);return n;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String digest(Object o)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PveAdaptationStudy.JSON.writeValueAsBytes(o)));}
    private static void write(Path root,String name,Object o)throws Exception{Files.writeString(root.resolve(name),PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(o),StandardOpenOption.CREATE_NEW);}
}
