package ru.homyakin.seeker.locale.group;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

import ru.homyakin.seeker.game.group.entity.personage.GroupMemberDetails;
import ru.homyakin.seeker.game.group.entity.personage.GroupMembersPageResult;
import ru.homyakin.seeker.game.online.entity.PersonageLastOnline;
import ru.homyakin.seeker.game.group.passive.GroupPassiveEffect;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.group.entity.GroupProfile;
import ru.homyakin.seeker.game.group.error.JoinGroupMemberError;
import ru.homyakin.seeker.game.group.entity.GroupTaxSnapshot;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.locale.GroupMembersOnlineIndicator;
import ru.homyakin.seeker.utils.StringNamedTemplate;
import ru.homyakin.seeker.utils.TimeUtils;

public class GroupManagementLocalization {
    private static final Resources<GroupManagementResource> resources = new Resources<>();

    public static void add(Language language, GroupManagementResource resource) {
        resources.add(language, resource);
    }

    public static String groupInfo(
        Language language,
        GroupProfile group,
        GroupTaxSnapshot taxSnapshot,
        List<GroupPassiveEffect> groupPassiveEffects
    ) {
        if (group.isRegistered()) {
            return registeredGroupInfo(language, group, taxSnapshot, groupPassiveEffects);
        }
        return unregisteredGroupInfo(language, group, taxSnapshot, groupPassiveEffects);
    }

