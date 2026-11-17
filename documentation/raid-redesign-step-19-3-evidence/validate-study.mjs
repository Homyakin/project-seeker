// Independent reader of saved evidence. Does not launch Java, a simulator or a database.
// From the repository root: node validate-study.mjs RUN REPLAY NEW_REPORT
// SCREEN and CONFIRM are separate fixed phases; this reader never launches battles.
// All comparisons include every JSON field. NEW_REPORT must not already exist.
import fs from 'node:fs';
import path from 'node:path';
import crypto from 'node:crypto';
import zlib from 'node:zlib';
import readline from 'node:readline';
import assert from 'node:assert/strict';

const [run, replay, output, ...extra] = process.argv.slice(2);
assert(run && replay && output && extra.length === 0, 'Expected RUN REPLAY NEW_REPORT');
assert(!fs.existsSync(output), 'Output report already exists');
const inputsOnly = replay === '--inputs-only';
const evidence = path.dirname(run);
const protocol = JSON.parse(fs.readFileSync(path.join(run, 'protocol.json')));
const summary = inputsOnly ? null : JSON.parse(fs.readFileSync(path.join(run, 'summary.json')));
const templates = ['ASH_WIND', 'SHADOW_NEEDLES', 'IRON_PHALANX', 'CRYSTAL_STORM'];
const tiers = ['BEGINNER', 'MID', 'STRONG'];
const fixedRatios = ['0.92','1.10','1.30','1.50','1.75','2.00','2.40'];
const phase = protocol.phase;
assert(['SCREEN','CONFIRM'].includes(phase),'registered phase');
const ratios = protocol.ratios, pairCount = phase === 'SCREEN' ? 20 : 80;
const generationRoot = phase === 'SCREEN' ? 20_261_203_000_000 : 20_261_204_000_000;
const battleRoot = phase === 'SCREEN' ? 30_261_203_000_000 : 30_261_204_000_000;
const logicalCases = 72, expectedBattles = logicalCases * ratios.length * pairCount * 2;
assert.equal(new Set(ratios).size,ratios.length);
if (phase === 'SCREEN') assert.deepEqual(ratios,fixedRatios);
else {
  assert.equal(ratios.length,2); assert.equal(ratios[0],'0.92');
  assert(fixedRatios.includes(ratios[1]) && ratios[1] !== '0.92');
}
const skillCodes = ['COUNTER_ATTACK','THORNS','DOUBLE_ATTACK','BERSERK','HIT_AND_RUN','BLEEDING','KNOCKBACK',
  'SELF_HEAL','PRECISE_STRIKE','RETREAT','FEINT','GUARD','PENETRATION','ACCUMULATION','TEMPO_BREAK'];
const sha = value => crypto.createHash('sha256').update(value).digest('hex');
const hash = file => sha(fs.readFileSync(file));
const stable = value => Array.isArray(value) ? value.map(stable) : value && typeof value === 'object'
  ? Object.fromEntries(Object.keys(value).sort().map(key => [key, stable(value[key])])) : value;
const same = (a, b, label) => assert.deepEqual(stable(a), stable(b), label);
const near = (a, b, label) => assert(Number.isFinite(a) && Number.isFinite(b)
  && Math.abs(a - b) <= Math.max(1, Math.abs(a), Math.abs(b)) * 1e-11, `${label}: ${a} != ${b}`);
const compare = (actual, expected, label) => {
  if (typeof expected === 'number') return near(actual, expected, label);
  if (expected === null || typeof expected !== 'object') return same(actual, expected, label);
  same(Object.keys(actual).sort(), Object.keys(expected).sort(), `${label} keys`);
  for (const key of Object.keys(expected)) compare(actual[key], expected[key], `${label}.${key}`);
};
const add = (object, key, amount = 1) => { object[key] = (object[key] || 0) + amount; };
const mean = values => values.reduce((a, b) => a + b, 0) / values.length;
const distribution = values => {
  if (values.length === 0) return {count: 0};
  const ordered = [...values].sort((a, b) => a - b), q = p => ordered[Math.ceil(p * ordered.length) - 1];
  return {count: values.length, mean: mean(values), min: ordered[0], p50: q(.5), p90: q(.9),
    p95: q(.95), max: ordered.at(-1)};
};
const interval = (values, delta = false) => {
  const m = mean(values), width = Math.sqrt(Math.log(40) * (delta ? 2 : .5) / values.length);
  return {low: Math.max(delta ? -1 : 0, m - width), high: Math.min(1, m + width), unclippedHalfWidth: width};
};
// Mirrors the numerical definition, not Java execution or precomputed player ratings.
// The old dodge max(1, 1-dodge/100), uncapped initiative and slot-one clamp are intentional.
const weights = {CLOTH: [.75, 1.25, .9, 1.1], LEATHER: [.9, 1.1, 1.25, .75],
  PLATE: [1.25, .75, 1.1, .9], ARCANE: [1.1, .9, .75, 1.25]};
