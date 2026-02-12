package ru.homyakin.seeker.telegram.command.group.valentine;

import java.util.Optional;

import org.springframework.stereotype.Component;

import io.vavr.control.Either;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.valentine.action.ValentineService;
import ru.homyakin.seeker.game.valentine.entity.ValentineError;
import ru.homyakin.seeker.game.valentine.entity.ValentineResult;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.locale.valentine.ValentineLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.GroupUserService;
import ru.homyakin.seeker.telegram.group.models.GroupTg;
import ru.homyakin.seeker.telegram.models.MentionInfo;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.telegram.models.UserType;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.User;
import ru.homyakin.seeker.telegram.utils.SendMessageBuilder;

@Component
public class SendValentineExecutor extends CommandExecutor<SendValentine> {
    private final GroupUserService groupUserService;
    private final GroupTgService groupTgService;
    private final UserService userService;
    private final ValentineService valentineService;
    private final TelegramSender telegramSender;

    public SendValentineExecutor(
        GroupUserService groupUserService,
        GroupTgService groupTgService,
        UserService userService,
        ValentineService valentineService,
        TelegramSender telegramSender
    ) {
        this.groupUserService = groupUserService;
        this.groupTgService = groupTgService;
        this.userService = userService;
        this.valentineService = valentineService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SendValentine command) {
        final var groupUser = groupUserService.getAndActivateOrCreate(command.groupId(), command.userId());
        final var group = groupUser.first();
        final var senderUser = groupUser.second();

        if (command.tag().isPresent()) {
            executeCrossGroup(command, group, senderUser);
        } else {
            executeSameGroup(command, group, senderUser);
        }
    }

    private Optional<User> resolveTargetUser(MentionInfo mentionInfo) {
        if (mentionInfo.userType() != UserType.USER) {
            return Optional.empty();
        }
        return userService.getByMention(mentionInfo);
    }

    private void executeSameGroup(
        SendValentine command,
        GroupTg group,
        User senderUser
    ) {
        if (command.mentionInfo().isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .text(ValentineLocalization.noTarget(group.language()))
                    .build()
            );
            return;
        }

        final var targetUserOpt = resolveTargetUser(command.mentionInfo().get());
        if (targetUserOpt.isEmpty()) {
            telegramSender.send(
                SendMessageBuilder.builder()
                    .chatId(group.id())
                    .text(ValentineLocalization.userNotFound(group.language()))
                    .build()
            );
            return;
        }

        final var result = valentineService.sendToSameGroup(
            senderUser.personageId(),
            targetUserOpt.get().personageId(),
            group.domainGroupId()
        );

