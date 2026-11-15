# Portable independent pilot validation

The original validate-split-screen.cjs and validation.json remain byte-unchanged.

The separate validate-split-screen-portable.cjs changes only how prebattle-freeze.sha256 paths are read: it derives the original producing root from the first helper entry, rejects paths outside it, and resolves each relative path inside the ROOT supplied on the command line. It never opens the old absolute file path.

The portable copy was run against the packaged evidence, not the original producer directory:

```sh
/home/user/.cache/codex-runtimes/codex-primary-runtime/dependencies/node/bin/node /tmp/seeker-split-independent-validation.cEYqwS/validate-split-screen-portable.cjs /home/user/projects/project-seeker/documentation/raid-redesign-step-15-4-1-evidence/screen-study
```

Result: PASS, 6000 rows, 4138628 assertions. All states/pairScores/statistics and raw hashes are exactly equal to the original independent report. Assertions increase only by one inside-root check per freeze entry. No Java, battles, production, target or DB changes.

Portable validator SHA256: 0152f996b9c74f0b81f88700af8724ac669423a6c61a768620d46c60cb2af570.
Raw gzip SHA256: 618cf6f483d3503b19b340dd4712a7c652a310f005d6561f93a9b0a1292ca444.
Raw plain SHA256: ac73a325903d80f2b860d255a80c8b86fdb2cac21281253353bf5a33708eabb4.

