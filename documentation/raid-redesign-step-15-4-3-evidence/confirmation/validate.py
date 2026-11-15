#!/usr/bin/env python3
"""Streaming owner audit of the already completed frozen confirmation, no battles."""
import collections,gzip,hashlib,json,math,pathlib,subprocess,sys
root=pathlib.Path(sys.argv[1]);load=lambda p:json.loads(p.read_text());f=root/'frozen-preparation';run=root/'screen'
policy=load(f/'confirmation-policy.json');manifest=load(f/'manifest.json');inputs=load(f/'inputs.json');seeds=load(f/'seeds.json');summary=load(run/'summary.json')
assert hashlib.sha256((f/'confirmation-policy.json').read_bytes()).hexdigest()=='3876690b6ceb542c1bf2a3112d2b6e194ae922c028f706bd2b08686c11e515cf'
for name in ('manifest.json','inputs.json','seeds.json','construction-checks.json','prepared-pair0.json'):assert (run/name).read_bytes()==(f/name).read_bytes(),name
subprocess.run(['sha256sum','-c',str(root/'prebattle-freeze.sha256')],check=True,stdout=subprocess.DEVNULL)
alloc={(c,rule):a['pairs'] for a in policy['allocations'] for c in a['caseIds'] for rule in a['rules']};bycase={c['id']:c for c in inputs};seedmap={(s['groupId'],s['pair']):s for s in seeds};seen=set();outcomes=collections.Counter();cells=collections.defaultdict(collections.Counter);scores=collections.defaultdict(dict);rounds=collections.Counter();events=collections.Counter();complete=collections.Counter();logs=0;total=0
with gzip.open(run/'battles.jsonl.gz','rt') as stream:
 for line in stream:
  row=json.loads(line);c=row['case'];key=(c['id'],row['rule']);p=row['pair'];rev=row['reverse'];k=(*key,p,rev)
  assert key in alloc and 0<=p<alloc[key] and k not in seen;seen.add(k);assert row['mirror']==rev and c==bycase[c['id']]
  s=seedmap[c['groupId'],p]
  for name in ('generationSeed','battleSeed','npcSeed'):assert row[name]==s[name]
  assert row['npcSeedUsed'] is False
  e=row['ruleAssessment'];assert e==row['calibrationAssessment'] and row['actualPlayerAssessment']==e['Q']
  t=row['target'];assert t['requestedTarget']==t['baselineTarget']*e['discount']==row['calibration']['targetPower']
  vs=row['contributions'];hs=sorted(x['H'] for x in vs);ds=sorted(x['D'] for x in vs);H=D=0.
  for x in hs:H+=x
  for x in ds:D+=x
  assert H*D==e['Q']
  pi=sorted(x['p'] for x in vs);B=0.;T=pi[-1];K=1
  for i,x in enumerate(pi):
   B+=(2.*(len(pi)-i)-1)*x;k2=i+1;v=(float(k2)*k2)*pi[-k2]
   if v>T:T=v;K=k2
  assert e['rawB']==B and e['rawT']==T and e['bestK']==K
  eq=all(x['H']==vs[0]['H'] and x['D']==vs[0]['D'] for x in vs)
  F=e['Q'] if row['rule']=='BASELINE' or eq else e['Q']/2+min(T,e['Q'])/2
  assert e['F']==F and e['discount']==(1 if e['Q']==0 else F/e['Q'])
  ids=[x['id'] for x in row['playerTraits']+row['enemyTraits']];assert len(ids)==len(set(ids))
  assert len(row['playerStats'])==len(row['playerTraits'])==len(c['members']) and len(row['enemyStats'])==len(row['enemyTraits'])==t['count']
  for traits,stats in ((row['playerTraits'],row['playerStats']),(row['enemyTraits'],row['enemyStats'])):
   for actor,stat in zip(traits,stats):assert stat['initialHealth']==actor['hp'] and 0<=stat['remainHealth']<=actor['hp']
  outcome=row['outcome'];assert outcome in ('WIN','LOSS','DRAW','TIMEOUT');outcomes[outcome]+=1;cells[key][outcome]+=1
  if outcome!='TIMEOUT':
   alivep=any(x['remainHealth']>0 for x in row['playerStats']);alivee=any(x['remainHealth']>0 for x in row['enemyStats']);assert outcome==('WIN' if alivep else 'LOSS' if alivee else 'DRAW')
   assert not(alivep and alivee);value={'WIN':1.,'LOSS':0.,'DRAW':.5}[outcome];rounds[key]+=row['rounds'];events[key]+=row['actionCount'];complete[key]+=1
  else:value=None
  scores[key][p,rev]=value
  assert ('actionLog' in row)==('traceEvents' in row)==(p==0)
  if p==0:logs+=1;assert len(row['actionLog']['events'])==row['actionCount']
  total+=1
assert total==len(seen)==80000 and len(cells)==37 and logs==74
for cell in summary['cases']:
 key=(cell['case']['id'],cell['rule']);n=alloc[key];assert cell['pairs']==n and dict(cells[key])==cell['outcomes']
 pairs=[None if scores[key][p,False] is None or scores[key][p,True] is None else (scores[key][p,False]+scores[key][p,True])/2 for p in range(n)]
 assert cell['pairScores']==pairs and len(scores[key])==2*n
 finished=[x for x in pairs if x is not None];mean=sum(finished)/len(finished) if finished else 0
 assert math.isclose(cell['pairMean'],mean,abs_tol=1e-14) and cell['winrate']==cells[key]['WIN']/(2*n)
 assert cell['meanRounds']==rounds[key]/complete[key] and cell['meanEvents']==events[key]/complete[key]
assert summary['manifest']==manifest
sha=hashlib.sha256((run/'battles.jsonl.gz').read_bytes()).hexdigest()
receipt=dict(status='PASS',executions=total,modelConditions=len(cells),uniqueConditions=len(inputs),seedRecords=len(seeds),pair0FullLogs=logs,outcomes=dict(outcomes),rawGzipSHA256=sha,scope='Owner streaming structural/numeric/summary audit; no new battles, no independent sampling or full balance acceptance')
(run/'validation.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
