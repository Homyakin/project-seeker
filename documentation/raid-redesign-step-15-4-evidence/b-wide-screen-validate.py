#!/usr/bin/env python3
"""Independent raw-file validator. No Java, production access or battles."""
import collections
import gzip
import hashlib
import json
import math
import pathlib
import sys

root=pathlib.Path(sys.argv[1])
manifest=json.loads((root/'manifest.json').read_text())
inputs=json.loads((root/'inputs.json').read_text())
summary=json.loads((root/'summary.json').read_text())
assert summary['manifest']==manifest
cases={c['id']:c for c in inputs}
assert len(cases)==519 and list(cases)==list(range(1,len(cases)+1))
assert manifest['bridgeValidation']['status']=='EXACT_ACTUAL_CURRENT_BASELINE_BRIDGE'
assert manifest['bridgeValidation']['conditions']==6
assert manifest['effectsBridgeValidation']['status']=='EXACT_ACTUAL_PREPARED_EFFECTFUL_PLAYERS'
assert manifest['effectsBridgeValidation']['conditions']==6
assert manifest['anchors']['Cnew']==.47754531235323505 and manifest['anchors']['screenA']==.6046927918979718
assert manifest['anchors']['midAnchorFactoryCharacteristicsExact'] and manifest['anchors']['soloAnchorFactoryCharacteristicsExact']
assert collections.Counter(c['variant'] for c in inputs)==dict(BASE=306,NORMAL_APPEND=30,WEAK_APPEND=30,EFFECTS=6,LOW_SOLO=45,SPECIALIZED=48,HIGH_SPECIALIZED=54)
main_set={(c['tier'],c['originalSize'],c['level']) for c in inputs if c['variant']=='BASE'}
assert main_set=={(t,n,l) for t in ('BEGINNER','MID','STRONG') for n in (*range(2,17),29,30) for l in (10,20,25,30,40,50)}
pairs=manifest['pairs']
models=manifest['models']
AT=('SLASH','BLUNT','PIERCE','MAGICAL')
def norm(value,key=None):
    if isinstance(value,dict): return {k:norm(v,k) for k,v in sorted(value.items())}
    if isinstance(value,list):
        vs=[norm(v) for v in value]
        return sorted(vs) if key in ('slots','availableOnSlots') else vs
    return value
def eq(a,b): assert norm(a)==norm(b)
eq(inputs,json.loads((root.parent/'preparation'/'inputs.json').read_text()))
eq(manifest,json.loads((root.parent/'preparation'/'manifest.json').read_text()))
def close(a,b): assert math.isfinite(a) and math.isfinite(b) and math.isclose(a,b,rel_tol=3e-14,abs_tol=1e-9),(a,b)
def summed(vs):
    out=0.0
    for v in sorted(vs):out+=v
    return out
def score(model,parts):
    if model=='Q_CURRENT':return summed(p['effectiveHealth'] for p in parts)*summed(p['normalRate'] for p in parts)
    ps=sorted(p['selfProduct'] for p in parts)
    if model=='P_SUM':return summed(ps)
    assert model=='B_TWO_ANCHOR'
    out=0.0
    for i,p in enumerate(ps):out+=(2*(len(ps)-i)-1)*p
    return out
def actor(p):
    mu=sum(p['damageTakenMultipliers'][t] for t in AT)/4
    ehp=p['health']/mu
    attack=max((sum(r['attack'].get(t,0) for t in AT) for r in p['attacksByRange']),default=0)
    critical=1+min(1,max(0,p['critChance']/100))*(p['critMultiplier']-1)
    freq=min(1000,p['initiative'])/1000
    rate=0 if attack==0 or critical==0 or freq==0 else attack*critical*freq
    return dict(id=p['id'],hp=p['health'],mu=mu,effectiveHealth=ehp,maximumAttack=attack,
                criticalFactor=critical,frequency=freq,normalRate=rate,selfProduct=ehp*rate,skills=p['skills'])
weak=next(c['members'][-1] for c in inputs if c['variant']=='WEAK_APPEND')
for item in weak['items']:
    assert item['rarity']=='COMMON' and item['enhanceLevel']==0 and item['modifier'] is None
