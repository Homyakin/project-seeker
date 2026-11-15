const fs=require('node:fs'),path=require('node:path'),crypto=require('node:crypto');
const root=__dirname;
const sha=p=>crypto.createHash('sha256').update(fs.readFileSync(p)).digest('hex');
function files(dir){return fs.readdirSync(dir,{withFileTypes:true}).flatMap(e=>e.isDirectory()?files(path.join(dir,e.name)):[path.join(dir,e.name)]);}
const validation=JSON.parse(fs.readFileSync(path.join(root,'common-preparation-validation.json')));
if(validation.verdict!=='PASS'||validation.helperSHA256!==sha(path.join(root,'HistoricalCausalProbe.java')))throw Error('Preparation/helper changed');
const basis=JSON.parse(fs.readFileSync(path.join(root,'source-basis.json')));
for(const e of basis.checks)if(sha(path.join(basis.target,e.path))!==e.sha256)throw Error('Historical source changed: '+e.path);
const classFiles=files(path.join(root,'classes-6150')).sort().map(p=>({path:p,sha256:sha(p)}));
const sourceFiles=['HistoricalCausalProbe.java','HistoricalConstructionProbe.java','linkage-only/ru/homyakin/seeker/locale/LocalizationCoverage.java','source-basis.json','common-preparation-validation.json','preparation-frozen/manifest.json'].map(p=>({path:path.join(root,p),sha256:sha(path.join(root,p))}));
fs.writeFileSync(path.join(root,'runtime-freeze.json'),JSON.stringify({purpose:'BEFORE_FIRST_BATTLE_FROZEN_HISTORICAL_RUNTIME',classFiles,sourceFiles,sourceCommit:basis.commit,commonFixtureSHA256:validation.commonFixtureSHA256,executionLimit:480},null,2)+'\n');
console.log(JSON.stringify({frozen:true,classes:classFiles.length,sourceFiles:sourceFiles.length,executionLimit:480}));
