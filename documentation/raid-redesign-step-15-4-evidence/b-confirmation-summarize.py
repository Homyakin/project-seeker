#!/usr/bin/env python3
"""Fixed9-family confirmation analysis, preserving all approved conditions."""
import collections,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);source=json.loads((root/'summary.json').read_text());ss=source['cases']
groups=collections.defaultdict(list)
for s in ss:groups[s['case']['groupId']].append(s)
pointwiseHalf=100*math.sqrt(2*math.log(40)/500)
familyHalf=100*math.sqrt(2*math.log(2*9/.05)/500)
comparisons=[];caseStats=[]
for gid,items in groups.items():
    base=next(s for s in items if s['case']['variant']=='BASE')
    for s in items:
        c=s['case'];row={k:c[k] for k in ('id','groupId','style','tier','originalSize','level','variant')}
        row.update(N=len(c['members']),winratePP=100*s['winrate'],pairMeanPP=100*s['pairMean'],meanRounds=s['meanRounds'],outcomes=s['outcomes'])
        caseStats.append(row)
        if s is base:continue
        ds=[a-b for a,b in zip(s['pairScores'],base['pairScores'])]
        delta=100*sum(ds)/500
        comp=dict(row,baseCaseId=base['case']['id'],baseWinratePP=100*base['winrate'],deltaPP=delta,
                  pairedDeltas=ds,pointwise95HoeffdingPP=[max(-100,delta-pointwiseHalf),min(100,delta+pointwiseHalf)],
                  simultaneous9Family95HoeffdingPP=[max(-100,delta-familyHalf),min(100,delta+familyHalf)],
                  ordinaryLargeHarmConfirmed=(c['variant']=='NORMAL_APPEND' and delta+familyHalf < -20),
                  weakNegativeConfirmed=(c['variant']=='WEAK_APPEND' and delta+familyHalf<0))
        comparisons.append(comp)
assert len(ss)==18 and len(comparisons)==9 and len(groups)==9
ritual=next(s for s in ss if s['case']['groupId']==9)
result=dict(purpose='FIXED_B_TWO_ANCHOR_CONFIRMATION_NOT_PRODUCTION_ACTIVATION',conditions=18,pairsPerCondition=500,battles=18000,
            fixedConstants=dict(C=.47754531235323505,a=.6046927918979718),cases=caseStats,comparisons=comparisons,
            inference=dict(pointwise95DifferenceHalfPP=pointwiseHalf,simultaneous9Family95DifferenceHalfPP=familyHalf,
                           formula='Differences in [-1,1]; Hoeffding sqrt(2*ln(2/delta)/n); Bonferroni delta=.05/9',
                           selection='One fixed approved18-condition confirmation on novel SHA48 seeds; all conditions completed without adaptive stopping'),
            ritualBeginner15L50=dict(winratePP=100*ritual['winrate'],pairMeanPP=100*ritual['pairMean'],
                                    pointwise95HoeffdingMeanPP=[100*x for x in ritual['pointwise95Hoeffding']],
                                    policy='Separate descriptive pointwise interval, not part of9-difference simultaneous family'),
            conclusion='REJECT_UNNORMALIZED_B_CANDIDATE_AS_GENERAL_FIX: original3to4 strongly improves, but three ordinary high-level even-to-odd additions independently confirm >20pp harm; weak ritual remains negative; beginnerRitual15L50 wins97percent',
            limitations=['No claim all builds should be50/50','No per-cell fitting or production changes',
                         'All confidence statements concern these fixed fixtures/seeds, not universal matchmaking guarantees',
                         'The weak-addition loss is not automatically authorized by an ordinary-addition20pp criterion',
                         'Scalar-score blind spots and concentration/formation effects remain distinct from combat-engine correctness'])
(root/'analysis.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:result[k] for k in ('conditions','pairsPerCondition','battles','conclusion','ritualBeginner15L50')},indent=2))
