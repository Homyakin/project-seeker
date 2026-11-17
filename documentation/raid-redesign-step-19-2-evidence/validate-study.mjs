// Independent reader of saved evidence. Does not launch Java, a simulator or a database.
// From the repository root: node validate-study.mjs RUN REPLAY NEW_REPORT
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
const evidence = path.dirname(run);
const protocol = JSON.parse(fs.readFileSync(path.join(run, 'protocol.json')));
const summary = JSON.parse(fs.readFileSync(path.join(run, 'summary.json')));
const templates = ['ASH_WIND', 'SHADOW_NEEDLES', 'IRON_PHALANX', 'CRYSTAL_STORM'];
const tiers = ['BEGINNER', 'MID', 'STRONG'];
const roles = ['GUARDIAN', 'BRUISER', 'SKIRMISHER', 'ASSASSIN', 'RANGER_PHYSICAL',
  'RANGER_MAGICAL', 'BREAKER_PHYSICAL', 'BREAKER_MAGICAL', 'TACTICIAN_PHYSICAL', 'TACTICIAN_MAGICAL'];
const versions = ['BASELINE', 'CURRENT'];
const skillCodes = ['COUNTER_ATTACK','THORNS','DOUBLE_ATTACK','BERSERK','HIT_AND_RUN','BLEEDING','KNOCKBACK',
  'SELF_HEAL','PRECISE_STRIKE','RETREAT','FEINT','GUARD','PENETRATION','ACCUMULATION','TEMPO_BREAK'];
