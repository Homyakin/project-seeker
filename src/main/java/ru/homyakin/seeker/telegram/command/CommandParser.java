package ru.homyakin.seeker.telegram.command;

import java.util.Optional;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.message.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.common.help.SelectHelp;
import ru.homyakin.seeker.telegram.command.group.action.MigrateFromGroup;
import ru.homyakin.seeker.telegram.command.group.raid.JoinRaid;
import ru.homyakin.seeker.telegram.command.group.report.RaidReportInGroup;
import ru.homyakin.seeker.telegram.command.group.settings.ChangeGroupName;
import ru.homyakin.seeker.telegram.command.group.settings.GetGroupSettings;
import ru.homyakin.seeker.telegram.command.group.settings.SetTimeZone;
import ru.homyakin.seeker.telegram.command.group.settings.ToggleEventInterval;
import ru.homyakin.seeker.telegram.command.group.settings.ToggleHideGroup;
import ru.homyakin.seeker.telegram.command.group.spin.Spin;
import ru.homyakin.seeker.telegram.command.group.duel.AcceptDuel;
import ru.homyakin.seeker.telegram.command.group.duel.DeclineDuel;
import ru.homyakin.seeker.telegram.command.group.duel.StartDuel;
import ru.homyakin.seeker.telegram.command.group.language.GroupChangeLanguage;
import ru.homyakin.seeker.telegram.command.group.action.JoinGroup;
import ru.homyakin.seeker.telegram.command.group.action.LeftGroup;
import ru.homyakin.seeker.telegram.command.group.language.GroupSelectLanguage;
import ru.homyakin.seeker.telegram.command.group.profile.GetProfileInGroup;
import ru.homyakin.seeker.telegram.command.common.help.ShowHelp;
import ru.homyakin.seeker.telegram.command.group.stats.GetGroupStats;
import ru.homyakin.seeker.telegram.command.group.stats.GetPersonageStats;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.ConsumeOrder;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.GetTavernMenu;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.Order;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.ThrowOrder;
import ru.homyakin.seeker.telegram.command.group.top.TopGroupRaidWeek;
import ru.homyakin.seeker.telegram.command.group.top.TopList;
import ru.homyakin.seeker.telegram.command.group.top.TopRaidWeek;
import ru.homyakin.seeker.telegram.command.group.top.TopRaidWeekGroup;
import ru.homyakin.seeker.telegram.command.group.top.TopSpin;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.command.user.badge.SelectBadge;
import ru.homyakin.seeker.telegram.command.user.badge.ShowBadges;
import ru.homyakin.seeker.telegram.command.user.change_name.InitChangeName;
import ru.homyakin.seeker.telegram.command.user.characteristics.CancelResetCharacteristics;
import ru.homyakin.seeker.telegram.command.user.characteristics.IncreaseCharacteristic;
import ru.homyakin.seeker.telegram.command.user.characteristics.ConfirmResetCharacteristics;
import ru.homyakin.seeker.telegram.command.user.feedback.InitFeedback;
import ru.homyakin.seeker.telegram.command.user.item.Inventory;
import ru.homyakin.seeker.telegram.command.user.item.PutOnItem;
import ru.homyakin.seeker.telegram.command.user.item.TakeOffItem;
import ru.homyakin.seeker.telegram.command.user.item.drop.ConfirmDropItem;
import ru.homyakin.seeker.telegram.command.user.item.drop.DropItem;
import ru.homyakin.seeker.telegram.command.user.item.drop.RejectDropItem;
import ru.homyakin.seeker.telegram.command.user.navigation.ReceptionDesk;
import ru.homyakin.seeker.telegram.command.user.characteristics.LevelUp;
import ru.homyakin.seeker.telegram.command.user.navigation.StartUser;
import ru.homyakin.seeker.telegram.command.user.language.UserChangeLanguage;
import ru.homyakin.seeker.telegram.command.user.language.UserSelectLanguage;
import ru.homyakin.seeker.telegram.command.user.personal_quest.GetBulletinBoard;
import ru.homyakin.seeker.telegram.command.user.personal_quest.TakePersonalQuest;
import ru.homyakin.seeker.telegram.command.user.profile.GetProfileInPrivate;
import ru.homyakin.seeker.telegram.command.user.characteristics.ResetCharacteristics;
import ru.homyakin.seeker.telegram.command.user.report.RaidReport;
import ru.homyakin.seeker.telegram.command.user.setting.ToggleHidePersonage;
import ru.homyakin.seeker.telegram.command.user.shop.BuyItem;
import ru.homyakin.seeker.telegram.command.user.shop.OpenShop;
import ru.homyakin.seeker.telegram.command.user.shop.SellItem;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.user.state.UserStateService;
import ru.homyakin.seeker.telegram.utils.TelegramUtils;

@Component
public class CommandParser {
    private final UserStateService userStateService;

    public CommandParser(UserStateService userStateService) {
        this.userStateService = userStateService;
    }

    public Optional<Command> parse(Update update) {
        final Optional<Command> command;
        if (update.hasMyChatMember()) {
            command = parseMyChatMember(update.getMyChatMember());
        } else if (update.hasMessage()) {
            command = parseMessage(update.getMessage());
        } else if (update.hasCallbackQuery()) {
            command = parseCallback(update.getCallbackQuery());
        } else {
            command = Optional.empty();
        }

        return command;
    }

    private Optional<Command> parseMyChatMember(ChatMemberUpdated chatMember) {
        if (TelegramUtils.isGroupChat(chatMember.getChat())) {
            return Optional.ofNullable(
                switch (chatMember.getNewChatMember().getStatus()) {
                    case "left" -> LeftGroup.from(chatMember);
                    case "member" -> JoinGroup.from(chatMember);
                    default -> null;
                }
            );
        } else {
            //TODO обработка сообщений из лички
            return Optional.empty();
        }
    }

