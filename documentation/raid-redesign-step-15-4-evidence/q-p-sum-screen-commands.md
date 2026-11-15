# Step15.4 fixed Q/Psum screen: outside-production evidence

No production source, target, runtime configuration, database or bot was changed.
The helper calls the current private Myconid V3 factory reflectively and actual
`Battle.process`, not a copied combat model. Current-source/class pivot hashes
are in `source-basis.sha256` / `class-basis.sha256` (paths relative to repository
root). They were rechecked after both screens. The parent owns the complete
203-source step15.3 basis; these files identify this helper's direct pivots.

The completed Q/Psum screen is 36 conditions × 2 models × 20 mirrored pairs =
2880 fights. Each condition has 20 independent seed pairs, not 40 independent
observations. Models and causal variants intentionally share seeds. Do not pool
them into an independent sample. No timeout or draw occurred. This is exploratory
comparison, not final acceptance, not an all-50/50 target and not activation.

`P_SUM` is symmetric sum of individual neutral products, using precisely the
current EHP and max-profile normal-rate components. One C=.7298795655036827 was
chosen from the exact actual MID MAIN N3 L25 NPCs; a=.7 was retained. The helper's
six-condition Q bridge exactly matched the actual generator's calibration,
characteristics, UUIDs and gauges. Psum's single anchor preserves exact NPC
characteristics, rather than merely similar scores.

Result: reject Psum with fixed a. Its ordinary N3→4 result worsens dramatically:
MID L25 37.5→0%, STRONG L25 60→5% in these 20-pair screens. Solo also becomes
0%, and both solo/+weak losing is not a correction of the weak-addition effect.
The separately calculated two-anchor Psum a=1.388730865600158 was **not run**:
it preserves solo but raises h4 from .86875 to about1.17007, further strengthening
already excessive N4 NPCs. This is a causal structural rejection, not outcome fit.

All complete raw rows, inputs, snapshots, final actor stats and action/trace
SHA256 are retained in `screen/battles.jsonl.gz`; no losing rows were removed.
`screen/Step154FormulaProbe.java` is the exact frozen source used. `validate.py`
independently recomputes neutral components from actual battle-init snapshots,
scores, targets, NPC weighted characteristics and V3 profiles, outcomes from
remaining HP, prefix preservation, seeds and summaries. It does not independently
validate the combat engine or promise cross-JVM replay (not rerun here).

## Exact successful commands

Run from `/home/user/projects/project-seeker/game`. The output directory must
not already exist (`prepare` and `screen` create it). For an intentional replay,
change only the final output path to a fresh directory; this does not increase
the independent sample.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-prototype.bca9F6/classes /tmp/seeker-step15-4-prototype.bca9F6/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-prototype.bca9F6/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-prototype.bca9F6/preparation > /tmp/seeker-step15-4-prototype.bca9F6/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=20 -cp "/tmp/seeker-step15-4-prototype.bca9F6/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-prototype.bca9F6/screen > /tmp/seeker-step15-4-prototype.bca9F6/screen.log 2>&1
python3 /tmp/seeker-step15-4-prototype.bca9F6/validate.py /tmp/seeker-step15-4-prototype.bca9F6/screen
```

Hash check uses repository root cwd, not game cwd:

```sh
sha256sum -c /tmp/seeker-step15-4-prototype.bca9F6/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-prototype.bca9F6/class-basis.sha256
```

During inspection, two read-only checks used unavailable `node` / incorrect
`game` cwd for these relative hash paths and failed without changing artifacts.
They were corrected using Python and repository cwd; no failed battle run exists.
