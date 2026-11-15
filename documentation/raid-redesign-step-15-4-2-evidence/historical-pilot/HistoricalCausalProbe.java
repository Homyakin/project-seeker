import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.battle.BattlePersonageStats;
import ru.homyakin.seeker.game.battle.Position;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.targeting.TargetingTactic;
import ru.homyakin.seeker.game.event.raid.generator.MyconidColonyGenerator;
import ru.homyakin.seeker.game.item.catalog.ItemModifiersToml;
import ru.homyakin.seeker.game.item.catalog.ItemObjectsToml;
import ru.homyakin.seeker.game.item.models.DefaultItems;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemObject;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.MathUtils;
import ru.homyakin.seeker.utils.RandomUtils;
import ru.homyakin.seeker.utils.ResourceUtils;

/** Isolated coherent historical rules; the project source files are unchanged. */
public final class HistoricalCausalProbe {
    static final ObjectMapper MAPPER = new ObjectMapper().registerModule(new Jdk8Module())
        .enable(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS);
    static final String COMMIT = "6150d562546cca22bed139c4d0fa23456e4d801e";
    static final String GEN_NAMESPACE = "MYCONID-15.4.2-CAUSAL-PILOT-GEN-20261108-v1";
    static final String BATTLE_NAMESPACE = "MYCONID-15.4.2-CAUSAL-PILOT-BATTLE-20261108-v1";
    static final int PAIRS = 30;
    static final int ROUND_LIMIT = 2000;
    static final List<Integer> LEVELS = List.of(10, 25);
    static final Map<String,ItemObject> OBJECTS = ResourceUtils.calc("game-data/item_objects_catalog.toml", ItemObjectsToml::load)
        .orElseThrow().itemObjects().stream().collect(Collectors.toMap(ItemObject::code, Function.identity()));
    static final Map<ActiveEnum,Modifier> MODIFIERS = ResourceUtils.calc("game-data/item_modifiers_catalog.toml", ItemModifiersToml::load)
        .orElseThrow().modifiers().stream().collect(Collectors.toMap(Modifier::activeEnum, Function.identity()));
    static final Map<String,Item> DEFAULTS = List.of(DefaultItems.MAIN_FIST, DefaultItems.OFF_FIST,
        DefaultItems.SHIRT, DefaultItems.PANTS, DefaultItems.SHOES, DefaultItems.HELMET, DefaultItems.GLOVES)
        .stream().collect(Collectors.toMap(i -> i.object().code(), Function.identity()));

    record ItemSpec(String code, ItemRarity rarity, ActiveEnum active, int enhanceLevel) {}
    record Member(String name, Position position, TargetingTactic tactic, List<ItemSpec> items) {}
    record Fixture(int id, String name, String group, List<Member> members) {}
    record Prepared(List<BattlePersonage> players, List<BattlePersonage> enemies) {}

