package ru.homyakin.seeker.game.battle;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.zip.GZIPOutputStream;
import ru.homyakin.seeker.game.battle.Step12RepresentativeBattleRunner.Observation;
import ru.homyakin.seeker.game.battle.Step12RepresentativeMatrix.Scenario;

/** Replays one recorded representative battle without changing the study matrix or its inputs. */
public final class Step12RepresentativeReplay {
    private Step12RepresentativeReplay() {
    }

    public static void main(String[] args) throws Exception {
        if (args.length != 4) {
            throw new IllegalArgumentException("Usage: <caseCode> <seed> <reverse:true|false> <output.json.gz>");
        }
        final String caseCode = args[0];
        final long seed = Long.parseLong(args[1]);
        if (!args[2].equals("true") && !args[2].equals("false")) {
            throw new IllegalArgumentException("reverse must be true or false");
        }
        final boolean reverse = Boolean.parseBoolean(args[2]);
        final var outputPath = Path.of(args[3]).toAbsolutePath().normalize();
        final var scenario = Step12RepresentativeMatrix.scenarios().stream()
            .filter(candidate -> candidate.code().equals(caseCode))
            .findFirst()
            .orElseThrow(() -> new IllegalArgumentException("Unknown representative case: " + caseCode));
        final var observation = Step12RepresentativeBattleRunner.observe(
            scenario.left(), scenario.right(), scenario.level(), seed, reverse, 2000,
            scenario.disabledLeft(), scenario.disabledRight()
        );
        final var mapper = JsonMapper.builder()
            .addModule(new Jdk8Module()).addModule(new JavaTimeModule()).build();
        Files.createDirectories(outputPath.getParent());
        try (final var output = new GZIPOutputStream(Files.newOutputStream(
            outputPath, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE
        ))) {
            mapper.writeValue(output, new Replay(scenario, seed, reverse, observation));
        }
        System.out.printf("%s seed=%d reverse=%s outcome=%s rounds=%d output=%s%n",
            caseCode, seed, reverse, observation.outcome(), observation.rounds(), outputPath);
    }

    private record Replay(Scenario scenario, long seed, boolean reverse, Observation observation) {
    }
}
