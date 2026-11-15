#!/usr/bin/env python3
"""Independent saved-data check for the frozen50-cell SAME/CROSS screen; no Java."""
import collections,gzip,hashlib,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);m=json.loads((root/'manifest.json').read_text());cases=json.loads((root/'inputs.json').read_text());seeds=json.loads((root/'seeds.json').read_text());summary=json.loads((root/'summary.json').read_text())
assert m==summary['manifest'] and m['conditions']==50 and m['pairs']==30 and m['battles']==6000
assert m['models']==['SAME','CROSS'] and m['bridges']['status']=='PASS'
assert m['bridges']['actualUnweightedFactoryRebuilds']==30 and m['bridges']['levelMonotonicConstructionChecks']==400
assert len(cases)==50 and [c['id'] for c in cases]==list(range(1,51)) and len(seeds)==780
bycase={c['id']:c for c in cases};AT=('SLASH','BLUNT','PIERCE','MAGICAL')
def norm(v,k=None):
    if isinstance(v,dict):return {k:norm(x,k) for k,x in sorted(v.items())}
    if isinstance(v,list):
        xs=[norm(x) for x in v]
        return sorted(xs) if k in ('slots','availableOnSlots') else xs
    return v
def eq(a,b):assert norm(a)==norm(b)
def close(a,b):assert math.isfinite(a) and math.isfinite(b) and math.isclose(a,b,rel_tol=4e-14,abs_tol=1e-8),(a,b)
def stable(vs):
    out=0.0
    for v in sorted(vs):out+=v
    return out
prep=root.parent/'frozen-preparation'
if prep.exists():
    for name in ('manifest.json','inputs.json','seeds.json'):eq(json.loads((root/name).read_text()),json.loads((prep/name).read_text()))
seedmap={(s['groupId'],s['pair']):s for s in seeds}
assert len(seedmap)==780
for s in seeds:
    for field,ns in [('generationSeed',m['generationNamespace']),('battleSeed',m['battleNamespace'])]:
        assert s[field]==int.from_bytes(hashlib.sha256(f"{ns}/{s['groupId']}/{s['pair']}".encode()).digest()[:6],'big')
        assert 0<=s[field]<2**48
for field in ('generationSeed','battleSeed'):assert len({s[field] for s in seeds})==780
groups=collections.defaultdict(list)
for c in cases:groups[c['groupId']].append(c)
weak=next(c['members'][-1] for c in cases if c['variant']=='WEAK_APPEND')
for item in weak['items']:assert item['rarity']=='COMMON' and item['enhanceLevel']==0 and item['modifier'] is None
for cs in groups.values():
    for c in cs:
        if c['variant'].endswith('APPEND'):
            base=next(x for x in cs if x['variant']=='BASE');eq(c['members'][:-1],base['members'])
            if c['variant']=='WEAK_APPEND':eq(c['members'][-1],weak)
def striptypes(members):
    ms=json.loads(json.dumps(members))
    for member in ms:
        for item in member['items']:
            for a in item['object']['attacks']:a.pop('attackType')
            if item['object']['defense']:item['object']['defense'].pop('defenseType')
    return ms
counters=[c for c in cases if c['variant'].startswith('COUNTER_')];assert len(counters)==4
for c in counters:eq(striptypes(c['members']),striptypes(counters[0]['members']))
refP=m['anchors']['referencePlayers'];refE=m['anchors']['referenceNPC']
assert m['anchors']['referenceDefMultiplier']==4.414285714285715
def metrics(parts):return dict(H=stable(p['H'] for p in parts),D=stable(p['D'] for p in parts))
def actor(p):
    mu=stable(p['damageTakenMultipliers'][t] for t in AT)/4
    attack=max(stable(r['attack'].get(t,0) for t in AT) for r in p['attacksByRange'])
    critical=1+min(1,max(0,p['critChance']/100))*(p['critMultiplier']-1);frequency=min(1000,p['initiative'])/1000
    return dict(id=p['id'],hp=p['health'],mu=mu,H=p['health']/mu,maximumAttack=attack,criticalFactor=critical,frequency=frequency,D=attack*critical*frequency,skills=p['skills'])
