package ru.homyakin.seeker.telegram.command.group.management;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.group.action.GroupRegistrationCommand;
import ru.homyakin.seeker.game.group.action.InitGroupRegistrationCommand;
import ru.homyakin.seeker.game.group.error.GroupRegistrationError;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.group.GroupManagementLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class GroupRegistrationExecutor extends CommandExecutor<GroupRegistration> {
    private final GroupUserService groupUserService;
    private final InitGroupRegistrationCommand initGroupRegistrationCommand;
    private final GroupRegistrationCommand groupRegistrationCommand;
    private final TelegramSender telegramSender;

    public GroupRegistrationExecutor(
        GroupUserService groupUserService,
        InitGroupRegistrationCommand initGroupRegistrationCommand,
        GroupRegistrationCommand groupRegistrationCommand,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.initGroupRegistrationCommand = initGroupRegistrationCommand;
        this.groupRegistrationCommand = groupRegistrationCommand;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(GroupRegistration command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupTgId(), command.userId());
        final var groupTg = groupUser.first();
        final var user = groupUser.second();

        if (command.tag().isPresent()) {
            registerGroup(groupTg, user.personageId(), command.tag().get());
        } else {
            final var result = initGroupRegistrationCommand.execute(groupTg.domainGroupId());
            if (result.isLeft()) {
                telegramSender.send(
                    SendMessageBuilder
                        .builder()
                        .chatId(command.groupTgId())
                        .text(GroupManagementLocalization.alreadyRegisteredGroup(groupTg.language()))
                        .build()
                );
            } else {
                telegramSender.send(
                    SendMessageBuilder
                        .builder()
                        .chatId(command.groupTgId())
                        .text(GroupManagementLocalization.groupRegistration(groupTg.language(), result.get()))
                        .build()
                );
            }
        }
    }

    private void registerGroup(
        GroupTg groupTg,
        PersonageId personageId,
        String tag
    ) {
        final var result = groupRegistrationCommand.execute(groupTg.domainGroupId(), personageId, tag);

        final var messageBuilder = SendMessageBuilder.builder().chatId(groupTg.id());
        final var text = result.fold(
            error -> switch (error) {
                case GroupRegistrationError.GroupAlreadyRegistered _ ->
                    GroupManagementLocalization.alreadyRegisteredGroup(groupTg.language());
                case GroupRegistrationError.HiddenGroup _ ->
                    CommonLocalization.forbiddenForHiddenGroup(groupTg.language());
                case GroupRegistrationError.InvalidTag _ ->
                    GroupManagementLocalization.incorrectTag(groupTg.language());
                case GroupRegistrationError.NotEnoughMoney notEnoughMoney ->
                    GroupManagementLocalization.notEnoughMoneyForGroupRegistration(groupTg.language(), notEnoughMoney.required());
                case GroupRegistrationError.PersonageInAnotherGroup _ ->
                    GroupManagementLocalization.registrationPersonageInAnotherGroup(groupTg.language());
                case GroupRegistrationError.TagAlreadyTaken _ ->
                    GroupManagementLocalization.tagAlreadyTaken(groupTg.language());
                case GroupRegistrationError.NotAdmin _ ->
                    CommonLocalization.onlyAdminAction(groupTg.language());
            },
            _ -> GroupManagementLocalization.successGroupRegistration(groupTg.language())
        );
        messageBuilder.text(text);
        telegramSender.send(messageBuilder.build());
    }
}
