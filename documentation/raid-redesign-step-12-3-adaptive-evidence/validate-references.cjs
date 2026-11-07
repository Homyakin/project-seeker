// Node.js 24. Compare newly exported references with the saved battle inputs, not win-rate targets.
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const zlib = require('node:zlib');
const crypto = require('node:crypto');

const directory = process.argv[2];
assert.ok(directory, 'Usage: node validate-references.cjs <extracted-audit> [reference-json] [previous-reference-json]');
const referencePath = process.argv[3] || path.join(__dirname, 'current-reference-teams.json');
const previousPath = process.argv[4] || path.join(__dirname, '..',
  'raid-redesign-step-12-3-corrections-evidence', 'reference-teams.json');
const current = JSON.parse(fs.readFileSync(referencePath));
const previous = JSON.parse(fs.readFileSync(previousPath));
// These two JSON arrays represent Java sets; iteration order may differ between JVM launches.
function normalizeSets(value, key) {
  if (Array.isArray(value)) {
    const result = value.map(entry => normalizeSets(entry));
    return key === 'slots' || key === 'availableOnSlots' ? result.sort() : result;
  }
  return value && typeof value === 'object'
    ? Object.fromEntries(Object.entries(value).map(([name, entry]) => [name, normalizeSets(entry, name)])) : value;
}
const cases = JSON.parse(zlib.gunzipSync(fs.readFileSync(path.join(directory, 'confirmation-cases.json.gz'))));
const byCode = new Map(cases.map(value => [value.case.code, value]));
assert.equal(current.combat, 'SCALING_SKILLS_V3');
assert.equal(current.adaptiveCounterplay.tempo, 'ACCELERATED_V1');
assert.equal(current.teams.length, 35);
assert.deepEqual(normalizeSets(current.teams), normalizeSets(previous.teams), 'Existing 35 reference definitions changed');
assert.equal(current.adaptiveCounterplay.cases.length, 16);
const variants = {
  ORDINARY_BLEEDING: 'BASE_BLEEDING',
  SIX_UNCOMMON: 'BASE_SIX_UNCOMMON',
  HEAVY_BLEEDING: 'HEAVY_BLEEDING',
  HEAVY_DOUBLE_BLEEDING: 'HEAVY_DOUBLE_BLEED',
};
const rarityPoints = { COMMON: 0, UNCOMMON: 1, RARE: 2, EPIC: 3, LEGENDARY: 4 };
const seen = new Set();
let participants = 0;
let items = 0;
const grow = (base, level) => base + Math.floor(level * base / 60 + 0.5);
function member(value) {
  return {
    catalog: value.catalog,
    position: value.position,
    tactic: value.tactic,
    items: value.items.map(item => {
      const object = item.object;
      const level = item.enhanceLevel;
      items++;
      return {
        code: object.code, rarity: item.rarity, level,
        skill: item.modifier?.activeEnum || 'NONE',
        points: item.modifier ? rarityPoints[item.rarity] * object.slots.length : 0,
        health: grow(object.health, level), speed: object.speed,
        attacks: object.attacks.map(attack => ({ ...attack, attack: grow(attack.attack, level) })),
        defense: object.defense ? {
          type: object.defense.defenseType, value: grow(object.defense.defense, level),
        } : null,
      };
    }),
  };
}
function recorded(value) {
  return { catalog: value.catalog, position: value.build.position,
    tactic: value.build.tactic, items: value.items };
}
for (const pair of current.adaptiveCounterplay.cases) {
  const match = /^ADAPTIVE_(.+)_N(7|15)_L(0|6)$/.exec(pair.code);
  assert.ok(match && variants[match[1]], `Unexpected reference: ${pair.code}`);
  const size = Number(match[2]);
  const level = Number(match[3]);
  const code = `RESPONSE_${variants[match[1]]}_COMMAND_LEATHER_N${size}_L${level}`;
  assert.ok(!seen.has(code));
  seen.add(code);
  const saved = byCode.get(code);
  assert.ok(saved, `No confirmed observations for ${code}`);
  assert.equal(pair.size, size);
  assert.equal(pair.enhanceLevel, level);
  assert.equal(pair.raiderNominalGold, saved.leftNominalGold);
  assert.equal(pair.responseNominalGold, saved.rightNominalGold);
  assert.equal(pair.raiderNominalGold, pair.responseNominalGold);
  for (const [side, savedSide] of [['raiders', 'leftEquipment'], ['response', 'rightEquipment']]) {
    assert.equal(pair[side].length, size);
    assert.deepEqual(pair[side].map(member), saved[savedSide].map(recorded), `${code}: ${side}`);
    participants += size;
  }
}
assert.deepEqual([...seen].sort(), [...byCode.keys()].filter(code => code.startsWith('RESPONSE_')).sort());
console.log(JSON.stringify({
  existingReferenceTeamsUnchanged: 35, confirmedAdaptivePairsMatched: seen.size,
  comparisonNormalization: 'Only Java set arrays slots/availableOnSlots are sorted; team and item order are preserved.',
  participantDefinitionsMatched: participants, itemDefinitionsMatched: items,
  checked: ['catalog', 'positions', 'tactics', 'item order and codes', 'rarities', 'enhance levels',
    'modifiers', 'skill points', 'health', 'speed', 'attacks and ranges', 'defense', 'nominal prices'],
  scope: 'Exact reference inputs matched; no new battles, database checks or global acceptance.',
  referenceSha256: crypto.createHash('sha256').update(fs.readFileSync(referencePath)).digest('hex'),
}, null, 2));
