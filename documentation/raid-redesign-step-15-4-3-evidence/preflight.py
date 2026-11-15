#!/usr/bin/env python3
"""Independent saved-construction checks before15.4.3outcomes; no Java/battles."""
import collections,hashlib,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);r=root/'frozen-preparation';m=json.loads((r/'manifest.json').read_text());cs=json.loads((r/'inputs.json').read_text());ss=json.loads((r/'seeds.json').read_text());ps=json.loads((r/'prepared-pair0.json').read_text());policy=json.loads((r/'screen-policy.json').read_text())
assert hashlib.sha256((r/'screen-policy.json').read_bytes()).hexdigest()=='e41947b273b6db0defa748fcd0a8edbcbd1dec482570a5049aa1f0b7e9c858cf'
assert hashlib.sha256((root/'Step1543HeterogeneityProbe.java').read_bytes()).hexdigest()==policy['helperSHA256']
assert hashlib.sha256((r/'manifest.json').read_bytes()).hexdigest()==policy['manifestSHA256']
assert len(cs)==76 and len(ps)==224 and len(ss)==475 and (m['conditions'],m['modelConditions'],m['pairs'],m['battles'])==(76,224,25,11200)
assert [c['id'] for c in cs]==list(range(1,77))
assert {(s['groupId'],s['pair']) for s in ss}=={(g,p) for g in range(1,20) for p in range(25)}
for s in ss:
 for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace']),('npcSeed',m['npcNamespace'])]:assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/{s['groupId']}/{s['pair']}".encode()).digest()[:6],'big')
for field in ('generationSeed','battleSeed','npcSeed'):assert len({s[field] for s in ss})==475
bycase={c['id']:c for c in cs};seen=set();numeric=0;uniform=0
def close(a,b):assert math.isclose(a,b,rel_tol=5e-14,abs_tol=1e-8),(a,b)
def stable(values):
 total=0.0
 for v in sorted(values):total+=v
 return total
for x in ps:
 c=x['case'];assert c==bycase[c['id']];key=(c['id'],x['rule']);assert key not in seen;seen.add(key)
 assert x['rule'] in (['BASELINE'] if c['family']=='FIXED_NPC' else ['BASELINE','BLEND_B','BLEND_T'])
 vs=x['contributions'];e=x['playerEvaluation'];H=stable(y['H'] for y in vs);D=stable(y['D'] for y in vs);Q=H*D;close(e['Q'],Q)
 for y in vs:close(y['p'],y['H']*y['D'])
 p=sorted(y['p'] for y in vs);B=sum((2*(len(p)-i)-1)*v for i,v in enumerate(p));T=max(k*k*p[-k] for k in range(1,len(p)+1));K=next(k for k in range(1,len(p)+1) if k*k*p[-k]==T)
 close(e['rawB'],B);close(e['rawT'],T);assert e['bestK']==K
 equal=all(y['H']==vs[0]['H'] and y['D']==vs[0]['D'] for y in vs);assert e['exactSoloOrIdentical']==equal
 F=Q if x['rule']=='BASELINE' or equal else Q/2+min(B if x['rule']=='BLEND_B' else T,Q)/2;discount=1 if Q==0 else F/Q
 close(e['F'],F);close(e['discount'],discount)
 if equal:assert e['F']==e['Q'] and e['discount']==1;uniform+=1
 t=x['target'];cal=x['calibration'];basis=x['calibrationEvaluation'];N=len(x['players']);M=2 if c['family']=='FIXED_NPC' else max(2,N);front=M//2;back=M-front
 assert t['count']==M and t['frontCount']==front and t['backCount']==back
 close(t['frontWeight'],M/(2*front));close(t['backWeight'],M/(2*back));close(t['groupScaling'],1+.05*math.log(M)/math.log(4));close(t['sizeCorrection'],.7+.3*(3/M)**2);close(t['levelPowerBonus'],1+((c['level']-10)*.06+.14));close(t['encounterCorrection'],.5900075417594784)
 base=basis['Q']*t['levelPowerBonus']*t['groupScaling']*t['encounterCorrection']*t['sizeCorrection'];close(t['baselineTarget'],base);close(t['requestedTarget'],t['baselineTarget']*basis['discount']);close(cal['targetPower'],t['requestedTarget'])
 assert len(x['enemies'])==M and len(x['players'])==len(c['members']);ids=[a['id'] for a in x['players']+x['enemies']];assert len(set(ids))==len(ids)
 numeric+=1
fixed=[x for x in ps if x['case']['family']=='FIXED_NPC'];assert len(fixed)==2 and all(x['rule']=='BASELINE' for x in fixed) and fixed[0]['enemies']==fixed[1]['enemies'] and fixed[0]['target']==fixed[1]['target']
counterchecks=0
for rule in ('BASELINE','BLEND_B','BLEND_T'):
 for variant in ('BASE','NORMAL_APPEND'):
  xs=[x for x in ps if x['rule']==rule and x['case']['family']=='COUNTER_TWINS' and x['case']['variant']==variant];assert len(xs)==4
  for x in xs[1:]:assert x['enemies']==xs[0]['enemies'] and x['target']==xs[0]['target'];counterchecks+=1
assert sum(1 if c['family']=='FIXED_NPC' else 3 for c in cs)*25*2==11200
out=dict(status='PASS',conditions=76,modelConditions=224,seedRecords=475,numericSamples=numeric,exactUniformSamples=uniform,counterTwinChecks=counterchecks,fixedNPCExact=True,executionsAuthorized=11200,
         scope='Pre-outcome saved construction only, not balance acceptance; rootpolicy and helper/manifest hashes exact')
(root/'preflight-validation.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
