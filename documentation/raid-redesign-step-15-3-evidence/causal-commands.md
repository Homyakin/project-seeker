# Exact causal study commands

All commands ran from `/home/user/projects/project-seeker/game`. No Maven, bot, database, production write or target compilation was used. Only one heavy Java study ran at a time. Standard output and errors were redirected to the paths below. The existing compiled classes and dependency classpath were read only.

## Initial failed invocation, zero battles

`JAVA_HOME` was empty in this tool shell. This command exited 127 with `zsh:2: no such file or directory: /bin/java`, before any Java process or output directory was created. `confirmation.log` preserves the error.

```sh
causal_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
"$JAVA_HOME/bin/java" -Xmx512m -Dmyconid.study.suite=causal -Dmyconid.study.pairs=500 -Dmyconid.study.causalExtraLevels=20,25 -Dmyconid.study.causalGroupIds=29,30,41,42 -Dmyconid.study.causalVariants=BASE,NORMAL_APPEND,WEAK_APPEND -Dmyconid.study.seedOffset=2000000000 -Dmyconid.study.phase=STEP_15_3_N3_CAUSAL_CONFIRMATION -cp "target/classes:target/test-classes:$causal_classpath" ru.homyakin.seeker.game.battle.MyconidAdaptationStudy /tmp/seeker-step15-3-causal.rPWNKs/confirmation > /tmp/seeker-step15-3-causal.rPWNKs/confirmation.log 2>&1
```

## Four selected N3 groups, 500 pairs, 12000 battles

The loop mapping was independently enumerated before execution: extra levels20,25 give group29 MID/N3/L20,30 MID/N3/L25,41 STRONG/N3/L20,42 STRONG/N3/L25.

```sh
causal_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dmyconid.study.suite=causal -Dmyconid.study.pairs=500 -Dmyconid.study.causalExtraLevels=20,25 -Dmyconid.study.causalGroupIds=29,30,41,42 -Dmyconid.study.causalVariants=BASE,NORMAL_APPEND,WEAK_APPEND -Dmyconid.study.seedOffset=2000000000 -Dmyconid.study.phase=STEP_15_3_N3_CAUSAL_CONFIRMATION -cp "target/classes:target/test-classes:$causal_classpath" ru.homyakin.seeker.game.battle.MyconidAdaptationStudy /tmp/seeker-step15-3-causal.rPWNKs/confirmation > /tmp/seeker-step15-3-causal.rPWNKs/confirmation-run.log 2>&1
```

## Broad fixed screen, 20 pairs, 14400 battles

All 72 groups: MID/STRONG, original N1/2/3/7/8/15, levels1/10/20/25/30/40. Five variants BASE/NORMAL_APPEND/WEAK_APPEND/SINGLE_ITEM_ENHANCE/SINGLE_MODIFIER_RARITY. All five exist for every group; no rarity condition was skipped.

```sh
causal_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dmyconid.study.suite=causal -Dmyconid.study.pairs=20 -Dmyconid.study.causalExtraLevels=20,25,30,40 -Dmyconid.study.seedOffset=2100000000 -Dmyconid.study.phase=STEP_15_3_BROAD_CAUSAL_SCREEN -cp "target/classes:target/test-classes:$causal_classpath" ru.homyakin.seeker.game.battle.MyconidAdaptationStudy /tmp/seeker-step15-3-causal.rPWNKs/broad > /tmp/seeker-step15-3-causal.rPWNKs/broad.log 2>&1
```

## Independent fixed confirmation, 500 pairs, 21000 battles

The complete follow-up was declared before starting and not stopped on interim outcomes. With extra20,25,30,40: groups33/34 MID/N3/L20,L25;38 MID/N7/L25;43 MID/N8/L30;57/58 STRONG/N3/L20,L25;67 STRONG/N8/L30. Each gets BASE/NORMAL_APPEND/WEAK_APPEND, including normal controls alongside the newly selected weak-append signals. No C, a, skills, NPC profile or inputs were tuned.

```sh
causal_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dmyconid.study.suite=causal -Dmyconid.study.pairs=500 -Dmyconid.study.causalExtraLevels=20,25,30,40 -Dmyconid.study.causalGroupIds=33,34,38,43,57,58,67 -Dmyconid.study.causalVariants=BASE,NORMAL_APPEND,WEAK_APPEND -Dmyconid.study.seedOffset=2200000000 -Dmyconid.study.phase=STEP_15_3_INDEPENDENT_FIXED_CAUSAL_CONFIRMATION -cp "target/classes:target/test-classes:$causal_classpath" ru.homyakin.seeker.game.battle.MyconidAdaptationStudy /tmp/seeker-step15-3-causal.rPWNKs/independent-confirmation > /tmp/seeker-step15-3-causal.rPWNKs/independent-confirmation.log 2>&1
```

## Independent raw validation and pooling

`validate-causal.cjs` reads every complete raw line, checks original-member prefixes and prepared bases, seven-slot live weak gear, single-field development, seed mapping, initial-state Q, fixed target parameters, pair completeness, outcomes, differences and confidence intervals; it also verifies the compressed hash against the study manifest.

```sh
node validate-causal.cjs confirmation > confirmation-validation.json
node validate-causal.cjs broad > broad-validation.json
node validate-causal.cjs independent-confirmation > independent-confirmation-validation.json
node aggregate.cjs . > aggregate-single-look.json
node adjust-looks.cjs aggregate-single-look.json > aggregate.json
```

The aggregate independently reads saved first inputs and verifies exact semantic equality between the two500-pair cohorts, ignoring only schema Set serialization order. It checks non-overlapping generation/battle seed windows and pools pair differences, not individual mirrored battles. Raw gzip hashes retain provenance if the exact decoded streams are later losslessly repacked to xz.

The final pooled interval explicitly permits at most two views of each N3 comparison (first500, then pooled1000), because the second cohort was planned after inspecting the first. With range[-1,1], the final half-width is sqrt(2×ln(2×2/0.05)/1000)=about9.36 percentage points. The single-look8.59-point interval is preserved only as provenance and is not the primary decision interval. This adjustment is per comparison, not simultaneous over all four conditions/variants.
