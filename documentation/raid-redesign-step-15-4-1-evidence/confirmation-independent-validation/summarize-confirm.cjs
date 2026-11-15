'use strict';
// Pure postprocessing of independent validation, no battle execution.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
const file = process.argv[2] || path.join(__dirname, 'validation.json');
const input = JSON.parse(fs.readFileSync(file, 'utf8'));
if (input.status !== 'PASS' || input.rows !== 24000) throw Error('Expected independently validated 24000 stream');
const state = (id, model) => input.states.find(s => s.id === id && s.model === model);
function observed(s) {
  return s.recordedMemberContributions.map(x => ({ build: x.build,
    meanTurns: x.turns / s.rows, meanRecordedDamage: (x.normalDamage + x.criticalDamage + x.skillDamage) / s.rows,
    diedFraction: x.died / s.rows, zeroTurnsFraction: x.zeroTurns / s.rows }));
}
const comparisons = [];
for (const model of ['SAME', 'CROSS']) for (const [base, added, label] of [[9,10,'ORIGINAL_MAIN_N3_TO_N4'],[11,12,'RITUAL_N1_TO_N2_WEAK']]) {
  const a=state(base,model), b=state(added,model), x=a.representative,y=b.representative;
  comparisons.push({ model,label,baseline:x,added:y,
    playerHRatio:y.playerH/x.playerH,playerDRatio:y.playerD/x.playerD,
    targetHRatio:y.targetH/x.targetH,targetDRatio:y.targetD/x.targetD,
    npcCountBase:x.guardian.count+x.slinger.count,npcCountAdded:y.guardian.count+y.slinger.count,
    guardianTotalHPRatio:y.guardian.count*y.guardian.hp/(x.guardian.count*x.guardian.hp),
    slingerTotalHPRatio:y.slinger.count*y.slinger.hp/(x.slinger.count*x.slinger.hp),
    winrateBaseline:a.winrate,winrateAdded:b.winrate,delta:b.winrate-a.winrate,
    simultaneousAppendInterval:input.families.append.results.find(r=>r.model===model&&r.first===base&&r.second===added).simultaneousInterval,
    observedBaseline:observed(a),observedAdded:observed(b) });
}
const families=Object.fromEntries(Object.entries(input.families).map(([name,f])=>[name,{...f,results:f.results.map(({pairDifferences,...r})=>r)}]));
const output={purpose:'INDEPENDENT_NUMERIC_DECOMPOSITION_OF_COMPLETE_FRESH_CONFIRMATION_NO_NEW_BATTLES',
  validationSha256:crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex'),
  validatorSha256:input.validatorSha256,rawGzipSha256:input.rawGzipSha256,rawPlainSha256:input.rawPlainSha256,
  comparisons,families,limitations:[
    'Damage/turn contributions are averages of saved engine statistics; no independent event-log damage replay is claimed.',
    'Both target sums and changed role counts/positions form a package; these comparisons do not identify a single causal parameter.',
    'Two separate12-comparison families each have95 coverage, not joint95 across both families.',
    'No universal scalar impossibility,50percent target,acceptance of weak-player policy or production switch follows.']};
process.stdout.write(JSON.stringify(output,null,2)+'\n');

