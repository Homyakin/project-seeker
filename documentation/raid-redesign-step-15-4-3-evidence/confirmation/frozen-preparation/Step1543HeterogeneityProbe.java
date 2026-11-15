package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.JsonNode;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.HexFormat;
import java.security.MessageDigest;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.PveAdaptationStudy.InputMember;
import ru.homyakin.seeker.game.battle.Step123ReferenceTeams.Tier;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.event.pve.GeneratedPveEnemies;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling;
import ru.homyakin.seeker.game.event.pve.PveCharacteristicScaling.WeightedBase;
import ru.homyakin.seeker.game.event.pve.PveScalingDiagnostics;
import ru.homyakin.seeker.game.event.pve.PveTeamAssessment;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.models.*;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogLoader;
import ru.homyakin.seeker.game.item.catalog.EquipmentCatalogVersion;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;

/** Outside-production 15.4.3 fresh frozen confirmation; no production writes. */
public final class Step1543HeterogeneityProbe {
    private static final SkillFormulaVersion V3=SkillFormulaVersion.SCALING_SKILLS_V3;
    private static final MyconidColonyGenerator ACTUAL=new MyconidColonyGenerator();
    private static final Class<?> FORMATION=Arrays.stream(MyconidColonyGenerator.class.getDeclaredClasses()).filter(x->x.getSimpleName().equals("Formation")).findFirst().orElseThrow();
    private static final Constructor<?> FORMATION_CTOR=constructor(FORMATION,int.class);
    private static final Method COLONY=method(MyconidColonyGenerator.class,"teamColony",FORMATION,double.class,SkillFormulaVersion.class);
    private static final Method FRONT=method(FORMATION,"frontCount"),BACK=method(FORMATION,"backCount"),FW=method(FORMATION,"frontWeight"),BW=method(FORMATION,"backWeight");
    private static final double C=constant("TEAM_CALIBRATION_CORRECTION"),FLOOR=constant("GROUP_CORRECTION_FLOOR"),REF=constant("GROUP_CORRECTION_REFERENCE_COUNT"),GROUP=constant("GROUP_SCALE_COEF");
    private static final int GA=(int)constant("BASE_GUARDIAN_ATTACK"),GH=(int)constant("BASE_GUARDIAN_HEALTH"),GD=(int)constant("BASE_GUARDIAN_DEFENSE"),SA=(int)constant("BASE_SLINGER_ATTACK"),SH=(int)constant("BASE_SLINGER_HEALTH"),SD=(int)constant("BASE_SLINGER_DEFENSE");
    private static Path sourceInputs;
    private static final int LIMIT=2000;
    private static final String GEN="MYCONID-15.4.3-HETEROGENEITY-CONFIRM-GEN-20261108-v1",BATTLE="MYCONID-15.4.3-HETEROGENEITY-CONFIRM-BATTLE-20261108-v1",NPC="MYCONID-15.4.3-HETEROGENEITY-CONFIRM-NPC-20261108-v1";
    private static final Set<Integer> SELECTED=Set.of(1,2,3,4,5,6,7,8,9,10,11,12,13,14,19,20,25,26,49,50,51,52,53,54,55,56,63,64,65,66,67,68,69,70);
    private enum Rule { BASELINE,BLEND_B,BLEND_T }
    private record Case(int id,int groupId,String family,String style,Tier tier,int level,String variant,List<InputMember> members,String origin) { }
    private record Contribution(java.util.UUID id,double H,double D,double p) { }
    private record Evaluation(double Q,double rawB,double rawT,int bestK,double boundedB,double boundedT,double F,double discount,boolean exactSoloOrIdentical) { }
    private record Target(double assessment,double levelPowerBonus,int count,int frontCount,int backCount,double frontWeight,double backWeight,double groupScaling,double sizeCorrection,double encounterCorrection,double baselineTarget,double discount,double requestedTarget) { }
    private record Calibrated(GeneratedPveEnemies generated,Target target,Evaluation evaluation) { }
    private record Prepared(List<BattlePersonage> players,GeneratedPveEnemies generated,Target target,List<Contribution> contributions,Evaluation playerEvaluation,Evaluation calibrationEvaluation) { }

