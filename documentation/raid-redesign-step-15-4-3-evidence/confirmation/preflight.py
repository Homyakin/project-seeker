#!/usr/bin/env python3
"""Saved preparation and exact policy checks; no Java and no battles."""
import hashlib,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);r=root/'frozen-preparation'
load=lambda p:json.loads(p.read_text())
m=load(r/'manifest.json');cs=load(r/'inputs.json');ss=load(r/'seeds.json');ps=load(r/'prepared-pair0.json');policy=load(r/'confirmation-policy.json')
assert hashlib.sha256((r/'confirmation-policy.json').read_bytes()).hexdigest()=='3876690b6ceb542c1bf2a3112d2b6e194ae922c028f706bd2b08686c11e515cf'
pilot=load(pathlib.Path('/tmp/seeker-step15-4-3-heterogeneity.GwQy45/frozen-preparation/inputs.json'))
expected={c for a in policy['allocations'] for c in a['caseIds']}
assert len(cs)==34 and len(ps)==37 and len(ss)==11000
assert {c['id'] for c in cs}==expected and all(c==pilot[c['id']-1] for c in cs)
assert (m['conditions'],m['modelConditions'],m['battles'],m['seedRecords'])==(34,37,80000,11000)
alloc={(c,rule):a['pairs'] for a in policy['allocations'] for c in a['caseIds'] for rule in a['rules']}
assert sum(2*n for n in alloc.values())==80000
assert {(x['caseId'],rule):x['pairs'] for x in m['allocations'] for rule in x['rules']}==alloc
groups={int(k):v for k,v in policy['seedGroups'].items()}
assert {(s['groupId'],s['pair']) for s in ss}=={(g,p) for g,n in groups.items() for p in range(n)}
for s in ss:
 for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace']),('npcSeed',m['npcNamespace'])]:
  assert ns==policy[{'generationSeed':'generationNamespace','battleSeed':'battleNamespace','npcSeed':'npcNamespace'}[field]]
  assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/{s['groupId']}/{s['pair']}".encode()).digest()[:6],'big')
for field in ('generationSeed','battleSeed','npcSeed'):assert len({s[field] for s in ss})==11000
def close(a,b):assert math.isclose(a,b,rel_tol=5e-14,abs_tol=1e-8),(a,b)
def stable(values):
 s=0.
 for x in sorted(values):s+=x
 return s
seen=set();uniform=0
for x in ps:
 c=x['case'];key=(c['id'],x['rule']);assert key in alloc and key not in seen;seen.add(key)
 assert c==next(a for a in cs if a['id']==c['id']) and c['family']!='FIXED_NPC'
 vs=x['contributions'];e=x['playerEvaluation'];Q=stable(y['H'] for y in vs)*stable(y['D'] for y in vs);close(e['Q'],Q)
 for y in vs:close(y['p'],y['H']*y['D'])
 p=sorted(y['p'] for y in vs);B=sum((2*(len(p)-i)-1)*v for i,v in enumerate(p));T=max(k*k*p[-k] for k in range(1,len(p)+1));K=next(k for k in range(1,len(p)+1) if k*k*p[-k]==T)
 close(e['rawB'],B);close(e['rawT'],T);assert e['bestK']==K
 equal=all(y['H']==vs[0]['H'] and y['D']==vs[0]['D'] for y in vs);assert e['exactSoloOrIdentical']==equal
 F=Q if x['rule']=='BASELINE' or equal else Q/2+min(T,Q)/2;d=1 if Q==0 else F/Q
 close(e['F'],F);close(e['discount'],d)
 if equal:assert e['F']==e['Q'] and e['discount']==1;uniform+=1
 assert x['calibrationEvaluation']==e
 t=x['target'];cal=x['calibration'];M=max(2,len(x['players']));f=M//2;b=M-f
 assert (t['count'],t['frontCount'],t['backCount'])==(M,f,b)
 close(t['frontWeight'],M/(2*f));close(t['backWeight'],M/(2*b));close(t['groupScaling'],1+.05*math.log(M)/math.log(4));close(t['sizeCorrection'],.7+.3*(3/M)**2);close(t['levelPowerBonus'],1+((c['level']-10)*.06+.14));close(t['encounterCorrection'],.5900075417594784)
 close(t['baselineTarget'],Q*t['levelPowerBonus']*t['groupScaling']*t['encounterCorrection']*t['sizeCorrection']);close(t['requestedTarget'],t['baselineTarget']*d);close(cal['targetPower'],t['requestedTarget'])
 assert len(x['enemies'])==M
 ids=[a['id'] for a in x['players']+x['enemies']];assert len(set(ids))==len(ids)
assert seen==set(alloc)
twins=0
for variant in ('BASE','NORMAL_APPEND'):
 xs=[x for x in ps if x['case']['family']=='COUNTER_TWINS' and x['case']['variant']==variant];assert len(xs)==4
 for x in xs[1:]:assert x['enemies']==xs[0]['enemies'] and x['target']==xs[0]['target'];twins+=1
out=dict(status='PASS',conditions=34,modelConditions=37,seedRecords=11000,executionsAuthorized=80000,numericSamples=len(ps),exactUniformSamples=uniform,counterTwinChecks=twins,allFreshInputsByteEqualPilot=True,fixedNPCIncluded=False,scope='Pre-outcome construction and frozen policy only; no balance outcome or implementation claim')
(root/'preflight-validation.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
