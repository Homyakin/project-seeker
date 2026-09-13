package ru.homyakin.seeker.game.item.database;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import ru.homyakin.seeker.game.item.models.AttackType;
import ru.homyakin.seeker.game.item.models.ItemAttack;
import ru.homyakin.seeker.utils.JsonUtils;

class ItemAttackPersistenceCodecTest {
    @Test
    void Given_AttackParts_When_SerializingForDatabase_Then_RoundTripPreservesEveryPart() {
        final var attacks = List.of(
            new ItemAttack(AttackType.PIERCE, 1, 3, 300),
            new ItemAttack(AttackType.PIERCE, 3, 3, 120),
            new ItemAttack(AttackType.MAGICAL, 2, 4, 180)
        );
        final var jsonUtils = new JsonUtils();

        final var json = jsonUtils.mapToPostgresJson(attacks);
        final var restored = jsonUtils.fromString(json.getValue(), JsonUtils.ITEM_ATTACKS);

        Assertions.assertEquals(attacks, restored);
    }
}