    public static void main(String[] args)throws Exception {
        if(args.length!=2||!List.of("prepare","screen").contains(args[0]))throw new IllegalArgumentException("prepare|screen NEW_OUTPUT_DIRECTORY");
        final var out=Path.of(args[1]);Files.createDirectory(out);sourceInputs=Path.of(System.getProperty("hetero.sourceInputs","/tmp/seeker-step15-4-3-confirm.orJtCu/source-inputs"));
        final var cases=cases();final var bridge=bridge(cases);
        final var seeds=new ArrayList<Map<String,Object>>();for(final int group:cases.stream().map(Case::groupId).distinct().toList())for(int pair=0;pair<groupPairs(group);++pair)seeds.add(Map.of("groupId",group,"pair",pair,"generationSeed",seed(GEN,group,pair),"battleSeed",seed(BATTLE,group,pair),"npcSeed",seed(NPC,group,pair)));
        final var samples=new ArrayList<Map<String,Object>>();
        for(final var c:cases)for(final var rule:rules(c)){final var p=RandomUtils.withSeed(seed(GEN,c.groupId(),0),()->prepare(c,rule,seed(NPC,c.groupId(),0)));samples.add(Map.of("case",c,"rule",rule,"target",p.target(),"calibration",p.generated().calibration(),"diagnostics",p.generated().diagnostics(),"contributions",p.contributions(),"playerEvaluation",p.playerEvaluation(),"calibrationEvaluation",p.calibrationEvaluation(),"players",traits(p.players()),"enemies",traits(p.generated().enemies())));}
        final var manifest=new LinkedHashMap<String,Object>();manifest.put("purpose","15.4.3_ONE_FRESH_TARGET_ONLY_BLEND_T_CONFIRMATION_NOT_IMPLEMENTATION_OR_FULL_BALANCE_ACCEPTANCE");manifest.put("conditions",34);manifest.put("pairPolicy",Map.of("groups1to4",1500,"groups5to7",1000,"group13",500,"group16",1500));manifest.put("rules",List.of(Rule.BASELINE,Rule.BLEND_T));manifest.put("modelConditions",37);manifest.put("battles",80000);manifest.put("seedRecords",11000);manifest.put("roundLimit",LIMIT);manifest.put("bridge",bridge);
        manifest.put("allocations",cases.stream().map(c->Map.of("caseId",c.id(),"groupId",c.groupId(),"pairs",pairs(c),"rules",rules(c))).toList());manifest.put("preOutcomeRootPolicySHA256","3876690b6ceb542c1bf2a3112d2b6e194ae922c028f706bd2b08686c11e515cf");manifest.put("pilotHelperSHA256","29804e3355279d65147c91e6c71b0fc0231b2070fce80b62603cbcebfc3deaf9");
        manifest.put("formula","pi=Hi*Di;B=sum_ijmin(pi,pj), ascendingp weighted sum coefficients2*(N-i)-1;T=max_k k*k*p_desc(k),smallestk on exactties;F=Q/2+min(alt,Q)/2;solo/bitidenticalH,D returnsQexact;Q0:F0,d1;otherwise d=F/Q;BASELINEd1");manifest.put("numericPolicy","Neutral components reproduce actual15.2 fixedAttackType iteration and sortedteam sums; B/T computed from ascendingpi; finitecheck,fail if mathematicalB<=Q orT<=B violated by >64maxULP; min-clamp alt toQ, d clamped[.5,1] only within64ULP; equalH/D testedbitexact");
        manifest.put("targetOrder","baselineTarget=actualQ*q*g*C*h left-associative; requestedTarget=baselineTarget*discount. q/g/C/h/weights/search/bases unchanged; no folding discount into levelPowerBonus");manifest.put("factory","Actual15.2 privateFormation/teamColony and constants read-onlyreflection; samePveCharacteristicScaling bounds/search;trialsisolatedseed0 asproduction");
        manifest.put("generationNamespace",GEN);manifest.put("battleNamespace",BATTLE);manifest.put("npcNamespace",NPC);manifest.put("seedPolicy","SHA256(namespace+'/'+groupId+'/'+pair) first48BE; no rule/variant; fresh namespaces disjoint from pilot; thirdNPCseed recorded but unused in every confirmation condition");
        manifest.put("fixedNPCPolicy","NOT_INCLUDED; NPC namespace and npcSeed fields explicitly unused; no fixed-NPC diagnostic in this confirmation");manifest.put("effects","NONE");manifest.put("combat",List.of("SCALING_V5",V3.name(),BattleTempoVersion.ACCELERATED_V1.name()));manifest.put("telemetry","All initial/final traits,stats,counts,action/trace hashes; full actionLog.events and separate traceEvents for pair0 only each condition/model/mirror");manifest.put("limitations","One frozen confirmation, no adaptive extension or pooling with pilot; no universal50/50 claim; exact root policy defines separate primary K11 and secondary K20 simultaneous95 families; stripped controls fill only freed slots with actual unupgraded V5 defaults; large-group/high-level guards remain descriptive pilot evidence");
        write(out,"inputs.json",cases);write(out,"construction-checks.json",bridge);write(out,"prepared-pair0.json",samples);write(out,"seeds.json",seeds);write(out,"manifest.json",manifest);
        if(args[0].equals("prepare"))return;
        final var summaries=new ArrayList<Map<String,Object>>();try(var raw=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(out.resolve("battles.jsonl.gz"),StandardOpenOption.CREATE_NEW)),StandardCharsets.UTF_8))){for(final var c:cases)for(final var rule:rules(c)){summaries.add(run(c,rule,raw));System.out.println("done="+summaries.size()+" "+rule+" case"+c.id()+" "+c.family()+" "+c.variant());}}
        write(out,"summary.json",Map.of("manifest",manifest,"cases",summaries));
    }
    private static Prepared prepare(Case c,Rule rule,long npcSeed) {
        final var players=players(c.members());final var basis=c.family().equals("FIXED_NPC")?players.subList(0,1):players;
        final var result=c.family().equals("FIXED_NPC")?RandomUtils.withSeed(npcSeed,()->calibrate(basis,c.level(),Rule.BASELINE)):calibrate(basis,c.level(),rule);
        final var ids=new java.util.HashSet<java.util.UUID>();for(final var p:java.util.stream.Stream.concat(players.stream(),result.generated().enemies().stream()).toList())if(!ids.add(p.id()))throw new IllegalStateException("DuplicateUUID");
        return new Prepared(players,result.generated(),result.target(),contributions(players),evaluate(players,rule),result.evaluation());
    }
    private static Calibrated calibrate(List<BattlePersonage> players,int level,Rule rule) {
        final var evaluation=evaluate(players,rule);final double discount=evaluation.discount();final int count=Math.max(2,players.size());final Object formation=formation(count);
        final int front=(int)number(FRONT,formation),back=(int)number(BACK,formation);final double fw=number(FW,formation),bw=number(BW,formation);
        final double assessment=PveTeamAssessment.assess(players),q=q(level),g=1+MathUtils.log(4,count)*GROUP,h=FLOOR+(1-FLOOR)*Math.pow(REF/count,2);
        final double baseline=assessment*q*g*C*h,target=baseline*discount;
        final double cap=PveCharacteristicScaling.maxWeightedMultiplier(new WeightedBase(GA,fw),new WeightedBase(GH,fw),new WeightedBase(GD,1),new WeightedBase(SA,bw),new WeightedBase(SH,bw),new WeightedBase(SD,1));
        final var calibration=PveCharacteristicScaling.search(target,cap,m->RandomUtils.withSeed(0,()->PveTeamAssessment.assess(colony(formation,m))));
        final var diagnostics=new PveScalingDiagnostics(PveTeamAssessment.VERSION,assessment,Map.of("enemyCount",(double)count,"frontCount",(double)front,"backCount",(double)back,"frontWeight",fw,"backWeight",bw,"groupSizeScaling",g,"sizeCorrection",h,"powerBonus",q,"encounterCorrection",C));
        return new Calibrated(new GeneratedPveEnemies(colony(formation,calibration.multiplier()),calibration,Optional.of(diagnostics)),new Target(assessment,q,count,front,back,fw,bw,g,h,C,baseline,discount,target),evaluation);
    }
    private static Evaluation evaluate(List<BattlePersonage> ps,Rule rule){final var cs=contributions(ps);final double Q=PveTeamAssessment.assess(ps);final double[] pi=cs.stream().mapToDouble(Contribution::p).sorted().toArray();double B=0,T=pi[pi.length-1];int best=1;for(int i=0;i<pi.length;++i){B+=(2.0*(pi.length-i)-1)*pi[i];final int k=i+1;final double t=((double)k*k)*pi[pi.length-k];if(t>T){T=t;best=k;}}if(!Double.isFinite(B)||!Double.isFinite(T))throw new IllegalStateException("NonfiniteB/T");final double tol=64*Math.max(Math.ulp(Q),Math.max(Math.ulp(B),Math.ulp(T)));if(B>Q+tol||T>B+tol)throw new IllegalStateException("MathematicalB/Tboundviolated");final double bb=Math.min(B,Q),bt=Math.min(T,Q);final var first=cs.getFirst();final boolean uniform=cs.stream().allMatch(x->Double.doubleToLongBits(x.H())==Double.doubleToLongBits(first.H())&&Double.doubleToLongBits(x.D())==Double.doubleToLongBits(first.D()));final double F=Q==0?0:rule==Rule.BASELINE||uniform?Q:Q/2+(rule==Rule.BLEND_B?bb:bt)/2;double d=Q==0?1:F/Q;if(d<.5-64*Math.ulp(1.0)||d>1+64*Math.ulp(1.0)||!Double.isFinite(d))throw new IllegalStateException("Discountoutsidebounds");d=Math.clamp(d,.5,1);return new Evaluation(Q,B,T,best,bb,bt,F,d,uniform);}
    private static List<BattlePersonage> players(List<InputMember> ms) {
        return ms.stream().map(m->{final var p=BattlePersonage.forCombat(m.items(),m.position(),PersonageEffects.EMPTY,Optional.of(m.build()),V3);p.setTargetingTactic(m.tactic());return p;}).toList();
    }
    private static List<Contribution> contributions(List<BattlePersonage> ps) {
        return ps.stream().map(p->{double mu=0;for(final var t:AttackType.values())mu+=p.defenseReductions().get(t);mu/=AttackType.values().length;
            double attack=0;for(final var profile:p.attacksByRange()){double a=0;for(final var t:AttackType.values())a+=profile.attack().getOrDefault(t,0);attack=Math.max(a,attack);}
            final double H=p.maxHealth()/mu,D=attack*(1+Math.clamp(p.critChance()/100.0,0,1)*(p.critMultiplier()-1))*CombatRules.cappedTurnFrequency(p.initiative());return new Contribution(p.id(),H,D,H*D);}).toList();
    }
    private static Map<String,Object> bridge(List<Case> cases) {
        int exact=0,prefix=0,sourceExact=0;
        for(final var c:cases)if(!c.family().equals("FIXED_NPC"))for(final long seed:List.of(1543L,281474976710655L)) {
            final var expected=RandomUtils.withSeed(seed,()->{final var p=players(c.members());final var g=ACTUAL.generateCalibrated(p,q(c.level()),V3);return List.of(traits(p),traits(g.enemies()),g.calibration(),g.diagnostics(),RandomUtils.getWithMax(1000000));});
            final var actual=RandomUtils.withSeed(seed,()->{final var p=prepare(c,Rule.BASELINE,0);return List.of(traits(p.players()),traits(p.generated().enemies()),p.generated().calibration(),p.generated().diagnostics(),RandomUtils.getWithMax(1000000));});
            if(!expected.equals(actual))throw new IllegalStateException("Actual15.2baseline differs case"+c.id());++exact;
            final var p=RandomUtils.withSeed(seed,()->prepare(c,Rule.BASELINE,0));final var cs=p.contributions();
            final double[] hs=cs.stream().mapToDouble(Contribution::H).sorted().toArray(),ds=cs.stream().mapToDouble(Contribution::D).sorted().toArray();double H=0,D=0;for(double x:hs)H+=x;for(double x:ds)D+=x;
            if(H*D!=PveTeamAssessment.assess(p.players()))throw new IllegalStateException("Neutralparts differ");
        }
        for(final var c:cases)if(c.family().equals("ORDINARY")&&c.variant().equals("BASE")) {
            final var current=canonical("MAIN",c.tier(),3,c.level());if(!current.equals(c.members()))throw new IllegalStateException("Archivedordinary changed");++sourceExact;
        }
        for(final String style:List.of("MAIN","PIERCE_ARCANE","RITUAL"))if(!strong(style).equals(canonical(style,Tier.STRONG,1,1).getFirst()))throw new IllegalStateException("Archivedstrong changed");else ++sourceExact;
        for(final var c:cases)if(c.variant().equals("NORMAL_APPEND")||c.variant().equals("WEAK_APPEND")) {
            final var base=cases.stream().filter(x->x.family().equals(c.family())&&x.groupId()==c.groupId()&&x.style().equals(c.style())&&x.variant().equals("BASE")).findFirst().orElseThrow();
            if(!c.members().subList(0,base.members().size()).equals(base.members()))throw new IllegalStateException("Append changed prefix");++prefix;
        }
        final var weak=weak();final var actor=RandomUtils.withSeed(0,()->players(List.of(weak)).getFirst());if(actor.maxHealth()!=840||!actor.isAlive())throw new IllegalStateException("Weak input");
        if(cases.stream().anyMatch(c->c.family().equals("FIXED_NPC")))throw new IllegalStateException("FixedNPC not authorized in confirmation");
        return Map.of("status","PASS","actual15.2FullCalibrationTraitsUuidGaugeAndRngTail",exact,"actualNeutralPartsExact",exact,"archivedInputsCurrentEquality",sourceExact,"appendPrefixes",prefix,"weakHP",840,"fixedNPCIncluded",false,"battles",0,"reflection","READONLY_CONSTRUCTOR_AND_PRIVATE_METHODS_NO_FIELD_WRITES");
    }
    private static List<Case> cases()throws Exception {
        final var out=new ArrayList<Case>();int group=0;final var ordinary=PveAdaptationStudy.JSON.readTree(Files.readString(sourceInputs.resolve("ordinary-selected-inputs.json"))).get("cases");final var weak=weak();
        for(final var tier:List.of(Tier.MID,Tier.STRONG))for(final int level:List.of(20,25)) {
            ++group;final var node=ordinary.get(tier.name());final var base=members(node.get("base"));final var bruiser=member(node.get("ordinaryBruiser"));
            add(out,group,"ORDINARY","MAIN",tier,level,"BASE",base,"15.3causal-confirmation archivedBASE");add(out,group,"ORDINARY","MAIN",tier,level,"NORMAL_APPEND",append(base,bruiser),"15.3archivedordinaryBRUISER");add(out,group,"ORDINARY","MAIN",tier,level,"WEAK_APPEND",append(base,weak),"15.3actualweak840HP");
        }
        final var styles=List.of("MAIN","PIERCE_ARCANE","RITUAL");
        for(int index=0;index<styles.size();++index) {
            final var style=styles.get(index);final int level=List.of(5,15,25).get(index);++group;
            for(final int n:List.of(1,2,3,7,15,30))add(out,group,"STRONG_FILLERS",style,Tier.STRONG,level,n==1?"BASE":"FILLERS_N"+n,fillers(strong(style),weak,n),"15.3confirmedfixedFirstMember+unchangedweak");
        }
        for(final var style:styles) {++group;for(final int n:List.of(1,30))add(out,group,"STRONG_FILLERS_HIGH",style,Tier.STRONG,50,n==1?"BASE":"FILLERS_N30",fillers(strong(style),weak,n),"Independenthighlevelabusecontrol");}
        for(final int level:List.of(25,50)) {++group;for(final int n:List.of(1,2,3,7,15,30))add(out,group,"WEAK_ONLY","WEAK_ONLY",Tier.BEGINNER,level,n==1?"BASE":"WEAK_N"+n,fillers(weak,weak,n),"Actual840HPweakonly");}
        ++group;final var node=ordinary.get("MID");final var base=members(node.get("base"));final var normal=append(base,member(node.get("ordinaryBruiser")));
        for(final var attack:List.of(AttackType.PIERCE,AttackType.SLASH))for(final var defense:List.of(DefenseType.ARCANE,DefenseType.LEATHER)) {
            final var style=attack+"_"+defense;add(out,group,"COUNTER_TWINS",style,Tier.MID,25,"BASE",retag(base,attack,defense),"Numericallyidenticalretag,notbalancefit");add(out,group,"COUNTER_TWINS",style,Tier.MID,25,"NORMAL_APPEND",retag(normal,attack,defense),"Fourthordinaryalsoretagged");
        }
        for(final int level:List.of(30,50)) {++group;for(final int n:level==30?List.of(7,8,15,30):List.of(8,30))add(out,group,"MAIN_LARGE","MAIN",Tier.MID,level,"ACTUAL_N"+n,canonical("MAIN",Tier.MID,n,level),"ActualcanonicalMAINnotfillers");}
        final var n8=PveAdaptationStudy.JSON.readTree(Files.readString(sourceInputs.resolve("n8-selected-inputs.json")));++group;final var n8base=members(n8.get("base"));add(out,group,"CONFIRMED_N8","MAIN",Tier.MID,30,"BASE",n8base,"15.3archivedcase211");add(out,group,"CONFIRMED_N8","MAIN",Tier.MID,30,"WEAK_APPEND",append(n8base,member(n8.get("appendedWeak"))),"15.3archivedcase213");
        for(int i=0;i<styles.size();++i)for(final boolean weapon:List.of(true,false)){final var s=styles.get(i);add(out,5+i,"STRIPPED",s,Tier.STRONG,List.of(5,15,25).get(i),weapon?"STRIP_MAIN_WEAPON":"STRIP_BODY",List.of(strip(strong(s),weapon),weak),"RemoveactualmainhandcarrierorBODY; fillfreedslotsactualV5defaults0;weakunchanged");}
        ++group;add(out,group,"FIXED_NPC","RITUAL",Tier.STRONG,25,"BASE",List.of(strong("RITUAL")),"DiagnosticfrozenNPCseparateNPCseed");add(out,group,"FIXED_NPC","RITUAL",Tier.STRONG,25,"WEAK_APPEND",List.of(strong("RITUAL"),weak),"SameNPCstatsUUIDgauge;realgeometrynotproductionmodel");
        ++group;final var mixed=List.of(canonical("MAIN",Tier.STRONG,3,25).get(0),canonical("MAIN",Tier.MID,3,25).get(1),canonical("MAIN",Tier.BEGINNER,3,25).get(2));add(out,group,"MIXED_TIERS","MAIN",Tier.MID,25,"BASE",mixed,"ActualSTRONGguardian+MIDR+BEGINNERM");add(out,group,"MIXED_TIERS","MAIN",Tier.MID,25,"NORMAL_APPEND",append(mixed,member(node.get("ordinaryBruiser"))),"Prefixunchanged+ordinaryMIDBRUISER");
        ++group;add(out,group,"SWORD_CONTINUITY","RITUAL",Tier.STRONG,25,"SWORD_PLUS0",List.of(strong("RITUAL"),upgradeSword(weak,0)),"Actualweaksword0,defaults0;notpromisedbestKcrossing");add(out,group,"SWORD_CONTINUITY","RITUAL",Tier.STRONG,25,"SWORD_PLUS1",List.of(strong("RITUAL"),upgradeSword(weak,1)),"Onlyactualobtainableswordenhance+1,defaultsuntouched");
        if(out.size()!=76||group!=19)throw new IllegalStateException("Original pilot matrix changed");final var selected=out.stream().filter(c->SELECTED.contains(c.id())).toList();if(selected.size()!=34||selected.stream().mapToInt(c->rules(c).size()).sum()!=37||selected.stream().mapToInt(c->rules(c).size()*pairs(c)*2).sum()!=80000)throw new IllegalStateException("Approved confirmation matrix changed");return selected;
    }
    private static InputMember strip(InputMember strong,boolean weapon){final var slots=java.util.EnumSet.noneOf(PersonageSlot.class);final var items=new ArrayList<Item>();for(final var i:strong.items())if(!i.object().slots().contains(weapon?PersonageSlot.MAIN_HAND:PersonageSlot.BODY)){items.add(i);slots.addAll(i.object().slots());}for(final var d:EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V5).defaultItems())if(d.slots().stream().noneMatch(slots::contains)){items.add(Item.fromObject(d));slots.addAll(d.slots());}if(!slots.equals(java.util.EnumSet.allOf(PersonageSlot.class)))throw new IllegalStateException("Strippeddefaultscoverage");return new InputMember(strong.build(),strong.position(),strong.tactic(),List.copyOf(items));}
    private static InputMember upgradeSword(InputMember weak,int level){return new InputMember(weak.build(),weak.position(),weak.tactic(),weak.items().stream().map(i->i.object().code().equals("sword")?new Item(i.object(),i.modifier(),i.rarity(),level):i).toList());}
    private static List<Rule> rules(Case c){return Set.of(14,20,26).contains(c.id())?List.of(Rule.BASELINE,Rule.BLEND_T):List.of(Rule.BLEND_T);}
    private static int pairs(Case c){return groupPairs(c.groupId());}
    private static int groupPairs(int group){return switch(group){case 1,2,3,4,16->1500;case 5,6,7->1000;case 13->500;default->throw new IllegalArgumentException("Unauthorized confirmation group "+group);};}
    private static InputMember strong(String style) {try{final var name=switch(style){case "MAIN"->"main";case "PIERCE_ARCANE"->"pierce";case "RITUAL"->"ritual";default->throw new IllegalArgumentException(style);};return member(PveAdaptationStudy.JSON.readTree(Files.readString(sourceInputs.resolve("fillers-"+name+"-confirmation-inputs.json"))).get(style).get("fixedFirstMember"));}catch(Exception e){throw new IllegalStateException(e);}}
    private static InputMember weak() {try{return member(PveAdaptationStudy.JSON.readTree(Files.readString(sourceInputs.resolve("fillers-main-confirmation-inputs.json"))).get("MAIN").get("inputsBySize").get("2").get(1));}catch(Exception e){throw new IllegalStateException(e);}}
    private static InputMember member(JsonNode n){return PveAdaptationStudy.JSON.convertValue(n,InputMember.class);}
    private static List<InputMember> members(JsonNode n){final var out=new ArrayList<InputMember>();for(final var x:n)out.add(member(x));return List.copyOf(out);}
    private static List<InputMember> canonical(String style,Tier tier,int size,int level){return PveAdaptationStudy.inputFor(new PveAdaptationStudy.Cell(PveAdaptationStudy.Mode.RAID,"MYCONID_COLONY",style,tier,size,level,false));}
    private static List<InputMember> append(List<InputMember> ms,InputMember m){final var out=new ArrayList<>(ms);out.add(m);return List.copyOf(out);}
    private static List<InputMember> fillers(InputMember first,InputMember weak,int n){final var out=new ArrayList<InputMember>();out.add(first);while(out.size()<n)out.add(weak);return List.copyOf(out);}
    private static void add(List<Case> out,int group,String family,String style,Tier tier,int level,String variant,List<InputMember> ms,String origin){out.add(new Case(out.size()+1,group,family,style,tier,level,variant,ms,origin));}
    private static List<InputMember> retag(List<InputMember> ms,AttackType at,DefenseType dt){return ms.stream().map(m->new InputMember(m.build(),m.position(),m.tactic(),m.items().stream().map(i->{final var o=i.object();return new Item(new ItemObject(o.code(),o.slots(),o.attacks().stream().map(a->new ItemAttack(at,a.minRange(),a.maxRange(),a.attack())).toList(),o.defense().map(d->new ItemDefense(dt,d.defense())),o.health(),o.critChance(),o.dodgeChance(),o.critMultiplier(),o.speed(),o.baseThreat(),o.impact(),o.progressionVersion(),o.locales()),i.modifier(),i.rarity(),i.enhanceLevel());}).toList())).toList();}
    private static List<Map<String,Object>> traits(List<BattlePersonage> ps){return ps.stream().map(p->{final var r=new LinkedHashMap<String,Object>();r.put("id",p.id());r.put("gauge",p.initiativeGauge());r.put("hp",p.maxHealth());r.put("attacks",p.attacksByRange());r.put("defenses",p.defenses());r.put("reductions",p.defenseReductions());r.put("crit",p.critChance());r.put("dodge",p.dodgeChance());r.put("critMultiplier",p.critMultiplier());r.put("speed",p.initiative());r.put("threat",p.totalThreat());r.put("impact",p.impactStrength());r.put("position",p.startPosition());r.put("range",p.range());r.put("tactic",p.targetingTactic());r.put("skills",p.skillSnapshots());r.put("items",p.itemSnapshots());r.put("legacyPower",p.legacyPower());r.put("scalingRules",p.usesScalingCombatRules());return(Map<String,Object>)r;}).toList();}
    private static Map<String,Object> run(Case c,Rule rule,BufferedWriter raw)throws Exception {
        final var scores=new ArrayList<Double>();final var outcomes=new TreeMap<String,Integer>();long rounds=0,events=0;int completed=0;
        for(int pair=0;pair<pairs(c);++pair){final long gen=seed(GEN,c.groupId(),pair),battle=seed(BATTLE,c.groupId(),pair),npc=seed(NPC,c.groupId(),pair);double score=0;boolean complete=true;
            for(final boolean reverse:List.of(false,true)){final var p=RandomUtils.withSeed(gen,()->prepare(c,rule,npc));final var row=new LinkedHashMap<String,Object>();
                row.put("case",c);row.put("rule",rule);row.put("pair",pair);row.put("reverse",reverse);row.put("mirror",reverse);row.put("generationSeed",gen);row.put("battleSeed",battle);row.put("npcSeed",npc);row.put("npcSeedUsed",c.family().equals("FIXED_NPC"));row.put("actualPlayerAssessment",p.playerEvaluation().Q());row.put("contributions",p.contributions());row.put("ruleAssessment",p.playerEvaluation());row.put("calibrationAssessment",p.calibrationEvaluation());row.put("target",p.target());row.put("calibration",p.generated().calibration());row.put("diagnostics",p.generated().diagnostics());row.put("playerTraits",traits(p.players()));row.put("enemyTraits",traits(p.generated().enemies()));
                try{final var result=RandomUtils.withSeed(battle,()->new Battle(battle).process(reverse?p.players():p.generated().enemies(),reverse?p.generated().enemies():p.players(),LIMIT,BattleTempoVersion.ACCELERATED_V1));final boolean pa=p.players().stream().anyMatch(BattlePersonage::isAlive),ea=p.generated().enemies().stream().anyMatch(BattlePersonage::isAlive);final String outcome=pa?"WIN":ea?"LOSS":"DRAW";outcomes.merge(outcome,1,Integer::sum);score+=pa?.5:ea?0:.25;rounds+=result.rounds();events+=result.actionLog().events().size();++completed;
                    row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());row.put("actionSha256",digest(result.actionLog()));row.put("traceSha256",digest(result.actionLog().traceEvents()));row.put("actionCount",result.actionLog().events().size());row.put("normalAttempts",result.actionLog().traceEvents().stream().filter(BattleTraceEvent.NormalAttackAttempt.class::isInstance).count());row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());row.put("actualPlayerPositions",positions(p.players(),result));row.put("actualEnemyPositions",positions(p.generated().enemies(),result));if(pair==0){row.put("actionLog",result.actionLog());row.put("traceEvents",result.actionLog().traceEvents());}
                }catch(IllegalStateException e){if(e.getMessage()==null||!e.getMessage().startsWith("Battle did not finish within"))throw e;complete=false;outcomes.merge("TIMEOUT",1,Integer::sum);row.put("outcome","TIMEOUT");row.put("rounds",LIMIT);}
                row.put("playerStats",p.players().stream().map(BattlePersonage::battlePersonageStats).toList());row.put("enemyStats",p.generated().enemies().stream().map(BattlePersonage::battlePersonageStats).toList());raw.write(PveAdaptationStudy.JSON.writeValueAsString(row));raw.newLine();
            }scores.add(complete?score:null);
        }
        final var finished=scores.stream().filter(java.util.Objects::nonNull).toList();final double mean=finished.stream().mapToDouble(Double::doubleValue).average().orElse(0),half=finished.isEmpty()?1:Math.sqrt(Math.log(40)/(2*finished.size()));
        return Map.of("case",c,"rule",rule,"pairs",pairs(c),"pairScores",scores,"outcomes",outcomes,"winrate",outcomes.getOrDefault("WIN",0)/(2.0*pairs(c)),"pointwise95Hoeffding",List.of(Math.max(0,mean-half),Math.min(1,mean+half)),"pairMean",mean,"meanRounds",completed==0?0:rounds/(double)completed,"meanEvents",completed==0?0:events/(double)completed);
    }
    private static List<Position> positions(List<BattlePersonage> ps,BattleResult r){return ps.stream().map(p->r.initState().lines().get(r.initState().personagesById().get(p.id()).lineIndex()).position()).toList();}
    private static long seed(String ns,int group,int pair){try{final var b=MessageDigest.getInstance("SHA-256").digest((ns+"/"+group+"/"+pair).getBytes(StandardCharsets.UTF_8));long n=0;for(int i=0;i<6;++i)n=n<<8|(b[i]&255L);return n;}catch(Exception e){throw new IllegalStateException(e);}}
    private static String digest(Object x)throws Exception{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(PveAdaptationStudy.JSON.writeValueAsBytes(x)));}
    private static Object formation(int n){try{return FORMATION_CTOR.newInstance(n);}catch(Exception e){throw new IllegalStateException(e);}}
    @SuppressWarnings("unchecked") private static List<BattlePersonage> colony(Object formation,double m){try{return(List<BattlePersonage>)COLONY.invoke(ACTUAL,formation,m,V3);}catch(Exception e){throw new IllegalStateException(e);}}
    private static double number(Method method,Object o){try{return((Number)method.invoke(o)).doubleValue();}catch(Exception e){throw new IllegalStateException(e);}}
    private static Method method(Class<?> owner,String name,Class<?>...types){try{final var m=owner.getDeclaredMethod(name,types);m.setAccessible(true);return m;}catch(Exception e){throw new IllegalStateException(e);}}
    private static Constructor<?> constructor(Class<?> owner,Class<?>...types){try{final var c=owner.getDeclaredConstructor(types);c.setAccessible(true);return c;}catch(Exception e){throw new IllegalStateException(e);}}
    private static double constant(String name){try{final Field f=MyconidColonyGenerator.class.getDeclaredField(name);f.setAccessible(true);return((Number)f.get(null)).doubleValue();}catch(Exception e){throw new IllegalStateException(e);}}
    private static double q(int level){return 1+((level-10)*.06+.14);}
    private static void write(Path root,String name,Object o)throws Exception{Files.writeString(root.resolve(name),PveAdaptationStudy.JSON.writerWithDefaultPrettyPrinter().writeValueAsString(o),StandardOpenOption.CREATE_NEW);}
}
