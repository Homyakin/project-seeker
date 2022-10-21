package ru.homyakin.seeker.locale;

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
                :bust_in_silhouette:Безымянный
                :military_medal:Уровень: %d
                :star2:Опыт: %d/%d
                """
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
            //TODO вынести иконки отдельно
            {
                LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_CHAT.name(),
                ":star2:Топ персонажей в чате по опыту:%n%s"
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