    public static String groupTaxDetails(Language language, GroupTaxSnapshot tax) {
        final var params = new HashMap<String, Object>();
        params.put("tax_level", tax.taxLevel());
        params.put("current_members", tax.memberCount());
        params.put("leaved_members", tax.leavedCount());
        params.put("duration", taxRecalcCountdownHuman(language, tax).orElse("—"));
        params.put("tax_after", tax.taxAfterNextRecalc());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupTaxDetails),
            params
        );
    }

    private static Optional<String> taxRecalcCountdownHuman(Language language, GroupTaxSnapshot tax) {
        if (tax.taxAfterNextRecalc() == tax.taxLevel()) {
            return Optional.empty();
        }
        return tax.nextRecalcAt().flatMap(nextAt -> {
            final var until = Duration.between(TimeUtils.moscowTime(), nextAt);
            if (until.isNegative() || until.isZero()) {
                return Optional.empty();
            }
            return Optional.of(CommonLocalization.duration(language, until));
        });
    }

    private static void putTaxLineParams(Language language, HashMap<String, Object> params, GroupTaxSnapshot tax) {
        params.put("tax_level", tax.taxLevel());
        final var duration = taxRecalcCountdownHuman(language, tax)
            .map(d -> " · " + d)
            .orElse("");
        params.put("duration", duration);
    }

    private static String registeredGroupInfo(
        Language language,
        GroupProfile group,
        GroupTaxSnapshot tax,
        List<GroupPassiveEffect> groupPassiveEffects
    ) {
        final var params = new HashMap<String, Object>();
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("group_stats_command", CommandType.GROUP_STATS.getText());
        params.put("outpost_command", CommandType.SHOW_OUTPOST.getText());
        params.put("group_settings_command", CommandType.SETTINGS.getText());
        params.put("group_commands_command", CommandType.GROUP_COMMANDS.getText());
        params.put("members_count", group.memberCount());
        params.put(
            "group_passive_effects",
            CommonLocalization.formatGroupInfoPassiveEffectsSection(language, groupPassiveEffects)
        );
        putTaxLineParams(language, params, tax);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::registeredGroupInfo),
            params
        );
    }

    private static String unregisteredGroupInfo(
        Language language,
        GroupProfile group,
        GroupTaxSnapshot tax,
        List<GroupPassiveEffect> groupPassiveEffects
    ) {
        final var params = new HashMap<String, Object>();
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("register_group_command", CommandType.REGISTER_GROUP.getText());
        params.put("group_stats_command", CommandType.GROUP_STATS.getText());
        params.put("outpost_command", CommandType.SHOW_OUTPOST.getText());
        params.put("group_settings_command", CommandType.SETTINGS.getText());
        params.put("group_commands_command", CommandType.GROUP_COMMANDS.getText());
        params.put("members_count", group.memberCount());
        params.put(
            "group_passive_effects",
            CommonLocalization.formatGroupInfoPassiveEffectsSection(language, groupPassiveEffects)
        );
        putTaxLineParams(language, params, tax);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::unregisteredGroupInfo),
            params
        );
    }

    public static String alreadyRegisteredGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::alreadyRegisteredGroup);
    }

    public static String groupRegistration(Language language, int requiredMonolithLevel) {
        final var params = new HashMap<String, Object>();
        params.put("register_group_command", CommandType.REGISTER_GROUP.getText());
        params.put("required_monolith_level", requiredMonolithLevel);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupRegistration),
            params
        );
    }

    public static String registrationPersonageInAnotherGroup(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::registrationPersonageInAnotherGroup);
    }

    public static String registrationPersonageNotGroupMember(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::registrationPersonageNotGroupMember),
            Collections.singletonMap("group_join_command", CommandType.JOIN_GROUP.getText())
        );
    }

    public static String incorrectTag(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::incorrectTag);
    }

    public static String tagAlreadyTaken(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::tagAlreadyTaken);
    }

    public static String registrationRequiresMonolith(Language language, int requiredMonolithLevel) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::registrationRequiresMonolith),
            Collections.singletonMap("required_monolith_level", requiredMonolithLevel)
        );
    }

    public static String successGroupRegistration(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::successGroupRegistration),
            Collections.singletonMap("group_join_command", CommandType.JOIN_GROUP.getText())
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
        if (!group.isRegistered()) {
            return leaveGroupSuccess(language, personage, joinTimeout);
        }
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

    public static String groupCommands(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("group_join_command", CommandType.JOIN_GROUP.getText());
        params.put("group_leave_command", CommandType.LEAVE_GROUP.getText());
        params.put("change_tag_command", CommandType.CHANGE_TAG.getText());
        params.put("group_tax_command", CommandType.GROUP_TAX.getText());
        params.put("group_members_command", CommandType.GROUP_MEMBERS.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupCommands),
            params
        );
    }

    public static String groupMembersLine(Language language, String onlineStatusEmoji, PersonageLastOnline personage) {
        final var params = new HashMap<String, Object>();
        params.put("online_status_emoji", onlineStatusEmoji);
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put(
            "group_member_command",
            CommandType.GROUP_MEMBER.getText() + TextConstants.TG_COMMAND_DELIMITER + personage.id().value()
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupMembersLine),
            params
        );
    }

    public static String groupMembersList(
        Language language,
        GroupMembersPageResult result
    ) {
        if (result.totalMembers() == 0) {
            return resources.getOrDefault(language, GroupManagementResource::groupMembersEmpty);
        }
        final var now = TimeUtils.moscowTime();
        final var memberLines = result.rows().stream()
            .map(row -> {
                final var age = Duration.between(row.lastOnline(), now);
                final var onlineType = result.onlineConvertor().apply(age);
                return groupMembersLine(
                    language,
                    GroupMembersOnlineIndicator.emoji(onlineType),
                    row
                );
            })
            .toList();
        final var params = new HashMap<String, Object>();
        params.put("range_from", result.rangeFrom());
        params.put("range_to", result.rangeTo());
        params.put("total", result.totalMembers());
        params.put("group_members_lines", String.join("\n", memberLines));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupMembersList),
            params
        );
    }

    public static String groupMembersPaginationPrevButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::groupMembersPaginationPrevButton);
    }

    public static String groupMembersPaginationNextButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::groupMembersPaginationNextButton);
    }

    public static String groupMemberNotFound(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::groupMemberNotFound);
    }

    public static String groupMemberProfileCard(
        Language language,
        GroupMemberDetails details,
        Characteristics equippedCharacteristics
    ) {
        final var now = TimeUtils.moscowTime();
        final var today = now.toLocalDate();
        final var personageLine = groupMemberOnline(language, details.lastOnline().personageLastOnline(), now);
        final var groupLine = details.lastOnline().membershipLastOnline()
            .map(mt -> groupMemberOnline(language, mt, now))
            .orElseGet(() -> resources.getOrDefault(language, GroupManagementResource::groupMemberNeverOnlineInGroup));
        final var params = new HashMap<String, Object>();
        params.put("short_profile", CommonLocalization.shortProfile(language, details.personage(), equippedCharacteristics));
        params.put("duration_since_personage_online", personageLine);
        params.put("duration_since_group_personage_online", groupLine);
        params.put("online_streak_icon", Icons.ONLINE_STREAK);
        params.put("personage_online_streak", details.personage().onlineStreak().effective(today));
        params.put(
            "group_online_streak",
            details.lastOnline().membershipStreak()
                .map(streak -> streak.effective(today))
                .orElse(0)
        );
        params.put(
            "kick_command",
            CommandType.GROUP_KICK.getText() + TextConstants.TG_COMMAND_DELIMITER + details.personage().id().value()
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::groupMemberProfileCard),
            params
        );
    }

    public static String kickConfirmationRequired(Language language, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::kickConfirmationRequired),
            params
        );
    }

    public static String kickConfirmButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::kickConfirmButton);
    }

    public static String kickCancelButton(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::kickCancelButton);
    }

    public static String kickConfirmed(Language language, Personage personage, Personage admin, Duration joinTimeout) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        params.put("admin_badge_with_name", LocaleUtils.personageNameWithBadge(admin));
        params.put("duration", CommonLocalization.duration(language, joinTimeout));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::kickConfirmed),
            params
        );
    }

    public static String kickCanceled(Language language, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("personage_badge_with_name", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::kickCanceled),
            params
        );
    }

    public static String kickNotMember(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::kickNotMember);
    }

    public static String kickPrivateMessage(Language language, Group group, Personage admin, Duration joinTimeout) {
        final var params = new HashMap<String, Object>();
        params.put("group_name_with_badge", LocaleUtils.groupNameWithBadge(group));
        params.put("admin_badge_with_name", LocaleUtils.personageNameWithBadge(admin));
        params.put("duration", CommonLocalization.duration(language, joinTimeout));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, GroupManagementResource::kickPrivateMessage),
            params
        );
    }

    public static String kickCannotKickSelf(Language language) {
        return resources.getOrDefault(language, GroupManagementResource::kickCannotKickSelf);
    }

    private static String groupMemberOnline(Language language, LocalDateTime past, LocalDateTime now) {
        var elapsed = Duration.between(past, now);
        if (elapsed.isNegative()) {
            elapsed = Duration.ZERO;
        }
        if (elapsed.compareTo(Duration.ofMinutes(10)) < 0) {
            return CommonLocalization.durationJustNow(language);
        }
        return CommonLocalization.durationWithDays(language, elapsed);
    }
}
