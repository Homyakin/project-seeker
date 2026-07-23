package ru.homyakin.seeker.telegram.command.common.help;

import java.util.Optional;

import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import ru.homyakin.seeker.game.battle.skill.ActiveSkillSlots;
import ru.homyakin.seeker.game.personage.models.PersonageSlot;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.help.HelpLocalization;
import ru.homyakin.seeker.telegram.TelegramSender;
import ru.homyakin.seeker.telegram.command.CommandExecutor;
import ru.homyakin.seeker.telegram.group.GroupTgService;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;
import ru.homyakin.seeker.telegram.user.UserService;
import ru.homyakin.seeker.telegram.user.models.UserId;
import ru.homyakin.seeker.telegram.utils.EditMessageTextBuilder;
import ru.homyakin.seeker.telegram.utils.InlineKeyboards;

@Component
public class SelectHelpExecutor extends CommandExecutor<SelectHelp> {
    private final UserService userService;
    private final GroupTgService groupTgService;
    private final TelegramSender telegramSender;

    public SelectHelpExecutor(UserService userService, GroupTgService groupTgService, TelegramSender telegramSender) {
        this.userService = userService;
        this.groupTgService = groupTgService;
        this.telegramSender = telegramSender;
    }

    @Override
    public void execute(SelectHelp command) {
        final Language language;
        // TODO подумать над айдишками
        if (command.isPrivate()) {
            language = userService.forceGetFromPrivate(UserId.from(command.chatId())).language();
        } else {
            language = groupTgService.getOrCreate(GroupTgId.from(command.chatId())).language();
        }
        final var section = HelpSection.findForce(command.helpSection());
        final var content = contentFor(section, language, command.skillsPage(), command.skillsSlotFilter());

        telegramSender.send(EditMessageTextBuilder.builder()
            .chatId(command.chatId())
            .messageId(command.messageId())
            .text(content.text())
            .keyboard(content.keyboard())
            .build()
        );
    }

    private HelpContent contentFor(
        HelpSection section,
        Language language,
        int skillsPage,
        Optional<PersonageSlot> skillsSlotFilter
    ) {
        return switch (section) {
            case MAIN -> new HelpContent(HelpLocalization.main(language), InlineKeyboards.helpKeyboard(language));
            case RAIDS -> new HelpContent(HelpLocalization.raids(language), InlineKeyboards.helpKeyboard(language));
            case DUELS -> new HelpContent(HelpLocalization.duels(language), InlineKeyboards.helpKeyboard(language));
            case MENU -> new HelpContent(HelpLocalization.menu(language), InlineKeyboards.helpKeyboard(language));
            case PERSONAGE -> new HelpContent(
                HelpLocalization.personage(language),
                InlineKeyboards.helpKeyboard(language)
            );
            case INFO -> new HelpContent(HelpLocalization.info(language), InlineKeyboards.helpKeyboard(language));
            case SEASONS -> new HelpContent(HelpLocalization.seasons(language), InlineKeyboards.helpKeyboard(language));
            case BATTLE_SYSTEM, BATTLE_GENERAL -> new HelpContent(
                HelpLocalization.battleSystem(language),
                InlineKeyboards.battleHelpKeyboard(language)
            );
            case BATTLE_MATRIX -> new HelpContent(
                HelpLocalization.battleMatrix(language),
                InlineKeyboards.battleHelpKeyboard(language)
            );
            case BATTLE_SKILLS -> skillsContent(language, skillsPage, skillsSlotFilter);
        };
    }

    private HelpContent skillsContent(Language language, int page, Optional<PersonageSlot> slotFilter) {
        final var skills = ActiveSkillSlots.sortedSkills(slotFilter);
        if (skills.isEmpty()) {
            return new HelpContent(
                HelpLocalization.battleSkillsEmpty(language),
                InlineKeyboards.battleSkillsHelpKeyboard(language, 0, 0, slotFilter)
            );
        }
        final var totalPages = skills.size();
        final var safePage = Math.min(Math.max(page, 0), totalPages - 1);
        return new HelpContent(
            HelpLocalization.battleSkill(language, skills.get(safePage)),
            InlineKeyboards.battleSkillsHelpKeyboard(language, safePage, totalPages, slotFilter)
        );
    }

    private record HelpContent(String text, InlineKeyboardMarkup keyboard) {
    }
}
