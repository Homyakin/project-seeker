#!/usr/bin/env python3
"""Independent saved-data structural/score validation, no battles or Java."""
import collections,gzip,hashlib,json,math,pathlib,sys
r=pathlib.Path(sys.argv[1]);m=json.loads((r/'manifest.json').read_text());cs=json.loads((r/'inputs.json').read_text());seeds=json.loads((r/'seeds.json').read_text());summ=json.loads((r/'summary.json').read_text());prepared=json.loads((r/'prepared-pair0.json').read_text())
n=m['pairs'];by={c['id']:c for c in cs};sm={(s['groupId'],s['pair']):s for s in seeds};pp={p['caseId']:p for p in prepared}
assert summ['manifest']==m and len(cs)==len(summ['cases'])==m['conditions'] and m['battles']==len(cs)*n*2
for name in ('manifest.json','fixture-manifest.json','inputs.json','seeds.json','prepared-pair0.json'):assert (r/name).read_bytes()==(r.parent/'frozen-preparation'/name).read_bytes(),name
assert len(sm)==len(seeds)
for s in seeds:
 for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace'])]:assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/{s['groupId']}/{s['pair']}".encode()).digest()[:6],'big') and 0<=s[field]<2**48
def norm(v,k=None):
 if isinstance(v,dict):return {k:norm(x,k) for k,x in sorted(v.items())}
 if isinstance(v,list):
  xs=[norm(x) for x in v];return sorted(xs) if k in ('slots','availableOnSlots') else xs
 return v
