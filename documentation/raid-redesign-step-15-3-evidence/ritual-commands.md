# Independent RITUAL confirmation, step 15.3

Production15.2 unchanged, C=0.5900075417594784/a=0.7. No source overlays,
Maven, application context, database or bot. Runtime AxiomJDK25.0.1, target24.

Cases294/306/318 are RITUAL N15 L50, BEGINNER/MID/STRONG, respectively.
The complete525-case numbering is retained. Each condition has500 new mirrored
pairs, not1000 independent observations. Offset3100000000 is separate from the
exploratory offset3000000000. No coefficient was fitted to the confirmation.

From `/home/user/projects/project-seeker/game`:

```sh
/home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m \
  -Dmyconid.study.suite=full -Dmyconid.study.pairs=500 \
  -Dmyconid.study.extraStyles=COMMAND_CUIRASS,RITUAL,PIERCE_ARCANE,RAPIER_ARCANE \
  -Dmyconid.study.extraLevels=20,30,40 -Dmyconid.study.caseIds=294,306,318 \
  -Dmyconid.study.seedOffset=3100000000 \
  -Dmyconid.study.phase=STEP153_RITUAL_CONFIRMATION \
  -cp "target/test-classes:target/classes:$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)" \
  ru.homyakin.seeker.game.battle.MyconidAdaptationStudy \
  /tmp/seeker-step153-ritual.zc5GXk/run \
  > /tmp/seeker-step153-ritual.zc5GXk/run.log 2>&1
```

Independent streaming validation:

```sh
node /tmp/seeker-step153-screen.5Zg4N2/analyze.cjs \
  /tmp/seeker-step153-ritual.zc5GXk/run 3 500 3100000000 \
  STEP153_RITUAL_CONFIRMATION > /tmp/seeker-step153-ritual.zc5GXk/analysis.json
node /tmp/seeker-step153-ritual.zc5GXk/inputs.cjs \
  /tmp/seeker-step153-ritual.zc5GXk/run/battles.jsonl.gz \
  > /tmp/seeker-step153-ritual.zc5GXk/inputs.json
```

Raw gzip SHA256:dd2107f66b8a5ae488a335f55d3c22d9ca7b1bf474e5290b3b2ff0b4a50a4885.
All3000 records validated: seeds, unique pairs/orders, summary counts, Q from
actual initial combat snapshots, weighted HP/ATK/DEF, NPC counts, positions,
V3/four points and the exact current target expression. No draws or timeouts.

| Tier | Wins/1000 | Win rate | Pointwise95% Hoeffding interval by500 pairs |
| --- | ---: | ---: | ---: |
| BEGINNER |614|61.4%|55.3–67.5%|
| MID |924|92.4%|86.3–98.5%|
| STRONG |981|98.1%|92.0–100%|

The samples support a strong specialized response, not passage by diverse
ordinary teams without resistance. They are not a reason on their own to
retune C/a or global skills. Other participant-addition risks are investigated
separately and can block acceptance independently of this response.
