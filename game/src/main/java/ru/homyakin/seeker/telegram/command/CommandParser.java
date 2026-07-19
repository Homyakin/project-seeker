package ru.homyakin.seeker.telegram.command;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.CallbackQuery;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.chatmember.ChatMemberUpdated;
import org.telegram.telegrambots.meta.api.objects.message.Message;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.common.help.SelectHelp;
import ru.homyakin.seeker.telegram.command.common.help.ShowHelp;
import ru.homyakin.seeker.telegram.command.common.world_raid.JoinWorldRaid;
import ru.homyakin.seeker.telegram.command.common.world_raid.WorldRaidDonate;
import ru.homyakin.seeker.telegram.command.group.action.JoinGroup;
import ru.homyakin.seeker.telegram.command.group.action.LeftGroup;
import ru.homyakin.seeker.telegram.command.group.action.MigrateFromGroup;
import ru.homyakin.seeker.telegram.command.group.badge.SelectGroupBadge;
import ru.homyakin.seeker.telegram.command.group.badge.ShowGroupBadges;
import ru.homyakin.seeker.telegram.command.group.duel.AcceptDuel;
import ru.homyakin.seeker.telegram.command.group.duel.DeclineDuel;
import ru.homyakin.seeker.telegram.command.group.duel.StartDuel;
import ru.homyakin.seeker.telegram.command.group.language.GroupChangeLanguage;
import ru.homyakin.seeker.telegram.command.group.language.GroupSelectLanguage;
import ru.homyakin.seeker.telegram.command.group.management.CancelJoinGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.ChangeGroupName;
import ru.homyakin.seeker.telegram.command.group.management.ChangeGroupTag;
import ru.homyakin.seeker.telegram.command.group.management.ConfirmJoinGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.GroupCommands;
import ru.homyakin.seeker.telegram.command.group.management.GroupMember;
import ru.homyakin.seeker.telegram.command.group.management.GroupMembers;
import ru.homyakin.seeker.telegram.command.group.management.GroupInfo;
import ru.homyakin.seeker.telegram.command.group.management.GroupTax;
import ru.homyakin.seeker.telegram.command.group.management.GroupRegistration;
import ru.homyakin.seeker.telegram.command.group.management.KickGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.JoinGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.LeaveGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.LeaveGroupMemberCancel;
import ru.homyakin.seeker.telegram.command.group.management.LeaveGroupMemberConfirm;
import ru.homyakin.seeker.telegram.command.group.management.ConfirmKickGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.CancelKickGroupMember;
import ru.homyakin.seeker.telegram.command.group.management.settings.GetGroupSettings;
import ru.homyakin.seeker.telegram.command.group.management.settings.SetTimeZone;
import ru.homyakin.seeker.telegram.command.group.management.settings.ToggleEventInterval;
import ru.homyakin.seeker.telegram.command.group.management.settings.ToggleHideGroup;
import ru.homyakin.seeker.telegram.command.group.profile.GetProfileInGroup;
import ru.homyakin.seeker.telegram.command.group.raid.JoinRaid;
import ru.homyakin.seeker.telegram.command.group.report.RaidReportInGroup;
import ru.homyakin.seeker.telegram.command.group.outpost.ShowOutpost;
import ru.homyakin.seeker.telegram.command.group.stats.GetGroupStats;
import ru.homyakin.seeker.telegram.command.group.stats.GetPersonageStats;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.ConsumeOrder;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.GetTavernMenu;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.Order;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.ThrowOrder;
import ru.homyakin.seeker.telegram.command.group.tavern_menu.ThrowOrderToGroup;
import ru.homyakin.seeker.telegram.command.group.top.TopGroupRaidLevel;
import ru.homyakin.seeker.telegram.command.group.top.TopGroupRaidWeek;
import ru.homyakin.seeker.telegram.command.group.top.TopList;
import ru.homyakin.seeker.telegram.command.group.top.TopPowerGroup;
import ru.homyakin.seeker.telegram.command.group.top.TopRaidWeek;
import ru.homyakin.seeker.telegram.command.group.top.TopRaidWeekGroup;
import ru.homyakin.seeker.telegram.command.group.top.TopOutpostBuildSession;
import ru.homyakin.seeker.telegram.command.group.top.TopOutpostSeasonMaterials;
import ru.homyakin.seeker.telegram.command.group.top.TopTavernSpent;
import ru.homyakin.seeker.telegram.command.group.top.TopWorkerOfDay;
import ru.homyakin.seeker.telegram.command.group.worker.WorkerOfDay;
import ru.homyakin.seeker.telegram.command.group.world_raid.GroupWorldRaidReport;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.command.user.battle_position.UserChangeBattlePosition;
import ru.homyakin.seeker.telegram.command.user.battle_position.UserSelectBattlePosition;
import ru.homyakin.seeker.telegram.command.user.badge.SelectPersonageBadge;
import ru.homyakin.seeker.telegram.command.user.badge.ShowPersonageBadges;
import ru.homyakin.seeker.telegram.command.user.bulletin_board.GetBulletinBoard;
import ru.homyakin.seeker.telegram.command.user.bulletin_board.ShowWorldRaidInfo;
import ru.homyakin.seeker.telegram.command.user.bulletin_board.TakePersonalQuest;
import ru.homyakin.seeker.telegram.command.user.bulletin_board.WorldRaidResearchTop;
import ru.homyakin.seeker.telegram.command.user.change_name.InitChangeName;
import ru.homyakin.seeker.telegram.command.user.contraband.ForceOpenContraband;
import ru.homyakin.seeker.telegram.command.user.contraband.OpenContrabandAsReceiver;
import ru.homyakin.seeker.telegram.command.user.contraband.SellContraband;
import ru.homyakin.seeker.telegram.command.user.contraband.ViewContraband;
import ru.homyakin.seeker.telegram.command.user.feedback.InitFeedback;
import ru.homyakin.seeker.telegram.command.user.group.LeaveGroupInPrivate;
import ru.homyakin.seeker.telegram.command.user.group.LeaveGroupInPrivateCancel;
import ru.homyakin.seeker.telegram.command.user.group.LeaveGroupInPrivateConfirm;
import ru.homyakin.seeker.telegram.command.user.item.ApplyLoadout;
import ru.homyakin.seeker.telegram.command.user.item.CancelCreateLoadout;
import ru.homyakin.seeker.telegram.command.user.item.CancelDeleteLoadout;
import ru.homyakin.seeker.telegram.command.user.item.ConfirmDeleteLoadout;
import ru.homyakin.seeker.telegram.command.user.item.CreateLoadout;
import ru.homyakin.seeker.telegram.command.user.item.DefaultLoadouts;
import ru.homyakin.seeker.telegram.command.user.item.DeleteLoadout;
import ru.homyakin.seeker.telegram.command.user.item.Inventory;
import ru.homyakin.seeker.telegram.command.user.item.LoadoutsList;
import ru.homyakin.seeker.telegram.command.user.item.OpenDefaultLoadoutEvent;
import ru.homyakin.seeker.telegram.command.user.item.OpenLoadout;
import ru.homyakin.seeker.telegram.command.user.item.PutOnItem;
import ru.homyakin.seeker.telegram.command.user.item.RenameLoadout;
import ru.homyakin.seeker.telegram.command.user.item.SaveLoadout;
import ru.homyakin.seeker.telegram.command.user.item.SelectInventory;
import ru.homyakin.seeker.telegram.command.user.item.TakeOffItem;
import ru.homyakin.seeker.telegram.command.user.item.ToggleDefaultLoadout;
import ru.homyakin.seeker.telegram.command.user.language.UserChangeLanguage;
import ru.homyakin.seeker.telegram.command.user.language.UserSelectLanguage;
import ru.homyakin.seeker.telegram.command.user.navigation.ReceptionDesk;
import ru.homyakin.seeker.telegram.command.user.navigation.StartUser;
import ru.homyakin.seeker.telegram.command.user.outpost.OpenOutpostBuilding;
import ru.homyakin.seeker.telegram.command.user.outpost.OpenOutpostMenu;
import ru.homyakin.seeker.telegram.command.user.outpost.OpenOutpostMenuInline;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostBuildingContributeView;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostCancelStartBuilding;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostConfirmStartBuilding;
import ru.homyakin.seeker.telegram.command.user.outpost.CancelOutpostDonate;
import ru.homyakin.seeker.telegram.command.user.outpost.ConfirmOutpostDonate;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostDonateItem;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostOpenBuildPicker;
import ru.homyakin.seeker.telegram.command.user.outpost.OutpostSelectStartBuilding;
import ru.homyakin.seeker.telegram.command.user.profile.CancelEvent;
import ru.homyakin.seeker.telegram.command.user.profile.GetProfileInPrivate;
import ru.homyakin.seeker.telegram.command.user.report.RaidReport;
import ru.homyakin.seeker.telegram.command.user.setting.GetPersonageSettings;
import ru.homyakin.seeker.telegram.command.user.setting.SetPersonageSetting;
import ru.homyakin.seeker.telegram.command.user.setting.ToggleHidePersonage;
import ru.homyakin.seeker.telegram.command.user.shop.AddModifier;
import ru.homyakin.seeker.telegram.command.user.shop.CancelSellItem;
import ru.homyakin.seeker.telegram.command.user.shop.ConfirmEnhance;
import ru.homyakin.seeker.telegram.command.user.shop.ConfirmSellItem;
import ru.homyakin.seeker.telegram.command.user.shop.BuyItem;
import ru.homyakin.seeker.telegram.command.user.shop.EnhanceInfo;
import ru.homyakin.seeker.telegram.command.user.shop.OpenEnhanceTable;
import ru.homyakin.seeker.telegram.command.user.shop.OpenShop;
import ru.homyakin.seeker.telegram.command.user.shop.Repair;
import ru.homyakin.seeker.telegram.command.user.shop.SellItem;
import ru.homyakin.seeker.telegram.command.user.shop.ShopOpenEnhanceInline;
import ru.homyakin.seeker.telegram.command.user.shop.ShopSelectRandomBoxes;
import ru.homyakin.seeker.telegram.command.user.shop.ShopSelectSlot;
import ru.homyakin.seeker.telegram.command.user.stats.BattleStats;
import ru.homyakin.seeker.telegram.command.user.stats.PersonageStatsGlobal;
import ru.homyakin.seeker.telegram.command.user.world_raid.UserWorldRaidReport;
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
        final var startCommand = parsePrivateStartCommand(message);
        if (startCommand.isPresent()) {
            return startCommand;
        }
        return CommandType.getFromString(message.getText())
            .map(commandType -> switch (commandType) {
                case CHANGE_LANGUAGE -> UserChangeLanguage.from(message);
                case START -> StartUser.from(message);
                case GET_PROFILE, BACK -> GetProfileInPrivate.from(message);
                case SHOW_HELP -> ShowHelp.from(message);
                case RECEPTION_DESK -> ReceptionDesk.from(message);
                case INIT_CHANGE_NAME -> InitChangeName.from(message);
                case RAID_REPORT -> RaidReport.from(message);
                case SHOW_BADGES -> ShowPersonageBadges.from(message);
                case INVENTORY -> Inventory.from(message);
                case PUT_ON -> PutOnItem.from(message);
                case TAKE_OFF -> TakeOffItem.from(message);
                case OPEN_SHOP -> OpenShop.from(message);
                case BUY_ITEM -> BuyItem.from(message);
                case SELL_ITEM -> SellItem.from(message);
                case BULLETIN_BOARD -> GetBulletinBoard.from(message);
                case TAKE_PERSONAL_QUEST -> TakePersonalQuest.from(message, false);
                case TAKE_PERSONAL_QUEST_COMMAND -> TakePersonalQuest.from(message, true);
                case TOGGLE_HIDE_PERSONAGE -> ToggleHidePersonage.from(message);
                case INIT_FEEDBACK -> InitFeedback.from(message);
                case PERSONAGE_STATS -> PersonageStatsGlobal.from(message);
                case BATTLE_STATS -> BattleStats.from(message);
                case CHANGE_BATTLE_POSITION -> UserChangeBattlePosition.from(message);
                case SETTINGS -> GetPersonageSettings.from(message);
                case SHOW_WORLD_RAID_INFO -> ShowWorldRaidInfo.from(message);
                case WORLD_RAID_DONATE -> WorldRaidDonate.from(message);
                case WORLD_RAID_RESEARCH_TOP -> WorldRaidResearchTop.from(message);
                case WORLD_RAID_REPORT -> UserWorldRaidReport.from(message);
                case ENHANCE_TABLE -> OpenEnhanceTable.from(message);
                case ENHANCE_INFO -> EnhanceInfo.from(message);
                case CONFIRM_ENHANCE -> ConfirmEnhance.from(message);
                case ADD_MODIFIER -> AddModifier.from(message);
                case REPAIR -> Repair.from(message);
                case CANCEL_EVENT -> CancelEvent.from(message);
                case LEAVE_GROUP -> LeaveGroupInPrivate.from(message);
                case SHOW_OUTPOST -> OpenOutpostMenu.from(message);
                case OPEN_OUTPOST_BUILDING -> OpenOutpostBuilding.from(message);
                case OUTPOST_DONATE_ITEM -> OutpostDonateItem.tryParse(message).orElse(null);
                case VIEW_CONTRABAND -> ViewContraband.from(message);
                // case HELP_LOVE -> HelpLove.from(message);
                default -> null;
            });
    }

    private Optional<Command> parsePrivateStartCommand(Message message) {
        final var text = message.getText();
        if (text == null || !text.startsWith("/start")) {
            return Optional.empty();
        }
        final var parts = text.split(" ", 2);
        if (parts.length < 2) {
            return Optional.empty();
        }
        return CommandType.getFromStartArgument(parts[1])
            .flatMap(type -> commandFromPrivateStartPayload(type, message));
    }

    private Optional<Command> commandFromPrivateStartPayload(CommandType type, Message message) {
        return Optional.ofNullable(switch (type) {
            case OPEN_OUTPOST_MENU -> OpenOutpostMenu.from(message);
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
                case SHOW_OUTPOST -> ShowOutpost.from(message);
                case WORKER_OF_DAY -> WorkerOfDay.from(message);
                case WORKER_OF_DAY_TOP -> TopWorkerOfDay.from(message);
                case PERSONAGE_STATS -> GetPersonageStats.from(message);
                case RAID_REPORT -> RaidReportInGroup.from(message);
                case TOP_RAID_WEEK -> TopRaidWeek.from(message);
                case TOP_RAID_WEEK_GROUP -> TopRaidWeekGroup.from(message);
                case TOP -> TopList.from(message);
                case TOP_TAVERN_SPENT -> TopTavernSpent.from(message);
                case TOP_OUTPOST_SEASON_MATERIALS -> TopOutpostSeasonMaterials.from(message);
                case TOP_OUTPOST_BUILD_SESSION -> TopOutpostBuildSession.from(message);
                case SETTINGS -> GetGroupSettings.from(message);
                case SET_TIME_ZONE -> SetTimeZone.from(message);
                case THROW_ORDER -> ThrowOrder.from(message);
                case CHANGE_GROUP_NAME -> ChangeGroupName.from(message);
                case TOGGLE_HIDE_GROUP -> ToggleHideGroup.from(message);
                case TOP_GROUP_RAID_WEEK -> TopGroupRaidWeek.from(message);
                case TOP_GROUP_RAID_LEVEL -> TopGroupRaidLevel.from(message);
                case TOP_POWER_GROUP -> TopPowerGroup.from(message);
                case GROUP_INFO -> GroupInfo.from(message);
                case REGISTER_GROUP -> GroupRegistration.from(message);
                case JOIN_GROUP -> JoinGroupMember.from(message);
                case LEAVE_GROUP -> LeaveGroupMember.from(message);
                case GROUP_COMMANDS -> GroupCommands.from(message);
                case GROUP_MEMBERS -> GroupMembers.fromMessage(message);
                case GROUP_MEMBER -> GroupMember.tryParse(message).orElse(null);
                case GROUP_KICK -> KickGroupMember.tryParse(message).orElse(null);
                case GROUP_TAX -> GroupTax.from(message);
                case WORLD_RAID_REPORT -> GroupWorldRaidReport.from(message);
                case WORLD_RAID_DONATE -> WorldRaidDonate.from(message);
                case CHANGE_TAG -> ChangeGroupTag.from(message);
                case THROW_ORDER_TO_GROUP -> ThrowOrderToGroup.from(message);
                case SHOW_BADGES_GROUP -> ShowGroupBadges.from(message);
                // case SEND_VALENTINE -> SendValentine.from(message);
                // case HELP_LOVE -> HelpLove.from(message);
                default -> null;
            });
    }

    private Optional<Command> parseCallback(CallbackQuery callback) {
        if (callback.getMessage().isUserMessage()) {
            return parsePrivateCallback(callback);
        } else if (TelegramUtils.isGroupMessage(callback.getMessage())) {
            return parseGroupCallback(callback);
        } else if (TelegramUtils.isChannelMessage(callback.getMessage())) {
            return parseChannelCallback(callback);
        } else {
            return Optional.empty();
        }
    }

    private Optional<Command> parsePrivateCallback(CallbackQuery callback) {
        final var text = callback.getData().split(TextConstants.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case GET_PROFILE -> GetProfileInPrivate.from(callback);
                case SELECT_LANGUAGE -> UserSelectLanguage.from(callback);
                case SELECT_BATTLE_POSITION -> UserSelectBattlePosition.from(callback);
                case SELECT_HELP -> SelectHelp.from(callback);
                case SELECT_BADGE -> SelectPersonageBadge.from(callback);
                case TOGGLE_PERSONAGE_SETTING -> SetPersonageSetting.from(callback);
                case LEAVE_GROUP_CONFIRM -> LeaveGroupInPrivateConfirm.from(callback);
                case LEAVE_GROUP_CANCEL -> LeaveGroupInPrivateCancel.from(callback);
                case FORCE_OPEN_CONTRABAND -> ForceOpenContraband.from(callback);
                case SELL_TO_BLACK_MARKET -> SellContraband.from(callback);
                case OPEN_CONTRABAND_AS_RECEIVER -> OpenContrabandAsReceiver.from(callback);
                case OPEN_OUTPOST_MENU_INLINE -> OpenOutpostMenuInline.from(callback);
                case OUTPOST_BUILD_PICKER_OPEN -> OutpostOpenBuildPicker.from(callback);
                case OUTPOST_BUILD_SELECT_BUILDING -> OutpostSelectStartBuilding.from(callback);
                case OUTPOST_BUILD_CONFIRM -> OutpostConfirmStartBuilding.from(callback);
                case OUTPOST_BUILD_CANCEL -> OutpostCancelStartBuilding.from(callback);
                case OUTPOST_BUILD_CONTRIBUTE -> OutpostBuildingContributeView.from(callback);
                case OPEN_OUTPOST_BUILDING_INLINE -> OpenOutpostBuilding.fromInlineCallback(callback);
                case SHOP_RANDOM_BOXES -> ShopSelectRandomBoxes.from(callback);
                case SHOP_ENHANCE_INLINE -> ShopOpenEnhanceInline.from(callback);
                case SHOP_SELECT_SLOT -> ShopSelectSlot.from(callback);
                case CONFIRM_SELL_ITEM -> ConfirmSellItem.from(callback);
                case CANCEL_SELL_ITEM -> CancelSellItem.from(callback);
                case CONFIRM_OUTPOST_DONATE -> ConfirmOutpostDonate.from(callback);
                case CANCEL_OUTPOST_DONATE -> CancelOutpostDonate.from(callback);
                case SELECT_INVENTORY -> SelectInventory.from(callback);
                case OPEN_LOADOUT -> OpenLoadout.from(callback);
                case CREATE_LOADOUT -> CreateLoadout.from(callback);
                case SAVE_LOADOUT -> SaveLoadout.from(callback);
                case APPLY_LOADOUT -> ApplyLoadout.from(callback);
                case RENAME_LOADOUT -> RenameLoadout.from(callback);
                case DELETE_LOADOUT -> DeleteLoadout.from(callback);
                case CONFIRM_DELETE_LOADOUT -> ConfirmDeleteLoadout.from(callback);
                case CANCEL_DELETE_LOADOUT -> CancelDeleteLoadout.from(callback);
                case LOADOUTS_LIST -> LoadoutsList.from(callback);
                case CANCEL_CREATE_LOADOUT -> CancelCreateLoadout.from(callback);
                case DEFAULT_LOADOUTS -> DefaultLoadouts.from(callback);
                case OPEN_DEFAULT_LOADOUT_EVENT -> OpenDefaultLoadoutEvent.from(callback);
                case TOGGLE_DEFAULT_LOADOUT -> ToggleDefaultLoadout.from(callback);
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
                case LEAVE_GROUP_CONFIRM -> LeaveGroupMemberConfirm.from(callback);
                case LEAVE_GROUP_CANCEL -> LeaveGroupMemberCancel.from(callback);
                case JOIN_WORLD_RAID -> JoinWorldRaid.from(callback);
                case CONFIRM_GROUP_JOIN -> ConfirmJoinGroupMember.from(callback);
                case CANCEL_GROUP_JOIN -> CancelJoinGroupMember.from(callback);
                case CONFIRM_GROUP_KICK -> ConfirmKickGroupMember.from(callback);
                case CANCEL_GROUP_KICK -> CancelKickGroupMember.from(callback);
                case SELECT_BADGE -> SelectGroupBadge.from(callback);
                case GROUP_MEMBERS_INLINE -> GroupMembers.fromCallback(callback);
                default -> null;
            });
    }

    private Optional<Command> parseChannelCallback(CallbackQuery callback) {
        final var text = callback.getData().split(TextConstants.CALLBACK_DELIMITER)[0];
        return CommandType.getFromString(text)
            .map(commandType -> switch (commandType) {
                case JOIN_WORLD_RAID -> JoinWorldRaid.from(callback);
                default -> null;
            });
    }

}