def eq(a,b):assert norm(a)==norm(b)
def close(a,b):assert math.isclose(a,b,rel_tol=4e-14,abs_tol=1e-8),(a,b)
def dump(o):return json.dumps(o,ensure_ascii=False,separators=(',',':')).encode()
def sig(o):return hashlib.sha256(dump(norm(o))).hexdigest()
rows=0;seen=set();outcomes=collections.Counter();rawhash=hashlib.sha256();data=collections.defaultdict(dict);mirrors={};bases={};fullLogs=0;prefixes=0;pair0=0;scalarChecks=0;scalarStatChecks=0;v3PowerChecks=0
with gzip.open(r/'battles.jsonl.gz','rb') as f:
 for line in f:
  rows+=1;rawhash.update(line);x=json.loads(line);c=x['case'];cid=c['id'];phase=c['phase'];p=x['pair'];rev=x['reverse'];key=(cid,p,rev)
  assert key not in seen;seen.add(key);eq(c,by[cid]);assert x['phase']==phase['id'] and x['scenarioId']==c['scenarioId'] and x['level']==c['level'] and x['mirror']==rev and 0<=p<n
  s=sm[c['groupId'],p];assert x['generationSeed']==s['generationSeed'] and x['battleSeed']==s['battleSeed']
  cal=x['calibration'];diag=x['diagnostics'];players=x['playerTraits'];enemies=x['enemyTraits'];N=len(players);M=max(2,N);F=M//2
  assert len(enemies)==M and N==len(c['members']) and len({a['id'] for a in players+enemies})==N+M
  assert all(a['hp']>0 for a in players+enemies)
  assert diag['actualPlayerScalingRules']==[a['scalingRules'] for a in players] and diag['actualEnemyScalingRules']==[a['scalingRules'] for a in enemies]
  assert diag['globalScalingSkillOrder']==any(a['scalingRules'] for a in players+enemies)
  assert diag['enemyRanges']==[1]*F+[phase['slingerRange']]*(M-F)
  for a,version in [(a,phase['playerSkills']) for a in players]+[(a,phase['npcSkills']) for a in enemies]:assert all(k['formulaVersion']==version for k in a['skills'])
  for a in players+enemies:
   if phase['force']:assert a['scalingRules']
   if not a['scalingRules']:assert not any(k['formulaVersion']!='LEGACY_SKILLS_V1' for k in a['skills'])
   if a['scalingRules'] and (phase['playerSkills']!='LEGACY_SKILLS_V1' if a in players else phase['npcSkills']!='LEGACY_SKILLS_V1'):
    atk=sum(a['attacks'][0]['attack'].values());mu=sum(a['reductions'].values())/4;effective=max(1,max(1,atk)*(1+a['crit']/100*(a['critMultiplier']-1)))
    # V3's actual legacyPower has no legacy itemSkills ratings. This deliberately reproduces its historical dodge clamp.
    value=max(1,a['hp'])*max(1,effective)/mu/max(1,1-a['dodge']/100)*(max(1,a['speed'])/1000)
    close(value,a['legacyPower']);v3PowerChecks+=1
  close(cal['actualPlayerLegacyPower'],math.fsum(a['legacyPower'] for a in players));close(cal['actualEnemyLegacyPower'],math.fsum(a['legacyPower'] for a in enemies))
  close(cal['q'],1+((c['level']-10)*.06+.14))
  if not phase['actualQ']:
   g=1+math.log(M)/math.log(4 if phase['allLog4'] or M>7 else 2.2)*.05;close(cal['groupScaling'],g);close(cal['encounterCorrection'],.77)
   close(cal['targetPower'],cal['actualPlayerLegacyPower']*cal['q']*g*.77);scalarChecks+=1
   for i,a in enumerate(enemies):
    guard=i<F;hp,atk,de=(420,70,160) if guard else (280,130,70);mult=cal['multiplier']
    assert a['hp']==max(1,math.ceil(hp*mult)) and a['defenses']=={'ARCANE':max(1,math.ceil(de*mult))}
    assert len(a['attacks'])==(1 if guard else phase['slingerRange']) and all(b['attack']=={'MAGICAL':max(1,math.ceil(atk*mult))} for b in a['attacks'])
    scalarStatChecks+=1
  else:
   d=diag['generationDiagnostics'];assert d['assessmentVersion']=='NEUTRAL_TEAM_V1'
   params=d['parameters'];close(cal['targetPower'],d['participantAssessment']*cal['q']*params['groupSizeScaling']*params['encounterCorrection']*params['sizeCorrection'])
   close(cal['multiplier'],cal['result']['multiplier'])
  for i,a in enumerate(enemies):
   guard=i<F;assert a['range']==(1 if guard else phase['slingerRange']) and a['position']==('FRONT' if guard else 'BACK')
   assert a['speed']==(190 if guard else 160) and a['crit']==(4 if guard else 8) and a['dodge']==(3 if guard else 5) and a['critMultiplier']==(1.5 if guard else 1.7)
   assert a['skills']==[dict(code='KNOCKBACK' if guard else 'PRECISE_STRIKE',points=4,formulaVersion=phase['npcSkills'])]
  if c['scenarioId']==4:assert players[-1]['hp']==(720 if phase['gear']=='OLD' else 840)
  cohort=(phase['id'],c['level'],p)
  if c['scenarioId'] in (1,3,5):bases[cohort,c['scenarioId']]=sig(players)
  else:assert sig(players[:-1])==bases[cohort,c['scenarioId']-1];prefixes+=1
  mirrorTraits=sig([players,enemies,cal,diag])
  if (cid,p) in mirrors:assert mirrors[cid,p]==mirrorTraits
  else:mirrors[cid,p]=mirrorTraits
  if p==0:
   q=pp[cid];eq(players,q['players']);eq(enemies,q['enemies']);eq(cal,q['calibration']);eq(diag,q['diagnostics']);pair0+=1
  outcome=x['outcome'];assert outcome in ('WIN','LOSS','DRAW','TIMEOUT');outcomes[outcome]+=1
  if outcome!='TIMEOUT':
   init=x['initState'];ps=init['personagesById'];assert len(ps)==N+M
   covered=[pid for ln in init['lines'] for pid in ln['personageIds']];assert sorted(covered)==sorted(ps)
   for actors,isplayer in [(players,True),(enemies,False)]:
    for a in actors:
     i=ps[a['id']];assert i['firstTeam']==(rev if isplayer else not rev)
     for t,k in [('hp','health'),('gauge','initiativeGauge'),('attacks','attacksByRange'),('defenses','defenses'),('reductions','damageTakenMultipliers'),('crit','critChance'),('dodge','dodgeChance'),('critMultiplier','critMultiplier'),('speed','initiative'),('range','range'),('threat','totalThreat'),('impact','impactStrength'),('tactic','targetingTactic'),('skills','skills'),('items','items')]:eq(a[t],i[k])
   for stats,actors in [(x['playerStats'],players),(x['enemyStats'],enemies)]:
    assert len(stats)==len(actors)
    for stat,actor in zip(stats,actors):assert stat['initialHealth']==actor['hp'] and 0<=stat['remainHealth']<=actor['hp']
   pa=any(a['remainHealth']>0 for a in x['playerStats']);ea=any(a['remainHealth']>0 for a in x['enemyStats']);assert not(pa and ea);assert outcome==('WIN' if pa else 'LOSS' if ea else 'DRAW')
   if p<2:
    log=x['actionLog'];assert set(log)=={'events'} # traceEvents is @JsonIgnore; explicitly audited separately.
    assert hashlib.sha256(dump(log)).hexdigest()==x['actionSha256']
    assert len(log['events'])==x['actionCount']
    assert sum(e['type']=='MovedTowardEnemy' for e in log['events'])==x['moves'] and sum(e['type']=='PersonageForcedMove' for e in log['events'])==x['forcedMoves']
    assert sum(e['type']=='RoundStarted' for e in log['events'])==x['rounds'];fullLogs+=1
   else:assert 'actionLog' not in x
  data[cid][p,rev]=dict(outcome=outcome,rounds=x['rounds'],events=x.get('actionCount'))
