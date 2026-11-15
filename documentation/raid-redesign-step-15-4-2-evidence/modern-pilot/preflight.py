#!/usr/bin/env python3
"""Saved construction/seed/phase checks before any causal-pilot battle."""
import collections,hashlib,json,pathlib,sys
r=pathlib.Path(sys.argv[1]);m=json.loads((r/'manifest.json').read_text());cs=json.loads((r/'inputs.json').read_text());ss=json.loads((r/'seeds.json').read_text());f=json.loads((r/'fixture-manifest.json').read_text());prepared=json.loads((r/'prepared-pair0.json').read_text())
assert (m['conditions'],m['pairs'],m['battles'],m['combinedApprovedBattles'])==(84,30,5040,5520)
assert len(cs)==len(prepared)==84 and len(ss)==120
assert [c['id'] for c in cs]==list(range(1,85))
assert collections.Counter(c['phase']['id'] for c in cs)==dict([(f'C{i}',8) for i in range(10)]+[('C10',4)])
phases={p['id']:p for p in m['phases']}
for a,b,field in [('C0','C1','gear'),('C1','C2','force'),('C2','C3','playerSkills'),('C3','C4','npcSkills'),('C4','C5','tempo'),('C5','C6','slingerRange'),('C6','C7','allLog4'),('C4','C8','slingerRange'),('C8','C6','tempo')]:
 assert {k for k in phases[a] if phases[a][k]!=phases[b][k]}=={'id',field},(a,b)
assert m['bridge']['status']=='PASS' and m['bridge']['head32OracleFullTraitsAndRngTail']==24
assert m['bridge']['oldWeakHP']==720 and m['bridge']['newWeakHP']==840
assert {(s['groupId'],s['pair']) for s in ss}=={(g,p) for g in (101,102,251,252) for p in range(30)}
for s in ss:
 for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace'])]:
  assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/{s['groupId']}/{s['pair']}".encode()).digest()[:6],'big')
for field in ('generationSeed','battleSeed'):assert len({s[field] for s in ss})==120
spec={(x['scenarioId'],x['gear']):x['members'] for x in f['fixtureSpecs']}
for gear in ('OLD','V5'):
 assert spec[2,gear][:-1]==spec[1,gear] and spec[4,gear][:-1]==spec[3,gear]
 for sid in range(1,5):
  for member in spec[sid,gear]:
   for item in member['items']:
    assert item['enhanceLevel']==(0 if member['build']=='WEAK_WITH_DEFAULTS' else 6 if sid>=3 else 3)
    assert item['modifier'] not in ('GUARD','ACCUMULATION','PENETRATION','TEMPO_BREAK')
 assert spec[1,gear][0]['items'][1]['rarity']=='RARE' and spec[1,gear][0]['items'][1]['modifier'] is None
 mage=spec[1,gear][2]
 assert mage['position']=='MID' and mage['tactic']=='EXPLOIT_WEAKNESS'
 assert mage['items'][0]==dict(code='staff',rarity='RARE',enhanceLevel=3,modifier='DOUBLE_ATTACK')
 assert mage['items'][1]==dict(code='orb',rarity='COMMON',enhanceLevel=3,modifier=None)
for c,p in zip(cs,prepared):
 assert c['id']==p['caseId'] and c['phase']['id']==p['phase'] and c['level']==p['level'] and c['scenarioId']==p['scenarioId']
 assert c['level'] in (10,25) and c['groupId']==c['level']*10+(2 if c['scenarioId'] in (3,4) else 1)
 assert len(p['enemies'])==max(2,len(c['members'])) and len(p['players'])==len(c['members'])
 ids=[x['id'] for x in p['players']+p['enemies']];assert len(set(ids))==len(ids)
 assert all(x['hp']>0 for x in p['players']+p['enemies'])
 for i,x in enumerate(p['enemies']):
  assert x['range']==(1 if i<len(p['enemies'])//2 else c['phase']['slingerRange'])
  assert len(x['skills'])==1 and x['skills'][0]['points']==4 and x['skills'][0]['formulaVersion']==c['phase']['npcSkills']
 if c['scenarioId']==4:assert p['players'][-1]['hp']==(720 if c['phase']['gear']=='OLD' else 840)
out=dict(status='PASS',conditions=84,modernBattles=5040,expectedHistoricalBattles=480,pairs=30,seedRecords=120,bridge=m['bridge'],
         scope='Construction only before outcomes; phasesC0..C7onefactor, C4/C5/C6/C8tempo-range2x2, C9/C10aggregate references; no runtime balancing claim',
         files={n:hashlib.sha256((r/n).read_bytes()).hexdigest() for n in ('manifest.json','inputs.json','seeds.json','fixture-manifest.json','prepared-pair0.json')})
(r/'preflight-validation.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