groups=collections.defaultdict(list)
for c in inputs:groups[c['groupId']].append(c)
for group in groups.values():
    base=next((c for c in group if c['variant']=='BASE'),None)
    for c in group:
        if c['variant'].endswith('APPEND'):
            assert base is not None
            eq(c['members'][:-1],base['members'])
            if c['variant']=='WEAK_APPEND':eq(c['members'][-1],weak)
            if c['variant']=='NORMAL_APPEND':
                next_base=next(x for x in inputs if x['variant']=='BASE' and x['tier']==c['tier'] and x['originalSize']==c['originalSize']+1 and x['level']==c['level'])
                eq(c['members'],next_base['members'])
        if c['variant']=='EFFECTS':
            assert base is not None and c['effects'] is True
            eq(c['members'],base['members'])
data=collections.defaultdict(dict)
seen=set();rows=0;outcomes=collections.Counter();weak_alive_checks=0
streamhash=hashlib.sha256()
with gzip.open(root/'battles.jsonl.gz','rb') as raw:
    for line in raw:
        streamhash.update(line);r=json.loads(line);rows+=1
        c=r['case'];cid=c['id'];model=r['model'];pair=r['pair'];rev=r['reverse']
        eq(c,cases[cid]);assert model in models and 0<=pair<pairs and isinstance(rev,bool)
        key=(cid,model,pair,rev);assert key not in seen;seen.add(key)
        seed=manifest['seedOffset']+c['groupId']*1000+pair
        assert r['generationSeed']==manifest['generationRoot']+seed
        assert r['battleSeed']==manifest['battleRoot']+seed
        assert r['outcome']!='TIMEOUT' and 0<r['rounds']<=manifest['roundLimit']
        assert len(r['actionSha256'])==len(r['traceSha256'])==64
        assert all(ch in '0123456789abcdef' for ch in r['actionSha256']+r['traceSha256'])
        init=r['initState'];byid=init['personagesById']
        effect_expected={}
        if c['effects']:
            for kind,percent,characteristic in [('MENU_ITEM_EFFECT',20,'ATTACK'),('WORKER_OF_DAY_EFFECT',15,'HEALTH')]:
                effect_expected[kind]=dict(effect={'@class':'ru.homyakin.seeker.game.effect.Effect$Multiplier','percent':percent,'characteristic':characteristic},expireDateTime=None)
        eq(r['sourceEffects'],effect_expected);eq(r['combatEffects'],effect_expected)
        assert len(r['actualEnemyPositions'])==len(r['enemyParts'])
        assert all(isinstance(r[k],int) and r[k]>=0 for k in ('actionCount','normalAttempts','moves','forcedMoves'))
        assert len(byid)==len(c['members'])+max(2,len(c['members']))
        fullids=[]
        for line in init['lines']:
            for pid in line['personageIds']:
                pp=byid[pid];assert pp['lineIndex']==line['lineIndex'] and pp['firstTeam']==line['firstTeam'];fullids.append(pid)
        assert sorted(fullids)==sorted(byid)
        for field,player in [('playerParts',True),('enemyParts',False)]:
            assert len(r[field])==(len(c['members']) if player else max(2,len(c['members'])))
            for part in r[field]:
                actual=byid[part['id']];assert actual['firstTeam']==(rev if player else not rev)
                assert actual['health']>0
                expected=actor(actual)
                for k,v in expected.items():
                    if isinstance(v,(int,float)):close(part[k],v)
                    else:eq(part[k],v)
        m=max(2,len(c['members']));f=m//2;s=m-f;mul=r['calibration']['multiplier']
        for i,part in enumerate(r['enemyParts']):
            p=byid[part['id']];front=i<f;weight=m/(2*(f if front else s))
            hp,atk,defense=(420,70,160) if front else (280,130,70)
            assert p['health']==math.ceil(hp*(mul*weight))
            assert part['maximumAttack']==math.ceil(atk*(mul*weight))
            assert p['defenses']=={'ARCANE':math.ceil(defense*mul)}
            assert p['initiative']==(190 if front else 160)
            assert p['critChance']==(4 if front else 8) and p['dodgeChance']==(3 if front else 5)
            assert p['range']==(1 if front else 3)
            assert len(p['attacksByRange'])==(1 if front else 3)
            assert all(x['attack']=={'MAGICAL':part['maximumAttack']} for x in p['attacksByRange'])
            assert init['lines'][p['lineIndex']]['position']==('FRONT' if front else 'BACK')
            assert r['actualEnemyPositions'][i]==('FRONT' if front else 'BACK')
            assert p['skills']==[{'code':'KNOCKBACK' if front else 'PRECISE_STRIKE','points':4,'formulaVersion':'SCALING_SKILLS_V3'}]
        if c['variant']=='WEAK_APPEND':
            assert r['playerParts'][-1]['hp']==840;weak_alive_checks+=1
        if c['style']=='WEAK_ONLY':assert all(p['hp']==840 for p in r['playerParts'])
        if c['style']=='STRONG_PLUS_WEAK':assert all(p['hp']==840 for p in r['playerParts'][1:])
        close(r['playerScore'],score(model,r['playerParts']));close(r['enemyScore'],score(model,r['enemyParts']))
        close(r['calibration']['achievedPower'],r['enemyScore'])
        bonus=1+((c['level']-10)*.06+.14)
        g=1+math.log(m)/math.log(4)*.05
        h=r['a']+(1-r['a'])*math.pow(3/m,2)
        close(r['calibration']['targetPower'],r['playerScore']*bonus*g*r['C']*h)
        assert r['a']==(manifest['anchors']['screenA'] if model!='Q_CURRENT' else .7)
        assert r['C']==(manifest['anchors']['oldC'] if model=='Q_CURRENT' else manifest['anchors']['Cnew'])
        pa=any(p['remainHealth']>0 for p in r['playerStats']);ea=any(p['remainHealth']>0 for p in r['enemyStats'])
        assert not(pa and ea)
        assert r['outcome']==('WIN' if pa else 'LOSS' if ea else 'DRAW')
        mirror_hash=hashlib.sha256(json.dumps([r['playerParts'],r['enemyParts'],r['calibration']],sort_keys=True,separators=(',',':')).encode()).hexdigest()
        data[(cid,model)][(pair,rev)]={k:r[k] for k in ('generationSeed','battleSeed','outcome','rounds')}
        data[(cid,model)][(pair,rev)]['mirrorHash']=mirror_hash
        outcomes[r['outcome']]+=1
