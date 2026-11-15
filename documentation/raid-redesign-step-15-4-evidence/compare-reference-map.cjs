const fs=require('node:fs');
const javaPath='/tmp/seeker-step15-4-bnorm-wide.bB1Bsl/preparation/manifest.json';
const nodePath=`${__dirname}/composition-correction.json`;
const java=JSON.parse(fs.readFileSync(javaPath,'utf8'));
const node=JSON.parse(fs.readFileSync(nodePath,'utf8'));
const rows=[];
for(let M=2;M<=30;M++){
  const a=java.fixedConcentrationCorrection.byCount[M];
  const b=node.factors.find(x=>x.M===M).r;
  rows.push({M,actualJavaFactory:a,pureNodeFactory:b,exact:Object.is(a,b),difference:a-b});
}
const checks={
  mReference:Object.is(java.fixedConcentrationCorrection.referenceMultiplier,node.constants.mReference),
  npc3Score:Object.is(java.fixedConcentrationCorrection.referenceNPC3Score,node.constants.B3Reference),
  C:Object.is(java.anchors.Cnew,node.constants.C),
  a:Object.is(java.anchors.screenA,node.constants.a),
  r2:Object.is(java.anchors.rNorm2,node.constants.r2)
};
console.log(JSON.stringify({purpose:'INDEPENDENT_READ_ONLY_FACTORY_ARITHMETIC_COMPARISON_NO_BATTLES',javaPath,nodePath,
  checks,rows,allExact:rows.every(x=>x.exact)&&Object.values(checks).every(Boolean),
  maxAbsoluteDifference:Math.max(...rows.map(x=>Math.abs(x.difference))),
  bridgeReports:{originalCurrentNPC:java.bridgeValidation,actualEffects:java.effectsBridgeValidation,
    allNPCCount2or3:java.N2N3UnnormalizedBBridge,
    midAnchorFactoryCharacteristicsExact:java.anchors.midAnchorFactoryCharacteristicsExact,
    soloAnchorFactoryCharacteristicsExact:java.anchors.soloAnchorFactoryCharacteristicsExact},
  limitations:['Checks saved Java factory manifest values against independent arithmetic; does not rerun Java or verify every raw battle',
    'Bridge status fields are reported from Java manifest, not independently recomputed by this script',
    'Arithmetic agreement is not evidence that outcomes are balanced']},null,2));
