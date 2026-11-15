#!/usr/bin/env python3
"""Read-only joint K3 confidence calculation from two validated saved summaries."""
import hashlib,json,math,pathlib,sys
modern=pathlib.Path(sys.argv[1]);historical=pathlib.Path(sys.argv[2]);policy=json.loads((modern.parent/'inference-policy.json').read_text())
c=json.loads((modern/'summary.json').read_text());h=json.loads((historical/'summary.json').read_text())
assert policy['confidence']['familySize']==3 and policy['pairsPerCondition']==500
cb={x['case']['scenarioId']:x for x in c['cases']};hb={x['scenarioId']:x for x in h['cases']}
assert set(cb)==set(hb)=={1,2}
dc=[];dh=[];did=[]
for i in range(500):
 vals=[cb[s]['pairScores'][i] for s in (1,2)]+[hb[s]['pairScores'][i] for s in (1,2)]
 assert all(v is not None and 0<=v<=1 for v in vals)
 a=vals[1]-vals[0];b=vals[3]-vals[2];dc.append(a);dh.append(b);did.append(a-b)
assert h['policySHA256']==hashlib.sha256((modern.parent/'inference-policy.json').read_bytes()).hexdigest()
half=math.sqrt(2*math.log(120)/500);rows=[]
for name,ds,support,mult in [('deltaH',dh,[-1,1],1),('deltaC9',dc,[-1,1],1),('DID',did,[-2,2],2)]:
 mean=math.fsum(ds)/500;radius=half*mult
 rows.append(dict(name=name,n=500,meanPP=mean*100,simultaneous95K3HoeffdingPP=[max(support[0],mean-radius)*100,min(support[1],mean+radius)*100],halfWidthPP=radius*100,pairedDifferences=ds))
out=dict(status='PASS',familySize=3,alpha=.05,pilotExcluded=True,comparisons=rows,
         conclusion='Both selected append differences are below zero with simultaneous95K3intervals. DID includes zero: sample improvement does not establish a reliable improvement or acceptable20ppharm margin. No50/50requirement.',
         limitations=['OnlyfixedcommonprojectionL25; not universal probabilities or monotonicity','C9 is an aggregate actual15.2 reference, not one-factor change from historical engine','Shared phase/variant seeds and mirrors are correlated;500pair indices are observations','No formula/parameter changes or additional runs after this one confirmation'],
         sources={str(p):hashlib.sha256(p.read_bytes()).hexdigest() for p in (modern/'summary.json',historical/'summary.json',modern.parent/'inference-policy.json')})
(modern/'joint-confidence.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps({**out,'comparisons':[{k:v for k,v in x.items() if k!='pairedDifferences'} for x in rows]},indent=2))
