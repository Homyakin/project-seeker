package ru.homyakin.seeker.game.personage.models;

import io.vavr.control.Either;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import ru.homyakin.seeker.game.battle.BattlePersonage;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.item.models.PutOnItemError;
import ru.homyakin.seeker.game.item.models.TakeOffItemError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.errors.EnergyStillSame;
import ru.homyakin.seeker.game.personage.models.errors.NameError;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughLevelingPoints;
import ru.homyakin.seeker.game.personage.models.errors.NotEnoughMoney;
import ru.homyakin.seeker.game.personage.models.errors.PersonageEventError;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.personal.CharacteristicLocalization;
import ru.homyakin.seeker.utils.models.Success;

public record Personage(
    PersonageId id,
    String name,
    Money money,
    Characteristics characteristics,
    Energy energy,
    BadgeView badge,
    Characteristics itemCharacteristics
) {
    public Personage addMoney(Money money) {
        return new Personage(
            id,
            name,
            this.money.add(money),
            characteristics,
            energy,
            badge,
            itemCharacteristics
        );
    }

    public Either<NameError, Personage> changeName(String name) {
        return validateName(name).map(this::copyWithName);
    }

    public static Either<NameError, String> validateName(String name) {
        if (name.length() < MIN_NAME_LENGTH || name.length() > MAX_NAME_LENGTH) {
            return Either.left(new NameError.InvalidLength(MIN_NAME_LENGTH, MAX_NAME_LENGTH));
        }
        if (!NAME_PATTERN.matcher(name).matches()) {
            return Either.left(new NameError.NotAllowedSymbols());
        }
        return Either.right(name);
    }

    public String shortProfile(Language language) {
        return CommonLocalization.shortProfile(language, this);
    }

    public String fullProfile(Language language) {
        final var profile = CommonLocalization.fullProfile(language, this);

        return characteristics.hasUnspentLevelingPoints()
            ? CharacteristicLocalization.profileLevelUp(language) + "\n\n" + profile : profile;
    }

    public Characteristics calcTotalCharacteristics() {
        return characteristics.add(itemCharacteristics);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementStrength() {
        return characteristics.incrementStrength().map(this::copyWithCharacteristics);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementAgility() {
        return characteristics.incrementAgility().map(this::copyWithCharacteristics);
    }

    public Either<NotEnoughLevelingPoints, Personage> incrementWisdom() {
        return characteristics.incrementWisdom().map(this::copyWithCharacteristics);
    }

    public Either<EnergyStillSame, Personage> regenEnergyIfNeed() {
        return energy
            .regenIfNeeded()
            .map(this::copyWithEnergy);
    }

    public Personage nullifyEnergy(LocalDateTime energyChangeTime) {
        return copyWithEnergy(Energy.createZero(energyChangeTime));
    }

    public String badgeWithName() {
        return badge().icon() + name;
    }

    public Either<NotEnoughMoney, Personage> resetStats() {
        if (money.lessThan(RESET_STATS_COST)) {
            return Either.left(new NotEnoughMoney(RESET_STATS_COST));
        }

        return Either.right(
            addMoney(RESET_STATS_COST.negative())
                .copyWithCharacteristics(characteristics.reset())
        );
    }

    public Either<PersonageEventError, Success> hasEnoughEnergyForEvent() {
        return energy.isEnoughForEvent()
            .mapLeft(PersonageEventError.NotEnoughEnergy::new);
    }

    public int maxBagSize() {
        return MAX_BAG_SIZE;
    }

    public boolean hasSpaceInBag(List<Item> items) {
        return items.size() < maxBagSize();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Personage other) {
            return this.id == other.id;
        }
        return false;
    }

    public BattlePersonage toBattlePersonage() {
        return new BattlePersonage(
            id.value(),
            calcTotalCharacteristics(),
            this
        );
    }

    public BattlePersonage toBattlePersonageUsingEnergy() {
        return new BattlePersonage(
            id.value(),
            calcTotalCharacteristics().multiply(energy),
            this
        );
    }

    public Either<PutOnItemError, Success> canPutOnItem(List<Item> personageItems, Item item) {
        if (!item.personageId().map(it -> it.equals(id)).orElse(false)) {
            return Either.left(PutOnItemError.PersonageMissingItem.INSTANCE);
        }
        if (item.isEquipped()) {
            return Either.left(PutOnItemError.AlreadyEquipped.INSTANCE);
        }

        final var personageBusySlots = new HashMap<PersonageSlot, Integer>();
        for (final var slot : item.object().slots()) {
            personageBusySlots.computeIfPresent(slot, (k, v) -> v + 1);
            personageBusySlots.putIfAbsent(slot, 1);
        }
        for (final var personageItem : personageItems) {
            if (personageItem.isEquipped()) {
                for (final var slot : personageItem.object().slots()) {
                    personageBusySlots.computeIfPresent(slot, (k, v) -> v + 1);
                }
            }
        }
        final var missingSlots = new ArrayList<PersonageSlot>();
        for (final var entry : personageBusySlots.entrySet()) {
            if (entry.getValue() > personageAvailableSlots.getOrDefault(entry.getKey(), 0)) {
                missingSlots.add(entry.getKey());
            }
        }
        if (missingSlots.isEmpty()) {
            return Either.right(Success.INSTANCE);
        }
        return Either.left(new PutOnItemError.RequiredFreeSlots(missingSlots));
    }

    public Either<TakeOffItemError, Success> canTakeOffItem(List<Item> personageItems, Item item) {
        if (!item.personageId().map(it -> it.equals(id)).orElse(false)) {
            return Either.left(TakeOffItemError.PersonageMissingItem.INSTANCE);
        }
        if (!item.isEquipped()) {
            return Either.left(TakeOffItemError.AlreadyTakenOff.INSTANCE);
        }
        int itemsInBag = 0;
        for (final var personageItem : personageItems) {
            if (personageItem.isEquipped()) {
                ++itemsInBag;
            }
        }

        if (itemsInBag >= maxBagSize()) {
            return Either.left(TakeOffItemError.NotEnoughSpaceInBag.INSTANCE);
        }
        return Either.right(Success.INSTANCE);
    }

    public List<PersonageSlot> getFreeSlots(List<Item> personageItems) {
        final var freeSlots = new HashMap<>(personageAvailableSlots);
        for (final var item : personageItems) {
            if (item.isEquipped()) {
                for (final var slot : item.object().slots()) {
                    freeSlots.computeIfPresent(slot, (k, v) -> v - 1);
                }
            }
        }
        final var result = new ArrayList<PersonageSlot>();
        for (final var entry : freeSlots.entrySet()) {
            for (int i = 0; i < entry.getValue(); ++i) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public static Personage createDefault() {
        return createDefault(TextConstants.DEFAULT_NAME);
    }

    public static Personage createDefault(String name) {
        return new Personage(
            PersonageId.from(0L),
            name,
            Money.from(10),
            Characteristics.createDefault(),
            Energy.createDefault(),
            BadgeView.STANDARD,
            Characteristics.createZero()
        );
    }

    private static final int MIN_NAME_LENGTH = 3;
    private static final int MAX_NAME_LENGTH = 25;
    private static final int MAX_BAG_SIZE = 10;
    private static final String CYRILLIC = "а-яА-ЯёЁ";
    private static final String ENGLISH = "a-zA-Z";
    private static final String NUMBERS = "0-9";
    private static final String SPECIAL = "_\\-\\.#№ ";
    private static final Pattern NAME_PATTERN = Pattern.compile("[" + CYRILLIC + ENGLISH + NUMBERS + SPECIAL + "]+");
    public static final Money RESET_STATS_COST = new Money(50);
    private static final Map<PersonageSlot, Integer> personageAvailableSlots = new HashMap<>() {{
        put(PersonageSlot.MAIN_HAND, 1);
        put(PersonageSlot.OFF_HAND, 1);
    }};

    private Personage copyWithName(String name) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy,
            badge,
            itemCharacteristics
        );
    }

    private Personage copyWithCharacteristics(Characteristics characteristics) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy,
            badge,
            itemCharacteristics
        );
    }

    private Personage copyWithEnergy(Energy energy) {
        return new Personage(
            id,
            name,
            money,
            characteristics,
            energy,
            badge,
            itemCharacteristics
        );
    }
}
