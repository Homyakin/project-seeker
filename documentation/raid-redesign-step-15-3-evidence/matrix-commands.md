# Independent production matrix for step 15.3

Production: approved step 15.2, no source/class overlays or parameter changes.
Starting constants: C=0.5900075417594784, a=0.7; V5/V3/ACCELERATED_V1.
525 conditions, 20 mirrored pairs per condition, 21000 attempted battles.
This is exploratory evidence, not 525 independent balance acceptances.

From `/home/user/projects/project-seeker/game`:

```sh
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m \
  -Dmyconid.study.suite=full -Dmyconid.study.pairs=20 \
  -Dmyconid.study.extraStyles=COMMAND_CUIRASS,RITUAL,PIERCE_ARCANE,RAPIER_ARCANE \
  -Dmyconid.study.extraLevels=20,30,40 \
  -Dmyconid.study.seedOffset=3000000000 -Dmyconid.study.phase=STEP153_SCREEN \
  -cp "target/test-classes:target/classes:$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)" \
  ru.homyakin.seeker.game.battle.MyconidAdaptationStudy \
  /tmp/seeker-step153-screen.5Zg4N2/run \
  > /tmp/seeker-step153-screen.5Zg4N2/run.log 2>&1
```

Java source compilation target 24, runtime AxiomJDK25.0.1. No Maven, database,
application startup or bot connection was used by this independent run.
