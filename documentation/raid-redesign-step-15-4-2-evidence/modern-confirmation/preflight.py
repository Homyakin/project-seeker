#!/usr/bin/env python3
"""Fresh C9 confirmation freeze checks, before any outcome."""
import hashlib,json,pathlib,sys
r=pathlib.Path(sys.argv[1]);f=r/'frozen-preparation';m=json.loads((f/'manifest.json').read_text());cs=json.loads((f/'inputs.json').read_text());ss=json.loads((f/'seeds.json').read_text());policy=json.loads((r/'inference-policy.json').read_text());old=pathlib.Path('/tmp/seeker-step15-4-2-causal.1HOgPK/frozen-preparation')
assert hashlib.sha256((r/'inference-policy.json').read_bytes()).hexdigest()=='b4c8ca144c8b454e6a123405390ea985f2f041ed9669a77a441233a51e714ebf'
assert (m['conditions'],m['pairs'],m['battles'])==(2,500,2000) and len(cs)==2 and len(ss)==500
assert [c['scenarioId'] for c in cs]==[1,2] and all(c['phase']['id']=='C9' and c['level']==25 and c['groupId']==251 for c in cs)
assert m['generationNamespace']==policy['generationNamespace'] and m['battleNamespace']==policy['battleNamespace']
assert hashlib.sha256((f/'fixture-manifest.json').read_bytes()).hexdigest()==policy['fixtureManifestSHA256']
assert (f/'fixture-manifest.json').read_bytes()==(old/'fixture-manifest.json').read_bytes()
assert {(s['groupId'],s['pair']) for s in ss}=={(251,i) for i in range(500)}
for s in ss:
 for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace'])]:assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/251/{s['pair']}".encode()).digest()[:6],'big')
for field in ('generationSeed','battleSeed'):assert len({s[field] for s in ss})==500
op=json.loads((old/'prepared-pair0.json').read_text());cp=json.loads((f/'prepared-pair0.json').read_text())
for x in cp:
 prior=next(z for z in op if z['phase']=='C9' and z['scenarioId']==x['scenarioId'] and z['level']==25)
 assert x['calibration']==prior['calibration'] and x['diagnostics']==prior['diagnostics']
 for side in ('players','enemies'):
  for a,b in zip(x[side],prior[side]):assert {k:v for k,v in a.items() if k not in ('id','gauge')}=={k:v for k,v in b.items() if k not in ('id','gauge')}
receipt=dict(status='PASS',conditions=2,pairs=500,modernExecutions=2000,scope='Fresh C9 only, same calibration/traits except expected new UUID/gauges, exact sharedfixture and policy, no outcomes read',
             files={n:hashlib.sha256((f/n).read_bytes()).hexdigest() for n in ('manifest.json','fixture-manifest.json','inputs.json','seeds.json','prepared-pair0.json')})
(f/'preflight-validation.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