        result.fold(
            error -> {
                telegramSender.send(
                    SendMessageBuilder.builder()
                        .chatId(group.id())
                        .text(mapError(group.language(), error))
                        .build()
                );
                return null;
            },
            success -> {
                final var senderMention = TgPersonageMention.of(success.sender(), senderUser.id());
                final var receiverMention = TgPersonageMention.of(success.receiver(), targetUserOpt.get().id());

                telegramSender.send(
                    SendMessageBuilder.builder()
                        .chatId(group.id())
                        .text(ValentineLocalization.sameGroupResult(
                            group.language(),
                            senderMention,
                            receiverMention,
                            success.goldCost().value(),
                            success.energyCost(),
                            success.senderBadgeAwarded(),
                            success.receiverBadgeAwarded()
                        ))
                        .build()
                );
                return null;
            }
        );
    }

    private void executeCrossGroup(
        SendValentine command,
        GroupTg group,
        User senderUser
    ) {
        final var tag = command.tag().orElseThrow();

        if (command.mentionInfo().isPresent()) {
            // Send to specific user in another group
            final var targetUserOpt = resolveTargetUser(command.mentionInfo().get());
            if (targetUserOpt.isEmpty()) {
                telegramSender.send(
                    SendMessageBuilder.builder()
                        .chatId(group.id())
                        .text(ValentineLocalization.userNotFound(group.language()))
                        .build()
                );
                return;
            }

            final var result = valentineService.sendToOtherGroup(
                senderUser.personageId(),
                group.domainGroupId(),
                tag,
                targetUserOpt.get().personageId()
            );

            handleCrossGroupResult(result, group, senderUser);
        } else {
            // Send to random user in another group
            final var result = valentineService.sendToRandomInGroup(
                senderUser.personageId(),
                group.domainGroupId(),
                tag
            );

            handleCrossGroupResult(result, group, senderUser);
        }
    }

    private void handleCrossGroupResult(
        Either<ValentineError, ValentineResult> result,
        GroupTg group,
        User senderUser
    ) {
        result.fold(
            error -> {
                telegramSender.send(
                    SendMessageBuilder.builder()
                        .chatId(group.id())
                        .text(mapError(group.language(), error))
                        .build()
                );
                return null;
            },
            success -> {
                final var senderMention = TgPersonageMention.of(success.sender(), senderUser.id());
                final var receiverUser = userService.getByPersonageIdForce(success.receiver().id());
                final var receiverMention = TgPersonageMention.of(success.receiver(), receiverUser.id());

                // Message in sender's group
                String senderText;
                if (success instanceof ValentineResult.RandomInGroup randomResult) {
                    senderText = ValentineLocalization.randomGroupResult(
                        group.language(),
                        senderMention,
                        randomResult.targetGroup(),
                        success.goldCost().value(),
                        success.energyCost(),
                        success.senderBadgeAwarded()
                    );
                } else if (success instanceof ValentineResult.OtherGroup otherResult) {
                    senderText = ValentineLocalization.otherGroupResult(
                        group.language(),
                        senderMention,
                        receiverMention,
                        otherResult.targetGroup(),
                        success.goldCost().value(),
                        success.energyCost(),
                        success.senderBadgeAwarded()
                    );
                } else {
                    throw new IllegalStateException("Unexpected result type: " + success.getClass());
                }
                telegramSender.send(
                    SendMessageBuilder.builder()
                        .chatId(group.id())
                        .text(senderText)
                        .build()
                );

                // Message in target group
                if (success instanceof ValentineResult.OtherGroup otherGroup) {
                    sendTargetGroupNotification(otherGroup.senderGroup(), otherGroup.targetGroup(),
                        receiverMention, success.receiverBadgeAwarded());
                } else if (success instanceof ValentineResult.RandomInGroup randomInGroup) {
                    sendTargetGroupNotification(randomInGroup.senderGroup(), randomInGroup.targetGroup(),
                        receiverMention, success.receiverBadgeAwarded());
                }

                return null;
            }
        );
    }

    private void sendTargetGroupNotification(
        Group senderGroup,
        Group targetGroup,
        TgPersonageMention receiverMention,
        boolean receiverBadgeAwarded
    ) {
        final var targetGroupTg = groupTgService.forceGet(targetGroup.id());
        telegramSender.send(
            SendMessageBuilder.builder()
                .chatId(targetGroupTg.id())
                .text(ValentineLocalization.receivedFromOtherGroup(
                    targetGroupTg.language(), receiverMention, senderGroup, receiverBadgeAwarded
                ))
                .build()
        );
    }

    private String mapError(Language language, ValentineError error) {
        return switch (error) {
            case ValentineError.NotRegisteredGroup _ -> ValentineLocalization.notRegisteredGroup(language);
            case ValentineError.NotGroupMember _ -> ValentineLocalization.notGroupMember(language);
            case ValentineError.NotEnoughMoney e -> ValentineLocalization.notEnoughMoney(language, e.required().value());
            case ValentineError.NotEnoughEnergy e -> ValentineLocalization.notEnoughEnergy(language, e.required());
            case ValentineError.TargetGroupNotFound _ -> ValentineLocalization.targetGroupNotFound(language);
            case ValentineError.TargetGroupNotActive _ -> ValentineLocalization.targetGroupNotActive(language);
            case ValentineError.TargetGroupIsEmpty _ -> ValentineLocalization.targetGroupIsEmpty(language);
            case ValentineError.CannotSendToSelf _ -> ValentineLocalization.cannotSendToSelf(language);
            case ValentineError.ReceiverNotRegistered _ -> ValentineLocalization.receiverNotRegistered(language);
            case ValentineError.ReceiverNotInTargetGroup _ -> ValentineLocalization.receiverNotInTargetGroup(language);
            case ValentineError.ReceiverNotActiveInGroup _ -> ValentineLocalization.receiverNotActiveInGroup(language);
            case ValentineError.SendToThisGroup _ -> ValentineLocalization.sendToThisGroup(language);
            case ValentineError.InternalError _ -> CommonLocalization.internalError(language);
        };
    }
}
