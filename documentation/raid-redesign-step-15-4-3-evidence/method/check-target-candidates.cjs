'use strict';
// Mathematical checks only. No battle/JVM/source changes. Fixed seed/count before the random loop.
const assert=require('node:assert/strict');
const SEED=0x15432026,COUNT=5000,SIZES=[...Array.from({length:30},(_,i)=>i+1),50];
let rngState=SEED>>>0,checks=0;function random(){rngState^=rngState<<13;rngState^=rngState>>>17;rngState^=rngState<<5;return (rngState>>>0)/4294967296;}
function check(x,label){checks++;assert.ok(x,label);}const sum=a=>a.toSorted((x,y)=>x-y).reduce((s,x)=>s+x,0);
function values(a){
 check(a.length>0,'nonempty math team');for(const x of a)check(Number.isFinite(x.H)&&x.H>=0&&Number.isFinite(x.D)&&x.D>=0,'finite nonnegative H/D');
 const p=a.map(x=>x.H*x.D).toSorted((x,y)=>x-y),Q=sum(a.map(x=>x.H))*sum(a.map(x=>x.D)),B=sum(p.map((x,i)=>x*(2*(a.length-i)-1))),F=(Q+B)/2;
 check([Q,B,F].every(Number.isFinite),'finite within selected safe numerical domain');return {Q,B,F};
}
function near(a,b,label){check(Math.abs(a-b)<=1e-11*Math.max(1,Math.abs(a),Math.abs(b)),label);}
let maxRelativeBToQ=0,minRelativeBToQ=1;
for(let k=0;k<COUNT;k++){
 const n=SIZES[k%SIZES.length],a=Array.from({length:n},()=>({H:10**(-6+12*random()),D:10**(-6+12*random())}));
 const v=values(a),ps=a.map(x=>x.H*x.D),quadratic=sum(ps.flatMap(x=>ps.map(y=>Math.min(x,y))));
 near(v.B,quadratic,'sorted B equals double min sum');check(v.B>=sum(ps)*(1-1e-12),'B >= sum own products');check(v.B<=n*sum(ps)*(1+1e-12),'B <= N sum own products');
 check(v.B<=v.Q*(1+1e-12),'B <= Q');check(v.F>=.5*v.Q*(1-1e-12)&&v.F<=v.Q*(1+1e-12),'blend bounded halfQ..Q');
 const perm=a.toReversed(),vp=values(perm);check(v.Q===vp.Q&&v.B===vp.B&&v.F===vp.F,'exact permutation stability through sorted numeric sums');
 for(const axis of ['H','D']){const idx=k%n,b=a.map(x=>({...x}));b[idx][axis]*=1.1;const w=values(b);check(w.Q>=v.Q*(1-1e-14)&&w.B>=v.B*(1-1e-14)&&w.F>=v.F*(1-1e-14),'one positive statistic improvement never reduces target');}
 const H=1+random()*100,D=1+random()*100,uniform=values(Array.from({length:n},()=>({H,D})));near(uniform.Q,n*n*H*D,'uniform Q');near(uniform.B,uniform.Q,'identical actor unchanged');
 const scaleH=.2,scaleD=3,scaled=values(a.map(x=>({H:x.H*scaleH,D:x.D*scaleD})));near(scaled.F,v.F*scaleH*scaleD,'global separate H/D unit scale invariance');
 maxRelativeBToQ=Math.max(maxRelativeBToQ,v.B/v.Q);minRelativeBToQ=Math.min(minRelativeBToQ,v.B/v.Q);
}
const zero=values([{H:0,D:0},{H:0,D:0}]);check(zero.Q===0&&zero.B===0&&zero.F===0,'zero target defined without F/Q');
const carry=[{H:10,D:100}],tank=[...carry,{H:1000,D:0}],c=values(carry),t=values(tank);check(t.B===c.B,'pure B ignores zero-D tank health');near(t.F-c.F,(t.Q-c.Q)/2,'blend retains half added health cross');
const complementary=values([{H:100,D:1},{H:1,D:100}]);check(complementary.Q===10201&&complementary.B===400&&complementary.F===5300.5,'equal p actors can get nearly halfQ discount');
const large=values(Array.from({length:50},()=>({H:1e150,D:1e150})));check(large.Q>0,'large safe finite scale');
process.stdout.write(JSON.stringify({status:'PASS',purpose:'MATHEMATICAL_TARGET_AUDIT_ONLY_NO_BATTLES',seed:SEED,count:COUNT,sizes:SIZES,checks,observedBToQ:{min:minRelativeBToQ,max:maxRelativeBToQ},
 witnesses:{zero,soloCarry:c,carryPlusZeroDamageTank:t,equalProductsComplementary:complementary,finiteLarge:large},
 limitations:['Safe finite numerical domain is tested, not arbitrary Java double overflow proof.','Monotone target is not monotone victory probability under adaptive enemies.','No actual battle strength, meta acceptance or stripping exploit absence follows.']},null,2)+'\n');