assert rows==m['battles'] and len(data)==m['conditions']
for s in summ['cases']:
 rs=data[s['case']['id']];assert len(rs)==n*2;counts=collections.Counter(x['outcome'] for x in rs.values());assert dict(counts)==s['outcomes'];scores=[]
 for p in range(n):
  vals=[rs[p,b]['outcome'] for b in (False,True)];scores.append(None if 'TIMEOUT' in vals else sum(1 if v=='WIN' else .5 if v=='DRAW' else 0 for v in vals)/2)
 eq(scores,s['pairScores']);done=[x for x in scores if x is not None];mean=sum(done)/len(done) if done else 0;half=math.sqrt(math.log(40)/(2*len(done))) if done else 1
 close(s['pairMean'],mean);close(s['winrate'],counts['WIN']/(n*2));eq(s['pointwise95Hoeffding'],[max(0,mean-half),min(1,mean+half)])
 completed=[x for x in rs.values() if x['outcome']!='TIMEOUT'];close(s['meanRounds'],sum(x['rounds'] for x in completed)/len(completed));close(s['meanActions'],sum(x['events'] for x in completed)/len(completed))
out=dict(status='PASS',rows=rows,conditions=m['conditions'],pairs=n,outcomes=dict(outcomes),fullEventLogsVerified=fullLogs,playerPrefixChecks=prefixes,pair0FrozenSnapshotChecks=pair0,
         scalarTargetChecks=scalarChecks,scalarIntegerStatsChecks=scalarStatChecks,v3ActualLegacyPowerChecks=v3PowerChecks,rawGzipSha256=hashlib.sha256((r/'battles.jsonl.gz').read_bytes()).hexdigest(),rawJsonlSha256=rawhash.hexdigest(),
         scope='Saved-data validation only; n30pilot is descriptive, no acceptance or universal50/50 claim. Modern full event logs exclude @JsonIgnore traceEvents; audit preserves this correction. No new battles.')
(r/'validation.json').write_text(json.dumps(out,indent=2)+'\n');print(json.dumps(out,indent=2))
