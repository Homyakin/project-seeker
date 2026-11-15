# Step15.4 fixed-reference concentration normalization: exploratory B_NORM

Outside production only. No production/target/configuration edits, Maven,
database or bot. Exact actual private Myconid V3 factory and actual battles.
The519conditions are identical to the previously approved broad B screen;
inputs and manifests were saved before any battles. Exactly10380fights ran,
10mirroredpairs per condition; no timeout/draw, early stop or sample extension.

Symmetric B=Σij min(p_i,p_j), p_i=EHP_i×max-profile-neutral-normalRate_i,
computed by ascending Σ(2×(N−i)−1)×p_i. Target uses the precise operation order
`Bplayers * levelBonus * g(M) * C * h(M,aNew) * r(M)`.

The reference is actual15.2 MID MAIN3L25 NPCs:
mref=4.414285714285715, Bnpc3ref=1466224.9940141398.
The fixed correction is:
`r(M)=B(actualFactoryNPC(M,mref))/((M/3)^2*Bnpc3ref)`.
The complete2..30table is computed once, with factory RNG isolated at seed0,
then reused without consulting players, level, searched multiplier or outcomes.
r2=1.2945040158323535, r3=1. C=.47754531235323505 remains unchanged.
Second-anchor h2 is divided by r2, giving aNew=.8766292004637334. Both values
are hard-asserted against the approved project constants, not outcome-fitted.

Both actual NPC anchors remain characteristic-for-characteristic exact. An
additional144-condition bridge covers every M2/3 condition of this screen and
matches unnormalized B NPC characteristics, UUIDs and gauges exactly, including
effect cases. Six Q_CURRENT actual-generator bridges and six actual effectful
`PveAdaptationStudy.prepare` player bridges pass, too.

This is a substantial recalibration, not an innocuous local correction. Adjusted
a and r strengthen all large NPC teams. Maintaining the neutral reference and
roughly smooth multipliers does not guarantee smooth actual encounter outcomes.

Exploration: causal MID6→7L25=75→5% normal and75→25% weak; STRONG6→7L25=
65→30% normal. All51MAIN size/tier combinations atL50 and all18highSpecialized
L50 conditions lose every sampled fight. Both0% at oldL50 boundaries is not a
correction of the addition penalty: the difficulty boundary moved downwards.
Only10seedpairs/cell; these are risk signals awaiting separately fixed SHA48
confirmation, not formal probabilities or acceptance. Adjacent-table comparisons
use different seed groups and must not be mistaken for paired causal trials.

Full input/modifier/rarity/enhancement records, actual init positions, per-actor
final statistics, effect maps, calibration and neutral parts, full seed pairs,
action counts and action/trace hashes remain in every raw row. Losses retained.
`validate.py` independently recomputes component scores from actual init, checks
the fixed correction in every target, NPC weighted HP/Atk and unweighted Def,
V3points/profiles/rows, actual effects, seeds/mirrors, prefix/weak defaults and
canonical next MAIN member, final HP outcomes and pair summaries. It passes all
10380rows. It is not an independent combat-engine implementation and no new
cross-JVM battle replay was performed for this exploratory model.

## Exact successful commands

cwd `/home/user/projects/project-seeker/game`. New output directory required.
To replay, change only output path to a new directory, not seeds or constants.
This replay would not add independent observations.

```sh
formula_classpath=$(< /tmp/seeker-step15-3.LlzF3u/classpath.txt)
/home/user/.jdks/axiomjdk-25.0.1/bin/javac --release 24 -cp "target/classes:target/test-classes:$formula_classpath" -d /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/classes /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/Step154FormulaProbe.java
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -cp "/tmp/seeker-step15-4-bnorm-wide.bB1Bsl/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe prepare /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/preparation > /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/preparation.log 2>&1
env JAVA_HOME=/home/user/.jdks/axiomjdk-25.0.1 /home/user/.jdks/axiomjdk-25.0.1/bin/java -Xmx512m -Dformula.pairs=10 -cp "/tmp/seeker-step15-4-bnorm-wide.bB1Bsl/classes:target/classes:target/test-classes:$formula_classpath" ru.homyakin.seeker.game.battle.Step154FormulaProbe screen /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/screen > /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/screen.log 2>&1
python3 /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/validate.py /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/screen > /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/validation.log
python3 /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/summarize.py /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/screen
```

Roots are GEN2052110815400 and BATTLE3052110815400, i.e.original pilot roots
+20_000_000_000. Seeds=root+groupId×1000+pair, model/variant absent. This is a
new linear exploratory stream; the previous B streams and sources were not
overwritten. Any subsequent confirmation requires its own approved fixed list
and fresh SHA48 namespace, not an adaptive continuation of these seeds.

```sh
# cwd /home/user/projects/project-seeker
sha256sum -c /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/source-basis.sha256
sha256sum -c /tmp/seeker-step15-4-bnorm-wide.bB1Bsl/class-basis.sha256
```
