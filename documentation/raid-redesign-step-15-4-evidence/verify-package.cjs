const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {digest, readArchivedFile} = require('./archived-file.cjs');
const near = (a, b) => assert(Math.abs(a - b) < 1e-9, `${a} != ${b}`);
(async () => {
  const manifest = JSON.parse(fs.readFileSync(path.join(__dirname, 'manifest.json')));
  const index = JSON.parse(fs.readFileSync(path.join(__dirname, 'compressed-metadata.json')));
  let artifacts = 0, battles = 0;
  const results = [];
  for (const run of manifest.runs) {
    for (const artifact of run.artifacts) {
      assert.equal(digest(await readArchivedFile(__dirname, artifact.file)), artifact.sha256, artifact.file);
      ++artifacts;
    }
    const summary = JSON.parse(await readArchivedFile(__dirname, run.name + '-summary.json'));
    const validation = JSON.parse(await readArchivedFile(__dirname, run.name + '-validation.json'));
    assert.equal(validation.status, 'PASS');
    assert.equal(validation.rows, run.battles);
    assert.equal(validation.rawGzipSha256, run.originalGzipSha256);
    assert.equal(validation.rawUncompressedSha256, run.jsonlSha256);
    assert.equal(validation.outcomes.DRAW || 0, 0);
    assert.equal(validation.outcomes.TIMEOUT || 0, 0);
    let count = 0;
    for (const row of summary.cases) {
      const outcomes = Object.values(row.outcomes).reduce((sum, value) => sum + value, 0);
      assert.equal(outcomes, row.pairs * 2);
      assert.equal(row.pairScores.length, row.pairs);
      near(row.pairMean, row.pairScores.reduce((sum, value) => sum + value, 0) / row.pairs);
      near(row.winrate, (row.outcomes.WIN || 0) / outcomes);
      count += outcomes;
    }
    assert.equal(count, run.battles);
    const result = {name: run.name, conditions: validation.conditions, models: validation.models,
      pairsPerCondition: validation.pairsPerCondition, battles: count, rawValidationStatus: validation.status};
    if (run.name === 'b-confirmation' || run.name === 'bnorm-confirmation') {
      const analysis = JSON.parse(await readArchivedFile(__dirname, run.name + '-analysis.json'));
      const byId = new Map(summary.cases.map(value => [value.case.id, value]));
      assert.equal(analysis.comparisons.length, 9);
      const half = 100 * Math.sqrt(2 * Math.log(2 * 9 / .05) / 500);
      for (const comparison of analysis.comparisons) {
        const base = byId.get(comparison.baseCaseId), append = byId.get(comparison.id);
        assert.equal(base.pairs, 500); assert.equal(append.pairs, 500);
        const differences = append.pairScores.map((value, i) => value - base.pairScores[i]);
        const delta = 100 * differences.reduce((sum, value) => sum + value, 0) / 500;
        near(delta, comparison.deltaPP);
        differences.forEach((value, i) => near(value, comparison.pairedDeltas[i]));
        near(Math.max(-100, delta - half), comparison.simultaneous9Family95HoeffdingPP[0]);
        near(Math.min(100, delta + half), comparison.simultaneous9Family95HoeffdingPP[1]);
      }
      result.pairedFamilyComparisonsRecomputed = 9;
      result.familyHalfWidthPP = half;
    }
    results.push(result); battles += count;
  }
  for (const entry of index) {
    assert.equal(digest(await readArchivedFile(__dirname, entry.file)), entry.rawSha256);
  }
  assert.equal(battles, 60080); assert.equal(manifest.totalBattles, battles);
  console.log(JSON.stringify({status: 'PACKAGE_CONTENT_AND_DERIVED_SUMMARIES_VALID', artifacts,
    compressedMetadataFiles: index.length, totalBattles: battles, runs: results,
    note: 'Integrity and saved-data verification only, not balance acceptance or a new battle run.'}, null, 2));
})().catch(error => {console.error(error); process.exitCode = 1;});
