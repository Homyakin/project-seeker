package ru.homyakin.seeker.locale;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;

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
                LocalizationKeys.JOIN_BOSS_EVENT.name(),
                "Присоединиться к рейду" + TextConstants.RAID_ICON
            },

            {
                LocalizationKeys.BOSS_BATTLE_STARTS.name(),
                "Битва начнётся через"
            },
            {
                LocalizationKeys.HOURS_SHORT.name(),
                "ч."
            },
            {
                LocalizationKeys.MINUTES_SHORT.name(),
                "мин."
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
                    TextConstants.LEVEL_ICON,
                    "%d",
                    TextConstants.EXP_ICON,
                    "%d/%d"
                )
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
                LocalizationKeys.TOP_PERSONAGES_BY_EXP_IN_GROUP.name(),
                TextConstants.EXP_ICON + "Топ персонажей в чате по опыту:%n%s"
            },
            {
                LocalizationKeys.HELP.name(),
                """
                Социальная RPG в Telegram!
                Просто добавь в чат и участвуй в событиях.
                Официальный канал с новостями - %s.
                
                Доступные команды (в личке и в чате):
                %s - сменить язык;
                %s - показать профиль;
                %s - данное сообщение;
                
                Только для чата:
                %s - показать топ игроков по опыту в чате;
                
                Только для лички:
                %s - сменить имя;
                
                Исходный код игры <a href="%s">здесь</a>.
                """.formatted(
                    TextConstants.TELEGRAM_CHANNEL_USERNAME,
                    CommandType.CHANGE_LANGUAGE.getText(),
                    CommandType.GET_PROFILE.getText(),
                    CommandType.HELP.getText(),
                    CommandType.TOP.getText(),
                    CommandType.CHANGE_NAME.getText(),
                    TextConstants.SOURCE_LINK
                )
            },
            {
                LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name(),
                "Введите имя через пробел после команды: \"/name Имя\""
            },
            {
                LocalizationKeys.NAME_TOO_LONG.name(),
                "Имя не должно превышать %d символов"
            },
            {
                LocalizationKeys.SUCCESS_NAME_CHANGE.name(),
                "Имя успешно изменено!"
            },
            {
                LocalizationKeys.PROFILE_LEVEL_UP.name(),
                "Есть неизрасходованные очки прокачки! Жми /level_up"
            },
            {
                LocalizationKeys.NOT_ENOUGH_LEVELING_POINTS.name(),
                "Недостаточно очков прокачки. Подкачайся и приходи позже."
            },
            {
                LocalizationKeys.CHOOSE_LEVEL_UP_CHARACTERISTIC.name(),
                "Выберите характеристику для прокачки:"
            },
            {
                LocalizationKeys.SUCCESS_LEVEL_UP.name(),
                "Прокачка завершена:white_check_mark:"
            },
            {
                LocalizationKeys.PROFILE_BUTTON.name(),
                TextConstants.PROFILE_ICON + "Профиль"
            },
            {
                LocalizationKeys.LANGUAGE_BUTTON.name(),
                TextConstants.LANGUAGE_ICON + "Язык"
            },
            {
                LocalizationKeys.DUEL_MUST_BE_REPLY.name(),
                "Дуэль должна быть ответом на сообщение другого пользователя"
            },
            {
                LocalizationKeys.DUEL_REPLY_MUST_BE_TO_USER.name(),
                "Вашим противником должен быть пользователь"
            },
            {
                LocalizationKeys.DUEL_WITH_YOURSELF.name(),
                "Нельзя устроить дуэль с самим собой!"
            },
            {
                LocalizationKeys.PERSONAGE_ALREADY_START_DUEL.name(),
                "Вы уже начали другую дуэль! Дождитесь ей окончания."
            },
            {
                LocalizationKeys.INIT_DUEL.name(),
                "Искатель " + TextConstants.LEVEL_ICON + "%d %s вызывает на дуэль " +
                    "искателя " + TextConstants.LEVEL_ICON + "%d %s.\n\n" +
                    "Каким будет его ответ?"
            },
            {
                LocalizationKeys.NOT_DUEL_ACCEPTING_PERSONAGE.name(),
                "Это не вас вызвали на дуэль!"
            },
            {
                LocalizationKeys.EXPIRED_DUEL.name(),
                "Вызов на дуэль остался проигнорированным"
            },
            {
                LocalizationKeys.DECLINED_DUEL.name(),
                "Принимающая сторона отклонила вызов!"
            },
            {
                LocalizationKeys.FINISHED_DUEL.name(),
                "Искатель " + TextConstants.LEVEL_ICON + "%d %s одержал верх над " +
                    TextConstants.LEVEL_ICON + "%d %s"
            },
            {
                LocalizationKeys.ACCEPT_DUEL_BUTTON.name(),
                "Принять вызов" + TextConstants.DUEL_ACCEPT_ICON
            },
            {
                LocalizationKeys.DECLINE_DUEL_BUTTON.name(),
                "Отказаться :open_hands:" //TODO в иконки
            },
        };

    @Override
    protected Object[][] getContents() {
        return contents;
    }
}
