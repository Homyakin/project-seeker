// Generate isolated copies of the captured real generator; never edit working game sources.
// Run from repository root: node prepare-candidates.mjs NEW_CANDIDATE_ROOT CLASSPATH JAVAC
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import {spawnSync} from 'node:child_process';
import assert from 'node:assert/strict';

const [out, classpath, javac, ...extra] = process.argv.slice(2);
assert(out && classpath && javac && extra.length === 0);
assert(!fs.existsSync(out), 'Output must be new');
const evidence = path.dirname(out);
const ratios = ['0.92', '1.10', '1.30', '1.50', '1.75', '2.00', '2.40'];
const sourcePath = 'game/src/main/java/ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.java';
const classPath = 'ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator';
const expectedSourceSha256 = '838e31ca00f740e78633b85f4621c143ef72a08f44944493e8b3fb76adc4d751';
const sha = bytes => crypto.createHash('sha256').update(bytes).digest('hex');
const hash = file => sha(fs.readFileSync(file));
const source = fs.readFileSync(sourcePath, 'utf8');
assert.equal(sha(source), expectedSourceSha256, 'Captured post-19.2, pre-calibration source');
const from = 'final double targetPower = players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * SAFE_POWER_RATIO;';
const toTemplate = 'final double targetPower = players.stream().mapToDouble(BattlePersonage::legacyPower).sum()'
  + '\n            * (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3 ? {RATIO} : SAFE_POWER_RATIO);';
assert.equal(source.split(from).length, 2, 'Exactly one numerical change site');
fs.mkdirSync(out);
const baselineSourcePath = path.join(evidence, 'baseline-source', sourcePath);
fs.mkdirSync(path.dirname(baselineSourcePath), {recursive:true});
fs.copyFileSync(sourcePath, baselineSourcePath, fs.constants.COPYFILE_EXCL);
const oldProtocol = JSON.parse(fs.readFileSync('documentation/raid-redesign-step-19-2-evidence/run-v2/protocol.json'));
const snapshotPaths = Object.keys(oldProtocol.publicSourceSha256).sort();
for (const file of snapshotPaths) assert.equal(hash(file), oldProtocol.publicSourceSha256[file], file);
const archive = path.join(evidence, 'pre-calibration-sources.tar.gz');
assert(!fs.existsSync(archive));
const archived = spawnSync('tar', ['-czf', archive, ...snapshotPaths], {encoding:'utf8'});
assert.equal(archived.status, 0, archived.stderr);
const metadata = {
  purpose:'Single V3 ratio substitution in captured post-19.2 real generator; no runtime override',
  baselineSourcePath, baselineSourceSha256:expectedSourceSha256,
  sourceArchive:archive, sourceArchiveSha256:hash(archive),
  sourceArchiveFileCount:snapshotPaths.length,
  substitution:{from,toTemplate},
  compiler:'javac 25.0.1 --release 24 -encoding UTF-8',
  candidates:{}
};
for (const ratio of ratios) {
  const candidateSource = source.replace(from, toTemplate.replace('{RATIO}', ratio));
  const file = path.join(out, 'sources', `ratio-${ratio}`, sourcePath);
  const classesDirectory = path.join(out, `ratio-${ratio}`);
  fs.mkdirSync(path.dirname(file), {recursive:true});
  fs.mkdirSync(classesDirectory);
  fs.writeFileSync(file, candidateSource, {flag:'wx'});
  const compiled = spawnSync(javac, ['--release','24','-encoding','UTF-8','-cp',classpath,'-d',classesDirectory,file],
    {encoding:'utf8'});
  assert.equal(compiled.status,0,compiled.stdout + compiled.stderr);
  const classSha256ByPath = Object.fromEntries(['.class','$1.class'].map(suffix => {
    const relative = classPath + suffix;
    return [relative,hash(path.join(classesDirectory,relative))];
  }));
  metadata.candidates[ratio] = {sourcePath:file,sourceSha256:hash(file),classesDirectory,classSha256ByPath};
}
fs.writeFileSync(path.join(out,'metadata.json'),JSON.stringify(metadata,null,2)+'\n',{flag:'wx'});
console.log(JSON.stringify({candidateCount:ratios.length,baselineSourceSha256:expectedSourceSha256,
  metadataSha256:hash(path.join(out,'metadata.json')),sourceArchiveSha256:metadata.sourceArchiveSha256}));