def hashed(x):return hashlib.sha256(json.dumps(norm(x),sort_keys=True,separators=(',',':')).encode()).hexdigest()
rarity={'COMMON':0,'UNCOMMON':1,'RARE':2,'EPIC':3,'LEGENDARY':4}
def itemskills(member):
    skills=collections.Counter()
    for i in member['items']:
        if i['modifier'] and i['rarity']!='COMMON':skills[i['modifier']['activeEnum']]+=rarity[i['rarity']]*len(i['object']['slots'])
    return dict(skills)
seen=set();rows=0;outcomes=collections.Counter();rawhash=hashlib.sha256();data=collections.defaultdict(dict);modelcohort={};basecohort={};countercohort={}
prefixChecks=0;counterChecks=0;modelChecks=0;weakChecks=0;maxProductRelativeDelta=0
with gzip.open(root/'battles.jsonl.gz','rb') as raw:
    for line in raw:
        rawhash.update(line);r=json.loads(line);rows+=1;c=r['case'];cid=c['id'];model=r['model'];pair=r['pair'];reverse=r['reverse'];key=(cid,model,pair,reverse)
        assert key not in seen;seen.add(key);eq(c,bycase[cid]);assert model in ('SAME','CROSS') and 0<=pair<30
        s=seedmap[c['groupId'],pair];assert r['generationSeed']==s['generationSeed'] and r['battleSeed']==s['battleSeed']
        assert r['outcome'] in ('WIN','LOSS','DRAW') and 0<r['rounds']<=2000
        assert len(r['actionSha256'])==len(r['traceSha256'])==64
        n=len(c['members']);M=max(2,n);F=M//2;S=M-F;init=r['initState'];ps=init['personagesById'];assert len(ps)==n+M
        assert len(r['unitSeeds'])==M and len(set(r['unitSeeds']))==M and all(0<=x<2**48 for x in r['unitSeeds'])
        assert len(r['playerParts'])==n and len(r['enemyParts'])==M
        expectedEffects={}
        if c['effects']:
            for kind,percent,char in [('MENU_ITEM_EFFECT',20,'ATTACK'),('WORKER_OF_DAY_EFFECT',15,'HEALTH')]:expectedEffects[kind]=dict(effect={'@class':'ru.homyakin.seeker.game.effect.Effect$Multiplier','percent':percent,'characteristic':char},expireDateTime=None)
        eq(r['sourceEffects'],expectedEffects);eq(r['combatEffects'],expectedEffects)
        covered=[]
        for ln in init['lines']:
            for pid in ln['personageIds']:
                assert ps[pid]['lineIndex']==ln['lineIndex'] and ps[pid]['firstTeam']==ln['firstTeam'];covered.append(pid)
        assert sorted(covered)==sorted(ps)
        for field,player in [('playerParts',True),('enemyParts',False)]:
            for p in r[field]:
                a=ps[p['id']];assert a['firstTeam']==(reverse if player else not reverse) and a['health']>0
                for k,v in actor(a).items():
                    if isinstance(v,(int,float)):close(p[k],v)
                    else:eq(p[k],v)
        for i,p in enumerate(r['playerParts']):
            expected=itemskills(c['members'][i]);actual={x['code']:x['points'] for x in ps[p['id']]['skills']};assert actual==expected
            assert all(x['formulaVersion']=='SCALING_SKILLS_V3' for x in ps[p['id']]['skills'])
        player=metrics(r['playerParts']);enemy=metrics(r['enemyParts']);eq(player,r['playerMetrics']);cal=r['calibration']
        scale=math.sqrt(1+((c['level']-10)*.06+.14))/math.sqrt(m['anchors']['q25']);h=player['H']/refP['H'];d=player['D']/refP['D']
        close(cal['levelScale'],scale);close(cal['hRatio'],h);close(cal['dRatio'],d)
        targetH=refE['H']*scale*(h if model=='SAME' else d);targetD=refE['D']*scale*(d if model=='SAME' else h)
        close(cal['targetH'],targetH);close(cal['targetD'],targetD)
        natural=m['naturalByCount'][str(M)];close(cal['hpMultiplier'],targetH/natural['H']);close(cal['attackMultiplier'],targetD/natural['D'])
        close(cal['achievedH'],enemy['H']);close(cal['achievedD'],enemy['D'])
        boundH=stable(1/p['mu'] for p in r['enemyParts']);boundD=stable(p['criticalFactor']*p['frequency'] for p in r['enemyParts'])
        close(cal['healthRoundingBound'],boundH);close(cal['damageRoundingBound'],boundD)
        assert targetH-1e-7<=enemy['H']<=targetH+boundH+1e-7 and targetD-1e-7<=enemy['D']<=targetD+boundD+1e-7
        for i,p in enumerate(r['enemyParts']):
            a=ps[p['id']];g=i<F;hp,atk,defense=(420,70,707) if g else (280,130,309)
            assert a['health']==max(1,math.ceil(hp*cal['hpMultiplier'])) and p['maximumAttack']==max(1,math.ceil(atk*cal['attackMultiplier']))
            assert a['defenses']=={'ARCANE':defense} and a['initiative']==(190 if g else 160)
            assert a['critChance']==(4 if g else 8) and a['dodgeChance']==(3 if g else 5) and a['critMultiplier']==(1.5 if g else 1.7)
            assert a['totalThreat']==(35 if g else 15) and a['impactStrength']==80 and a['range']==(1 if g else 3)
            assert a['skills']==[dict(code='KNOCKBACK' if g else 'PRECISE_STRIKE',points=4,formulaVersion='SCALING_SKILLS_V3')]
            assert len(a['attacksByRange'])==(1 if g else 3) and all(x['attack']=={'MAGICAL':p['maximumAttack']} for x in a['attacksByRange'])
            position=init['lines'][a['lineIndex']]['position'];assert position==('FRONT' if g else 'BACK') and position==r['actualEnemyPositions'][i]
        if c['variant']=='WEAK_APPEND':assert r['playerParts'][-1]['hp']==840;weakChecks+=1
        if c['style']=='WEAK_ONLY':assert all(p['hp']==840 for p in r['playerParts'])
        if c['style']=='STRONG_PLUS_WEAK':assert all(p['hp']==840 for p in r['playerParts'][1:])
        idsGauges=[(p['id'],ps[p['id']]['initiativeGauge']) for p in r['enemyParts']]
        modelkey=(cid,pair,reverse)
        compact=dict(playerParts=r['playerParts'],playerInit=[ps[p['id']] for p in r['playerParts']],unitSeeds=r['unitSeeds'],npcIdsGauges=idsGauges)
        product=cal['targetH']*cal['targetD']
        if modelkey in modelcohort:
            first=modelcohort[modelkey];eq(first['compact'],compact);close(first['product'],product);modelChecks+=1
            maxProductRelativeDelta=max(maxProductRelativeDelta,abs(product-first['product'])/max(1,product))
            if cid==1: eq(first['calibration'],cal)
        else:modelcohort[modelkey]=dict(compact=compact,product=product,calibration=cal)
        cohort=(c['groupId'],model,pair,reverse)
        semantic=[]
        for p in r['playerParts'][:c['originalSize']]:
            x=dict(ps[p['id']]);index=x.pop('lineIndex');x['relativeRow']=init['lines'][index]['position'];semantic.append(x)
        if c['variant']=='BASE':basecohort[cohort]=dict(semantic=hashed(semantic),idsGauges=[(x['id'],x['initiativeGauge']) for x in semantic])
        if c['variant'].endswith('APPEND'):
            assert hashed(semantic)==basecohort[cohort]['semantic'];prefixChecks+=1
        if c['variant']=='EFFECTS':eq([(x['id'],x['initiativeGauge']) for x in semantic],basecohort[cohort]['idsGauges'])
        if c['variant'].startswith('COUNTER_'):
            counterkey=(c['groupId'],pair,reverse)
            signature=dict(calibration=cal,npcInit=[ps[p['id']] for p in r['enemyParts']],unitSeeds=r['unitSeeds'],playerMetrics=player)
            if counterkey in countercohort:eq(countercohort[counterkey],signature);counterChecks+=1
            else:countercohort[counterkey]=signature
        pa=any(x['remainHealth']>0 for x in r['playerStats']);ea=any(x['remainHealth']>0 for x in r['enemyStats']);assert not(pa and ea)
        assert r['outcome']==('WIN' if pa else 'LOSS' if ea else 'DRAW')
        data[cid,model][pair,reverse]=dict(outcome=r['outcome'],rounds=r['rounds'],actionSha256=r['actionSha256'],traceSha256=r['traceSha256'],mirrorHash=hashed([r['playerParts'],r['enemyParts'],cal,r['unitSeeds']]))
        outcomes[r['outcome']]+=1
