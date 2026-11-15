# Step15.4 symmetric minimum-pair candidate: outside-production screen

Approved exploratory comparison only. No production/target edits, Maven,
database or bot. Uses actual current private Myconid V3 factory and real battles.
Exact Java source is also frozen in `screen/Step154FormulaProbe.java`.

Candidate B=Σij min(p_i,p_j), p_i=EHP_i×normalRate_i. Both player and NPC teams
use the same neutral components. For reproducible floating operations the code
sorts p ascending then accumulates (2×(N−i)−1)×p_i for zero-based i. This is
mathematically the same as Σp+2Σi<j min(p_i,p_j), not legacyPower.

Two fixed exact actual15.2 NPC anchors, not winrate fitting, give:
C=.47754531235323505; r2=.7135166925639456;
h2=1.4941340101275353; a=.6046927918979718.
The reference MID MAIN N3 L25 and STRONG MAIN N1 L5 NPC characteristics are
both preserved exactly; six Q_CURRENT bridge conditions also pass exactly.
Unchanged bases, role weights, fixed floor front count, g, bounded h family,
old level bonus, accelerated combat and V3 skills. No local cell fitting.

Screen: identical36 inputs, levels, group/pair seeds and order pairs as the
Q/Psum screen at `/tmp/seeker-step15-4-prototype.bca9F6`. One model ×20pairs
×2orders =1440 fights, 20 independent pairs per condition. No timeout/draw.
This new model is correlated with the Q comparison through common seeds;
it is not another independent confirmation sample of Q's winrate.

Exploration shows ordinary3→4 no longer harms at the old boundary; e.g.
MID L25 37.5→90%, STRONG60→97.5%; weak additions yield100%. Solo+weak now
MAIN70→80%, PIERCE72.5→77.5%, RITUAL40→30%. These are small-sample rates.
Large groups are much easier: MID N30 L40/L50=95/47.5%, STRONG100/62.5%
(Q baseline0/0); strong+29weak and weak-only30 remain0 on L30/50. Odd/even
new boundaries4→5/6→7 have NOT been tested. This does not accept B or15.3,
does not establish ideal progression, and does not justify changing production.

All1440 full raw rows are preserved, including losses. `validate.py` independently
checks canonical inputs/prefixes/weak840 defaults, seed/mirror consistency,
neutral components from init, symmetric scores and targets, exact weighted
HP/attack/unweighted defense, correct NPC V3 skills/rows/ranges, final HP outcomes
and pair summaries. Pointwise20-pair intervals remain broad/not simultaneous.
No repeated-JVM battle replay was performed for this exploratory screen.

## Exact successful commands

cwd `/home/user/projects/project-seeker/game`; new output paths must not exist.
For deliberate replay replace only output path with another fresh directory.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-b-min.siriKz/classes /tmp/seeker-step15-4-b-min.siriKz/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-b-min.siriKz/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-b-min.siriKz/preparation > /tmp/seeker-step15-4-b-min.siriKz/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=20 -cp "/tmp/seeker-step15-4-b-min.siriKz/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-b-min.siriKz/screen > /tmp/seeker-step15-4-b-min.siriKz/screen.log 2>&1
python3 /tmp/seeker-step15-4-b-min.siriKz/validate.py /tmp/seeker-step15-4-b-min.siriKz/screen
```

Generation root2032110815400, battle root3032110815400, offset0. Exact seeds
are roots+groupId×1000+pair, models/variants excluded. Trials use isolated seed0
and do not consume final construction RNG. Whole source/class pivot hashes
match the Q/Psum basis. Hash-check commands use repository root cwd:

```sh
sha256sum -c /tmp/seeker-step15-4-b-min.siriKz/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-b-min.siriKz/class-basis.sha256
```
