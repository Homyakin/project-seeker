package ru.homyakin.seeker.game.battle;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.EnumSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.TreeMap;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.ItemRarity;
import ru.homyakin.seeker.game.item.storm.StormEnhanceConfig;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;

/**
 * Read-only economy context for reference inputs, using repository application.properties.
 * Expected attempts include failures and one-level rollbacks. They concern one fresh item and
 * are expectations, not a percentile, time estimate, guaranteed budget or full modifier cost.
 * No application context, database, battle, random draw or global configuration is activated.
 */
final class Step123ReferenceEconomy {
    private static final String PREFIX = "homyakin.seeker.item.storm-enhance.";

    private final StormEnhanceConfig storm;
    private final Properties properties;
    private final Map<String, String> values;

    private Step123ReferenceEconomy(Properties properties) {
        this.properties = properties;
        final var selected = new TreeMap<String, String>();
        properties.stringPropertyNames().stream().filter(key -> key.startsWith(PREFIX))
            .forEach(key -> selected.put(key, properties.getProperty(key)));
        values = Map.copyOf(selected);
        storm = new StormEnhanceConfig();
        storm.setBaseCost(integer("base-cost"));
        storm.setCostMultiplierNumerator(integer("cost-multiplier-numerator"));
        storm.setCostMultiplierDenominator(integer("cost-multiplier-denominator"));
        storm.setBaseSuccessPercent(integer("base-success-percent"));
        storm.setSuccessMultiplier(decimal("success-multiplier"));
        storm.setFailurePeakLevel(integer("failure-peak-level"));
        storm.setFailureBaseWeight(decimal("failure-base-weight"));
        storm.setFailureGrowMultiplier(decimal("failure-grow-multiplier"));
        storm.setFailureDecayMultiplier(decimal("failure-decay-multiplier"));
        storm.setRollbackFromLevel(integer("rollback-from-level"));
        storm.setRollbackBaseWeight(decimal("rollback-base-weight"));
        storm.setRollbackGrowMultiplier(decimal("rollback-grow-multiplier"));
        // The legacy bonus setter mutates global state. V1 stats use ItemProgression instead.
        // Its configured value remains part of the fingerprint without activating that setter.
    }

    static Step123ReferenceEconomy configured() throws IOException {
        final var properties = new Properties();
        try (final var input = Step123ReferenceEconomy.class.getResourceAsStream("/application.properties")) {
            if (input == null) {
                throw new IOException("Repository application.properties is missing");
            }
            properties.load(input);
        }
        return new Step123ReferenceEconomy(properties);
    }

    Map<String, String> values() {
        return values;
    }

    /** E(0 -> N) = sum d_l, where d_l = (1 + rollback_l * d_(l-1)) / success_l. */
    double expectedAttemptsFromZero(int targetLevel) {
        if (targetLevel < 0 || targetLevel > storm.maxPriceSupportedStateLevel()) {
            throw new IllegalArgumentException("Target must be within the configured price domain");
        }
        double increment = 0;
        double total = 0;
        for (int level = 0; level < targetLevel; level++) {
            final var probabilities = storm.probabilitiesForLevel(level);
            increment = (100.0 + probabilities.rollbackPercent() * increment)
                / probabilities.successPercent();
            total += increment;
        }
        return total;
    }

    /** Common bases and one rarity-upgrade chain; desired modifiers and storm shards are excluded. */
    long nominalGold(List<Item> items) {
        long total = 0;
        for (final var item : items) {
            total += Long.parseLong(properties.getProperty("homyakin.seeker.shop.commonPrice"))
                * 2 * item.object().slots().size();
            var rarity = ItemRarity.COMMON;
            while (rarity != item.rarity()) {
                rarity = rarity.next().orElseThrow();
                final var key = "homyakin.seeker.shop." + rarity.name().toLowerCase(Locale.ROOT) + "Price";
                total += (int) (Integer.parseInt(properties.getProperty(key)) * 1.5);
            }
        }
        return total;
    }

    /**
     * SHA-256 of all configured storm properties and every probability/cost result in the
     * price API domain, including all 127 nonempty slot masks. Source hashes supplement this
     * observable fingerprint in the evidence manifest.
     */
    String fingerprint() {
        final var transcript = new StringBuilder("Step123ReferenceEconomy/v1\n");
        new TreeMap<>(values).forEach((key, value) -> transcript.append(key).append('=').append(value).append('\n'));
        final var slots = PersonageSlot.values();
        final int maximum = storm.maxPriceSupportedStateLevel();
        transcript.append("maxPriceSupportedStateLevel=").append(maximum).append('\n');
        for (int level = 0; level < maximum; level++) {
            final var probabilities = storm.probabilitiesForLevel(level);
            transcript.append(level).append(':').append(probabilities.successPercent()).append(',')
                .append(probabilities.failurePercent()).append(',').append(probabilities.rollbackPercent()).append('\n');
            for (int mask = 1; mask < (1 << slots.length); mask++) {
                final var occupied = EnumSet.noneOf(PersonageSlot.class);
                for (int index = 0; index < slots.length; index++) {
                    if ((mask & (1 << index)) != 0) {
                        occupied.add(slots[index]);
                    }
                }
                transcript.append(mask).append('=').append(storm.costForLevel(level, occupied).value()).append('\n');
            }
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(transcript.toString().getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException("SHA-256 is required by the Java platform", impossible);
        }
    }

    private int integer(String key) {
        return Integer.parseInt(values.get(PREFIX + key));
    }

    private double decimal(String key) {
        return Double.parseDouble(values.get(PREFIX + key));
    }
}
