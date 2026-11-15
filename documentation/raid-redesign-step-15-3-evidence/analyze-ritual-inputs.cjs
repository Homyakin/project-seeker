const fs = require('node:fs');
const zlib = require('node:zlib');
const readline = require('node:readline');
const wanted = new Set([294,306,318]);
const rarities = ['COMMON','UNCOMMON','RARE','EPIC','LEGENDARY'];
const prices = {COMMON:50,UNCOMMON:100,RARE:200,EPIC:350,LEGENDARY:500};
function compactMember(m) {
  const items = m.combatItems.map(i => ({code:i.object.code, slots:i.object.slots, rarity:i.rarity,
    enhanceLevel:i.enhanceLevel, modifier:i.modifier?.activeEnum || null,
    skillPoints: i.modifier ? rarities.indexOf(i.rarity) * i.object.slots.length : 0,
    attacks:i.object.attacks, defense:i.object.defense}));
  let nominalGold = 0;
  for (const i of items) {
    nominalGold += prices.COMMON * 2 * i.slots.length;
    for (let r=1;r<=rarities.indexOf(i.rarity);r++) nominalGold += Math.trunc(prices[rarities[r]]*1.5);
  }
  return {build:m.build,position:m.position,tactic:m.tactic,items,nominalGold};
}
(async()=>{
  const input = fs.createReadStream(process.argv[2]).pipe(zlib.createGunzip());
  const lines = readline.createInterface({input,crlfDelay:Infinity});
  const out = [];
  for await(const line of lines){
    if(!line)continue;
    const r=JSON.parse(line);
    if(!wanted.has(r.caseId))continue;
    const members=r.members.map(compactMember);
    out.push({caseId:r.caseId,cell:r.cell,guardian:members[0],ritual:members[1],
      guardianActualSkills:r.playerBasis[0].skills,ritualActualSkills:r.playerBasis[1].skills,
      allRestIdentical:members.slice(1).every(m=>JSON.stringify(m)===JSON.stringify(members[1])),
      nominalTeamGold:members.reduce((s,m)=>s+m.nominalGold,0),
      nominalScope:'Common bases and one rarity chain; excludes desired modifier acquisition, storm shards, enhancement gold/time/risk'});
    wanted.delete(r.caseId);
    if(!wanted.size){lines.close();input.destroy();break;}
  }
  if(wanted.size)throw new Error('Missing cases '+[...wanted]);
  console.log(JSON.stringify(out,null,2));
})().catch(e=>{console.error(e);process.exitCode=1;});
