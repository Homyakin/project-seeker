#!/usr/bin/env python3
"""Describe the fixed exploratory screen. No tests or parameter search."""
import collections,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);source=json.loads((root/'summary.json').read_text());ss=source['cases']
rows=[];bygroup=collections.defaultdict(list)
for s in ss:
    c=s['case']
    row={k:c[k] for k in ('id','groupId','style','tier','originalSize','level','variant','effects')}
    row.update(N=len(c['members']),winratePP=100*s['winrate'],pairScores=s['pairScores'],meanRounds=s['meanRounds'])
    rows.append(row);bygroup[c['groupId']].append(row)
causal=[];effects=[]
for row in rows:
    if not row['variant'].endswith('APPEND') and row['variant']!='EFFECTS':continue
    base=next(x for x in bygroup[row['groupId']] if x['variant']=='BASE')
    ds=[a-b for a,b in zip(row['pairScores'],base['pairScores'])]
    delta=100*sum(ds)/len(ds);half=100*math.sqrt(2*math.log(40)/len(ds))
    obj={k:row[k] for k in ('id','groupId','style','tier','originalSize','N','level','variant','effects')}
    obj.update(baseWinratePP=base['winratePP'],appendWinratePP=row['winratePP'],deltaPP=delta,
               pairedDeltas=ds,pointwise95HoeffdingDeltaPP=[max(-100,delta-half),min(100,delta+half)],
               sampleDropAtLeast20PP=delta<=-20)
    (effects if row['variant']=='EFFECTS' else causal).append(obj)
main={(r['tier'],r['N'],r['level']):r for r in rows if r['variant']=='BASE'}
adjacent=[]
for (tier,n,level),row in main.items():
    if (tier,n+1,level) not in main:continue
    nxt=main[tier,n+1,level];delta=nxt['winratePP']-row['winratePP']
    if delta<=-20:
        adjacent.append(dict(tier=tier,N=n,appendN=n+1,level=level,baseWinratePP=row['winratePP'],appendWinratePP=nxt['winratePP'],deltaPP=delta,
                             policy='NESTED_CANONICAL_MAIN_INPUTS_DIFFERENT_SEED_KEYS_NOT_A_PAIRED_CAUSAL_CONFIRMATION'))
high=[{k:r[k] for k in ('id','style','tier','N','level','winratePP')} for r in rows if r['variant']=='HIGH_SPECIALIZED' and r['level']==50]
low=[{k:r[k] for k in ('id','style','tier','N','level','winratePP')} for r in rows if r['variant']=='LOW_SOLO']
report=dict(purpose='FIXED_EXPLORATORY_B_SCREEN_NOT_BALANCE_ACCEPTANCE',conditions=len(rows),pairsPerCondition=source['manifest']['pairs'],
            actualFights=len(rows)*source['manifest']['pairs']*2,constantsFrozen=dict(C=.47754531235323505,a=.6046927918979718),
            cases=rows,causal=causal,effects=effects,adjacentMainSampleDropFlags=adjacent,highSpecializedL50=high,lowSolo=low,
            limitations=['10 independent mirrored pairs per condition only; 20 battles are not20 independent observations',
                         'All519 conditions were fixed before this screen, no per-cell fitting',
                         'Causal deltas use shared pair seeds but intervals are broad and pointwise, not simultaneous',
                         'Adjacent-MAIN matrix flags use different seed keys; they select future checks, not acceptance claims',
                         'Ritual N15 L50 all three tiers100% is a risk signal, not a formal universal win guarantee',
                         'None of these results activates the candidate or closes15.3'])
(root/'analysis.json').write_text(json.dumps(report,indent=2)+'\n')
print(json.dumps(dict(conditions=len(rows),fights=report['actualFights'],causal=len(causal),effects=len(effects),adjacentDropFlags=len(adjacent)),indent=2))