    static ItemSpec common(String code, int enhance) { return new ItemSpec(code, ItemRarity.COMMON, null, enhance); }
    static List<ItemSpec> withBody(List<ItemSpec> weapons, List<String> body, int enhance) {
        final var result = new ArrayList<>(weapons);
        body.forEach(code -> result.add(common(code, enhance)));
        return List.copyOf(result);
    }
    static Member guard() {
        return new Member("G", Position.FRONT, TargetingTactic.THREAT, withBody(List.of(common("mace",3),
            new ItemSpec("tower_shield",ItemRarity.RARE,null,3)),
            List.of("cuirass","greaves","sabatons","great_helm","gauntlets"),3));
    }
    static Member ranger() {
        return new Member("R", Position.MID, TargetingTactic.EXPLOIT_WEAKNESS,
            withBody(List.of(new ItemSpec("longbow",ItemRarity.UNCOMMON,ActiveEnum.DOUBLE_ATTACK,3)),
                List.of("robe","cloth_chausses","cloth_boots","hood","cloth_gloves"),3));
    }
    static Member mage() {
        return new Member("M", Position.MID, TargetingTactic.EXPLOIT_WEAKNESS,
            withBody(List.of(new ItemSpec("staff",ItemRarity.RARE,ActiveEnum.DOUBLE_ATTACK,3),common("orb",3)),
                List.of("robe","cloth_chausses","cloth_boots","hood","cloth_gloves"),3));
    }
    static Member bruiser(boolean strong) {
        final int enhance = strong ? 6 : 3;
        return new Member(strong ? "S" : "B", Position.FRONT, TargetingTactic.WOUNDED_HUNTER,
            withBody(List.of(new ItemSpec("sledgehammer",strong ? ItemRarity.RARE : ItemRarity.UNCOMMON,
                ActiveEnum.BERSERK,enhance)),
                List.of("breastplate","leather_chausses","boots","leather_helm","leather_gloves"),enhance));
    }
    static Member weak() {
        return new Member("W",Position.FRONT,TargetingTactic.THREAT,
            List.of(common("sword",0),common("off-fists",0),common("torn_jacket",0),common("leaky_pants",0),
                common("footcloths",0),common("panama",0),common("bandages",0)));
    }
    static List<Fixture> fixtures() {
        return List.of(new Fixture(1,"T3","T",List.of(guard(),ranger(),mage())),
            new Fixture(2,"T3_PLUS_ORDINARY","T",List.of(guard(),ranger(),mage(),bruiser(false))),
            new Fixture(3,"S1_STRONG","S",List.of(bruiser(true))),
            new Fixture(4,"S1_PLUS_WEAK","S",List.of(bruiser(true),weak())));
    }
    static Item item(ItemSpec spec) {
        final ItemObject object = OBJECTS.containsKey(spec.code()) ? OBJECTS.get(spec.code())
            : Optional.ofNullable(DEFAULTS.get(spec.code())).orElseThrow(() -> new IllegalArgumentException("Unknown old item " + spec.code())).object();
        final var modifier = Optional.ofNullable(spec.active()).map(active -> Optional.ofNullable(MODIFIERS.get(active)).orElseThrow());
        return new Item(object, modifier, spec.rarity(), spec.enhanceLevel());
    }
    static BattlePersonage person(Member member) {
        final var items = member.items().stream().map(HistoricalCausalProbe::item).toList();
        final var slots = EnumSet.noneOf(PersonageSlot.class);
        for(final var item : items) for(final var slot : item.object().slots())
            if(!slots.add(slot)) throw new IllegalArgumentException("Duplicate slot for " + member.name() + ": " + slot);
        if(!slots.equals(EnumSet.allOf(PersonageSlot.class))) throw new IllegalArgumentException("Uncovered old slots for " + member.name());
        final var person = new BattlePersonage(items,member.position(),Optional.of(member.name()));
        person.setTargetingTactic(member.tactic());
        if(!person.isAlive() || person.initiative()<=0) throw new IllegalArgumentException("Dead or immobile initial participant");
        return person;
    }
    static Prepared prepare(Fixture fixture,int level,long generationSeed) {
        return RandomUtils.withSeed(generationSeed, () -> {
            final var players=fixture.members().stream().map(HistoricalCausalProbe::person).toList();
            final double q=1+((level-10)*.06+.14);
            return new Prepared(players,new MyconidColonyGenerator().generate(players,q));
        });
    }
    static long seed(String namespace,int groupId,int pair) {
        try {
            final byte[] bytes=MessageDigest.getInstance("SHA-256").digest((namespace+"/"+groupId+"/"+pair).getBytes(StandardCharsets.UTF_8));
            long result=0;
            for(int i=0;i<6;i++)result=(result<<8)|(bytes[i]&255L);
            return result;
        } catch(Exception e) { throw new IllegalStateException(e); }
    }
    static Map<String,Object> snapshot(BattlePersonage p) {
        final var s = new LinkedHashMap<String,Object>();
        s.put("id",p.id());s.put("name",p.name());s.put("hp",p.health());s.put("maxHp",p.maxHealth());
        s.put("power",p.power());s.put("range",p.range());s.put("startPosition",p.startPosition());
        s.put("lineIndex",p.currentPosition());s.put("initiative",p.initiative());s.put("initiativeGauge",p.initiativeGauge());
        s.put("critChance",p.critChance());s.put("dodgeChance",p.dodgeChance());s.put("critMultiplier",p.critMultiplier());
        s.put("defenses",p.defenses());s.put("attacksByRange",p.attacksByRange());s.put("damageTakenMultipliers",p.defenseReductions());
        s.put("targetingTactic",p.targetingTactic());s.put("items",p.itemSnapshots());s.put("skills",p.skillSnapshots());
        s.put("totalThreat",p.totalThreat());
        return s;
    }
    static Map<String,Object> totals(List<BattlePersonage> team) {
        long damage=0,taken=0,turns=0,hp=0,normal=0,crit=0,skill=0;
        for(final var p:team){final var s=p.battlePersonageStats();damage+=s.damageDealt();taken+=p.actualDamageTaken();
            turns+=s.turnsCount();hp+=p.health();normal+=s.normalDamageDealt();crit+=s.critDamageDealt();skill+=s.skillDamageDealt();}
        return Map.of("damageDealt",damage,"actualDamageTaken",taken,"turns",turns,"remainingHp",hp,
            "normalDamageDealt",normal,"critDamageDealt",crit,"skillDamageDealt",skill);
    }
    static String hash(Object object) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(MAPPER.writeValueAsBytes(object))); }
        catch(Exception e){throw new IllegalStateException(e);}
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=2 || !Set.of("prepare","run").contains(args[0]))throw new IllegalArgumentException("prepare|run NEW_OUTPUT_DIRECTORY");
        final var output=Path.of(args[1]);Files.createDirectory(output);
        if(args[0].equals("prepare")){
            final var manifest=new LinkedHashMap<String,Object>();
            manifest.put("purpose","COHERENT_HISTORICAL_RULES_DIAGNOSIS_NO_PRODUCTION_CHANGE");
            manifest.put("commit",COMMIT);manifest.put("pairs",PAIRS);manifest.put("roundLimit",ROUND_LIMIT);
            manifest.put("levels",LEVELS);manifest.put("fixtures",fixtures());
            manifest.put("namespaces",Map.of("generation",GEN_NAMESPACE,"battle",BATTLE_NAMESPACE));
            manifest.put("groupId","10*level + (T=1,S=2)");manifest.put("seedDerivation","SHA256(namespace+'/'+groupId+'/'+pair), first six bytes big-endian positive long");
            manifest.put("classLocation",BattlePersonage.class.getProtectionDomain().getCodeSource().getLocation());
            manifest.put("npcRules","Literal historical MyconidColonyGenerator: all historical trial RNG retained; q=1+((L-10)*.06+.14), g(M), calibration .77; both NPC range1");
            final var construction=new ArrayList<Object>();
            for(final int level:LEVELS)for(final var fixture:fixtures()){
                final int groupId=10*level+(fixture.group().equals("T")?1:2);
                final long gen=seed(GEN_NAMESPACE,groupId,0),battle=seed(BATTLE_NAMESPACE,groupId,0);
                final var p=prepare(fixture,level,gen);
                final var c=new LinkedHashMap<String,Object>();c.put("fixture",fixture);c.put("level",level);
                c.put("generationSeed",gen);c.put("battleSeed",battle);
                c.put("playersPrepared",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                c.put("npcPrepared",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());
                new BattleContext(p.players(),p.enemies());
                c.put("playersPlaced",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                c.put("npcPlaced",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());construction.add(c);
            }
            manifest.put("construction",construction);
            MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("manifest.json").toFile(),manifest);
            System.out.println("Historical preparation only: 8 cases, no battles");return;
        }
        int rows=0;
        final var summaries=new ArrayList<Object>();
        try(final var writer=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(output.resolve("battles.jsonl.gz"))),StandardCharsets.UTF_8))){
            for(final int level:LEVELS)for(final var fixture:fixtures()){
                int wins=0,losses=0,draws=0,timeouts=0;long rounds=0;
                for(int pair=0;pair<PAIRS;pair++)for(int side=0;side<2;side++){
                    final int groupId=10*level+(fixture.group().equals("T")?1:2);
                    final long gen=seed(GEN_NAMESPACE,groupId,pair),battleSeed=seed(BATTLE_NAMESPACE,groupId,pair);
                    final var p=prepare(fixture,level,gen);
                    final var row=new LinkedHashMap<String,Object>();
                    row.put("phase","HISTORICAL_6150");row.put("fixtureId",fixture.id());row.put("fixtureName",fixture.name());
                    row.put("case",Map.of("phase",Map.of("id","H"),"scenarioId",fixture.id(),"level",level,"groupId",groupId));
                    row.put("reverse",side==0); // shared modern convention: true = players first
                    row.put("group",fixture.group());row.put("level",level);row.put("pair",pair);row.put("mirror",side==0?"PLAYERS_FIRST":"NPC_FIRST");
                    row.put("generationSeed",gen);row.put("battleSeed",battleSeed);
                    row.put("playersPrepared",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                    row.put("npcPrepared",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());
                    try{
                        final int mirror=side;
                        final var result=RandomUtils.withSeed(battleSeed,()->new Battle().process(
                            mirror==0?p.players():p.enemies(),mirror==0?p.enemies():p.players(),ROUND_LIMIT));
                        final boolean playerAlive=p.players().stream().anyMatch(BattlePersonage::isAlive);
                        final boolean npcAlive=p.enemies().stream().anyMatch(BattlePersonage::isAlive);
                        final String outcome=playerAlive?"WIN":npcAlive?"LOSS":"DRAW";
                        if(playerAlive)wins++;else if(npcAlive)losses++;else draws++;rounds+=result.rounds();
                        row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());
                        row.put("actionLog",result.actionLog());row.put("actionLogSHA256",hash(result.actionLog()));
                        row.put("personageStats",result.personageStats());
                        row.put("actionCount",result.actionLog().events().size());
                        row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());
                        row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
                        row.put("actualPlayerPositions",p.players().stream().map(person->result.initState().lines().get(result.initState().personagesById().get(person.id()).lineIndex()).position()).toList());
                        row.put("actualEnemyPositions",p.enemies().stream().map(person->result.initState().lines().get(result.initState().personagesById().get(person.id()).lineIndex()).position()).toList());
                    }catch(IllegalStateException e){
                        if(!e.getMessage().startsWith("Battle did not finish within "))throw e;
                        timeouts++;row.put("outcome","TIMEOUT");row.put("rounds",ROUND_LIMIT);row.put("error",e.getMessage());
                    }
                    row.put("playersTotals",totals(p.players()));row.put("npcTotals",totals(p.enemies()));
                    row.put("playerStats",p.players().stream().map(BattlePersonage::battlePersonageStats).toList());
                    row.put("enemyStats",p.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
                    writer.write(MAPPER.writeValueAsString(row));writer.newLine();rows++;
                }
                final var summary=new LinkedHashMap<String,Object>();
                summary.put("fixtureId",fixture.id());summary.put("fixtureName",fixture.name());summary.put("level",level);
                summary.put("wins",wins);summary.put("losses",losses);summary.put("draws",draws);summary.put("timeouts",timeouts);
                summary.put("executions",2*PAIRS);summary.put("winRate",wins/(double)(2*PAIRS));summary.put("meanRounds",rounds/(double)(2*PAIRS));
                summaries.add(summary);System.out.println(MAPPER.writeValueAsString(summary));
            }
        }
        if(rows!=480)throw new IllegalStateException("Unexpected historical execution count "+rows);
        MAPPER.writerWithDefaultPrettyPrinter().writeValue(output.resolve("summary.json").toFile(),Map.of("rows",rows,"cases",summaries));
    }
}
