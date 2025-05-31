package ru.homyakin.seeker.telegram.command.group.tavern_menu;

import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.tavern_menu.order.ThrowOrderToGroupCommand;
import ru.homyakin.seeker.locale.tavern_menu.TavernMenuLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class ThrowOrderToGroupExecutor extends CommandExecutor<ThrowOrderToGroup> {
    private final GroupUserService groupUserService;
    private final GroupTgService groupTgService;
    private final UserService userService;
    private final TelegramSender telegramSender;
    private final ThrowOrderToGroupCommand throwOrderToGroupCommand;

    public ThrowOrderToGroupExecutor(
        GroupUserService groupUserService,
        GroupTgService groupTgService,
        UserService userService,
        TelegramSender telegramSender,
        ThrowOrderToGroupCommand throwOrderToGroupCommand
    ) {
        this.groupUserService = groupUserService;
        this.groupTgService = groupTgService;
        this.userService = userService;
        this.telegramSender = telegramSender;
        this.throwOrderToGroupCommand = throwOrderToGroupCommand;
    }

    @Override
    public void execute(ThrowOrderToGroup command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var throwing = groupUser.second();
        if (command.tag().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .text(TavernMenuLocalization.throwToGroupMissingGroup(group.language()))
                    .build()
            );
            return;
        }

        final var result = throwOrderToGroupCommand.execute(
            throwing.personageId(),
            group.domainGroupId(),
            command.tag().get()
        );
        final var text = result.fold(
            error -> TavernMenuLocalization.throwToGroupError(group.language(), error),
            success -> TavernMenuLocalization.throwToGroupResult(group.language(), success)
        );

        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(group.id())
                .text(text)
                .build()
        );

        if (result.isRight()) {
            final var targetGroup = groupTgService.forceGet(result.get().targetGroup().id());
            final var targetUser = userService.getByPersonageIdForce(result.get().target().id());

            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(targetGroup.id())
                    .text(TavernMenuLocalization.throwFromGroupResult(
                        targetGroup.language(),
                        result.get().throwingGroup(),
                        TgPersonageMention.of(result.get().target(), targetUser.id()),
                        result.get().effect(),
                        result.get().category()
                    ))
                    .build()
            );
        }
    }
}
