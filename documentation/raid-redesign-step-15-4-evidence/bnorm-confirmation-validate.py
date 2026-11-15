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
assert len(cases)==17 and list(cases)==list(range(1,len(cases)+1))
assert manifest['bridgeValidation']['status']=='EXACT_ACTUAL_CURRENT_BASELINE_BRIDGE'
assert manifest['bridgeValidation']['conditions']==6
assert manifest['effectsBridgeValidation']['status']=='EXACT_ACTUAL_PREPARED_EFFECTFUL_PLAYERS'
assert manifest['effectsBridgeValidation']['conditions']==6
assert manifest['anchors']['Cnew']==.47754531235323505 and manifest['anchors']['screenA']==.8766292004637334
assert manifest['N2N3UnnormalizedBBridge']['status']=='EXACT_CHARACTERISTICS_UUID_GAUGE_VS_UNNORMALIZED_B'
assert manifest['N2N3UnnormalizedBBridge']['conditions']==2
assert manifest['fixedNpcDiagnosticBridge']['status']=='EXACT_FIXED_BASE_NPC_AND_PLAYER_PREFIX'
assert manifest['fixedNpcDiagnosticBridge']['conditions']==5
assert manifest['fixedConcentrationCorrection']['referenceMultiplier']==4.414285714285715
assert manifest['fixedConcentrationCorrection']['referenceNPC3Score']==1466224.9940141398
assert manifest['fixedConcentrationCorrection']['byCount']['2']==1.2945040158323535
assert manifest['anchors']['midAnchorFactoryCharacteristicsExact'] and manifest['anchors']['soloAnchorFactoryCharacteristicsExact']
assert collections.Counter(c['variant'] for c in inputs)==dict(BASE=8,NORMAL_APPEND=5,WEAK_APPEND=3,EFFECTS=1)
assert manifest['pairs']==500 and sum(c['diagnosticFixedNpc'] for c in inputs)==5
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
seeds=json.loads((root/'seeds.json').read_text())
eq(seeds,json.loads((root.parent/'preparation'/'seeds.json').read_text()))
assert len(seeds)==4000
seedmap={(s['groupId'],s['pair']):s for s in seeds};assert len(seedmap)==4000
def seed48(namespace,group,pair):
    return int.from_bytes(hashlib.sha256(f'{namespace}/{group}/{pair}'.encode()).digest()[:6],'big')
for group in range(1,9):
    for pair in range(500):
        s=seedmap[group,pair]
        for field,ns in [('generationSeed',manifest['generationNamespace']),('battleSeed',manifest['battleNamespace']),('npcGenerationSeed',manifest['npcNamespace'])]:
            assert s[field]==seed48(ns,group,pair) and 0<=s[field]<2**48
for field in ('generationSeed','battleSeed','npcGenerationSeed'):assert len({s[field] for s in seeds})==4000
references=json.loads((root.parent/'wide-reference-inputs.json').read_text())+json.loads((root.parent/'pilot-reference-inputs.json').read_text())
for c in inputs:
    if c['variant']=='WEAK_APPEND':continue
    matches=[r for r in references if r['variant'] in ('BASE','LOW_SOLO','SPECIALIZED','HIGH_SPECIALIZED') and not r.get('effects',False) and r['style']==c['style'] and r['tier']==c['tier'] and len(r['members'])==len(c['members']) and r['level']==c['level']]
    assert matches,(c['id'],'No independent archived canonical fixture')
    assert any(norm(c['members'])==norm(r['members']) for r in matches)
def close(a,b): assert math.isfinite(a) and math.isfinite(b) and math.isclose(a,b,rel_tol=3e-14,abs_tol=1e-9),(a,b)
def summed(vs):
    out=0.0
    for v in sorted(vs):out+=v
    return out
def score(model,parts):
    if model=='Q_CURRENT':return summed(p['effectiveHealth'] for p in parts)*summed(p['normalRate'] for p in parts)
    ps=sorted(p['selfProduct'] for p in parts)
    if model=='P_SUM':return summed(ps)
    assert model=='B_NORM_TWO_ANCHOR'
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
                next_base=next(x for x in references if x['variant']=='BASE' and x['style']=='MAIN' and x['tier']==c['tier'] and x['originalSize']==c['originalSize']+1 and x['level']==c['level'])
                eq(c['members'],next_base['members'])
        if c['variant']=='EFFECTS':
            assert base is not None and c['effects'] is True
            eq(c['members'],base['members'])