const javaSum = values => {
  let result = 0, compensation = 0, simple = 0;
  for (const value of values) {
    const adjusted = value - compensation, next = result + adjusted;
    compensation = (next - result) - adjusted; result = next; simple += value;
  }
  const corrected = result - compensation;
  return Number.isNaN(corrected) && !Number.isFinite(simple) ? simple : corrected;
};
const legacyPower = ({health, attackAtOne, critChance, dodgeChance, critMultiplier, speed, defenses}) => {
  const taken = javaSum([0, 1, 2, 3].map(type => {
    const defense = Object.entries(defenses).reduce((sum, [kind, value]) => sum + value * weights[kind][type], 0);
    return 1 - defense / (defense + 500);
  })) / 4;
  const damage = Math.max(1, Math.max(1, attackAtOne) * (1 + critChance / 100 * (critMultiplier - 1)));
  return Math.max(1, health) * damage / taken / Math.max(1, 1 - dodgeChance / 100)
    * (Math.max(1, speed) / 1000);
};
const fromItems = items => {
  const result = {health:0, attackAtOne:0, critChance:0, dodgeChance:0, critMultiplier:1.2,
    speed:0, baseThreat:0, defenses:{}, attacks:[], skills:{}};
  const points = {COMMON:0, UNCOMMON:1, RARE:2, EPIC:3, LEGENDARY:4};
  for (const item of items) {
    assert.equal(item.enhanceLevel, 0, 'combat item enhancement removed');
    const object = item.object;
    result.health += object.health; result.speed += object.speed;
    result.critChance += object.critChance; result.dodgeChance += object.dodgeChance;
    result.critMultiplier += object.critMultiplier; result.baseThreat += object.baseThreat;
    if (object.defense) add(result.defenses, object.defense.defenseType, object.defense.defense);
    for (const attack of object.attacks) {
      result.attacks.push(attack);
      if (attack.minRange <= 1 && attack.maxRange >= 1) result.attackAtOne += attack.attack;
    }
    assert(Object.hasOwn(points, item.rarity), 'known item rarity');
    if (item.modifier && points[item.rarity] > 0)
      add(result.skills, item.modifier.activeEnum, points[item.rarity] * object.slots.length);
  }
  return result;
};
// The frozen catalog uses only these scalar/array and item/attack/defense tables.
// Locale prose is irrelevant to numeric assessment and is checked by the source hashes.
const readCatalogTables = (file,rootName) => {
  const result = [], source = fs.readFileSync(file,'utf8'); let current, section;
  for (const untrimmed of source.split(/\r?\n/)) {
    const line = untrimmed.trim(); if (!line || line.startsWith('#')) continue;
    if (line === `[[${rootName}]]`) { current = {}; result.push(current); section = current; continue; }
    if (line === `[[${rootName}.attacks]]`) {
      assert(current); section = {}; (current.attacks ||= []).push(section); continue;
    }
    if (line === `[${rootName}.defense]`) { assert(current); section = current.defense = {}; continue; }
    if (line.startsWith('[')) { assert(line.startsWith(`[${rootName}.locales.`)); section = null; continue; }
    if (section === null) continue;
    const match = line.match(/^([A-Za-z][A-Za-z0-9]*)\s*=\s*(.+)$/); assert(match,`supported catalog syntax ${line}`);
    assert(section && !Object.hasOwn(section,match[1])); section[match[1]] = JSON.parse(match[2]);
  }
  return result;
};
const catalogRoot = 'game/src/main/resources/game-data/catalog/scaling_v5/';
const objects = readCatalogTables(catalogRoot + 'item_objects.toml','item');
const defaults = readCatalogTables(catalogRoot + 'default_items.toml','item');
const modifiers = readCatalogTables(catalogRoot + 'item_modifiers.toml','modifier');
assert.equal(objects.length,63); assert.equal(defaults.length,7); assert.equal(modifiers.length,15);
const catalogObjects = new Map([...objects,...defaults].map(object => [object.code,object]));
const catalogModifiers = new Map(modifiers.map(modifier => [modifier.activeEnum,modifier]));
assert.equal(catalogObjects.size,70); assert.equal(catalogModifiers.size,15);
const validateItem = item => {
  const expected = catalogObjects.get(item.object.code); assert(expected,'actual V5 catalog item');
  for (const key of ['code','health','critChance','dodgeChance','critMultiplier','speed','baseThreat','impact','progressionVersion'])
    same(item.object[key],expected[key],`V5 catalog ${expected.code}.${key}`);
  same(item.object.slots,[...expected.slots].sort(),'catalog slots');
  same(item.object.attacks,expected.attacks || [],'catalog attack vectors');
  same(item.object.defense,expected.defense || null,'catalog defense');
  if (item.modifier) {
    const expectedModifier = catalogModifiers.get(item.modifier.activeEnum); assert(expectedModifier,'catalog modifier');
    assert.equal(item.modifier.code,expectedModifier.code); assert.equal(item.modifier.type,expectedModifier.type);
    same(item.modifier.availableOnSlots,[...expectedModifier.slots].sort(),'modifier allowed slots');
    assert(expected.slots.some(slot => expectedModifier.slots.includes(slot)),'modifier-compatible slot');
    assert(expectedModifier.type === 'ANY' || expectedModifier.type === 'ATTACK' && expected.attacks?.length > 0
      || expectedModifier.type === 'DEFENSE' && expected.defense,'modifier-compatible item property');
  }
};
// Read ustar/GNU archives without a shell or extraction; GNU long names apply to exactly one following entry.
const tarEntries = file => {
  const data = zlib.gunzipSync(fs.readFileSync(file)), entries = new Map(); let longName = null;
  for (let offset = 0; offset + 512 <= data.length;) {
    const header = data.subarray(offset, offset + 512); offset += 512;
    if (header.every(byte => byte === 0)) break;
    const str = (start, length) => header.subarray(start, start + length).toString().replace(/\0.*$/s, '');
    const headerName = [str(345,155), str(0,100)].filter(Boolean).join('/').replace(/^\.\//, '');
    const size = parseInt(str(124,12).trim() || '0', 8), kind = str(156,1);
    assert(Number.isInteger(size) && size >= 0 && offset + size <= data.length, 'valid tar size');
    const bytes = data.subarray(offset, offset + size);
    if (kind === 'L') {
      assert.equal(longName,null,'one pending GNU long name');
      longName = bytes.toString().replace(/\0.*$/s,'').replace(/^\.\//,'');
    } else {
      const name = longName ?? headerName; longName = null;
      assert(!name.startsWith('/') && !name.split('/').includes('..'), 'safe tar member name');
      assert(['','0','5'].includes(kind),'only regular files and directories in frozen archives');
      if (kind === '' || kind === '0') { assert(!entries.has(name)); entries.set(name,bytes); }
    }
    offset += Math.ceil(size / 512) * 512;
  }
  assert.equal(longName,null,'no unconsumed GNU long name'); return entries;
};

// Binding and matrix checks are defined below; saved inputs must be frozen before outcomes.
assert.equal(protocol.logicalCaseCount,logicalCases);
assert.equal(protocol.caseCount,logicalCases * ratios.length);
assert.equal(protocol.attemptedBattles,expectedBattles);
if (!inputsOnly) {
  assert.equal(summary.phase,phase); same(summary.ratios,ratios,'summary ratios');
  assert.equal(summary.logicalCaseCount,logicalCases);
  assert.equal(summary.caseCount,logicalCases * ratios.length);
  assert.equal(summary.attemptedBattles,expectedBattles);
  assert.equal(summary.independentLogicalPairs,logicalCases * pairCount);
}
assert.equal(protocol.independentLogicalPairs,logicalCases * pairCount);
assert.equal(protocol.preBattleGenerationCount,logicalCases * pairCount * ratios.length);
assert.equal(protocol.generationRoot,generationRoot); assert.equal(protocol.battleRoot,battleRoot);
assert.equal(protocol.metric,'LEGACY_SUM_V1'); assert.equal(protocol.catalog,'SCALING_V5');
assert.equal(protocol.playerSkills,'SCALING_SKILLS_V3'); assert.equal(protocol.npcSkills,'SCALING_SKILLS_V3');
assert.equal(protocol.tempo,'ACCELERATED_V1'); assert.equal(protocol.tempoDamageMultiplier,1.75);
assert.equal(protocol.roundLimit,2000);
for (const [file,expected] of Object.entries(protocol.publicSourceSha256)) assert.equal(hash(file),expected,file);
for (const required of ['item_objects.toml','default_items.toml','item_modifiers.toml'])
  assert(Object.hasOwn(protocol.publicSourceSha256,catalogRoot + required),'real V5 resources are frozen inputs');
assert(Object.hasOwn(protocol.publicSourceSha256,path.relative(process.cwd(),path.resolve(evidence,'validate-study.mjs'))),
  'independent validator frozen before battles');
if (!inputsOnly) {
  assert.equal(summary.protocolSha256,hash(path.join(run,'protocol.json')));
  assert.equal(summary.rawSha256,hash(path.join(run,'battles.jsonl.gz')));
}
const generatorSource = 'game/src/main/java/ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.java';
const generatorClass = 'ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.class';
const generatorSyntheticClass = generatorClass.slice(0,-6) + '$1.class';
const expectedClassPaths = [generatorClass,generatorSyntheticClass].sort();
const originalSourceHash = '838e31ca00f740e78633b85f4621c143ef72a08f44944493e8b3fb76adc4d751';
const metadataFile = path.join(evidence,'candidate-generators/metadata.json');
const metadata = JSON.parse(fs.readFileSync(metadataFile));
const archiveMetadataFile = path.join(evidence,'candidate-archive.json');
const archiveMetadata = JSON.parse(fs.readFileSync(archiveMetadataFile));
assert.equal(hash(metadataFile),archiveMetadata.metadataSha256,'candidate metadata archive binding');
assert.equal(hash(archiveMetadata.archivePath),archiveMetadata.archiveSha256,'all isolated candidates archive');
assert.equal(archiveMetadata.metadataSha256,'f2e1527d2f0b027bd228b8645bbacb3761ba57e77cfdb8d276a225b0e51b7cc3');
assert.equal(archiveMetadata.archiveSha256,'4d05c137ddf6d6c9ca6b3da625ba6a7ff2adaeac798725fd7b24e82eac5d27b8');
assert.equal(hash(metadata.baselineSourcePath),originalSourceHash,'captured post-19.2 real generator');
assert.equal(metadata.baselineSourceSha256,originalSourceHash);
assert.equal(hash(metadata.sourceArchive),metadata.sourceArchiveSha256,'pre-calibration public inputs archive');
assert.equal(metadata.sourceArchiveSha256,'a04fa5551c36229b6e47c5dba9d9f241d90ff570b2ba39fa0d28a2adb225f5cd');
assert.equal(metadata.compiler,'javac 25.0.1 --release 24 -encoding UTF-8');
const sourceEntries = tarEntries(metadata.sourceArchive), candidateEntries = tarEntries(archiveMetadata.archivePath);
assert.equal(sourceEntries.size,256); assert.equal(metadata.sourceArchiveFileCount,sourceEntries.size);
assert.equal(candidateEntries.size,22,'seven source files, fourteen class files and one metadata file');
assert.equal(sha(candidateEntries.get(metadataFile)),hash(metadataFile),'metadata is inside the immutable archive');
assert.equal(sha(sourceEntries.get(generatorSource)),originalSourceHash);
const originalSource = sourceEntries.get(generatorSource).toString();
const replacementFrom = 'final double targetPower = players.stream().mapToDouble(BattlePersonage::legacyPower).sum() * SAFE_POWER_RATIO;';
const replacementTo = 'final double targetPower = players.stream().mapToDouble(BattlePersonage::legacyPower).sum()'
  + '\n            * (formulaVersion == SkillFormulaVersion.SCALING_SKILLS_V3 ? {RATIO} : SAFE_POWER_RATIO);';
same(metadata.substitution,{from:replacementFrom,toTemplate:replacementTo},'exact single V3-only target substitution');
assert.equal(originalSource.split(replacementFrom).length,2);
same(Object.keys(metadata.candidates).sort(),[...fixedRatios].sort(),'all seven frozen candidates, no later tuning');
for (const ratio of fixedRatios) {
  const candidate = metadata.candidates[ratio], expected = originalSource.replace(replacementFrom,replacementTo.replace('{RATIO}',ratio));
  assert.equal(fs.readFileSync(candidate.sourcePath,'utf8'),expected,'candidate differs at one V3 ratio site only');
  assert.equal(hash(candidate.sourcePath),candidate.sourceSha256);
  same(Object.keys(candidate.classSha256ByPath).sort(),expectedClassPaths,'complete isolated class family');
  for (const [relative,expectedHash] of Object.entries(candidate.classSha256ByPath)) {
    const file = path.join(candidate.classesDirectory,relative);
    assert.equal(hash(file),expectedHash,'actual isolated class bytes');
    const member = path.relative(process.cwd(),path.resolve(file));
    assert(candidateEntries.has(member),'candidate class archived');
    assert.equal(sha(candidateEntries.get(member)),expectedHash,'archived candidate class bytes');
  }
  assert.equal(sha(candidateEntries.get(path.relative(process.cwd(),path.resolve(candidate.sourcePath)))),candidate.sourceSha256,
    'candidate source archived');
}
assert(Object.hasOwn(protocol.publicSourceSha256,path.relative(process.cwd(),path.resolve(archiveMetadataFile))),
  'candidate outer archive binding frozen in phase protocol');
assert.equal(protocol.candidateMetadataSha256,hash(metadataFile));
same(protocol.candidateMetadata,metadata,'entire captured-source/candidate binding frozen in protocol');
assert.equal(path.resolve(protocol.candidateRootPath),path.resolve(path.dirname(metadataFile)));
same(Object.keys(protocol.candidateClassSha256ByRatio).sort(),[...ratios].sort(),'exact active isolated candidates');
for (const ratio of ratios)
  same(protocol.candidateClassSha256ByRatio[ratio],metadata.candidates[ratio].classSha256ByPath,'protocol actual candidate byte hashes');
same(Object.keys(protocol.currentClassSha256ByPath).sort(),expectedClassPaths,'complete current production class family');
for (const [relative,expectedHash] of Object.entries(protocol.currentClassSha256ByPath))
  assert.equal(hash(path.join('game/target/classes',relative)),expectedHash,'compiled actual production generator is frozen');
assert.equal(protocol.productionCandidateFullSnapshotComparisons,phase === 'CONFIRM' ? 5760 : 0,
  'actual selected production vs frozen isolated generator full-snapshot preflight count');
assert.equal(protocol.pairsPerCase,pairCount);

// These 72 cases and their weights are fixed independently of the Java matrix builder.
const samples = new Map(protocol.cases.map(entry => [entry.sample.caseId,entry.sample]));
assert.equal(protocol.cases.length,logicalCases); assert.equal(samples.size,logicalCases);
const expectedMatrix = [];
const matrixKey = s => [s.family,s.cell.encounter,s.cell.style,s.cell.tier,s.cell.size].join('|');
const expect = (family,template,style,tier,size) => expectedMatrix.push([family,template,style,tier,size].join('|'));
for (const template of templates) {
  for (const tier of tiers) for (const n of [3,4,5]) expect('A_MAIN',template,'MAIN',tier,n);
  for (const style of ['RITUAL','COMMAND_CUIRASS']) for (const n of [3,4,5])
    expect('C_DIRECTIONS',template,style,'MID',n);
  for (const n of [3,4,5]) expect('D_DEFAULTS',template,'FULL_DEFAULTS','BEGINNER',n);
}
same([...samples.values()].map(matrixKey).sort(),expectedMatrix.sort(),'exact independent N3–5 matrix');
const familyCounts = {}, seedByCase = new Map();
for (const [index,entry] of protocol.cases.entries()) {
  const s = entry.sample; assert.equal(s.caseId,index + 1);
  assert.equal(s.cell.mode,'ANOMALY_SAFE'); assert.equal(s.cell.raidLevel,0);
  assert.equal(s.cell.effects,false); assert.equal(s.members.length,s.cell.size);
  assert.equal(s.pairs,pairCount); assert.equal(entry.seeds.length,pairCount);
  add(familyCounts,s.family); seedByCase.set(s.caseId,entry.seeds);
  for (const [pair,seed] of entry.seeds.entries()) {
    assert.equal(seed.pair,pair);
    assert.equal(seed.generationSeed,generationRoot + s.caseId * 1000 + pair);
    assert.equal(seed.battleSeed,battleRoot + s.caseId * 1000 + pair);
    assert.equal(seed.expectedEnemyCount,s.cell.size);
  }
  assert.equal(new Set(entry.seeds.map(seed => seed.generationSeed)).size,pairCount);
}
same(familyCounts,{A_MAIN:36,C_DIRECTIONS:24,D_DEFAULTS:12},'fixed family weights');
const replayPairs = new Set(protocol.replayPairs.map(p => `${p.caseId}/${p.pair}`));
const expectedReplayPairs = [...samples.values()].filter(s =>
  s.family === 'A_MAIN' && s.cell.tier === 'MID' || phase === 'CONFIRM' &&
  (s.family === 'C_DIRECTIONS' && s.cell.style === 'COMMAND_CUIRASS' || s.family === 'D_DEFAULTS'))
  .map(s => `${s.caseId}/0`);
same([...replayPairs].sort(),expectedReplayPairs.sort(),'predeclared replay cases, all templates and N3/4/5');
assert.equal(replayPairs.size,phase === 'SCREEN' ? 12 : 36);

const scaled = (base, m) => Math.max(1, Math.ceil(base * m));
const npcPowerAt = (template, count, multiplier) => javaSum(Array.from({length:count}, () => legacyPower({
  health:scaled(340,multiplier), attackAtOne:scaled(90,multiplier), critChance:5, dodgeChance:4,
  critMultiplier:1.2 + .4, speed:180, defenses:{[{
    ASH_WIND:'CLOTH', SHADOW_NEEDLES:'LEATHER', IRON_PHALANX:'PLATE', CRYSTAL_STORM:'ARCANE'
  }[template]]:scaled(100,multiplier)}
})));
const nextDown = value => {
  const buffer = new ArrayBuffer(8), floats = new Float64Array(buffer), bits = new BigUint64Array(buffer);
  floats[0] = value; bits[0] -= 1n; return floats[0];
};
const independentSearch = (target, template, count) => {
  const maximum = nextDown(Math.floor(2_147_483_647 / 16) / 340);
  let evaluations = 1, low = 0, lowPower = npcPowerAt(template,count,0);
  if (lowPower >= target) return {multiplier:0,targetPower:target,achievedPower:lowPower,
    status:lowPower === target ? 'MATCHED' : 'MINIMUM_EXCEEDS_TARGET',evaluations};
  let high = Math.min(1,maximum), highPower = npcPowerAt(template,count,high); ++evaluations;
  for (let expansions = 0; highPower < target; ++expansions) {
    assert(high < maximum && expansions < 1024, 'reachable NPC target');
    low = high; lowPower = highPower; high = Math.min(maximum,high * 2);
    highPower = npcPowerAt(template,count,high); ++evaluations;
  }
  if (highPower === target) return {multiplier:high,targetPower:target,achievedPower:highPower,status:'MATCHED',evaluations};
  for (let step = 0; step < 64; ++step) {
    const middle = low + (high - low) / 2;
    if (middle <= low || middle >= high) break;
    const middlePower = npcPowerAt(template,count,middle); ++evaluations;
    if (middlePower === target) return {multiplier:middle,targetPower:target,achievedPower:middlePower,status:'MATCHED',evaluations};
    if (middlePower >= target) { high = middle; highPower = middlePower; }
    else { low = middle; lowPower = middlePower; }
  }
  const lower = target - lowPower < highPower - target;
  return {multiplier:lower ? low : high,targetPower:target,achievedPower:lower ? lowPower : highPower,
    status:'CLOSEST_STEP',evaluations};
};
const npcInfo = (template, index, count) => {
  let position, skill;
  switch (template) {
    case 'ASH_WIND': position = index === 0 ? 'FRONT' : 'MID'; skill = 'HIT_AND_RUN'; break;
    case 'SHADOW_NEEDLES': position = index < Math.ceil(count / 2) ? 'FRONT' : 'BACK';
      skill = position === 'FRONT' ? 'THORNS' : 'BLEEDING'; break;
    case 'IRON_PHALANX': position = 'FRONT'; skill = 'BERSERK'; break;
    case 'CRYSTAL_STORM': position = index === 0 ? 'FRONT' : 'BACK';
      skill = position === 'FRONT' ? 'KNOCKBACK' : 'PRECISE_STRIKE'; break;
    default: assert.fail('Unknown template');
  }
  const type = {ASH_WIND:'SLASH',SHADOW_NEEDLES:'PIERCE',IRON_PHALANX:'BLUNT',CRYSTAL_STORM:'MAGICAL'}[template];
  const armor = {ASH_WIND:'CLOTH',SHADOW_NEEDLES:'LEATHER',IRON_PHALANX:'PLATE',CRYSTAL_STORM:'ARCANE'}[template];
  const range = position === 'BACK' ? 3 : position === 'MID' || type === 'MAGICAL' ? 2 : 1;
  const actualPosition = ['FRONT','MID','BACK'][Math.min(['FRONT','MID','BACK'].indexOf(position),range - 1)];
  return {position,skill,type,armor,range,actualPosition,threat:{FRONT:30,MID:20,BACK:12}[position]};
};

// Domain checks for the independent formula, including the deliberately preserved old quirks.
const checkInput = {health:100,attackAtOne:10,critChance:0,dodgeChance:0,critMultiplier:1.2,speed:1000,defenses:{}};
assert.equal(legacyPower(checkInput),1000);
assert.equal(legacyPower({...checkInput,dodgeChance:90}),1000);
assert.equal(legacyPower({...checkInput,speed:2000}),2000);
assert.equal(legacyPower({...checkInput,attackAtOne:0}),100);

if (inputsOnly) {
  const report = {purpose:'INDEPENDENT_PREFLIGHT_NO_BATTLES',phase,passesInputChecks:true,
    logicalCases,ratioCases:logicalCases * ratios.length,plannedBattles:expectedBattles,
    originalSourceHash,candidateArchiveSha256:archiveMetadata.archiveSha256,
    checkedSourceFiles:Object.keys(protocol.publicSourceSha256).length,
    protocolSha256:hash(path.join(run,'protocol.json'))};
  fs.writeFileSync(output,JSON.stringify(report,null,2) + '\n',{flag:'wx'});
  console.log(JSON.stringify(report)); process.exit(0);
}


const fresh = () => ({outcomes:{},rounds:[],skills:{},skillsByKind:{},statuses:{},errors:[],metrics:{},
  byOrder:{},pairs:{},enemyCounts:{},byCount:{},enemyActions:{}});
const caseStats = new Map(), groups = new Map(), seen = new Set(), pairs = new Map(), crossRatioPlayers = new Map();
const originalRepeats = new Map();
let rowCount = 0, fullLogsChecked = 0, crossRatioPlayerContexts = 0;
async function* rows(file) {
  const lines = readline.createInterface({input:fs.createReadStream(file).pipe(zlib.createGunzip()),crlfDelay:Infinity});
  for await (const line of lines) { assert(line.length > 0); yield JSON.parse(line); }
}
const addMetrics = (stats,row) => {
  for (const metric of ['rounds','actionCount','normalAttempts','moves','forcedMoves'])
    (stats.metrics[metric] ||= []).push(row[metric]);
  for (const side of ['playerTotals','enemyTotals']) for (const metric of ['survivors','initialHealth','remainHealth',
    'normalAttacksHit','normalDamage','critDamage','skillDamage','turns'])
    (stats.metrics[`${side}.${metric}`] ||= []).push(row[side][metric]);
};
const collect = (stats,row) => {
  add(stats.outcomes,row.outcome); add(stats.statuses,row.calibration.status);
  stats.errors.push(Math.abs(row.enemyPower / row.calibration.targetPower - 1));
  add(stats.enemyCounts,row.enemyBasis.length);
  add(stats.byCount[row.enemyBasis.length] ||= {},row.outcome);
  if (row.outcome !== 'TIMEOUT') {
    stats.rounds.push(row.rounds); addMetrics(stats,row);
    for (const [skill,count] of Object.entries(row.skillEvents)) add(stats.skills,skill,count);
    for (const [kind,count] of Object.entries(row.skillEventsByKind)) add(stats.skillsByKind,kind,count);
    for (const enemy of row.enemyBasis) {
      const label = `${row.cell.encounter}/${enemy.declaredPosition}/${enemy.skills[0].code}`;
      const actions = stats.enemyActions[label] ||= {observations:0,turns:0,normalAttempts:0,moves:0,damage:0,noNormalAttempt:0};
      ++actions.observations;
      for (const field of ['turns','normalAttempts','moves','damage']) actions[field] += row.enemyActionCounts[enemy.id][field];
      actions.noNormalAttempt += Number(row.enemyActionCounts[enemy.id].normalAttempts === 0);
    }
  }
};
const summarize = stats => ({outcomes:stats.outcomes,attemptedBattles:Object.values(stats.outcomes).reduce((a,b) => a+b,0),
  winFraction:(stats.outcomes.WIN || 0) / Object.values(stats.outcomes).reduce((a,b) => a+b,0),
  rounds:distribution(stats.rounds),skills:stats.skills,skillsByKind:stats.skillsByKind,statuses:stats.statuses,
  enemyCounts:stats.enemyCounts,enemyActions:stats.enemyActions,calibrationAbsoluteRelativeError:distribution(stats.errors),
  metrics:Object.fromEntries(Object.entries(stats.metrics).map(([key,values]) => [key,distribution(values)]))});
const validateSnapshot = (snapshot,basis,expected,label) => {
  assert(snapshot,`${label} exists in actual battle`);
  assert.equal(snapshot.health,expected.health); assert.equal(snapshot.initiative,expected.speed);
  assert.equal(snapshot.critChance,expected.critChance); assert.equal(snapshot.dodgeChance,expected.dodgeChance);
  near(snapshot.critMultiplier,expected.critMultiplier,`${label} critical multiplier`);
  same(snapshot.defenses,expected.defenses,`${label} armor`);
  same(snapshot.skills,basis.skills,`${label} actual skill formula and points`);
};
const fullLogChecks = row => {
  const events = row.actionLog.events, traces = row.traceEvents;
  assert.equal(events.length,row.actionCount);
  assert.equal(events.filter(event => event.type === 'MovedTowardEnemy').length,row.moves);
  assert.equal(events.filter(event => event.type === 'PersonageForcedMove').length,row.forcedMoves);
  assert.equal(traces.filter(event => Object.hasOwn(event,'attemptId')).length,row.normalAttempts);
  assert.equal(sha(JSON.stringify(row.actionLog)),row.actionSha256,'full action content hash');
  assert.equal(sha(JSON.stringify(row.traceEvents)),row.traceSha256,'full trace content hash');
  const playerIds = new Set(row.playerBasis.map(p => p.id)), enemyIds = new Set(row.enemyBasis.map(p => p.id));
  const counts = Object.fromEntries(['PLAYER','ENEMY'].flatMap(side => skillCodes.map(skill => [`${side}:${skill}`,0])));
  const kinds = {}, enemyActions = Object.fromEntries([...enemyIds].map(id => [id,{normalAttempts:0,turns:0,moves:0,damage:0}]));
  for (const trace of traces) {
    if (Object.hasOwn(trace,'attemptId')) {
      if (enemyIds.has(trace.attackerId)) ++enemyActions[trace.attackerId].normalAttempts;
    } else if (Object.hasOwn(trace,'lineIndex') && enemyIds.has(trace.personageId)) ++enemyActions[trace.personageId].turns;
  }
  for (const event of events) {
    if (event.type === 'MovedTowardEnemy' && enemyIds.has(event.personageId)) ++enemyActions[event.personageId].moves;
    const damageSource = event.attackerId || event.sourceId;
    if (['DamageReceived','ScalingSkillDamage','SkillDamage','EffectDamage'].includes(event.type)
      && enemyIds.has(damageSource)) enemyActions[damageSource].damage += event.damageTaken;
    let source,kind;
    switch (event.type) {
      case 'SkillWindowUsed': source = event.personageId; kind = 'WINDOW'; break;
      case 'ScalingSkillDamage': source = event.sourceId; kind = event.periodic ? 'SCALING_PERIODIC_DAMAGE' : 'SCALING_DIRECT_DAMAGE'; break;
      case 'SkillDamage': source = event.sourceId; kind = 'LEGACY_DIRECT_DAMAGE'; break;
      case 'EffectDamage': source = event.sourceId; kind = 'LEGACY_PERIODIC_DAMAGE'; break;
      case 'PersonageHealed': source = event.personageId; kind = 'HEAL'; break;
      case 'PersonageAttackBuffed': source = event.personageId; kind = 'BUFF'; break;
      case 'PersonageForcedMove': source = event.sourceId; kind = 'FORCED_MOVE'; break;
      default: continue;
    }
    assert(skillCodes.includes(event.skill),'known skill event');
    const side = playerIds.has(source) ? 'PLAYER' : enemyIds.has(source) ? 'ENEMY' : null; assert(side,'real skill source');
    add(counts,`${side}:${event.skill}`); add(kinds,`${side}:${event.skill}:${kind}`);
  }
  same(counts,row.skillEvents,'full-log skill event counts'); same(kinds,row.skillEventsByKind,'full-log event kinds');
  same(enemyActions,row.enemyActionCounts,'full-log per-NPC actions, turns, movement and damage');
};
for await (const row of rows(path.join(run,'battles.jsonl.gz'))) {
  ++rowCount;
  assert.equal(row.phase,phase);
  const s = samples.get(row.caseId); assert(s,'registered logical case');
  assert.equal(row.logicalCaseId,row.caseId); assert(ratios.includes(row.ratio));
  assert.equal(row.family,s.family); same(row.cell,s.cell,'registered cell');
  assert.equal(typeof row.reverse,'boolean');
  assert(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < s.pairs);
  const key = `${row.caseId}/${row.ratio}/${row.pair}/${row.reverse}`;
  assert(!seen.has(key),'duplicate raw row'); seen.add(key);
  const seeds = seedByCase.get(row.caseId)[row.pair];
  assert.equal(row.generationSeed,seeds.generationSeed); assert.equal(row.battleSeed,seeds.battleSeed);
  assert.equal(row.catalog,'SCALING_V5'); assert.equal(row.playerSkills,'SCALING_SKILLS_V3');
  assert.equal(row.npcSkills,'SCALING_SKILLS_V3'); assert.equal(row.tempo,'ACCELERATED_V1');
  assert.equal(row.roundLimit,2000); assert.equal(row.calibrationMetric,'LEGACY_SUM_V1');
  assert.equal(row.members.length,s.cell.size); assert.equal(row.playerBasis.length,s.cell.size);
  assert.equal(row.enemyBasis.length,seeds.expectedEnemyCount);
  assert.equal(row.enemyCount,seeds.expectedEnemyCount);
  same(row.members.map(m => ({build:m.build,position:m.position,tactic:m.tactic,items:m.sourceItems})),s.members,'frozen input members');
  same(row.combatEffects,{},'anomaly removes temporary effects');
  same(row.sourceEffects,{},'registered study has no fabricated source effects');
  assert.equal(row.playerGenerationGauges.length,s.cell.size);
  assert.equal(row.enemyGenerationGauges.length,seeds.expectedEnemyCount);
  assert(Number.isInteger(row.postGenerationRandomDraw) && row.postGenerationRandomDraw >= 0
    && row.postGenerationRandomDraw < 1_000_000);
  const playerPowers = [];
  for (const [i,member] of row.members.entries()) {
    const combat = member.sourceItems.map(item => ({...item,enhanceLevel:0}));
    same(member.combatItems,combat,'only enhancement removed from copied items');
    const slots = combat.flatMap(item => item.object.slots);
    assert.equal(slots.length,7); assert.equal(new Set(slots).size,7,'legal complete equipment');
    const input = fromItems(combat); playerPowers.push(legacyPower(input));
    for (const item of combat) validateItem(item);
    const basis = row.playerBasis[i];
    near(basis.legacyPower,legacyPower(input),'independent individual player assessment');
    assert.equal(basis.health,input.health); assert.equal(basis.declaredPosition,member.position);
    assert.equal(basis.attack,input.attackAtOne); assert.equal(basis.maxHealth,input.health);
    assert.equal(basis.initiative,input.speed); assert.equal(basis.critChance,input.critChance);
    assert.equal(basis.dodgeChance,input.dodgeChance); near(basis.critMultiplier,input.critMultiplier,'player basis critical multiplier');
    assert.equal(basis.threat,input.baseThreat); assert.equal(basis.tactic,member.tactic);
    assert.equal(basis.initiativeGauge,row.playerGenerationGauges[i]);
    assert(Number.isInteger(basis.initiativeGauge) && basis.initiativeGauge >= 0 && basis.initiativeGauge <= 500);
    assert.equal(basis.defense,Object.values(input.defenses).reduce((a,b) => a+b,0));
    same(Object.fromEntries(basis.skills.map(skill => {
      assert.equal(skill.formulaVersion,'SCALING_SKILLS_V3'); return [skill.code,skill.points];
    })),input.skills,'real player modifiers and rarity points');
    if (row.outcome !== 'TIMEOUT') validateSnapshot(row.initState.personagesById[basis.id],basis,input,'player');
  }
  const playerPower = javaSum(playerPowers);
  near(row.playerPower,playerPower,'independent old assessment of unenhanced combat copies');
  const target = playerPower * Number(row.ratio);
  near(row.calibration.targetPower,target,'old assessment times exactly the registered V3 ratio');
  const independentlyCalibrated = independentSearch(target,s.cell.encounter,row.enemyBasis.length);
  compare(row.calibration,independentlyCalibrated,'independent bounded calibration including status/m/evaluations');
  const enemyPowers = [];
  for (const [index,enemy] of row.enemyBasis.entries()) {
    const info = npcInfo(s.cell.encounter,index,row.enemyBasis.length);
    const m = row.calibration.multiplier, health = scaled(340,m), attack = scaled(90,m), defense = scaled(100,m);
    assert.equal(enemy.health,health); assert.equal(enemy.attack,attack); assert.equal(enemy.defense,defense);
    assert.equal(enemy.range,info.range); assert.equal(enemy.declaredPosition,info.position);
    same(enemy.attackTypes,[info.type],'template attack type');
    same(enemy.skills,[{code:info.skill,points:4,formulaVersion:'SCALING_SKILLS_V3'}],'one legendary NPC skill');
    const input = {health,attackAtOne:attack,speed:180,critChance:5,dodgeChance:4,
      critMultiplier:1.2 + .4,defenses:{[info.armor]:defense}};
    enemyPowers.push(legacyPower(input));
    near(enemy.legacyPower,legacyPower(input),'independent individual NPC assessment');
    assert.equal(enemy.initiative,180); assert.equal(enemy.critChance,5); assert.equal(enemy.dodgeChance,4);
    near(enemy.critMultiplier,input.critMultiplier,'NPC basis critical multiplier');
    assert.equal(enemy.maxHealth,health); assert.equal(enemy.threat,info.threat);
    assert.equal(enemy.initiativeGauge,row.enemyGenerationGauges[index]);
    assert(Number.isInteger(enemy.initiativeGauge) && enemy.initiativeGauge >= 0 && enemy.initiativeGauge <= 500);
    same(enemy.attacksByRange,Array.from({length:info.range},(_,j) => ({range:j + 1,attack:{[info.type]:attack}})),
      'entire generated NPC distance vector');
    if (row.outcome !== 'TIMEOUT') {
      const snapshot = row.initState.personagesById[enemy.id]; validateSnapshot(snapshot,enemy,input,'NPC');
      assert.equal(snapshot.range,info.range);
      assert.equal(snapshot.totalThreat,info.threat);
      assert.equal(snapshot.initiativeGauge,enemy.initiativeGauge);
      same(snapshot.attacksByRange,Array.from({length:info.range},(_,j) => ({range:j + 1,attack:{[info.type]:attack}})),
        'entire actual NPC distance vector');
      assert.equal(row.actualEnemyPositions[index],info.actualPosition);
      assert.equal(row.initState.lines[snapshot.lineIndex].position,info.actualPosition,'actual engine placement');
    }
  }
  const enemyPower = javaSum(enemyPowers);
  near(row.enemyPower,enemyPower,'independent final NPC old assessment');
  near(row.calibration.achievedPower,enemyPower,'probe and final factory agree');
  assert(['WIN','LOSS','DRAW','TIMEOUT'].includes(row.outcome));
  if (row.outcome !== 'TIMEOUT') {
    assert.equal(row.outcome,row.playerTotals.survivors > 0 ? 'WIN' : row.enemyTotals.survivors > 0 ? 'LOSS' : 'DRAW');
    assert.equal(row.initState.tempoVersion,row.tempo);
    assert(Number.isInteger(row.rounds) && row.rounds >= 0 && row.rounds <= 2000);
    assert.match(row.actionSha256,/^[0-9a-f]{64}$/); assert.match(row.traceSha256,/^[0-9a-f]{64}$/);
    assert(row.playerTotals.survivors === 0 || row.enemyTotals.survivors === 0,'completed battle has a defeated side');
    assert.equal(row.playerTotals.initialHealth,row.playerBasis.reduce((sum,p) => sum + p.health,0));
    assert.equal(row.enemyTotals.initialHealth,row.enemyBasis.reduce((sum,p) => sum + p.health,0));
    same(Object.keys(row.enemyActionCounts).sort(),row.enemyBasis.map(p => p.id).sort(),'counters for each real NPC');
    for (const values of Object.values(row.enemyActionCounts)) for (const [field,value] of Object.entries(values))
      assert(Number.isSafeInteger(value) && value >= 0,`nonnegative NPC ${field}`);
    assert.equal(Object.values(row.enemyActionCounts).reduce((sum,a) => sum + a.turns,0),row.enemyTotals.turns);
    assert.equal(Object.values(row.enemyActionCounts).reduce((sum,a) => sum + a.damage,0),
      row.enemyTotals.normalDamage + row.enemyTotals.critDamage + row.enemyTotals.skillDamage);
    same(Object.keys(row.skillEvents).sort(),['PLAYER','ENEMY'].flatMap(side => skillCodes.map(skill => `${side}:${skill}`)).sort(),
      'both sides and all real skills, including zeros');
    for (const [key,value] of Object.entries(row.skillEvents)) {
      assert(Number.isSafeInteger(value) && value >= 0,'nonnegative skill event count');
      assert.equal(value,Object.entries(row.skillEventsByKind).filter(([kind]) => kind.startsWith(key + ':'))
        .reduce((sum,[,count]) => sum + count,0),'raw skill counter agrees with all event kinds');
    }
  }
  const invariantFields = ['members','sourceEffects','combatEffects','calibration','calibrationMetric','generationDiagnostics',
    'playerBasis','playerPower','enemyPower','playerGenerationGauges','enemyGenerationGauges','postGenerationRandomDraw'];
  const invariant = Object.fromEntries(invariantFields.map(field => {
    assert(Object.hasOwn(row,field),`raw invariant recorded: ${field}`); return [field,row[field]];
  }));
  invariant.enemyBasis = row.enemyBasis;
  const sharedPlayerKey = `${row.caseId}/${row.pair}/${row.reverse}`;
  const sharedPlayers = Object.fromEntries(['members','sourceEffects','combatEffects','playerBasis','playerPower',
    'playerGenerationGauges'].map(field => [field,row[field]]));
  if (crossRatioPlayers.has(sharedPlayerKey)) {
    const prior = crossRatioPlayers.get(sharedPlayerKey);
    same(prior.players,sharedPlayers,'same seeded players and combat copies across ratio candidates');
    assert(!prior.ratios.has(row.ratio)); prior.ratios.add(row.ratio);
    if (prior.ratios.size === ratios.length) { crossRatioPlayers.delete(sharedPlayerKey); ++crossRatioPlayerContexts; }
  } else crossRatioPlayers.set(sharedPlayerKey,{players:sharedPlayers,ratios:new Set([row.ratio])});
  const mirroredKey = `${row.caseId}/${row.ratio}/${row.pair}`;
  if (pairs.has(mirroredKey)) {
    const previous = pairs.get(mirroredKey); assert.notEqual(previous.reverse,row.reverse);
    same(previous.invariant,invariant,'mirrored generation invariants'); pairs.delete(mirroredKey);
  } else pairs.set(mirroredKey,{reverse:row.reverse,invariant});
  const caseKey = `${row.caseId}/${row.ratio}`, cs = caseStats.get(caseKey) || fresh(); caseStats.set(caseKey,cs);
  collect(cs,row); (cs.pairs[row.pair] ||= []).push(row.outcome);
  add(cs.byOrder[row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'] ||= {},row.outcome);
  const dimensions = [['ALL','ALL'],['TEMPLATE',s.cell.encounter],['SIZE',s.cell.size],['PROFILE',s.cell.tier],
    ['FAMILY',s.family],['STYLE',s.cell.style],['FAMILY_TEMPLATE',`${s.family}/${s.cell.encounter}`],
    ['MAIN_TEMPLATE_SIZE',`${s.cell.encounter}/${s.cell.size}`],['MAIN_TEMPLATE_PROFILE',`${s.cell.encounter}/${s.cell.tier}`],
    ['MAIN_PROFILE_SIZE',`${s.cell.tier}/${s.cell.size}`]];
  for (const [dimension,value] of dimensions) {
    if (dimension === 'MAIN_TEMPLATE_SIZE' && s.family !== 'A_MAIN') continue;
    if (['MAIN_TEMPLATE_PROFILE','MAIN_PROFILE_SIZE'].includes(dimension) && s.family !== 'A_MAIN') continue;
    const groupKey = `${row.ratio}/${dimension}/${value}`, stats = groups.get(groupKey) || fresh();
    groups.set(groupKey,stats); collect(stats,row);
  }
  if (s.family === 'A_MAIN') for (const role of new Set(row.members.map(member => member.build))) {
    const key = `${row.ratio}/MAIN_ROLE_EXPOSURE/${role}`, stats = groups.get(key) || fresh();
    groups.set(key,stats); collect(stats,row);
  }
  if (replayPairs.has(`${row.caseId}/${row.pair}`)) {
    assert(row.actionLog && row.traceEvents,'preselected original rows retain every event');
    fullLogChecks(row);
    originalRepeats.set(key,row); ++fullLogsChecked;
  } else assert(!Object.hasOwn(row,'actionLog') && !Object.hasOwn(row,'traceEvents'),'full logs only for fixed repeats');
}
assert.equal(rowCount,expectedBattles); assert.equal(seen.size,expectedBattles);
assert.equal(caseStats.size,logicalCases * ratios.length); assert.equal(pairs.size,0);
assert.equal(crossRatioPlayers.size,0); assert.equal(crossRatioPlayerContexts,logicalCases * pairCount * 2);
const expectedReplayRows = (phase === 'SCREEN' ? 12 : 36) * ratios.length * 2;
assert.equal(originalRepeats.size,expectedReplayRows); assert.equal(fullLogsChecked,expectedReplayRows);
assert.equal(summary.cases.length,logicalCases * ratios.length);
assert.equal(summary.comparisons.length,logicalCases * (ratios.length - 1));
const independentCases = new Map();
for (const result of summary.cases) {
  const key = `${result.caseId}/${result.ratio}`, s = samples.get(result.caseId), actual = caseStats.get(key);
  assert(s && actual); assert(!independentCases.has(key),'unique case summary');
  assert.equal(result.family,s.family); same(result.cell,s.cell,'summary cell'); assert.equal(result.pairs,s.pairs);
  same(result.outcomes,actual.outcomes,'independent summary outcomes');
  same(result.outcomesByOrder,actual.byOrder,'independent order results');
  same(result.outcomesByEnemyCount,actual.byCount,'independent count-branch results');
  same(result.skillEvents,actual.skills,'all summary skill counts'); same(result.skillEventsByKind,actual.skillsByKind,'all summary event-kind counts');
  same(result.calibrationStatuses,actual.statuses,'summary calibration status counts');
  compare(result.calibrationAbsoluteRelativeError,distribution(actual.errors),'all calibration errors');
  assert.equal(result.attemptedBattles,s.pairs * 2);
  assert.equal(result.completedBattles,s.pairs * 2 - (actual.outcomes.TIMEOUT || 0));
  near(result.playerWinsPerAttempt,(actual.outcomes.WIN || 0) / (s.pairs * 2),'summary win rate');
  const scores = Array.from({length:s.pairs},(_,i) => {
    const outcomes = actual.pairs[i]; assert.equal(outcomes.length,2);
    return outcomes.includes('TIMEOUT') ? null : outcomes.reduce((sum,outcome) => sum + (outcome === 'WIN' ? .5 : outcome === 'DRAW' ? .25 : 0),0);
  });
  const complete = scores.filter(value => value !== null);
  same(result.pairScoresByIndex,scores,'paired indices'); assert.equal(result.completePairs,complete.length);
  assert.equal(result.incompletePairs,s.pairs - complete.length);
  compare(result.pairedScore,distribution(complete),'independent pair-score distribution');
  if (complete.length) compare(result.pairedScore95Hoeffding,interval(complete),'independent paired interval');
  compare(result.completedBattleMetrics,Object.fromEntries(Object.entries(actual.metrics).map(([metric,values]) => [metric,distribution(values)])),
    'all independent completed-battle metric distributions');
  independentCases.set(key,{...summarize(actual),pairScoresByIndex:scores,pairedScore:distribution(complete),
    ...(complete.length ? {pairedScore95Hoeffding:interval(complete)} : {})});
}
assert.equal(independentCases.size,logicalCases * ratios.length);

const independentComparisons = [], comparisonKeys = new Set();
for (const result of summary.comparisons) {
  const id = result.logicalCaseId, s = samples.get(id), ratio = result.currentRatio;
  assert(s && ratios.includes(ratio) && ratio !== '0.92');
  const key = `${id}/${ratio}`; assert(!comparisonKeys.has(key)); comparisonKeys.add(key);
  assert.equal(result.baselineRatio,'0.92'); assert.equal(result.family,s.family);
  same(result.cell,s.cell,'comparison context');
  const baseline = independentCases.get(`${id}/0.92`), current = independentCases.get(`${id}/${ratio}`);
  const deltas = baseline.pairScoresByIndex.map((before,i) => before === null || current.pairScoresByIndex[i] === null
    ? null : current.pairScoresByIndex[i] - before);
  const complete = deltas.filter(value => value !== null);
  same(result.pairedDeltas,deltas,'same-seed paired deltas');
  compare(result.distribution,distribution(complete),'independent paired-delta distribution');
  if (complete.length) compare(result.pairedDelta95Hoeffding,interval(complete,true),'independent paired-delta interval');
  independentComparisons.push({logicalCaseId:id,family:s.family,cell:s.cell,baselineRatio:'0.92',currentRatio:ratio,
    pairedDeltas:deltas,distribution:distribution(complete),
    ...(complete.length ? {pairedDelta95Hoeffding:interval(complete,true)} : {})});
}
assert.equal(comparisonKeys.size,logicalCases * (ratios.length - 1));
const repeats = new Set();
for await (const row of rows(path.join(replay,'battles.jsonl.gz'))) {
  const key = `${row.caseId}/${row.ratio}/${row.pair}/${row.reverse}`;
  assert(!repeats.has(key),'duplicate repeat row'); repeats.add(key); assert(originalRepeats.has(key),'registered repeat');
  same(row,originalRepeats.get(key),'strict all-JSON repeat, no ignored fields or deleted events');
}
assert.equal(repeats.size,expectedReplayRows);
const timeouts = ratios.reduce((sum,ratio) => sum + (groups.get(`${ratio}/ALL/ALL`).outcomes.TIMEOUT || 0),0);

// Selection uses wins per attempted battle, not a mixture of unrelated families or mirrored trial counts.
const select = candidates => {
  const eligible = candidates.filter(c => c.timeouts === 0 && c.wins / c.battles >= .75 && c.wins / c.battles <= .90);
  eligible.sort((a,b) => Math.abs(a.wins * 5 - a.battles * 4) / a.battles
    - Math.abs(b.wins * 5 - b.battles * 4) / b.battles
    || Math.abs(Number(a.ratio) - .92) - Math.abs(Number(b.ratio) - .92) || Number(a.ratio) - Number(b.ratio));
  return eligible[0]?.ratio ?? null;
};
const mainByRatio = ratios.map(ratio => {
  const stats = groups.get(`${ratio}/FAMILY/A_MAIN`), main = summarize(stats);
  const paired = [...samples.values()].filter(s => s.family === 'A_MAIN')
    .flatMap(s => independentCases.get(`${s.caseId}/${ratio}`).pairScoresByIndex).filter(v => v !== null);
  assert.equal(main.attemptedBattles,36 * pairCount * 2);
  return {ratio,wins:stats.outcomes.WIN || 0,battles:main.attemptedBattles,timeouts:stats.outcomes.TIMEOUT || 0,
    winFraction:main.winFraction,independentPairIndices:36 * pairCount,pairedScore:distribution(paired),
    pairedScore95Hoeffding:interval(paired)};
});
let selection;
if (phase === 'SCREEN') {
  selection = {rule:'A_MAIN pooled wins/attempts in [0.75,0.90]; nearest 0.80; then nearest 0.92; then smaller ratio',
    selectedRatio:select(mainByRatio),screenRunPath:path.relative(process.cwd(),path.resolve(run)),
    screenProtocolSha256:hash(path.join(run,'protocol.json')),screenRawSha256:hash(path.join(run,'battles.jsonl.gz')),
    screenSummarySha256:hash(path.join(run,'summary.json')),mainByRatio};
} else {
  selection = protocol.selection;
  assert(selection && selection.selectedRatio === ratios[1]);
  assert.equal(hash(protocol.selectionJsonPath),protocol.selectionJsonSha256,'selection frozen before confirmation');
  same(JSON.parse(fs.readFileSync(protocol.selectionJsonPath)),selection,'complete selection is embedded without field changes');
  const screen = selection.screenRunPath;
  for (const [name,field] of [['protocol.json','screenProtocolSha256'],['battles.jsonl.gz','screenRawSha256'],['summary.json','screenSummarySha256']])
    assert.equal(hash(path.join(screen,name)),selection[field],`selection binds screening ${name}`);
  const screenProtocol = JSON.parse(fs.readFileSync(path.join(screen,'protocol.json')));
  const screenSummary = JSON.parse(fs.readFileSync(path.join(screen,'summary.json')));
  assert.equal(screenProtocol.phase,'SCREEN'); same(screenProtocol.ratios,fixedRatios,'all screened ratios disclosed');
  assert.equal(screenProtocol.generationRoot,20_261_203_000_000); assert.equal(screenProtocol.battleRoot,30_261_203_000_000);
  assert.equal(screenProtocol.attemptedBattles,20160); assert.equal(screenProtocol.logicalCaseCount,72);
  assert.equal(screenSummary.rawSha256,selection.screenRawSha256);
  assert.equal(screenSummary.protocolSha256,selection.screenProtocolSha256);
  const screenedSamples = new Map(screenProtocol.cases.map(c => [c.sample.caseId,c.sample]));
  same([...screenedSamples.values()].map(matrixKey).sort(),expectedMatrix,'screening uses the same exact matrix');
  for (const [id,s] of samples) {
    const prior = screenedSamples.get(id); assert(prior);
    same(prior.members,s.members,'confirmation uses unchanged input equipment and role assignments');
    same(prior.cell,s.cell,'confirmation uses unchanged cells'); assert.equal(prior.pairs,20);
  }
  const screenedCounts = new Map(fixedRatios.map(ratio => [ratio,{ratio,wins:0,battles:0,timeouts:0}]));
  const screenKeys = new Set(); let screenRows = 0;
  for await (const row of rows(path.join(screen,'battles.jsonl.gz'))) {
    ++screenRows; const s = screenedSamples.get(row.caseId); assert(s && fixedRatios.includes(row.ratio));
    const key = `${row.caseId}/${row.ratio}/${row.pair}/${row.reverse}`; assert(!screenKeys.has(key)); screenKeys.add(key);
    assert.equal(row.phase,'SCREEN'); assert.equal(typeof row.reverse,'boolean');
    assert(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < 20);
    assert.equal(row.generationSeed,20_261_203_000_000 + row.caseId * 1000 + row.pair);
    assert.equal(row.battleSeed,30_261_203_000_000 + row.caseId * 1000 + row.pair);
    if (s.family === 'A_MAIN') {
      const c = screenedCounts.get(row.ratio); ++c.battles; c.wins += Number(row.outcome === 'WIN');
      c.timeouts += Number(row.outcome === 'TIMEOUT');
    }
  }
  assert.equal(screenRows,20160); assert.equal(screenKeys.size,20160);
  for (const c of screenedCounts.values()) assert.equal(c.battles,1440);
  assert.equal(select([...screenedCounts.values()]),ratios[1],'independent raw-data selection, no confirmation reuse');
  for (const entry of selection.mainByRatio) {
    const c = screenedCounts.get(entry.ratio); assert(c);
    assert.equal(entry.wins,c.wins); assert.equal(entry.battles,c.battles); assert.equal(entry.timeouts,c.timeouts);
    near(entry.winFraction,c.wins / c.battles,'screening candidates not hidden or altered');
  }
  same(selection.mainByRatio.map(c => c.ratio),fixedRatios,'every candidate disclosed in fixed order');
}
const selectedMain = phase === 'CONFIRM' ? mainByRatio.find(c => c.ratio === ratios[1]) : null;
const confirmsPointTarget = selectedMain ? selectedMain.winFraction >= .75 && selectedMain.winFraction <= .90 : null;
const report = {purpose:'INDEPENDENT_TECHNICAL_VALIDATION_AND_PRESPECIFIED_MAIN_POINT_TARGET',phase,
  passesTechnicalChecks:timeouts === 0,confirmsMainPointTarget:confirmsPointTarget,
  logicalCases,ratioCases:logicalCases * ratios.length,battles:rowCount,
  independentLogicalPairIndices:logicalCases * pairCount,mirroredRatioPairs:logicalCases * pairCount * ratios.length,
  crossRatioPlayerContexts,
  exactAllJsonReplayBattles:repeats.size,fullOriginalLogsRecounted:fullLogsChecked,timeouts,
  checkedSourceFiles:Object.keys(protocol.publicSourceSha256).length,
  protocolSha256:hash(path.join(run,'protocol.json')),rawSha256:hash(path.join(run,'battles.jsonl.gz')),
  summarySha256:hash(path.join(run,'summary.json')),replaySha256:hash(path.join(replay,'battles.jsonl.gz')),
  originalSourceHash,candidateArchiveSha256:archiveMetadata.archiveSha256,
  mainByRatio,selection,
  caveat:'75–90% is an aggregate point target for fixed A_MAIN N3–5, not every cell or confidence interval. '
    + 'Mirrors, cross-ratio outcomes and repetitions are dependent. C_DIRECTIONS and D_DEFAULTS are separate diagnostics. '
    + 'Role exposure is not isolated causal role strength. Service routes and historical compatibility are tested separately.',
  aggregates:Object.fromEntries([...groups].map(([key,value]) => [key,summarize(value)])),
  cases:Object.fromEntries(independentCases),comparisons:independentComparisons};
fs.writeFileSync(output,JSON.stringify(report,null,2) + '\n',{flag:'wx'});
console.log(JSON.stringify({phase,logicalCases,ratioCases:logicalCases * ratios.length,battles:rowCount,
  exactAllJsonReplayBattles:repeats.size,passesTechnicalChecks:report.passesTechnicalChecks,
  confirmsMainPointTarget:confirmsPointTarget,timeouts,mainByRatio,selectedRatio:selection.selectedRatio}));
if (timeouts > 0) process.exitCode = 2;

