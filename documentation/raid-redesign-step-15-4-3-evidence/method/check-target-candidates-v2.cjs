'use strict';
// Independent mathematical-only review of the frozen-direction candidates. No battle/JVM/source changes.
const assert=require('node:assert/strict');
const SEED=0x15432027,COUNT=5000,SIZES=[...Array.from({length:30},(_,i)=>i+1),50];
let rngState=SEED>>>0,checks=0;function random(){rngState^=rngState<<13;rngState^=rngState>>>17;rngState^=rngState<<5;return (rngState>>>0)/4294967296;}
function check(x,label){checks++;assert.ok(x,label);}const sum=a=>a.toSorted((x,y)=>x-y).reduce((s,x)=>s+x,0);
function near(a,b,label){check(Math.abs(a-b)<=1e-11*Math.max(1,Math.abs(a),Math.abs(b)),label);}
function values(a){
 check(a.length>0,'nonempty math team');for(const x of a)check(Number.isFinite(x.H)&&x.H>=0&&Number.isFinite(x.D)&&x.D>=0,'finite nonnegative H/D');
 const p=a.map(x=>x.H*x.D).toSorted((x,y)=>y-x),Q=sum(a.map(x=>x.H))*sum(a.map(x=>x.D));
 const rawB=sum(p.map((x,i)=>x*(2*i+1)));let rawT=0,bestK=0;for(let i=0;i<p.length;i++){const x=(i+1)*(i+1)*p[i];if(x>rawT){rawT=x;bestK=i+1;}}
 // Candidate numeric policy under review: clamp only mathematical upper bound; no battle-derived coefficient.
 const B=Math.min(rawB,Q),T=Math.min(rawT,Q),F_B=.5*Q+.5*B,F_T=.5*Q+.5*T;
 check([Q,rawB,rawT,B,T,F_B,F_T].every(Number.isFinite),'finite in selected safe numerical domain');return {Q,rawB,rawT,B,T,F_B,F_T,bestK};
}
for(let k=0;k<COUNT;k++){
 const n=SIZES[k%SIZES.length],a=Array.from({length:n},()=>({H:10**(-6+12*random()),D:10**(-6+12*random())})),v=values(a),ps=a.map(x=>x.H*x.D),quadratic=sum(ps.flatMap(x=>ps.map(y=>Math.min(x,y))));
 near(v.rawB,quadratic,'sorted B equals double min sum');check(v.rawT<=v.rawB*(1+1e-12),'T <= B');check(v.rawB<=v.Q*(1+1e-12),'B <= Q');check(v.rawT>=Math.max(...ps)*(1-1e-12),'T >= largest self product');
 check(v.F_B>=.5*v.Q&&v.F_B<=v.Q&&v.F_T>=.5*v.Q&&v.F_T<=v.Q,'clamped blends in exact halfQ..Q interval');
 const vp=values(a.toReversed());check(JSON.stringify(v)===JSON.stringify(vp),'exact permutation stability');
 for(const axis of ['H','D']){const idx=k%n,b=a.map(x=>({...x}));b[idx][axis]*=1.1;const w=values(b);for(const m of ['Q','B','T','F_B','F_T'])check(w[m]>=v[m]*(1-1e-14),'one positive statistic improvement never reduces '+m);}
 const H=1+random()*100,D=1+random()*100,u=values(Array.from({length:n},()=>({H,D})));near(u.rawB,u.Q,'identical actors B=Q');near(u.rawT,u.Q,'identical actors T=Q');
 const scaled=values(a.map(x=>({H:x.H*.2,D:x.D*3})));near(scaled.F_B,v.F_B*.6,'blendB unit scale invariance');near(scaled.F_T,v.F_T*.6,'blendT unit scale invariance');
 const app={H:random(),D:random()},w=values([...a,app]);check(w.F_B>=v.F_B*(1-1e-14)&&w.F_T>=v.F_T*(1-1e-14),'adding nonnegative actor cannot reduce raw team target at fixed count correction');
}
const zero=values([{H:0,D:0},{H:0,D:0}]);check(zero.F_B===0&&zero.F_T===0,'zero defined without F/Q');
const carry=values([{H:10,D:100}]),tank=values([{H:10,D:100},{H:1000,D:0}]);near(tank.F_B-carry.F_B,(tank.Q-carry.Q)/2,'blendB half tank health cross');near(tank.F_T-carry.F_T,(tank.Q-carry.Q)/2,'blendT half tank health cross');
const complementary=values([{H:100,D:1},{H:1,D:100}]);check(complementary.F_B===5300.5&&complementary.F_T===5300.5,'equal products do not imply unchanged target');
const threshold=[.249999,.25,.250001].map(r=>({r,...values([{H:1,D:1},{H:Math.sqrt(r),D:Math.sqrt(r)}])}));for(let i=1;i<threshold.length;i++)check(threshold[i].F_T>=threshold[i-1].F_T,'bestK boundary monotone with continuous nearby values');
const tiered=values(Array.from({length:30},(_,i)=>({H:1/(i+1),D:1/(i+1)})));
const strongFillers=[1,2,3,7,15,30].map(N=>{const r=.22,actors=[{H:1,D:1},...Array.from({length:N-1},()=>({H:Math.sqrt(r),D:Math.sqrt(r)}))],v=values(actors);near(v.rawT,Math.max(1,N*N*r),'one strong/identical weak T identity');return {N,...v};});
const large=values(Array.from({length:50},()=>({H:1e150,D:1e150})));
process.stdout.write(JSON.stringify({status:'PASS',purpose:'MATHEMATICAL_ONLY_F_BLEND_B_AND_F_BLEND_T_NO_BATTLES',seed:SEED,count:COUNT,sizes:SIZES,checks,numericPolicy:'B=min(rawB,Q);T=min(rawT,Q);F=.5Q+.5metric; sorted sums/products, smallest bestK on strict maximum update.',witnesses:{zero,carry,tank,complementary,threshold,tiered,strongFillers,large},limitations:['Tested finite safe scale, not arbitrary double overflow proof.','No actual-battle or meta acceptance follows.','Best subgroup is a technical rectangle under ordered p, not actual best party strength.','Monotone raw target does not imply monotone victories; count correction can separately change with added player.']},null,2)+'\n');

