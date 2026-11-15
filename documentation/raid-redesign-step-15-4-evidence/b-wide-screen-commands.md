# Step15.4 fixed broad B screen — not a production change

Root approved the complete519-condition list before battles. The candidate's
C=.47754531235323505 and a=.6046927918979718 are unchanged and asserted exactly.
They come from exact actual15.2 NPCs at MID MAIN3L25 and STRONG MAIN1L5, not
outcomes. Both anchors retain all NPC characteristics exactly. Actual current
Myconid V3 factory, role weights, bases, levelbonus and accelerated battle remain.
There was no production/target edit, Maven, database or bot action.

The exploratory screen is519×10mirroredpairs×2orders=10380 actual battles:

-306 MAIN:BEGINNER/MID/STRONG,N2..16+29/30,L10/20/25/30/40/50.
-60 causal additions: MID/STRONG,N2/4/6/8/14,L20/25/30,normal/weak. The BASE
  is already in the306grid; APPEND retains that exact prefix and seed-group.
-6 actual effectful MAINMID:N3/15,L25/30/40,sharing BASEinputs/seeds.
-45 lowSolo:PIERCE/RAPIER all3tiers L1/5/10;MAIN all3tiers L1/5/10/20/25;
  RITUAL all3tiers L1/5/15/25.
-48 specialized3:FLANK/PRESSURE/RITUAL/PIERCE all3tiers L10/25/40/50.
-54 highSpecialized:RITUAL/PIERCE/RAPIER all3tiers,N7/15,L30/40/50.

`preparation/inputs.json` and manifest were written before the screen. Concrete
team sizes were asserted, including specialized profiles. `screen/inputs.json`
is checked for canonical equality with the prior dump. Every full raw row stores
complete item/modifier/rarity/enhancement inputs, actual init state and positions,
each actor's final statistics, outcomes, seed pair, calibration, score parts,
effect maps and action/trace hashes. No timeouts or draws occurred, no rows omitted.

Effects are the actual private `PveAdaptationStudy.combatEffects(cell)` result,
not a hand-invented combat effect. Six separate preparation controls compare
helper players against actual `PveAdaptationStudy.prepare(cell)` and exactly
match their characteristics, UUIDs and gauges. Actual fixture maps are20%ATTACK
and15%HEALTH; their null expiration is the original permanent test fixture.
The maps are archived in every row and independently checked.

Results and limitations: see `screen/analysis.json`. The old3→4 harm is removed
in the pilot, but this wider screen exposes potential renewed even→odd harm at
high levels: MID12→13L50=70→5%, MID6→7L40=80→35%, STRONG6→7L50=45→5%.
These adjacent MAIN inputs are nested but use different seed groups,10pairs each;
they are selection signals, not paired causal confirmations. Causal additions
here only coverL20/25/30, where most cells saturate, and thus do not resolveL50.
RITUAL15L50=100% for all3tiers is another risk signal. Effects do not guarantee
better winrate (e.g.MID3L25=45→25%), consistent with a remaining scalar limitation.

Neither B nor15.3 is accepted from this screen. There is no new difficulty
progression or requirement of50/50 for every build. The519conditions were fixed
upfront; no per-cell parameter fitting. Do not pool5190 mirrored-pair observations
into one probability. Shared causal/effect seeds create correlation between cells.
Future independent confirmations require separately fixed conditions and seeds.

## Exact successful commands

cwd `/home/user/projects/project-seeker/game`; output directories must not exist.
For a replay change only the output path to a new directory. Replaying does not
increase the independent sample. The source is frozen beside the raw stream.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-b-wide.i70lxr/classes /tmp/seeker-step15-4-b-wide.i70lxr/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-b-wide.i70lxr/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-b-wide.i70lxr/preparation > /tmp/seeker-step15-4-b-wide.i70lxr/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=10 -cp "/tmp/seeker-step15-4-b-wide.i70lxr/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-b-wide.i70lxr/screen > /tmp/seeker-step15-4-b-wide.i70lxr/screen.log 2>&1
python3 /tmp/seeker-step15-4-b-wide.i70lxr/validate.py /tmp/seeker-step15-4-b-wide.i70lxr/screen > /tmp/seeker-step15-4-b-wide.i70lxr/validation.log
python3 /tmp/seeker-step15-4-b-wide.i70lxr/summarize.py /tmp/seeker-step15-4-b-wide.i70lxr/screen
```

Seed roots are GEN2042110815400 and BATTLE3042110815400, i.e.pilot roots
+10_000_000_000. Each seed=root+groupId×1000+pair. All10pairs reconstruct fresh
actors and each pair runs both team orders. Model/variant is absent from seed;
trials have isolated seed0. These are intentionally linear exploratory seeds,
not the SHA48 namespaces proposed for any separately authorized confirmations.

Direct source/class pivot hashes were rechecked before and after battle run:

```sh
# cwd /home/user/projects/project-seeker
sha256sum -c /tmp/seeker-step15-4-b-wide.i70lxr/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-b-wide.i70lxr/class-basis.sha256
```
