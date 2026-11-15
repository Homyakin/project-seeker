#!/usr/bin/env python3
"""Last fixed nine-comparison composition diagnostic, no further simulations."""
import collections,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);source=json.loads((root/'summary.json').read_text());ss=source['cases']
groups=collections.defaultdict(list)
for s in ss:groups[s['case']['groupId']].append(s)
pointwiseHalf=100*math.sqrt(2*math.log(40)/500)
familyHalf=100*math.sqrt(2*math.log(2*9/.05)/500)
comparisons=[];caseStats=[];absoluteControls=[]
for gid,items in groups.items():
    base=next(s for s in items if s['case']['variant']=='BASE')
    for s in items:
        c=s['case'];row={k:c[k] for k in ('id','groupId','style','tier','originalSize','level','variant','effects','diagnosticFixedNpc')}
        row.update(N=len(c['members']),winratePP=100*s['winrate'],pairMeanPP=100*s['pairMean'],meanRounds=s['meanRounds'],outcomes=s['outcomes'])
        caseStats.append(row)
        if gid in (5,6):
            absoluteControls.append(dict(row,pointwise95HoeffdingMeanPP=[100*x for x in s['pointwise95Hoeffding']],
                                        policy='Separate descriptive pointwise bounded mean interval, outside9-difference simultaneous family'))
        if s is base:continue
        ds=[a-b for a,b in zip(s['pairScores'],base['pairScores'])]
        delta=100*sum(ds)/500;lo=max(-100,delta-familyHalf);hi=min(100,delta+familyHalf)
        comp=dict(row,baseCaseId=base['case']['id'],baseWinratePP=100*base['winrate'],deltaPP=delta,
                  pairedDeltas=ds,pointwise95HoeffdingPP=[max(-100,delta-pointwiseHalf),min(100,delta+pointwiseHalf)],
                  simultaneous9Family95HoeffdingPP=[lo,hi],
                  actualOrdinaryLargeHarmConfirmed=(not c['diagnosticFixedNpc'] and c['variant']=='NORMAL_APPEND' and hi < -20),
                  negativeEffectConfirmed=hi<0,positiveEffectConfirmed=lo>0)
        comparisons.append(comp)
assert len(ss)==17 and len(comparisons)==9 and len(groups)==8 and len(absoluteControls)==2
result=dict(purpose='FINAL_FIXED_B_NORM_CONFIRMATION_AND_FIXED_NPC_DIAGNOSTIC_NOT_GAME_ACTIVATION',conditions=17,pairsPerCondition=500,battles=17000,
            fixedConstants=dict(C=.47754531235323505,a=.8766292004637334),cases=caseStats,comparisons=comparisons,absoluteControls=absoluteControls,
            inference=dict(pointwise95DifferenceHalfPP=pointwiseHalf,simultaneous9Family95DifferenceHalfPP=familyHalf,
                           formula='Differences in[-1,1]; Hoeffding sqrt(2*ln(2/delta)/n); Bonferroni delta=.05/9',
                           selection='All17conditions and500pairs fixed before this final run, novel SHA48 three-stream seeds; no early stopping, extensions or subsequent runs'),
            conclusion='B_NORM_REJECTED_AS_READY_GENERAL_FIX: actualMID6to7L25 andSTRONG6to7L25 confirm ordinary harm beyond20pp; fixedNPCdiagnostics instead show positive normal andweak contributions. Actual weak addition remains negative; upper controls lose every sampled fight.',
            diagnosis='Within each diagnostic cohort the appended body is useful when exactly BASE NPC count/m/traits/skills/relativeFrontBackRows/UUID/gauge are retained. Absolute battlefield indices can shift when a new ally occupies a front row; that geometry is intentionally part of the body effect. Severe negative effects appear under recalibrated NPCs, not an inevitable penalty from another ally. This does not supply a correct replacement formula.',
            limitations=['No requirement of50percent for all builds','No production changes or outcome-fitting',
                         'Actual versus fixedNPC groups use independent group seeds and diagnostic NPC construction uses its own stream. Do not subtract their effects as a paired cross-model estimate.',
                         'The ordinary20pp criterion is not blanket authorization for weak-player harm',
                         'FixedNPC is an experimental control, not an accepted gameplay/matchmaking model',
                         'FixedNPC preserves declared and actual relative formation rows, not absolute battlefield lineIndex. In RITUAL+weak PLAYERS_FIRST, NPC indices1/3 become3/5 because ally rows expand; validation reports every such shift.',
                         'Group4effects difference is not statistically negative at the stated family confidence level',
                         'Absolute-control intervals are descriptive pointwise intervals, not jointly covered with9 causal comparisons',
                         'Finite fixtures and seeded samples do not prove universal monotonicity or a perfect balance'])
(root/'analysis.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps({k:result[k] for k in ('conditions','pairsPerCondition','battles','conclusion','diagnosis')},indent=2))