data=collections.defaultdict(dict)
basecohort={};fixed_npc_checks=0;prefix_checks=0;absolute_line_shifts=collections.Counter();shift_patterns={}
seen=set();rows=0;outcomes=collections.Counter();weak_alive_checks=0
streamhash=hashlib.sha256()
with gzip.open(root/'battles.jsonl.gz','rb') as raw:
    for line in raw:
        streamhash.update(line);r=json.loads(line);rows+=1
        c=r['case'];cid=c['id'];model=r['model'];pair=r['pair'];rev=r['reverse']
        eq(c,cases[cid]);assert model in models and 0<=pair<pairs and isinstance(rev,bool)
        key=(cid,model,pair,rev);assert key not in seen;seen.add(key)
        seed=seedmap[c['groupId'],pair]
        assert r['generationSeed']==seed['generationSeed']
        assert r['battleSeed']==seed['battleSeed']
        assert r['npcGenerationSeed']==seed['npcGenerationSeed']
        assert r['npcSeparateSeedUsed']==c['diagnosticFixedNpc']
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
        enemy_count=max(2,c['originalSize'] if c['diagnosticFixedNpc'] else len(c['members']))
        assert r['calibrationEnemyCount']==enemy_count
        assert len(byid)==len(c['members'])+enemy_count
        fullids=[]
        for line in init['lines']:
            for pid in line['personageIds']:
                pp=byid[pid];assert pp['lineIndex']==line['lineIndex'] and pp['firstTeam']==line['firstTeam'];fullids.append(pid)
        assert sorted(fullids)==sorted(byid)
        for field,player in [('playerParts',True),('enemyParts',False)]:
            assert len(r[field])==(len(c['members']) if player else enemy_count)
            for part in r[field]:
                actual=byid[part['id']];assert actual['firstTeam']==(rev if player else not rev)
                assert actual['health']>0
                expected=actor(actual)
                for k,v in expected.items():
                    if isinstance(v,(int,float)):close(part[k],v)
                    else:eq(part[k],v)
        m=enemy_count;f=m//2;s=m-f;mul=r['calibration']['multiplier']
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
        cohortkey=(c['groupId'],pair,rev)
        player_hash=hashlib.sha256(json.dumps(r['playerParts'][:c['originalSize']],sort_keys=True,separators=(',',':')).encode()).hexdigest()
        player_ids_gauges=[(p['id'],byid[p['id']]['initiativeGauge']) for p in r['playerParts'][:c['originalSize']]]
        npc_snapshots=[byid[p['id']] for p in r['enemyParts']]
        npc_indices=[p['lineIndex'] for p in npc_snapshots]
        npc_semantic_snapshots=[{k:v for k,v in p.items() if k!='lineIndex'} for p in npc_snapshots]
        npc_hash=hashlib.sha256(json.dumps(npc_semantic_snapshots,sort_keys=True,separators=(',',':')).encode()).hexdigest()
        if c['variant']=='BASE':
            basecohort[cohortkey]=dict(playerHash=player_hash,idsGauges=player_ids_gauges,npcHash=npc_hash,npcIndices=npc_indices,calibration=r['calibration'],playerScore=r['playerScore'])
        else:
            base=basecohort[cohortkey]
            eq(player_ids_gauges,base['idsGauges'])
            if c['variant'].endswith('APPEND'):
                assert player_hash==base['playerHash'];prefix_checks+=1
            if c['diagnosticFixedNpc']:
                assert npc_hash==base['npcHash'];eq(r['calibration'],base['calibration']);fixed_npc_checks+=1
                if npc_indices!=base['npcIndices']:
                    shiftkey=f"case{cid}/PLAYERS_FIRST={rev}"
                    absolute_line_shifts[shiftkey]+=1
                    shift_patterns[shiftkey]=dict(baseIndices=base['npcIndices'],appendIndices=npc_indices,relativeRows=r['actualEnemyPositions'])
        calibration_score=basecohort[cohortkey]['playerScore'] if c['diagnosticFixedNpc'] else r['playerScore']
        close(r['calibrationParticipantScore'],calibration_score)
        close(r['calibration']['achievedPower'],r['enemyScore'])
        bonus=1+((c['level']-10)*.06+.14)
        g=1+math.log(m)/math.log(4)*.05
        h=r['a']+(1-r['a'])*math.pow(3/m,2)
        correction=manifest['fixedConcentrationCorrection']['byCount'][str(m)]
        close(r['concentrationCorrection'],correction)
        close(r['calibration']['targetPower'],calibration_score*bonus*g*r['C']*h*correction)
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
             fixedNpcRowChecks=fixed_npc_checks,playerPrefixRowChecks=prefix_checks,
             npcAbsoluteLineIndexShiftRows=dict(absolute_line_shifts),npcAbsoluteLineIndexShiftPatterns=shift_patterns,
             checks=['approved17CellMatrix','preArchivedInputsAnd4000SeedTriples','SHA256First48BEThreeNamespaces','canonicalInputEquality','independentArchivedCanonicalFixtures','causalPrefixPreservation','normalAppendMatchesNextCanonicalTeam','weak840HPDefaults','pairSeedsAndMirrors','fixedNPCAllInitFieldsExceptAbsoluteLineIndexPlusExactRelativeRowsAndBaseCalibration',
                     'actualPreparedEffectsBridgeAndArchivedEffectMaps','actualEnemyPositions',
                     'actorNeutralComponentsFromBattleInit','npcWeightedFactoryHPAttackDefense','V3FourPointSkillsRowsProfiles',
                     'symmetricScoresAndTargetsWithFixedConcentrationCorrection','achievedCalibrationScore','outcomesFromFinalHP','summaryPairScoresCIAndRounds'],
             limitations=[f'{pairs} mirrored pairs per condition is exploration, not balance acceptance','Shared seeds across models/causal variants mean pooled observations are not independent','hashes cannot validate battle engine independently',
                          'no repeated-JVM reproducibility run in this screen','Absolute lineIndex can shift when adding an occupied ally row; exact relative FRONT/BACK and all other NPC init fields are checked and all index shifts reported','Per-cell summary intervals are pointwise; causal analysis.json separately supplies the fixed9-family correction'])
(root/'validation.json').write_text(json.dumps(receipt,indent=2)+'\n')
print(json.dumps(receipt,indent=2))
