package ru.homyakin.seeker.locale.group;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class GroupManagementLocalization {
    private static final Resources<GroupManagementResource> resources = new Resources<>();

    public static void add(Language language, GroupManagementResource resource) {
        resources.add(language, resource);
    }

    public static String groupInfo(Language language, GroupProfile group) {
        if (group.isRegistered()) {
            return registeredGroupInfo(language, group);
        }
        return unregisteredGroupInfo(language, group);
    }

    private static String registeredGroupInfo(Language language, GroupProfile group) {
        final var params = new HashMap<String, Object>();
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("money", group.money().value());
        params.put("money_icon", Icons.MONEY);
        params.put("group_stats_command", CommandType.GROUP_STATS.getText());
        params.put("group_settings_command", CommandType.SETTINGS.getText());
        params.put("group_commands_command", CommandType.GROUP_COMMANDS.getText());
        params.put("members_count", group.memberCount());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::registeredGroupInfo),
            params
        );
    }

    private static String unregisteredGroupInfo(Language language, GroupProfile group) {
        final var params = new HashMap<String, Object>();
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("money", group.money().value());
        params.put("money_icon", Icons.MONEY);
        params.put("register_group_command", CommandType.REGISTER_GROUP.getText());
        params.put("group_stats_command", CommandType.GROUP_STATS.getText());
        params.put("group_settings_command", CommandType.SETTINGS.getText());
        params.put("donate_money_command", CommandType.DONATE_MONEY.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::unregisteredGroupInfo),
            params
        );
    }

    public static String alreadyRegisteredGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::alreadyRegisteredGroup);
    }

    public static String groupRegistration(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("register_group_command", CommandType.REGISTER_GROUP.getText());
        params.put("money", money.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupRegistration),
            params
        );
    }

    public static String registrationPersonageInAnotherGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::registrationPersonageInAnotherGroup);
    }

    public static String notEnoughMoneyForGroupRegistration(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money", money.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::notEnoughMoneyForGroupRegistration),
            params
        );
    }

    public static String incorrectTag(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::incorrectTag);
    }

    public static String tagAlreadyTaken(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::tagAlreadyTaken);
    }

    public static String successGroupRegistration(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successGroupRegistration),
            Collections.singletonMap("group_join_command", CommandType.JOIN_GROUP.getText())
        );
    }

    public static String notEnoughMoneyForChangeTag(Language language, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("money", money.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::notEnoughMoneyForChangeTag),
            params
        );
    }

    public static String successChangeTag(Language language, String tag) {
        final var params = new HashMap<String, Object>();
        params.put("tag", tag);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successChangeTag),
            params
        );
    }

    public static String joinPersonageAlreadyInGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::joinPersonageAlreadyInGroup);
    }

    public static String joinPersonageInAnotherGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::joinPersonageInAnotherGroup);
    }

    public static String groupNotRegisteredAtJoin(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupNotRegisteredAtJoin),
            Collections.singletonMap("register_group_command", CommandType.REGISTER_GROUP.getText())
        );
    }

    public static String joinPersonageTimeout(Language language, JoinGroupMemberError.PersonageJoinTimeout error) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::joinPersonageTimeout),
            Collections.singletonMap("duration", CommonLocalization.duration(language, error.remain()))
        );
    }

    public static String successJoinGroup(Language language, Personage personage, Group group) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successJoinGroup),
            params
        );
    }

    public static String joinPersonageConfirmationRequired(Language language, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::joinPersonageConfirmationRequired),
            params
        );
    }

    public static String joinGroupConfirmButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::joinGroupConfirmButton);
    }

    public static String joinGroupCancelButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::joinGroupCancelButton);
    }

    public static String joinPersonageConfirmed(Language language, Personage personage, Personage admin) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("admin_badge_with_name", LocaleUtils.personageNameWithBadge(admin));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::joinPersonageConfirmed),
            params
        );
    }

    public static String joinPersonageCanceled(Language language, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::joinPersonageCanceled),
            params
        );
    }

    public static String joinConfirmNotMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::joinConfirmNotMember);
    }

    public static String leaveGroupSuccess(Language language, Personage personage, Duration joinTimeout) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("duration", CommonLocalization.duration(language, joinTimeout));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::leaveGroupSuccess),
            params
        );
    }

    public static String leaveGroupNotMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupNotMember);
    }

    public static String leaveGroupNotAnyMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupNotAnyMember);
    }

    public static String leaveGroupLastMemberConfirmation(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupLastMemberConfirmation);
    }

    public static String leaveGroupConfirmButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupConfirmButton);
    }

    public static String leaveGroupCancelButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupCancelButton);
    }

    public static String leaveGroupLastMemberSuccess(
        Language language,
        Personage personage,
        Group group,
        Duration joinTimeout
    ) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("duration", CommonLocalization.duration(language, joinTimeout));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::leaveGroupLastMemberSuccess),
            params
        );
    }

    public static String leaveGroupCancel(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::leaveGroupCancel);
    }

    public static String successDonate(Language language, Personage personage, Money money) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("money", money.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successDonate),
            params
        );
    }

    public static String notEnoughMoneyForDonate(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::notEnoughMoneyForDonate);
    }

    public static String successGiveMoney(
        Language language,
        Personage giver,
        Personage acceptor,
        Money money
    ) {
        final var params = new HashMap<String, Object>();
        params.put("giver_badge_with_name", LocaleUtils.personageNameWithBadge(giver));
        params.put("acceptor_badge_with_name", LocaleUtils.personageNameWithBadge(acceptor));
        params.put("money", money.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successGiveMoney),
            params
        );
    }

    public static String notEnoughMoneyForGive(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::notEnoughMoneyForGive);
    }

    public static String giveMoneyPersonageNotMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::giveMoneyPersonageNotMember);
    }

    public static String giverNotMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::giverNotMember);
    }

    public static String incorrectAcceptor(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("give_money_command", CommandType.GIVE_MONEY.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::incorrectAcceptor),
            params
        );
    }

    public static String acceptorNotFound(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::acceptorNotFound);
    }

    public static String incorrectAmount(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::incorrectAmount);
    }

    public static String groupCommands(Language language, Money changeTagPrice) {
        final var params = new HashMap<String, Object>();
        params.put("group_join_command", CommandType.JOIN_GROUP.getText());
        params.put("group_leave_command", CommandType.LEAVE_GROUP.getText());
        params.put("donate_money_command", CommandType.DONATE_MONEY.getText());
        params.put("give_money_command", CommandType.GIVE_MONEY.getText());
        params.put("change_tag_command", CommandType.CHANGE_TAG.getText());
        params.put("price", changeTagPrice.value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupCommands),
            params
        );
    }
}