    private Optional<Command> parseMessage(Message message) { //TODO разделить на group и private
        if (message.getMigrateFromChatId() != null) {
            return Optional.of(MigrateFromGroup.from(message));
        }
        if (!message.hasText()) {
            return Optional.empty();
        }
        if (message.isUserMessage()) {
            return parsePrivateMessage(message);
        } else if (TelegramUtils.isGroupMessage(message)) {
            return parseGroupMessage(message);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Command> parsePrivateMessage(Message message) {
        return userStateService
            .getUserStateById(new UserId(message.getFrom().getId()))
            .map(state -> state.nextCommand(message))
            .or(() -> commandByPrivateMessage(message));
    }

    private Optional<Command> commandByPrivateMessage(Message message) {
        return CommandType.getFromString(message.getText())
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> UserChangeLanguage.from(message);
                case START -> StartUser.from(message);
                case GET_PROFILE, BACK -> GetProfileInPrivate.from(message);
                case SHOW_HELP -> ShowHelp.from(message);
                case LEVEL_UP -> LevelUp.from(message);
                case RECEPTION_DESK -> ReceptionDesk.from(message);
                case RESET_CHARACTERISTICS -> ResetCharacteristics.from(message);
                case INIT_CHANGE_NAME -> InitChangeName.from(message);
                case RAID_REPORT -> RaidReport.from(message);
                case SHOW_BADGES -> ShowBadges.from(message);
                case INVENTORY -> Inventory.from(message);
                case PUT_ON -> PutOnItem.from(message);
                case TAKE_OFF -> TakeOffItem.from(message);
                case DROP_ITEM -> DropItem.from(message);
                case OPEN_SHOP -> OpenShop.from(message);
                case BUY_ITEM -> BuyItem.from(message);
                case SELL_ITEM -> SellItem.from(message);
                case BULLETIN_BOARD -> GetBulletinBoard.from(message);
                case TAKE_PERSONAL_QUEST -> TakePersonalQuest.from(message);
                case TOGGLE_HIDE_PERSONAGE -> ToggleHidePersonage.from(message);
                case INIT_FEEDBACK -> InitFeedback.from(message);
                default -> null;
            });
    }

    private Optional<Command> parseGroupMessage(Message message) {
        return CommandType.getFromString(message.getText().split("@")[0].split(" ")[0])
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> GroupChangeLanguage.from(message);
                case GET_PROFILE -> GetProfileInGroup.from(message);
                case SHOW_HELP -> ShowHelp.from(message);
                case START_DUEL -> StartDuel.from(message);
                case TAVERN_MENU -> GetTavernMenu.from(message);
                case ORDER -> Order.from(message);
                case GROUP_STATS -> GetGroupStats.from(message);
                case SPIN -> Spin.from(message);
                case SPIN_TOP -> TopSpin.from(message);
                case PERSONAGE_STATS -> GetPersonageStats.from(message);
                case RAID_REPORT -> RaidReportInGroup.from(message);
                case TOP_RAID_WEEK -> TopRaidWeek.from(message);
                case TOP_RAID_WEEK_GROUP -> TopRaidWeekGroup.from(message);
                case TOP -> TopList.from(message);
                case SETTINGS -> GetGroupSettings.from(message);
                case SET_TIME_ZONE -> SetTimeZone.from(message);
                case THROW_ORDER -> ThrowOrder.from(message);
                case CHANGE_GROUP_NAME -> ChangeGroupName.from(message);
                case TOGGLE_HIDE_GROUP -> ToggleHideGroup.from(message);
                case TOP_GROUP_RAID_WEEK -> TopGroupRaidWeek.from(message);
                default -> null;
            });
    }

    private Optional<Command> parseCallback(CallbackQuery callback) {
        if (callback.getMessage().isUserMessage()) {
            return parsePrivateCallback(callback);
        } else if (TelegramUtils.isGroupMessage(callback.getMessage())) {
            return parseGroupCallback(callback);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Command> parsePrivateCallback(CallbackQuery callback) {
        final var text = callback.getData().split(TextConstants.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case SELECT_LANGUAGE -> UserSelectLanguage.from(callback);
                case SELECT_HELP -> SelectHelp.from(callback);
                case CONFIRM_RESET_CHARACTERISTICS -> ConfirmResetCharacteristics.from(callback);
                case CANCEL_RESET_CHARACTERISTICS -> CancelResetCharacteristics.from(callback);
                case INCREASE_CHARACTERISTIC -> IncreaseCharacteristic.from(callback);
                case SELECT_BADGE -> SelectBadge.from(callback);
                case CONFIRM_DROP_ITEM -> ConfirmDropItem.from(callback);
                case REJECT_DROP_ITEM -> RejectDropItem.from(callback);
                default -> null;
            });
    }

    private Optional<Command> parseGroupCallback(CallbackQuery callback) {
        final var text = callback.getData().split(TextConstants.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case SELECT_LANGUAGE -> GroupSelectLanguage.from(callback);
                case JOIN_EVENT, JOIN_RAID -> JoinRaid.from(callback);
                case DECLINE_DUEL -> DeclineDuel.from(callback);
                case ACCEPT_DUEL -> AcceptDuel.from(callback);
                case SELECT_HELP -> SelectHelp.from(callback);
                case CONSUME_MENU_ITEM_ORDER -> ConsumeOrder.from(callback);
                case TOGGLE_EVENT_INTERVAL -> ToggleEventInterval.from(callback);
                default -> null;
            });
    }

}
