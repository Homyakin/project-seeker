package ru.homyakin.seeker.telegram.command.common.help;

import java.util.Set;
import java.util.stream.Collectors;

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
import ru.homyakin.seeker.telegram.utils.HelpInlineKeyboards;

@Component
public class SelectHelpExecutor extends CommandExecutor<SelectHelp> {
    public static final int SKILLS_PAGE_SIZE = 5;

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
        final var content = contentFor(section, language, command.skillsPage(), command.skillsSlotFilters());

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
        Set<PersonageSlot> skillsSlotFilters
    ) {
        return switch (section) {
            case MAIN -> new HelpContent(HelpLocalization.main(language), HelpInlineKeyboards.helpKeyboard(language));
            case RAIDS -> new HelpContent(HelpLocalization.raids(language), HelpInlineKeyboards.helpKeyboard(language));
            case DUELS -> new HelpContent(HelpLocalization.duels(language), HelpInlineKeyboards.helpKeyboard(language));
            case MENU -> new HelpContent(HelpLocalization.menu(language), HelpInlineKeyboards.helpKeyboard(language));
            case PERSONAGE -> new HelpContent(
                HelpLocalization.personage(language),
                HelpInlineKeyboards.helpKeyboard(language)
            );
            case INFO -> new HelpContent(HelpLocalization.info(language), HelpInlineKeyboards.helpKeyboard(language));
            case SEASONS -> new HelpContent(HelpLocalization.seasons(language), HelpInlineKeyboards.helpKeyboard(language));
            case BATTLE_SYSTEM, BATTLE_GENERAL -> new HelpContent(
                HelpLocalization.battleSystem(language),
                HelpInlineKeyboards.battleHelpKeyboard(language)
            );
            case BATTLE_MATRIX -> new HelpContent(
                HelpLocalization.battleMatrix(language),
                HelpInlineKeyboards.battleHelpKeyboard(language)
            );
            case BATTLE_SKILLS -> skillsContent(language, skillsPage, skillsSlotFilters);
        };
    }

    private HelpContent skillsContent(Language language, int page, Set<PersonageSlot> slotFilters) {
        final var skills = ActiveSkillSlots.sortedSkills(slotFilters);
        if (skills.isEmpty()) {
            return new HelpContent(
                HelpLocalization.battleSkillsEmpty(language),
                HelpInlineKeyboards.battleSkillsHelpKeyboard(language, 0, 0, slotFilters)
            );
        }
        final var totalPages = (skills.size() + SKILLS_PAGE_SIZE - 1) / SKILLS_PAGE_SIZE;
        final var safePage = Math.min(Math.max(page, 0), totalPages - 1);
        final var from = safePage * SKILLS_PAGE_SIZE;
        final var to = Math.min(from + SKILLS_PAGE_SIZE, skills.size());
        final var pageSkills = skills.subList(from, to);
        final var text = pageSkills.stream()
            .map(skill -> HelpLocalization.battleSkill(language, skill))
            .collect(Collectors.joining("\n\n"));
        return new HelpContent(
            text,
            HelpInlineKeyboards.battleSkillsHelpKeyboard(language, safePage, totalPages, slotFilters)
        );
    }

    private record HelpContent(String text, InlineKeyboardMarkup keyboard) {
    }
}
