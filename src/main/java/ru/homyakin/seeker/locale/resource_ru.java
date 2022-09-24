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
                "Событие закончилось!"
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