assert rows==len(cases)*len(models)*pairs*2
assert len(summary['cases'])==len(cases)*len(models)
summary_map={(s['case']['id'],s['model']):s for s in summary['cases']}
for key,rs in data.items():
    assert len(rs)==pairs*2
    su=summary_map[key];counts=collections.Counter(r['outcome'] for r in rs.values())
    assert dict(counts)==su['outcomes']
    ps=[]
    for pair in range(pairs):
        scores=[]
        a=rs[pair,False];b=rs[pair,True]
        assert a['generationSeed']==b['generationSeed'] and a['battleSeed']==b['battleSeed']
        assert a['mirrorHash']==b['mirrorHash']
        for rev in (False,True):
            outcome=rs[pair,rev]['outcome'];scores.append(1 if outcome=='WIN' else .5 if outcome=='DRAW' else 0)
        ps.append(sum(scores)/2)
    eq(su['pairScores'],ps);mean=sum(ps)/pairs
    close(su['winrate'],counts['WIN']/(2*pairs));close(su['pairMean'],mean)
    half=math.sqrt(math.log(40)/(2*pairs))
    for a,b in zip(su['pointwise95Hoeffding'],[max(0,mean-half),min(1,mean+half)]):close(a,b)
    close(su['meanRounds'],sum(r['rounds'] for r in rs.values())/(2*pairs))
receipt=dict(status='PASS',rows=rows,conditions=len(cases),models=models,pairsPerCondition=pairs,
             mirroredPairObservations=len(cases)*len(models)*pairs,outcomes=dict(outcomes),weakAliveChecks=weak_alive_checks,
             rawGzipSha256=hashlib.sha256((root/'battles.jsonl.gz').read_bytes()).hexdigest(),rawUncompressedSha256=streamhash.hexdigest(),
             checks=['approved519CellMatrix','canonicalInputEquality','causalPrefixPreservation','normalAppendMatchesNextCanonicalTeam','weak840HPDefaults','pairSeedsAndMirrors',
                     'actualPreparedEffectsBridgeAndArchivedEffectMaps','actualEnemyPositions',
                     'actorNeutralComponentsFromBattleInit','npcWeightedFactoryHPAttackDefense','V3FourPointSkillsRowsProfiles',
                     'symmetricScoresAndTargets','achievedCalibrationScore','outcomesFromFinalHP','summaryPairScoresCIAndRounds'],
             limitations=[f'{pairs} mirrored pairs per condition is exploration, not balance acceptance','Shared seeds across models/causal variants mean pooled observations are not independent','hashes cannot validate battle engine independently',
                          'no repeated-JVM reproducibility run in this screen','pointwise intervals are broad and not simultaneous'])
(root/'validation.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
