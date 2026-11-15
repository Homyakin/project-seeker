#!/usr/bin/env python3
"""Saved-data exploration summary, not an acceptance test or parameter search."""
import collections,json,math,pathlib,sys
root=pathlib.Path(sys.argv[1]);source=json.loads((root/'summary.json').read_text());ss=source['cases'];byId={(s['case']['id'],s['model']):s for s in ss}
cells=[];causal=[]
for cid in range(1,51):
    c=byId[cid,'SAME']['case'];row={k:c[k] for k in ('id','groupId','style','tier','originalSize','level','variant','effects')};row['N']=len(c['members'])
    row['models']={model:dict(winratePP=100*byId[cid,model]['winrate'],meanRounds=byId[cid,model]['meanRounds'],outcomes=byId[cid,model]['outcomes']) for model in ('SAME','CROSS')};cells.append(row)
    if not c['variant'].endswith('APPEND') and c['variant']!='EFFECTS':continue
    for model in ('SAME','CROSS'):
        added=byId[cid,model];base=next(s for s in ss if s['model']==model and s['case']['groupId']==c['groupId'] and s['case']['variant']=='BASE')
        ds=[a-b for a,b in zip(added['pairScores'],base['pairScores'])];delta=sum(ds)/30*100;half=100*math.sqrt(2*math.log(40)/30)
        causal.append(dict(caseId=cid,baseCaseId=base['case']['id'],model=model,style=c['style'],tier=c['tier'],originalSize=c['originalSize'],level=c['level'],variant=c['variant'],
                           baseWinratePP=100*base['winrate'],appendWinratePP=100*added['winrate'],deltaPP=delta,pairedDeltas=ds,
                           exploratoryPointwise95HoeffdingDeltaPP=[max(-100,delta-half),min(100,delta+half)],
                           policy='30pair exploratory pointwise interval only, no simultaneous guarantee and no acceptance by zero-zero or100-100 saturation'))
types=[r for r in cells if r['style']=='COUNTER']
result=dict(purpose='STEP15.4.1_FIXED50CELL_TWO_MODEL_EXPLORATION_NOT_ACCEPTANCE',conditions=50,models=['SAME','CROSS'],pairsPerModelCondition=30,battles=6000,cells=cells,causal=causal,counterType2x2=types,
            conclusion='NEITHER_SAME_NOR_CROSS_IS_READY: ordinary3to4 and several weak additions show large negative sample changes; upper controls saturate at loss. Countertype advantage remains despite identical targets and NPCs.',
            limitations=['30pair samples cannot decide all20pp harm margins; these are risk signals for separately approved fresh confirmation',
                         'SAME/CROSS differ only in H:D allocation and mathematically share target product; neither restores the removed row weights',
                         'Fixed NPC DEF707/309 is a prototype setting, not an accepted gameplay formula',
                         'No50/50 requirement for every matchup; no claim that every possible scalar or split model is impossible',
                         'All6000battles, both models and all50conditions retained, no outcome fitting or adaptive extensions',
                         'An independent-JVM replay has not been run for this screen; same-input countermodel hashes are checked within the existing data'])
(root/'analysis.json').write_text(json.dumps(result,indent=2)+'\n')
print(json.dumps(dict(conditions=50,battles=6000,causalComparisons=len(causal),conclusion=result['conclusion']),indent=2))
