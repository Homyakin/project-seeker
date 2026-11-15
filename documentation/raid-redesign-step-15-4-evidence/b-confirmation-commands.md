# Step15.4 unnormalized B: fixed independent confirmation

All18conditions and500mirroredpairs were approved before this one run. Exactly
18000battles completed. No adaptive early stop, sample extension, C/a tuning or
production/target/configuration/Maven/database/bot action. C=.47754531235323505,
a=.6046927918979718 remain exactly frozen from two actual15.2 NPC anchors.

Fixed cases (IDs are stable and one-based):

|Cases|Group|Fixture|Variants|
|---|---|---|---|
|1–2|1|STRONG MAIN1L5|BASE/WEAK|
|3–4|2|STRONG PIERCE1L15|BASE/WEAK|
|5–6|3|STRONG RITUAL1L25|BASE/WEAK|
|7–8|4|MID MAIN3L25|BASE/NORMAL|
|9–10|5|STRONG MAIN3L25|BASE/NORMAL|
|11–13|6|MID MAIN12L50|BASE/NORMAL/WEAK|
|14–15|7|MID MAIN6L40|BASE/NORMAL|
|16–17|8|STRONG MAIN6L50|BASE/NORMAL|
|18|9|BEGINNER RITUAL15L50|BASE|

NORMAL appends the next actual canonical MAIN member and retains the original
prefix. WEAK appends the existing living840HP seven-slot COMMON+0 modifierless
fixture. Solo RITUAL is the original one-member backline fixture, not a guardian.
Archived wide/pilot input references independently verify canonical identities.

New research seeds (runtime RNG implementation unchanged):
`SHA256(namespace+'/'+groupId+'/'+pair)`, first6digestbytes unsigned big-endian.
GEN namespace=`MYCONID-15.4-CONFIRM-GEN-20261108-v1`;
BATTLE namespace=`MYCONID-15.4-CONFIRM-BATTLE-20261108-v1`.
Model/variant absent. Both mirrors reconstruct fresh participants. All4500unique
group/pair seed records are dumped before battles in `preparation/seeds.json`;
Python independently checks the Java derivation and48-bit bounds, no collisions.
`preparation/inputs.json` and manifest were also saved before battle invocation.

Results: unnormalized B is rejected as a general correction, despite fixing
original3→4. On new500pairs, ordinary MID12→13L50=62→17.6%(−44.4pp),
MID6→7L40=85.8→36.3%(−49.5pp), STRONG6→7L50=50.7→7.1%(−43.6pp).
All three simultaneous9-comparison95%Hoeffding upper bounds are below−20pp.
RITUALsolo+weak47.5→29.1%(−18.4pp) remains a confirmed negative effect.
RITUALBEGINNER15L50 wins97%; descriptive pointwise95%mean bound90.93–100%.
Full numeric pair differences and intervals are in `screen/analysis.json`.

The independent unit is a mirrored pair, not a battle. Nine causal comparisons
use paired differences in[-1,1], pointwise half12.147pp and Bonferroni9-family
half15.344pp. `inference-policy.json` was fixed before reading confirmation
outcomes; selection by the earlier exploratory matrix is disclosed. The separate
ritual absolute-winrate interval is not included in that nine-difference family.
The ordinary20pp margin is not blanket permission to penalize weak additions.

All18000complete rows, including every loss, remain in `screen/battles.jsonl.gz`.
The source is frozen beside the raw stream. No draws/timeouts. Independent
`validate.py` passes all rows:pre-dump equality, seeds, canonical references,
prefixes/weak defaults, neutral components from actual init, weighted factory
HP/Atk and unweighted Def, V3skills/rows/ranges, target/calibration scores, final
HP outcomes, individual statistics and pair summary. Direct source/class pivot
hashes match before/after. No repeated-JVM battle replay was performed here.

## Exact commands

cwd `/home/user/projects/project-seeker/game`; output directory must be new.
For replay change only final output path to a fresh directory; it does not add
independent evidence. The helper rejects paircounts other than500 and offsets.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-b-confirm.9rPIEP/classes /tmp/seeker-step15-4-b-confirm.9rPIEP/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-b-confirm.9rPIEP/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-b-confirm.9rPIEP/preparation > /tmp/seeker-step15-4-b-confirm.9rPIEP/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=500 -cp "/tmp/seeker-step15-4-b-confirm.9rPIEP/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-b-confirm.9rPIEP/screen > /tmp/seeker-step15-4-b-confirm.9rPIEP/screen.log 2>&1
python3 /tmp/seeker-step15-4-b-confirm.9rPIEP/validate.py /tmp/seeker-step15-4-b-confirm.9rPIEP/screen > /tmp/seeker-step15-4-b-confirm.9rPIEP/validation.log
python3 /tmp/seeker-step15-4-b-confirm.9rPIEP/summarize.py /tmp/seeker-step15-4-b-confirm.9rPIEP/screen
```

```sh
# cwd /home/user/projects/project-seeker
sha256sum -c /tmp/seeker-step15-4-b-confirm.9rPIEP/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-b-confirm.9rPIEP/class-basis.sha256
```