assert rows==6000 and len(data)==100 and len(summary['cases'])==100
identicalCounterBattleChecks=0
for c in counters:
    for pair in range(30):
        for reverse in (False,True):
            for field in ('outcome','rounds','actionSha256','traceSha256'):
                assert data[c['id'],'SAME'][pair,reverse][field]==data[c['id'],'CROSS'][pair,reverse][field]
            identicalCounterBattleChecks+=1
for s in summary['cases']:
    rs=data[s['case']['id'],s['model']];assert len(rs)==60
    counts=collections.Counter(r['outcome'] for r in rs.values());assert dict(counts)==s['outcomes'];scores=[]
    for pair in range(30):
        a,b=rs[pair,False],rs[pair,True];assert a['mirrorHash']==b['mirrorHash']
        scores.append(sum(1 if rs[pair,rev]['outcome']=='WIN' else .5 if rs[pair,rev]['outcome']=='DRAW' else 0 for rev in (False,True))/2)
    eq(scores,s['pairScores']);mean=stable(scores)/30;half=math.sqrt(math.log(40)/60)
    close(mean,s['pairMean']);close(s['winrate'],counts['WIN']/60)
    for a,b in zip(s['pointwise95Hoeffding'],[max(0,mean-half),min(1,mean+half)]):close(a,b)
    close(s['meanRounds'],stable(x['rounds'] for x in rs.values())/60)
