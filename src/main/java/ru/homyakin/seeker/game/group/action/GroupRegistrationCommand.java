package ru.homyakin.seeker.game.group.action;

import io.vavr.control.Either;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.group.action.personage.CheckGroupPersonage;
import ru.homyakin.seeker.game.group.entity.GroupConfig;
import ru.homyakin.seeker.game.group.entity.GroupStorage;
import ru.homyakin.seeker.game.group.entity.personage.GroupPersonageStorage;
import ru.homyakin.seeker.game.group.error.GroupRegistrationError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.utils.models.Success;

import java.util.regex.Pattern;

@Component
public class GroupRegistrationCommand {
    private final GroupStorage groupStorage;
    private final GroupPersonageStorage groupPersonageStorage;
    private final CheckGroupPersonage checkGroupPersonage;
    private final GroupConfig config;

    public GroupRegistrationCommand(
        GroupStorage groupStorage,
        GroupPersonageStorage groupPersonageStorage,
        CheckGroupPersonage checkGroupPersonage,
        GroupConfig config
    ) {
        this.groupStorage = groupStorage;
        this.groupPersonageStorage = groupPersonageStorage;
        this.checkGroupPersonage = checkGroupPersonage;
        this.config = config;
    }

    public Either<GroupRegistrationError, Success> execute(
        GroupId groupId,
        PersonageId personageId,
        String tag
    ) {
        final var group = groupStorage.get(groupId).orElseThrow();
        if (group.isRegistered()) {
            return Either.left(GroupRegistrationError.GroupAlreadyRegistered.INSTANCE);
        }
        if (group.isHidden()) {
            return Either.left(GroupRegistrationError.HiddenGroup.INSTANCE);
        }
        if (groupPersonageStorage.getPersonageMemberGroup(personageId).hasGroup()) {
            return Either.left(GroupRegistrationError.PersonageInAnotherGroup.INSTANCE);
        }
        final var profile = groupStorage.getProfile(groupId).orElseThrow();
        if (profile.money().lessThan(config.registrationPrice())) {
            return Either.left(new GroupRegistrationError.NotEnoughMoney(config.registrationPrice()));
        }
        if (!validateTag(tag)) {
            return Either.left(GroupRegistrationError.InvalidTag.INSTANCE);
        }
        if (groupStorage.isTagExists(tag)) {
            return Either.left(GroupRegistrationError.TagAlreadyTaken.INSTANCE);
        }
        if (!checkGroupPersonage.isAdminInGroup(groupId, personageId)) {
            return Either.left(GroupRegistrationError.NotAdmin.INSTANCE);
        }

        groupStorage.setTagAndTakeMoney(groupId, tag, config.registrationPrice());
        groupPersonageStorage.setMemberGroup(personageId, groupId);
        return Either.right(Success.INSTANCE);
    }

    private boolean validateTag(String tag) {
        return tagPattern.matcher(tag).matches();
    }

    private static final Pattern tagPattern = Pattern.compile("^[A-Z]{3,4}$");
}