const generationRoot = 20_261_129_000_000, battleRoot = 30_261_129_000_000;
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
// Read ustar archives without executing a shell or trusting extraction paths.
const tarEntries = file => {
  const data = zlib.gunzipSync(fs.readFileSync(file)), entries = new Map();
  for (let offset = 0; offset + 512 <= data.length;) {
    const header = data.subarray(offset, offset + 512); offset += 512;
    if (header.every(byte => byte === 0)) break;
    const str = (start, length) => header.subarray(start, start + length).toString().replace(/\0.*$/s, '');
    const name = [str(345,155), str(0,100)].filter(Boolean).join('/').replace(/^\.\//, '');
    const size = parseInt(str(124,12).trim() || '0', 8), kind = str(156,1);
    assert(Number.isInteger(size) && size >= 0 && offset + size <= data.length, 'valid tar size');
    assert(!name.startsWith('/') && !name.split('/').includes('..'), 'safe tar member name');
    if (kind === '' || kind === '0') { assert(!entries.has(name)); entries.set(name, data.subarray(offset, offset + size)); }
    offset += Math.ceil(size / 512) * 512;
  }
  return entries;
};
const sourceArchiveFile = path.join(evidence, 'baseline-sources.tar.gz');
const classArchiveFile = path.join(evidence, 'baseline-classes-v2.tar.gz');
const originalSourceHash = '5467fb2c2b9cb264f12cf8780b56df3e8b580a8d5c25802638c3b69c1bd5b929';
const baselineClassHash = '5684e14f91f33696f84e3cc10b01ec38f7c877f20f02b1ecd363157e7dfc4d33';
const sourceArchiveHash = '7c8326904d3467e1054001d6d4adc17bd1fc3e6c07a3d7d929ecd3a6861aa9b3';
const classArchiveHash = '231753ee41f1b1a355e733ab5cbde5d4a17d99a94b6fabe037e8b454fc688a29';
assert.equal(hash(sourceArchiveFile), sourceArchiveHash, 'immutable original source archive');
assert.equal(hash(classArchiveFile), classArchiveHash, 'fresh original class archive');
const sourceEntries = tarEntries(sourceArchiveFile), classEntries = tarEntries(classArchiveFile);
const generatorSource = 'game/src/main/java/ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.java';
const generatorClass = 'ru/homyakin/seeker/game/event/anomaly/generator/AnomalySafePveGenerator.class';
const generatorSyntheticClass = generatorClass.slice(0,-6) + '$1.class';
const baselineClassSha256ByPath = {[generatorClass]:baselineClassHash,
  [generatorSyntheticClass]:'30952aff62ca91916671351a54e9d84051172e9531dd74efa686b36d3604ec56'};
const childFirstClasses = Object.keys(baselineClassSha256ByPath).map(name => name.replace(/\.class$/,'').replaceAll('/','.'));
assert.equal(sourceEntries.size, 4); assert.equal(classEntries.size, 2);
assert.equal(sha(sourceEntries.get(generatorSource)), originalSourceHash, 'original generator source bound to archive');
same([...classEntries.keys()].sort(),Object.keys(baselineClassSha256ByPath).sort(),'complete original generator class family');
for (const [name,expected] of Object.entries(baselineClassSha256ByPath))
  assert.equal(sha(classEntries.get(name)),expected,'original generator and synthetic switch class bound to archive');
assert.equal(hash(path.join(evidence,'baseline-classes.tar.gz')),
  'e6d4aad88915251fef70f2f68b7cc9cce9004895721555772e8f961fb85bd51c',
  'failed one-class setup is retained, not silently overwritten');

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
const npcInfo = (template, index, count, version) => {
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
  const range = version === 'BASELINE' ? type === 'MAGICAL' || position === 'BACK' ? 2 : 1
    : position === 'BACK' ? 3 : position === 'MID' || type === 'MAGICAL' ? 2 : 1;
  const actualPosition = ['FRONT','MID','BACK'][Math.min(['FRONT','MID','BACK'].indexOf(position),range - 1)];
  return {position,skill,type,armor,range,actualPosition,threat:{FRONT:30,MID:20,BACK:12}[position]};
};

// Domain checks for the independent formula, including the deliberately preserved old quirks.
const checkInput = {health:100,attackAtOne:10,critChance:0,dodgeChance:0,critMultiplier:1.2,speed:1000,defenses:{}};
assert.equal(legacyPower(checkInput),1000);
assert.equal(legacyPower({...checkInput,dodgeChance:90}),1000);
assert.equal(legacyPower({...checkInput,speed:2000}),2000);
assert.equal(legacyPower({...checkInput,attackAtOne:0}),100);

assert.equal(protocol.caseCount,272); assert.equal(protocol.logicalCaseCount,136);
assert.equal(protocol.attemptedBattles,7840); assert.equal(summary.caseCount,272);
assert.equal(summary.logicalCaseCount,136); assert.equal(summary.attemptedBattles,7840);
assert.equal(protocol.generationRoot,generationRoot); assert.equal(protocol.battleRoot,battleRoot);
assert.equal(protocol.metric,'LEGACY_SUM_V1'); assert.equal(protocol.safePowerRatio,.92);
assert.equal(protocol.catalog,'SCALING_V5'); assert.equal(protocol.playerSkills,'SCALING_SKILLS_V3');
assert.equal(protocol.npcSkills,'SCALING_SKILLS_V3'); assert.equal(protocol.tempo,'ACCELERATED_V1');
assert.equal(protocol.tempoDamageMultiplier,1.75); assert.equal(protocol.roundLimit,2000);
same(protocol.versions,versions,'exact old/current versions');
assert.equal(protocol.independentLogicalPairs,1960); assert.equal(summary.independentLogicalPairs,1960);
assert.equal(protocol.preBattleGenerationComparisons,1960,'all planned generators compared before outcomes');
assert.equal(protocol.baselineClassSha256,baselineClassHash);
assert.equal(protocol.baselineClassArchiveSha256,classArchiveHash);
assert.equal(protocol.baselineSourceArchiveSha256,sourceArchiveHash);
assert.equal(protocol.baselineClassArchivePath,path.relative(process.cwd(),path.resolve(classArchiveFile)));
assert.equal(protocol.baselineSourceArchivePath,path.relative(process.cwd(),path.resolve(sourceArchiveFile)));
same(protocol.baselineClassSha256ByPath,baselineClassSha256ByPath,'both original child-first classes');
same([...protocol.childFirstClasses].sort(),[...childFirstClasses].sort(),'class-loader boundary includes synthetic class');
same(Object.keys(protocol.currentClassSha256ByPath).sort(),Object.keys(baselineClassSha256ByPath).sort(),
  'complete current generator class family');
for (const [name,expected] of Object.entries(protocol.currentClassSha256ByPath))
  assert.equal(hash('game/target/classes/' + name),expected,'frozen compiled current generator class family');
assert.equal(protocol.currentClassSha256,hash('game/target/classes/' + generatorClass),'frozen compiled current generator');
const baselineMetadataFile = path.join(evidence,'baseline-metadata.json');
const baselineMetadata = JSON.parse(fs.readFileSync(baselineMetadataFile));
assert.equal(baselineMetadata.baselineSource,generatorSource);
assert.equal(baselineMetadata.baselineSourceSha256,originalSourceHash);
assert.equal(baselineMetadata.baselineClass,generatorClass);
assert.equal(baselineMetadata.baselineClassSha256,baselineClassHash);
assert.equal(baselineMetadata.sourceArchiveSha256,sourceArchiveHash);
assert.equal(baselineMetadata.classArchiveSha256,classArchiveHash);
assert.equal(baselineMetadata.classArchive,protocol.baselineClassArchivePath);
assert.equal(baselineMetadata.sourceArchive,protocol.baselineSourceArchivePath);
same([...baselineMetadata.isolatedClasses].sort(),[...childFirstClasses].sort(),'metadata states two isolated classes');
same(baselineMetadata.baselineClassFamilySha256,Object.fromEntries(Object.entries(baselineClassSha256ByPath)
  .map(([name,value]) => [name.replace(/\.class$/,'').replaceAll('/','.'),value])),
  'metadata binds both compiled classes to the original source compilation');
for (const [file,expected] of Object.entries(protocol.publicSourceSha256)) assert.equal(hash(file),expected,file);
for (const required of ['item_objects.toml','default_items.toml','item_modifiers.toml'])
  assert(Object.hasOwn(protocol.publicSourceSha256,catalogRoot + required),'real V5 resources are frozen inputs');
assert(Object.hasOwn(protocol.publicSourceSha256,path.relative(process.cwd(),path.resolve(evidence,'validate-study.mjs'))),
  'independent validator frozen before battles');
assert(Object.hasOwn(protocol.publicSourceSha256,path.relative(process.cwd(),path.resolve(baselineMetadataFile))),
  'baseline source/compiler/class binding is frozen');
assert.equal(summary.protocolSha256,hash(path.join(run,'protocol.json')));
assert.equal(summary.rawSha256,hash(path.join(run,'battles.jsonl.gz')));

// Matrix and raw-data checks follow below; all acceptance here is technical, not a win-rate target.
const samples = new Map(protocol.cases.map(entry => [entry.sample.caseId,entry.sample]));
assert.equal(protocol.cases.length,136); assert.equal(samples.size,136);
const expectedMatrix = [];
const matrixKey = s => [s.family,s.cell.encounter,s.cell.style,s.cell.tier,s.cell.size].join('|');
const expect = (family,template,style,tier,size) => expectedMatrix.push([family,template,style,tier,size].join('|'));
for (const template of templates) {
  for (const tier of tiers) for (const n of [1,2,3,4,5]) expect('A_MAIN',template,'MAIN',tier,n);
  for (const role of roles) expect('B_SOLO',template,role,'MID',1);
  for (const style of ['RITUAL','COMMAND_CUIRASS']) for (const n of [1,3,5])
    expect('C_DIRECTIONS',template,style,'MID',n);
  for (const n of [1,3,5]) expect('D_DEFAULTS',template,'FULL_DEFAULTS','BEGINNER',n);
}
same([...samples.values()].map(matrixKey).sort(),expectedMatrix.sort(),'complete independently specified matrix');
const familyCounts = {}, seedByCase = new Map();
for (const [index,entry] of protocol.cases.entries()) {
  const s = entry.sample; assert.equal(s.caseId,index + 1);
  assert.equal(s.cell.mode,'ANOMALY_SAFE'); assert.equal(s.cell.raidLevel,0);
  assert.equal(s.cell.effects,false); assert.equal(s.members.length,s.cell.size);
  assert.equal(s.pairs,s.family === 'A_MAIN' ? 20 : 10); assert.equal(entry.seeds.length,s.pairs);
  add(familyCounts,s.family); seedByCase.set(s.caseId,entry.seeds);
  for (const [pair,seed] of entry.seeds.entries()) {
    assert.equal(seed.pair,pair);
    assert(Number.isSafeInteger(seed.generationSeed) && seed.generationSeed >= generationRoot + s.caseId * 1000);
    assert.equal(seed.battleSeed,battleRoot + s.caseId * 1000 + pair);
    if (s.cell.size !== 2) {
      assert.equal(seed.generationSeed,generationRoot + s.caseId * 1000 + pair);
      assert.equal(seed.expectedEnemyCount,s.cell.size === 1 ? 2 : s.cell.size);
    } else {
      assert.equal(seed.expectedEnemyCount,pair < 10 ? 2 : 3,'fixed N2 strata before outcomes');
    }
  }
  assert.equal(new Set(entry.seeds.map(seed => seed.generationSeed)).size,s.pairs,'distinct generation seeds per cell');
}
same(familyCounts,{A_MAIN:60,B_SOLO:40,C_DIRECTIONS:24,D_DEFAULTS:12},'fixed family counts');
assert.equal([...samples.values()].reduce((sum,s) => sum + s.pairs * 4,0),7840);
const replayPairs = new Set(protocol.replayPairs.map(p => `${p.caseId}/${p.pair}`));
assert.equal(replayPairs.size,24,'24 fixed pairs, each in two versions and two orders');
const expectedReplayPairs = [];
for (const s of samples.values()) if (s.family === 'A_MAIN' && s.cell.tier === 'MID') {
  expectedReplayPairs.push(`${s.caseId}/0`); if (s.cell.size === 2) expectedReplayPairs.push(`${s.caseId}/10`);
}
same([...replayPairs].sort(),expectedReplayPairs.sort(),'exact predeclared repeat pair selection');
for (const key of replayPairs) {
  const [id,pair] = key.split('/').map(Number), s = samples.get(id); assert(s);
  assert(Number.isInteger(pair) && pair >= 0 && pair < s.pairs);
}
// Every template/size plus both N2 branches must be present in the preselected repeats.
for (const template of templates) for (const n of [1,2,3,4,5]) {
  const selected = [...replayPairs].map(key => key.split('/').map(Number)).filter(([id]) => {
    const s = samples.get(id); return s.cell.encounter === template && s.cell.size === n;
  });
  assert(selected.length >= (n === 2 ? 2 : 1),`replay covers ${template}/${n}`);
  if (n === 2) same(selected.map(([id,pair]) => seedByCase.get(id)[pair].expectedEnemyCount).sort(),[2,3],'both N2 repeat branches');
}

const fresh = () => ({outcomes:{},rounds:[],skills:{},skillsByKind:{},statuses:{},errors:[],metrics:{},
  byOrder:{},pairs:{},enemyCounts:{},byCount:{},enemyActions:{}});
const caseStats = new Map(), groups = new Map(), seen = new Set(), pairs = new Map(), coupled = new Map();
const originalRepeats = new Map();
let rowCount = 0, comparedGenerationContexts = 0, fullLogsChecked = 0;
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
const normalizedEnemy = enemy => Object.fromEntries(Object.entries(enemy).filter(([key]) => !['range','attacksByRange'].includes(key)));
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
  const s = samples.get(row.caseId); assert(s,'registered logical case');
  assert.equal(row.logicalCaseId,row.caseId); assert(versions.includes(row.version));
  assert.equal(row.family,s.family); same(row.cell,s.cell,'registered cell');
  assert.equal(typeof row.reverse,'boolean');
  assert(Number.isInteger(row.pair) && row.pair >= 0 && row.pair < s.pairs);
  const key = `${row.caseId}/${row.version}/${row.pair}/${row.reverse}`;
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
  const target = playerPower * .92;
  near(row.calibration.targetPower,target,'unchanged old-assessment target ratio 0.92');
  const independentlyCalibrated = independentSearch(target,s.cell.encounter,row.enemyBasis.length);
  compare(row.calibration,independentlyCalibrated,'independent bounded calibration including status/m/evaluations');
  const enemyPowers = [];
  for (const [index,enemy] of row.enemyBasis.entries()) {
    const info = npcInfo(s.cell.encounter,index,row.enemyBasis.length,row.version);
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
  invariant.enemyBasis = row.enemyBasis.map(normalizedEnemy);
  const couplingKey = `${row.caseId}/${row.pair}/${row.reverse}`;
  if (coupled.has(couplingKey)) {
    const previous = coupled.get(couplingKey); assert.notEqual(previous.version,row.version);
    same(previous.invariant,invariant,'only ranges change before battle; target, values, skills, IDs and RNG preserved');
    coupled.delete(couplingKey); ++comparedGenerationContexts;
  } else coupled.set(couplingKey,{version:row.version,invariant});
  const mirroredKey = `${row.caseId}/${row.version}/${row.pair}`;
  if (pairs.has(mirroredKey)) {
    const previous = pairs.get(mirroredKey); assert.notEqual(previous.reverse,row.reverse);
    same(previous.invariant,invariant,'mirrored generation invariants'); pairs.delete(mirroredKey);
  } else pairs.set(mirroredKey,{reverse:row.reverse,invariant});
  const caseKey = `${row.caseId}/${row.version}`, cs = caseStats.get(caseKey) || fresh(); caseStats.set(caseKey,cs);
  collect(cs,row); (cs.pairs[row.pair] ||= []).push(row.outcome);
  add(cs.byOrder[row.reverse ? 'PLAYERS_FIRST' : 'ENEMIES_FIRST'] ||= {},row.outcome);
  const dimensions = [['ALL','ALL'],['TEMPLATE',s.cell.encounter],['SIZE',s.cell.size],['PROFILE',s.cell.tier],
    ['FAMILY',s.family],['STYLE',s.cell.style],['FAMILY_TEMPLATE',`${s.family}/${s.cell.encounter}`],
    ['MAIN_TEMPLATE_SIZE',`${s.cell.encounter}/${s.cell.size}`],['N2_BRANCH',`${s.cell.encounter}/${row.enemyBasis.length}`]];
  for (const [dimension,value] of dimensions) {
    if (dimension === 'MAIN_TEMPLATE_SIZE' && s.family !== 'A_MAIN') continue;
    if (dimension === 'N2_BRANCH' && (s.family !== 'A_MAIN' || s.cell.size !== 2)) continue;
    const groupKey = `${row.version}/${dimension}/${value}`, stats = groups.get(groupKey) || fresh();
    groups.set(groupKey,stats); collect(stats,row);
  }
  if (replayPairs.has(`${row.caseId}/${row.pair}`)) {
    assert(row.actionLog && row.traceEvents,'preselected original rows retain every event');
    fullLogChecks(row);
    originalRepeats.set(key,row); ++fullLogsChecked;
  } else assert(!Object.hasOwn(row,'actionLog') && !Object.hasOwn(row,'traceEvents'),'full logs only for fixed repeats');
}
assert.equal(rowCount,7840); assert.equal(seen.size,7840); assert.equal(caseStats.size,272);
assert.equal(coupled.size,0); assert.equal(pairs.size,0); assert.equal(comparedGenerationContexts,3920);
assert.equal(originalRepeats.size,96); assert.equal(fullLogsChecked,96);
assert.equal(summary.cases.length,272); assert.equal(summary.comparisons.length,136);
const independentCases = new Map();
for (const result of summary.cases) {
  const key = `${result.caseId}/${result.version}`, s = samples.get(result.caseId), actual = caseStats.get(key);
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
assert.equal(independentCases.size,272);
const independentComparisons = [];
for (const result of summary.comparisons) {
  const id = result.logicalCaseId, s = samples.get(id); assert(s);
  assert(!independentComparisons.some(c => c.logicalCaseId === id),'one coupled comparison per logical case');
  assert.equal(result.family,s.family); same(result.cell,s.cell,'comparison context');
  const baseline = independentCases.get(`${id}/BASELINE`), current = independentCases.get(`${id}/CURRENT`);
  const deltas = baseline.pairScoresByIndex.map((before,i) => before === null || current.pairScoresByIndex[i] === null
    ? null : current.pairScoresByIndex[i] - before);
  const complete = deltas.filter(value => value !== null);
  same(result.pairedDeltas,deltas,'exact baseline/current paired deltas');
  compare(result.distribution,distribution(complete),'independent paired-delta distribution');
  if (complete.length) compare(result.pairedDelta95Hoeffding,interval(complete,true),'independent paired-delta interval');
  independentComparisons.push({logicalCaseId:id,family:s.family,cell:s.cell,pairedDeltas:deltas,
    distribution:distribution(complete),...(complete.length ? {pairedDelta95Hoeffding:interval(complete,true)} : {})});
}
assert.equal(independentComparisons.length,136);
const repeats = new Set();
for await (const row of rows(path.join(replay,'battles.jsonl.gz'))) {
  const key = `${row.caseId}/${row.version}/${row.pair}/${row.reverse}`;
  assert(!repeats.has(key),'duplicate repeat row'); repeats.add(key); assert(originalRepeats.has(key),'registered repeat');
  same(row,originalRepeats.get(key),'strict all-JSON repeat, no ignored fields or deleted events');
}
assert.equal(repeats.size,96);
const timeouts = versions.reduce((sum,version) => sum + (groups.get(`${version}/ALL/ALL`).outcomes.TIMEOUT || 0),0);
const conditionalN2 = Object.fromEntries(versions.map(version => [version,Object.fromEntries(templates.map(template => {
  const k2 = groups.get(`${version}/N2_BRANCH/${template}/2`), k3 = groups.get(`${version}/N2_BRANCH/${template}/3`);
  assert.equal(summarize(k2).attemptedBattles,60); assert.equal(summarize(k3).attemptedBattles,60);
  return [template,{K2:summarize(k2),K3:summarize(k3),equalBranchWinFraction:
    (summarize(k2).winFraction + summarize(k3).winFraction) / 2}];
}))]));
const report = {purpose:'INDEPENDENT_TECHNICAL_VALIDATION_NOT_BALANCE_ACCEPTANCE',passesTechnicalChecks:timeouts === 0,
  logicalCases:136,versionCases:272,battles:rowCount,independentLogicalPairIndices:1960,
  mirroredVersionPairs:3920,comparedGenerationContexts,exactAllJsonReplayBattles:repeats.size,
  fullOriginalLogsRecounted:fullLogsChecked,timeouts,checkedSourceFiles:Object.keys(protocol.publicSourceSha256).length,
  protocolSha256:hash(path.join(run,'protocol.json')),rawSha256:hash(path.join(run,'battles.jsonl.gz')),
  summarySha256:hash(path.join(run,'summary.json')),replaySha256:hash(path.join(replay,'battles.jsonl.gz')),
  baselineSourceSha256:originalSourceHash,baselineClassSha256:baselineClassHash,
  baselineClassSha256ByPath,currentClassSha256ByPath:protocol.currentClassSha256ByPath,childFirstClasses,
  baselineClassArchiveSha256:classArchiveHash,baselineSourceArchiveSha256:sourceArchiveHash,
  caveat:'No win-rate acceptance band. Mirrors and repeated runs are not additional independent trials. '
    + 'One shared simulator path is not proof of both actual service entry points. Historical compatibility is separate.',
  aggregates:Object.fromEntries([...groups].map(([key,stats]) => [key,summarize(stats)])),conditionalN2,
  cases:Object.fromEntries(independentCases),comparisons:independentComparisons};
fs.writeFileSync(output,JSON.stringify(report,null,2) + '\n',{flag:'wx'});
console.log(JSON.stringify({logicalCases:136,versionCases:272,battles:rowCount,exactAllJsonReplayBattles:repeats.size,
  passesTechnicalChecks:report.passesTechnicalChecks,timeouts,overall:Object.fromEntries(versions.map(version =>
    [version,summarize(groups.get(`${version}/ALL/ALL`))]))}));
if (timeouts > 0) process.exitCode = 2;
