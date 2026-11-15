# Derived commentary correction (no raw-data change)

Independent review found a stale limitation copied from unnormalized B in
`summarize.py` and therefore `screen/analysis.json`:

Before: `Ritual N15 L50 all three tiers100% is a risk signal, not a formal universal win guarantee`.

After: `All51 MAIN L50 and all18 high-specialized L50 cells have zero sampled wins; this does not fix addition penalties by itself`.

Only that commentary string in the derived summary script was corrected, the
derived analysis regenerated, and `artifacts.sha256` refreshed. The original
frozen Java sources, raw stream, inputs, manifests and main summary were not
edited. The numerical cells in the previous analysis already correctly showed
zero wins; this was a contradictory copied explanatory sentence, not a formula
or simulation defect. Preserve this audit when updating an archived analysis.

Raw gzip SHA256 remains:
`724f5ceab476101b6d5d606e99e08fa8c1b2fdf4e023f97444ba5eb9b82605f2`.
