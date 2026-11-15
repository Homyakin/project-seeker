#!/usr/bin/env python3
"""Fixed saved-data confirmation analysis. No Java, tuning or new samples."""
import json,math,pathlib,sys

root=pathlib.Path(sys.argv[1])
source=json.loads((root/'summary.json').read_text())
policy=json.loads((root.parent/'inference-policy.json').read_text())
ss=source['cases'];byid={(s['case']['id'],s['model']):s for s in ss}
assert len(ss)==24 and len(policy['caseOrder'])==12
assert policy['conditions']==12 and policy['mirroredPairsPerCondition']==500 and policy['executions']==24000
half=100*math.sqrt(2*math.log(480)/500)
pointwise_half=100*math.sqrt(2*math.log(40)/500)
cells=[]
for cid in range(1,13):
    c=byid[cid,'SAME']['case']
    cell={k:c[k] for k in ('id','groupId','style','tier','originalSize','level','variant','effects')}
    cell.update(name=policy['caseOrder'][cid-1],N=len(c['members']))
    cell['models']={model:dict(winratePP=100*byid[cid,model]['winrate'],pairScoreMeanPP=100*byid[cid,model]['pairMean'],
                             meanRounds=byid[cid,model]['meanRounds'],outcomes=byid[cid,model]['outcomes']) for model in ('SAME','CROSS')}
    cells.append(cell)

families={}
for name in ('append','counterChoice'):
    family=policy['families'][name];comparisons=[]
    for model in ('SAME','CROSS'):
        for first,second in family['comparisonsPerModel']:
            a=byid[first,model];b=byid[second,model]
            assert len(a['pairScores'])==len(b['pairScores'])==500
            ds=[y-x for x,y in zip(a['pairScores'],b['pairScores'])]
            assert all(-1<=d<=1 for d in ds)
            delta=math.fsum(ds)*100/500
            interval=[max(-100,delta-half),min(100,delta+half)]
            comparisons.append(dict(model=model,firstCaseId=first,secondCaseId=second,
                                    firstName=policy['caseOrder'][first-1],secondName=policy['caseOrder'][second-1],
                                    firstWinratePP=100*a['winrate'],secondWinratePP=100*b['winrate'],
                                    deltaPP=delta,pairedDeltas=ds,
                                    simultaneous95FamilyHoeffdingDeltaPP=interval,
                                    pointwise95HoeffdingDeltaPP=[max(-100,delta-pointwise_half),min(100,delta+pointwise_half)],
                                    excludesZero=interval[0]>0 or interval[1]<0,
                                    excludesNegative20PP=interval[0]>-20,
                                    confirmsNegative20PP=interval[1]<-20))
    assert len(comparisons)==12
    families[name]=dict(alpha=.05,comparisonsCount=12,halfWidthPP=half,
                        interval='Bonferroni-Hoeffding simultaneous95 within this12-comparison family only; no joint95 across the two families',
                        comparisons=comparisons)

result=dict(purpose='STEP15.4.1_LAST_FIXED12CELL_TWO_MODEL_CONFIRMATION_NOT_PRODUCTION_ACCEPTANCE',
            conditions=12,models=['SAME','CROSS'],pairsPerModelCondition=500,battles=24000,
            cells=cells,families=families,
            conclusion='NEITHER_SAME_NOR_CROSS_IS_READY: countertype choices remain beneficial at identical neutral targets/NPCs, but original MAIN ordinary addition retains a confirmed large penalty. Best counter addition shows a smaller sample penalty, not proof of an acceptable margin. No tuning or further battles.',
            limitations=[
                'The500mirrored pairs, not1000orders, are the observations; models/variants/type-twins share generation and battle seeds within each cohort',
                'The SAME/CROSS N3 counter baselines have identical inputs and executions; not independent evidence',
                'The append family and counter-choice family each have simultaneous95percent intervals separately, not jointly95percent across both',
                'The ordinary20percentage-point harm margin is diagnostic here; it is not silently a newly approved weak-player acceptance rule',
                'These12conditions do not establish universal balance or a50/50 target; counters and a changing meta are permitted',
                'No formula tuning, extra conditions, repeated peeking-based extension or separateJVM replay after this final run',
                'Defense707/309 and both split formulas remain unaccepted prototypes; actual production is unchanged'
            ])
(root/'analysis.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(dict(conditions=12,battles=24000,halfWidthPP=half,
                     appendComparisons=12,counterComparisons=12),indent=2))
