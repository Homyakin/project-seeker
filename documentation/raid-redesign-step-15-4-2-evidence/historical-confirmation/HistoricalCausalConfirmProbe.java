import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Battle;
import ru.homyakin.seeker.game.battle.BattleContext;
import ru.homyakin.seeker.game.battle.BattleEvent;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.utils.RandomUtils;

/** A single pre-authorized fresh confirmation. Frozen pilot sources are only read. */
public final class HistoricalCausalConfirmProbe {
    static final String GEN_NAMESPACE="MYCONID-15.4.2-CAUSAL-CONFIRM-GEN-20261108-v1";
    static final String BATTLE_NAMESPACE="MYCONID-15.4.2-CAUSAL-CONFIRM-BATTLE-20261108-v1";
    static final int PAIRS=500,LEVEL=25,GROUP=251,LIMIT=2000;
    static final List<HistoricalCausalProbe.Fixture> FIXTURES=HistoricalCausalProbe.fixtures().subList(0,2);
    static String fileHash(Path path) throws Exception {
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(Files.readAllBytes(path)));
    }
    public static void main(String[] args) throws Exception {
        if(args.length!=3||!Set.of("prepare","run").contains(args[0]))throw new IllegalArgumentException("prepare|run NEW_OUTPUT_DIRECTORY FROZEN_POLICY_PATH");
        final var out=Path.of(args[1]);final var policy=Path.of(args[2]);
        final var policyHash=fileHash(policy);Files.createDirectory(out);
        if(args[0].equals("prepare")){
            final var manifest=new LinkedHashMap<String,Object>();
            manifest.put("purpose","ONLY_AUTHORIZED_FRESH_HISTORICAL_T3_APPEND_CONFIRMATION");
            manifest.put("sourceCommit",HistoricalCausalProbe.COMMIT);manifest.put("policyPath",policy);manifest.put("policySHA256",policyHash);
            manifest.put("pairs",PAIRS);manifest.put("level",LEVEL);manifest.put("groupId",GROUP);manifest.put("fixtures",FIXTURES);
            manifest.put("namespaces",Map.of("generation",GEN_NAMESPACE,"battle",BATTLE_NAMESPACE));manifest.put("roundLimit",LIMIT);
            manifest.put("seedDerivation","SHA256(namespace+'/'+groupId+'/'+pair), first6 bytes big endian");
            manifest.put("historicalClassLocation",BattlePersonage.class.getProtectionDomain().getCodeSource().getLocation());
            manifest.put("sharedFixtureAndPreparationValidation","/tmp/seeker-step1542-historical.77NJth/common-preparation-validation.json");
            final var prepared=new ArrayList<Object>();
            for(final var f:FIXTURES){
                final long generation=HistoricalCausalProbe.seed(GEN_NAMESPACE,GROUP,0),battle=HistoricalCausalProbe.seed(BATTLE_NAMESPACE,GROUP,0);
                final var p=HistoricalCausalProbe.prepare(f,LEVEL,generation);
                final var row=new LinkedHashMap<String,Object>();row.put("fixture",f);row.put("generationSeed",generation);row.put("battleSeed",battle);
                row.put("playersPrepared",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                row.put("npcPrepared",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());
                new BattleContext(p.players(),p.enemies());
                row.put("playersPlaced",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                row.put("npcPlaced",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());prepared.add(row);
            }
            manifest.put("construction",prepared);
            HistoricalCausalProbe.MAPPER.writerWithDefaultPrettyPrinter().writeValue(out.resolve("manifest.json").toFile(),manifest);
            System.out.println("Confirmation construction only; no battles");return;
        }
        final var summaries=new ArrayList<Object>();int rows=0;
        try(final var writer=new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(out.resolve("battles.jsonl.gz"))),StandardCharsets.UTF_8))){
            for(final var f:FIXTURES){
                int wins=0,losses=0,draws=0,timeouts=0;long rounds=0;final var pairScores=new ArrayList<Double>();
                for(int pair=0;pair<PAIRS;pair++){
                    double score=0;boolean complete=true;
                    for(final boolean reverse:List.of(false,true)){
                        final long generation=HistoricalCausalProbe.seed(GEN_NAMESPACE,GROUP,pair),battle=HistoricalCausalProbe.seed(BATTLE_NAMESPACE,GROUP,pair);
                        final var p=HistoricalCausalProbe.prepare(f,LEVEL,generation);
                        final var row=new LinkedHashMap<String,Object>();
                        row.put("case",Map.of("phase",Map.of("id","H"),"scenarioId",f.id(),"level",LEVEL,"groupId",GROUP));
                        row.put("phase","H");row.put("scenarioId",f.id());row.put("level",LEVEL);row.put("pair",pair);row.put("reverse",reverse);
                        row.put("mirror",reverse?"PLAYERS_FIRST":"NPC_FIRST");row.put("generationSeed",generation);row.put("battleSeed",battle);
                        row.put("policySHA256",policyHash);
                        row.put("playersPrepared",p.players().stream().map(HistoricalCausalProbe::snapshot).toList());
                        row.put("npcPrepared",p.enemies().stream().map(HistoricalCausalProbe::snapshot).toList());
                        try{
                            final var result=RandomUtils.withSeed(battle,()->new Battle().process(reverse?p.players():p.enemies(),reverse?p.enemies():p.players(),LIMIT));
                            final boolean pa=p.players().stream().anyMatch(BattlePersonage::isAlive),ea=p.enemies().stream().anyMatch(BattlePersonage::isAlive);
                            final String outcome=pa?"WIN":ea?"LOSS":"DRAW";
                            if(pa){wins++;score+=.5;}else if(ea)losses++;else{draws++;score+=.25;}rounds+=result.rounds();
                            row.put("outcome",outcome);row.put("rounds",result.rounds());row.put("initState",result.initState());
                            row.put("actionLog",result.actionLog());row.put("actionLogSHA256",HistoricalCausalProbe.hash(result.actionLog()));
                            row.put("personageStats",result.personageStats());row.put("actionCount",result.actionLog().events().size());
                            row.put("moves",result.actionLog().events().stream().filter(BattleEvent.MovedTowardEnemy.class::isInstance).count());
                            row.put("forcedMoves",result.actionLog().events().stream().filter(BattleEvent.PersonageForcedMove.class::isInstance).count());
                            row.put("actualPlayerPositions",p.players().stream().map(person->result.initState().lines().get(result.initState().personagesById().get(person.id()).lineIndex()).position()).toList());
                            row.put("actualEnemyPositions",p.enemies().stream().map(person->result.initState().lines().get(result.initState().personagesById().get(person.id()).lineIndex()).position()).toList());
                        }catch(IllegalStateException e){
                            if(e.getMessage()==null||!e.getMessage().startsWith("Battle did not finish within "))throw e;
                            timeouts++;complete=false;row.put("outcome","TIMEOUT");row.put("rounds",LIMIT);row.put("error",e.getMessage());
                        }
                        row.put("playersTotals",HistoricalCausalProbe.totals(p.players()));row.put("npcTotals",HistoricalCausalProbe.totals(p.enemies()));
                        row.put("playerStats",p.players().stream().map(BattlePersonage::battlePersonageStats).toList());
                        row.put("enemyStats",p.enemies().stream().map(BattlePersonage::battlePersonageStats).toList());
                        writer.write(HistoricalCausalProbe.MAPPER.writeValueAsString(row));writer.newLine();rows++;
                    }
                    pairScores.add(complete?score:null);
                }
                final var summary=new LinkedHashMap<String,Object>();summary.put("scenarioId",f.id());summary.put("level",LEVEL);
                summary.put("pairs",PAIRS);summary.put("executions",2*PAIRS);summary.put("wins",wins);summary.put("losses",losses);
                summary.put("draws",draws);summary.put("timeouts",timeouts);summary.put("pairScores",pairScores);
                summary.put("winRate",wins/(double)(2*PAIRS));summary.put("meanRounds",rounds/(double)(2*PAIRS));summaries.add(summary);
                System.out.println("H scenario "+f.id()+" wins="+wins+" losses="+losses+" draws="+draws+" timeouts="+timeouts);
            }
        }
        if(rows!=2000)throw new IllegalStateException("Unauthorized confirmation count "+rows);
        HistoricalCausalProbe.MAPPER.writerWithDefaultPrettyPrinter().writeValue(out.resolve("summary.json").toFile(),Map.of("rows",rows,"policySHA256",policyHash,"cases",summaries));
    }
}
