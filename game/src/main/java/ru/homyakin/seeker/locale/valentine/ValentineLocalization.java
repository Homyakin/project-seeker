package ru.homyakin.seeker.locale.valentine;

import java.util.Collections;
import java.util.HashMap;

import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.valentine.entity.ValentineConfig;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class ValentineLocalization {
    private static final Resources<ValentineResource> resources = new Resources<>();

    public static void add(Language language, ValentineResource resource) {
        resources.add(language, resource);
    }

    public static String helpLove(Language language, ValentineConfig config) {
        final var params = new HashMap<String, Object>();
        params.put("love_command", CommandType.SEND_VALENTINE.getText());
        params.put("money_icon", Icons.MONEY);
        params.put("energy_icon", Icons.ENERGY);
        params.put("same_group_gold", config.sameGroupGoldCost());
        params.put("same_group_energy", config.sameGroupEnergyCost());
        params.put("other_group_gold", config.otherGroupGoldCost());
        params.put("other_group_energy", config.otherGroupEnergyCost());
        params.put("random_group_gold", config.randomGroupGoldCost());
        params.put("random_group_energy", config.randomGroupEnergyCost());
        params.put("badge_threshold", config.badgeThreshold());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::helpLove),
            params
        );
    }

    public static String sameGroupResult(
        Language language,
        PersonageMention sender,
        PersonageMention receiver,
        int moneyValue,
        int energyValue,
        boolean senderBadge,
        boolean receiverBadge
    ) {
        final var textParams = new HashMap<String, Object>();
        textParams.put("sender", sender.value());
        textParams.put("receiver", receiver.value());
        final var valentineText = StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, ValentineResource::sameGroupText),
            textParams
        );
        var result = wrapSenderResult(language, valentineText, moneyValue, energyValue);
        if (senderBadge) {
            result += badgeAwarded(language, sender);
        }
        if (receiverBadge) {
            result += badgeAwarded(language, receiver);
        }
        return result;
    }

    public static String otherGroupResult(
        Language language,
        PersonageMention sender,
        PersonageMention receiver,
        Group targetGroup,
        int moneyValue,
        int energyValue,
        boolean senderBadge
    ) {
        final var textParams = new HashMap<String, Object>();
        textParams.put("sender", sender.value());
        textParams.put("receiver", receiver.value());
        textParams.put("target_group", LocaleUtils.groupNameWithBadge(targetGroup));
        final var valentineText = StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, ValentineResource::otherGroupText),
            textParams
        );
        var result = wrapSenderResult(language, valentineText, moneyValue, energyValue);
        if (senderBadge) {
            result += badgeAwarded(language, sender);
        }
        return result;
    }

    public static String randomGroupResult(
        Language language,
        PersonageMention sender,
        Group targetGroup,
        int moneyValue,
        int energyValue,
        boolean senderBadge
    ) {
        final var textParams = new HashMap<String, Object>();
        textParams.put("sender", sender.value());
        textParams.put("target_group", LocaleUtils.groupNameWithBadge(targetGroup));
        final var valentineText = StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, ValentineResource::randomGroupText),
            textParams
        );
        var result = wrapSenderResult(language, valentineText, moneyValue, energyValue);
        if (senderBadge) {
            result += badgeAwarded(language, sender);
        }
        return result;
    }

    public static String receivedFromOtherGroup(
        Language language,
        PersonageMention receiver,
        Group fromGroup,
        boolean receiverBadge
    ) {
        final var textParams = new HashMap<String, Object>();
        textParams.put("receiver", receiver.value());
        textParams.put("from_group", LocaleUtils.groupNameWithBadge(fromGroup));
        final var valentineText = StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, ValentineResource::receivedFromOtherGroupText),
            textParams
        );
        var result = StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::receiverResult),
            Collections.singletonMap("valentine_text", valentineText)
        );
        if (receiverBadge) {
            result += badgeAwarded(language, receiver);
        }
        return result;
    }

    private static String wrapSenderResult(Language language, String valentineText, int moneyValue, int energyValue) {
        final var params = new HashMap<String, Object>();
        params.put("valentine_text", valentineText);
        params.put("money_value", moneyValue);
        params.put("money_icon", Icons.MONEY);
        params.put("energy_value", energyValue);
        params.put("energy_icon", Icons.ENERGY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::senderResult),
            params
        );
    }

    private static String badgeAwarded(Language language, PersonageMention personage) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::badgeAwarded),
            Collections.singletonMap("personage", personage.value())
        );
    }

    public static String notRegisteredGroup(Language language) {
        return resources.getOrDefault(language, ValentineResource::notRegisteredGroup);
    }

    public static String notGroupMember(Language language) {
        return resources.getOrDefault(language, ValentineResource::notGroupMember);
    }

    public static String notEnoughMoney(Language language, int requiredMoney) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("required_money", requiredMoney);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::notEnoughMoney),
            params
        );
    }

    public static String notEnoughEnergy(Language language, int requiredEnergy) {
        final var params = new HashMap<String, Object>();
        params.put("energy_icon", Icons.ENERGY);
        params.put("required_energy", requiredEnergy);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, ValentineResource::notEnoughEnergy),
            params
        );
    }

    public static String noTarget(Language language) {
        return resources.getOrDefault(language, ValentineResource::noTarget);
    }

    public static String targetGroupNotFound(Language language) {
        return resources.getOrDefault(language, ValentineResource::targetGroupNotFound);
    }

    public static String targetGroupNotActive(Language language) {
        return resources.getOrDefault(language, ValentineResource::targetGroupNotActive);
    }

    public static String targetGroupIsEmpty(Language language) {
        return resources.getOrDefault(language, ValentineResource::targetGroupIsEmpty);
    }

    public static String userNotFound(Language language) {
        return resources.getOrDefault(language, ValentineResource::userNotFound);
    }

    public static String cannotSendToSelf(Language language) {
        return resources.getOrDefault(language, ValentineResource::cannotSendToSelf);
    }

    public static String receiverNotRegistered(Language language) {
        return resources.getOrDefault(language, ValentineResource::receiverNotRegistered);
    }

    public static String receiverNotInTargetGroup(Language language) {
        return resources.getOrDefault(language, ValentineResource::receiverNotInTargetGroup);
    }

    public static String receiverNotActiveInGroup(Language language) {
        return resources.getOrDefault(language, ValentineResource::receiverNotActiveInGroup);
    }

    public static String sendToThisGroup(Language language) {
        return resources.getOrDefault(language, ValentineResource::sendToThisGroup);
    }
}
