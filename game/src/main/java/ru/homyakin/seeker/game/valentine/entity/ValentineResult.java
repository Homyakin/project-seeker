package ru.homyakin.seeker.game.valentine.entity;

import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;

public sealed interface ValentineResult {

    Personage sender();

    Personage receiver();

    Money goldCost();

    int energyCost();

    boolean senderBadgeAwarded();

    boolean receiverBadgeAwarded();

    record SameGroup(
        Personage sender,
        Personage receiver,
        Money goldCost,
        int energyCost,
        boolean senderBadgeAwarded,
        boolean receiverBadgeAwarded
    ) implements ValentineResult {
    }

    record OtherGroup(
        Personage sender,
        Personage receiver,
        Group senderGroup,
        Group targetGroup,
        Money goldCost,
        int energyCost,
        boolean senderBadgeAwarded,
        boolean receiverBadgeAwarded
    ) implements ValentineResult {
    }

    record RandomInGroup(
        Personage sender,
        Personage receiver,
        Group senderGroup,
        Group targetGroup,
        Money goldCost,
        int energyCost,
        boolean senderBadgeAwarded,
        boolean receiverBadgeAwarded
    ) implements ValentineResult {
    }
}
