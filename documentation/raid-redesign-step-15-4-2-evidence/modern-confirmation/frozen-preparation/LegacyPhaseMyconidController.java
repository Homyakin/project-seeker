package ru.homyakin.seeker.game.battle;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.models.*;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.utils.MathUtils;

/** Outside-production causal controller; HEAD's32-step scalar search, never the V3 Q branch. */
public final class LegacyPhaseMyconidController {
    public record Generated(List<BattlePersonage> enemies,double multiplier,double targetPower,double groupScaling) { }
    private final SkillFormulaVersion version;
    private final boolean force;
    private final int range;
    private final boolean allLog4;

    LegacyPhaseMyconidController(SkillFormulaVersion version,boolean force,int range,boolean allLog4) {
        this.version=version;this.force=force;this.range=range;this.allLog4=allLog4;
    }
    Generated generate(List<BattlePersonage> players,double q) {
        final int count=Math.max(2,players.size());
        final double g=1+MathUtils.log(!allLog4&&count<=7?2.2:4,count)*.05;
        final double target=players.stream().mapToDouble(BattlePersonage::legacyPower).sum()*q*g*.77;
        final double m=multiplier(count,target);
        final var enemies=new ArrayList<BattlePersonage>();
        for(int i=0;i<count/2;++i)enemies.add(factory(true,m,version,force,range));
        for(int i=count/2;i<count;++i)enemies.add(factory(false,m,version,force,range));
        return new Generated(List.copyOf(enemies),m,target,g);
    }
    private double multiplier(int count,double target) {
        double low=0,high=1;
        if(power(count,low)>=target)return low;
        while(power(count,high)<target) {low=high;high*=2;if(!Double.isFinite(high)||high>100000)throw new IllegalStateException("Unexpected unsafe historical search");}
        for(int i=0;i<32;++i) {
            final double mid=(low+high)/2;
            if(power(count,mid)>=target)high=mid;else low=mid;
        }
        final double lowDiff=Math.abs(power(count,low)-target),highDiff=Math.abs(power(count,high)-target);
        return lowDiff<highDiff?low:high;
    }
    private double power(int count,double m) {
        return factory(true,m,version,force,range).legacyPower()*(count/2)
            +factory(false,m,version,force,range).legacyPower()*(count-count/2);
    }
    static BattlePersonage factory(boolean guard,double m,SkillFormulaVersion version,boolean force,int slingerRange) {
        final var object=new ItemObject(null,Set.of(PersonageSlot.MAIN_HAND),
            Optional.of(new ItemAttack(AttackType.MAGICAL,1,guard?1:slingerRange,multiply(guard?70:130,m))),
            Optional.of(new ItemDefense(DefenseType.ARCANE,multiply(guard?160:70,m))),multiply(guard?420:280,m),
            guard?4:8,guard?3:5,guard?.3:.5,guard?190:160,guard?35:15,null);
        return Step1542CausalProbe.construct(List.of(new Item(object,Optional.of(new Modifier(guard?ActiveEnum.KNOCKBACK:ActiveEnum.PRECISE_STRIKE)),ItemRarity.LEGENDARY)),
            guard?Position.FRONT:Position.BACK,Optional.empty(),version,force);
    }
    private static int multiply(int base,double m) {
        final double n=Math.ceil(base*m);
        if(!Double.isFinite(n)||n>Integer.MAX_VALUE/16)throw new IllegalArgumentException("Unsafe historical characteristic");
        return Math.max(1,(int)n);
    }
}
