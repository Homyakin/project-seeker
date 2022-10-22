package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.infrastructure.TextConstants;

public class resource_ru extends AbstractResource {
    private static final Object[][] contents =
        {
            {
                LocalizationKeys.WELCOME_GROUP.name(),
                "Приветствую вас, Искатели."
            },
            {
                LocalizationKeys.WELCOME_USER.name(),
                "Приветствую тебя, Искатель."
            },
            {
                LocalizationKeys.CHOOSE_LANGUAGE.name(),
                "Выберите язык:"
            },
            {
                LocalizationKeys.ONLY_ADMIN_ACTION.name(),
                "Данное действие доступно только администраторам"
            },
            {
                LocalizationKeys.INTERNAL_ERROR.name(),
                "Произошла ошибка, попробуйте позже"
            },
            {
                LocalizationKeys.JOIN_EVENT.name(),
                "Присоединиться"
            },
            {
                LocalizationKeys.SUCCESS_JOIN_EVENT.name(),
                "Вы успешно присоединились к событию!"
            },
            {
                LocalizationKeys.USER_ALREADY_IN_THIS_EVENT.name(),
                "Вы уже участвуете в данном событии!"
            },
            {
                LocalizationKeys.USER_ALREADY_IN_OTHER_EVENT.name(),
                "Вы уже участвуете в другом событии!"
            },
            {
                LocalizationKeys.EXPIRED_EVENT.name(),
                "Событие завершилось!"
            },
            {
                LocalizationKeys.PROFILE_TEMPLATE.name(),
                """
                %s%s
                %sУровень: %s
                %sОпыт: %s
                """.formatted(
                    TextConstants.PROFILE_ICON,
                    "%s",
                    TextConstants.PROFILE_ICON,
                    "%d",
                    TextConstants.EXP_ICON,
                    "%d/%d"
                )
            },
            {
                LocalizationKeys.START_BOSS_EVENT.name(),
                "Обнаружен босс"
            },
            {
                LocalizationKeys.SUCCESS_BOSS.name(),
                "Босс был побеждён."
            },
            {
                LocalizationKeys.FAILURE_BOSS.name(),
                "Босс оказался сильнее искателей."
            },
            {
                LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_CHAT.name(),
                TextConstants.EXP_ICON + "Топ персонажей в чате по опыту:%n%s"
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
