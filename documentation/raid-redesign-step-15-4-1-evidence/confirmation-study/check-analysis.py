#!/usr/bin/env python3
"""Second saved-summary check of both predeclared inferential families."""
import json,math,pathlib,sys
r=pathlib.Path(sys.argv[1]);s=json.loads((r/'summary.json').read_text());a=json.loads((r/'analysis.json').read_text());p=json.loads((r.parent/'inference-policy.json').read_text())
by={(x['case']['id'],x['model']):x for x in s['cases']}
expected=[('COUNTER_PIERCE_ARCANE','BASE'),('COUNTER_PIERCE_ARCANE','NORMAL_APPEND'),('COUNTER_PIERCE_LEATHER','BASE'),('COUNTER_PIERCE_LEATHER','NORMAL_APPEND'),('COUNTER_SLASH_ARCANE','BASE'),('COUNTER_SLASH_ARCANE','NORMAL_APPEND'),('COUNTER_SLASH_LEATHER','BASE'),('COUNTER_SLASH_LEATHER','NORMAL_APPEND'),('MAIN','BASE'),('MAIN','NORMAL_APPEND'),('RITUAL','BASE'),('RITUAL','WEAK_APPEND')]
assert [(by[i,'SAME']['case']['style'],by[i,'SAME']['case']['variant']) for i in range(1,13)]==expected
for c in a['cells']:
 assert c['name']==p['caseOrder'][c['id']-1]
 for model in ('SAME','CROSS'):
  x=by[c['id'],model];y=c['models'][model]
  assert y['winratePP']==100*x['winrate'] and y['outcomes']==x['outcomes'] and y['meanRounds']==x['meanRounds']
count=0;half=100*math.sqrt(2*math.log(2*12/.05)/500)
for family in ('append','counterChoice'):
 f=a['families'][family];assert f['comparisonsCount']==12 and f['halfWidthPP']==half
 expected_pairs=[(model,x,y) for model in ('SAME','CROSS') for x,y in p['families'][family]['comparisonsPerModel']]
 assert [(x['model'],x['firstCaseId'],x['secondCaseId']) for x in f['comparisons']]==expected_pairs
 for c in f['comparisons']:
  b=by[c['firstCaseId'],c['model']];t=by[c['secondCaseId'],c['model']]
  diffs=[t['pairScores'][i]-b['pairScores'][i] for i in range(500)]
  assert diffs==c['pairedDeltas'];delta=math.fsum(diffs)/5
  assert math.isclose(delta,c['deltaPP'],abs_tol=1e-12)
  for actual,expected in zip(c['simultaneous95FamilyHoeffdingDeltaPP'],[max(-100,delta-half),min(100,delta+half)]):assert math.isclose(actual,expected,abs_tol=1e-12)
  assert c['excludesZero']==(delta-half>0 or delta+half<0)
  assert c['excludesNegative20PP']==(delta-half>-20)
  assert c['confirmsNegative20PP']==(delta+half<-20)
  count+=1
out=dict(status='PASS',cases=12,modelCases=24,appendComparisons=12,counterChoiceComparisons=12,pairedDeltasChecked=count*500,halfWidthPP=half,
         scope='Each predeclared family has separate simultaneous95percent coverage; not joint95percent across families. Saved data only, no battles.')
(r/'analysis-validation.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
