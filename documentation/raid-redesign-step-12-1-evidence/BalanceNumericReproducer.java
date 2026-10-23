import java.lang.reflect.InvocationTargetException;
import java.util.*;
import ru.homyakin.seeker.game.battle.*;
import ru.homyakin.seeker.game.battle.skill.active_impl.ActiveEnum;
import ru.homyakin.seeker.game.battle.skill.scaling.SkillFormulaVersion;
import ru.homyakin.seeker.game.item.catalog.*;
import ru.homyakin.seeker.game.item.models.*;
import ru.homyakin.seeker.game.item.storm.*;

public class BalanceNumericReproducer {
    public static void main(String[] args) throws Exception {
        var catalog = EquipmentCatalogLoader.loadValidated(EquipmentCatalogVersion.SCALING_V3);
        var config = new StormEnhanceConfig();
        var technical = new StormEnhanceTechnicalLimit(config);
        long allHealth = 0;
        long allAttacks = 0;
        for (var object : java.util.stream.Stream.concat(catalog.itemObjects().stream(),
            catalog.defaultItems().stream()).toList()) {
            int n = technical.maxStateLevel(object);
            var state = ItemProgression.state(object, n);
            allHealth += state.health();
            for (var part : state.attacks()) allAttacks += part.attack();
        }
        System.out.printf("CURRENT SCALING_V3 Ntech-price=%d, overestimate-all-items-H=%d, overestimate-all-parts-A=%d%n",
            config.maxPriceSupportedStateLevel(), allHealth, allAttacks);
        healProof();
        thornsProof();
        currentItemThresholds(catalog, technical);
    }

    private static void healProof() throws Exception {
        int h = 2_000_000_000;
        var p = person(h, 60, Map.of(ActiveEnum.SELF_HEAL, 8));
        var field = BattlePersonage.class.getDeclaredField("health");
        field.setAccessible(true);
        field.setInt(p, h - 1);
        int amount = (int) (((long) h * 30 + 200) / 400);
        int before = p.health();
        p.heal(amount);
        System.out.printf("ACTUAL heal H=%d before=%d formula-amount=%d after=%d alive=%s%n",
            h, before, amount, p.health(), p.isAlive());
        field.setInt(p, h - 1);
        var contextConstructor = BattleContext.class.getDeclaredConstructor(List.class, List.class,
            BattleRandom.class);
        contextConstructor.setAccessible(true);
        BattleRandom successful = (sequence, minimum, maximum) -> minimum;
        var context = (BattleContext) contextConstructor.newInstance(List.of(p),
            List.of(person(1_000, 60, Map.of())), successful);
        var method = Class.forName("ru.homyakin.seeker.game.battle.VersionedBattleTurn")
            .getDeclaredMethod("applySelfHeal", BattlePersonage.class, BattleContext.class,
                BattleActionLog.class, int.class);
        method.setAccessible(true);
        var log = new BattleActionLog();
        method.invoke(null, p, context, log, 1);
        System.out.printf("ACTUAL SCALING_SKILLS_V2 SELF_HEAL8 H=%d before=%d after=%d alive=%s log-events=%d%n",
            h, h - 1, p.health(), p.isAlive(), log.events().size());
        long low = 1, high = Integer.MAX_VALUE;
        while (low < high) {
            long middle = (low + high) / 2;
            long requested = (30 * middle + 200) / 400;
            if (middle - 1 + requested > Integer.MAX_VALUE) high = middle;
            else low = middle + 1;
        }
        System.out.printf("MINIMUM SELF_HEAL8 overflow near full health H=%d, rounded-heal=%d%n",
            low, (30*low+200)/400);
    }

    private static void thornsProof() throws Exception {
        int h = 715_827_900;
        var owner = person(h, 60, Map.of(ActiveEnum.THORNS, 8));
        var enemy = person(1_000, 60, Map.of());
        var context = new BattleContext(List.of(owner), List.of(enemy));
        var method = Class.forName("ru.homyakin.seeker.game.battle.VersionedBattleTurn")
            .getDeclaredMethod("applyThorns", BattlePersonage.class, BattlePersonage.class,
                int.class, BattleContext.class, BattleActionLog.class, int.class);
        method.setAccessible(true);
        try {
            method.invoke(null, owner, enemy, 1, context, new BattleActionLog(), 1);
            System.out.println("ACTUAL thorns unexpectedly succeeded");
        } catch (InvocationTargetException e) {
            System.out.printf("ACTUAL thorns H=%d A1=60 exact-basis=%d exact-raw=%s throws=%s at=%s%n",
                h, Math.min(3L*h,40L*60), "15", e.getCause(), e.getCause().getStackTrace()[1]);
        }
    }

    private static void currentItemThresholds(LoadedEquipmentCatalog catalog,
        StormEnhanceTechnicalLimit technical) {
        var minAttack = catalog.itemObjects().stream()
            .filter(o -> o.attacks().stream().anyMatch(a -> a.minRange() == 1))
            .max(Comparator.comparingInt(o -> o.attacks().stream()
                .filter(a -> a.minRange() == 1).mapToInt(ItemAttack::attack).sum()))
            .orElseThrow();
        int low = 0, high = ItemProgression.maxSupportedLevel(minAttack);
        while (low < high) {
            int level = low + (high - low) / 2;
            long a = minAttack.attacks().stream().filter(v -> v.minRange() == 1)
                .mapToLong(v -> ItemProgression.valueAtLevel(v.attack(), level)).sum();
            if (40L*a > Integer.MAX_VALUE) high = level;
            else low = level + 1;
        }
        int level = low;
        long a = minAttack.attacks().stream().filter(v -> v.minRange() == 1)
            .mapToLong(v -> ItemProgression.valueAtLevel(v.attack(), level)).sum();
        System.out.printf("CURRENT item-first-single-attack-overflow code=%s level=%d Ntech=%d A1=%d%n",
            minAttack.code(), level, technical.maxStateLevel(minAttack), a);
        System.out.printf("MINIMUM THORNS integer intermediate overflow H=%d or A1=%d; first multiple-of-60 bases H=%d A1=%d%n",
            Integer.MAX_VALUE/3 + 1, Integer.MAX_VALUE/40 + 1,
            (Integer.MAX_VALUE/3L/60 + 1)*60, (Integer.MAX_VALUE/40L/60+1)*60);
    }

    private static BattlePersonage person(int health, int attack, Map<ActiveEnum,Integer> skills) {
        var object = new ItemObject(null, Set.of(),
            List.of(new ItemAttack(AttackType.SLASH,1,1,attack)), Optional.empty(),
            health,0,0,0,100,10,0,ItemProgressionVersion.V1,Map.of());
        return BattlePersonage.forScalingSkills(List.of(Item.fromObject(object)),Position.FRONT,
            skills, SkillFormulaVersion.SCALING_SKILLS_V2);
    }
}