receipt=dict(status='PASS',rows=rows,conditions=50,models=['SAME','CROSS'],pairsPerModelCondition=30,outcomes=dict(outcomes),
             sourceFrozenBeforeOutcomes=True,modelIdentityChecks=modelChecks,playerPrefixChecks=prefixChecks,countertwinsExactChecks=counterChecks,weak840Checks=weakChecks,
             identicalInputCounterModelBattleChecks=identicalCounterBattleChecks,
             maxSameCrossTargetProductRelativeDelta=maxProductRelativeDelta,rawGzipSha256=hashlib.sha256((root/'battles.jsonl.gz').read_bytes()).hexdigest(),rawJsonlSha256=rawhash.hexdigest(),
             checks=['50FrozenInputs780SHA48Seeds','SortedNeutralComponentsFromActualInit','SAME/CROSSAnchorRatioTargets','ClosedformNoRowWeightsHpAtkFixedDEF707/309','IntegerRoundingBounds','V3ExactlyFourSkillPoints','UniqueUUIDsAnd48bitUnitSeeds','ModelAndCountertwinsNPCIdentitiesGauges','FullPlayerPrefixExceptExpectedAbsoluteFieldIndexShift','ExactCounter2x2TargetsAndNPCs','ActualEffects','FinalHPOutcomes','PairSummaryAndIntervals'],
             limitations=['30pairs per condition is exploration, not balance acceptance','Mirrors and common model/variant cohorts are correlated','No new JVM replay or independent combat-engine implementation'])
(root/'validation.json').write_text(json.dumps(receipt,indent=2)+'\n');print(json.dumps(receipt,indent=2))
