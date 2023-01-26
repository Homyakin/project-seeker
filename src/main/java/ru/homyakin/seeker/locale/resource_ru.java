package ru.homyakin.seeker.locale;

import java.util.Collections;
import java.util.HashMap;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.telegram.command.CommandType;
import ru.homyakin.seeker.utils.StringNamedTemplate;

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
                ${profile_icon}${personage_name}
                ${level_icon}Уровень: ${personage_level}
                ${exp_icon}Опыт: ${personage_exp}/${next_level_exp}
                """
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
                TextConstants.EXP_ICON + "Топ персонажей в чате по опыту:"
            },
            {
                LocalizationKeys.HELP.name(),
                StringNamedTemplate.format("""
                        Социальная RPG в Telegram!
                        Просто добавь в чат и участвуй в событиях.
                        Официальный канал с новостями - ${news_channel_username}.
                                        
                        Доступные команды (в личке и в чате):
                        ${language_command} - сменить язык;
                        ${profile_command} - показать профиль;
                        ${help_command} - данное сообщение;
                                        
                        Только для чата:
                        ${top_command} - показать топ игроков по опыту в чате;
                        ${duel_command} - вызвать другого искателя на дуэль. Должно быть ответом на сообщение другого пользователя;
                                        
                        Только для лички:
                        ${name_command} - сменить имя;
                                        
                        Исходный код игры <a href="${github_link}">здесь</a>.
                        """,
                    new HashMap<>() {{
                        put("news_channel_username", TextConstants.TELEGRAM_CHANNEL_USERNAME);
                        put("language_command", CommandType.CHANGE_LANGUAGE.getText());
                        put("profile_command", CommandType.GET_PROFILE.getText());
                        put("help_command", CommandType.HELP.getText());
                        put("top_command", CommandType.TOP.getText());
                        put("duel_command", CommandType.START_DUEL.getText());
                        put("name_command", CommandType.CHANGE_NAME.getText());
                        put("github_link", TextConstants.SOURCE_LINK);
                    }}
                )
            },
            {
                LocalizationKeys.CHANGE_NAME_WITHOUT_NAME.name(),
                StringNamedTemplate.format(
                    "Введите имя через пробел после команды: \"${name_command} Имя\"",
                    Collections.singletonMap("name_command", CommandType.CHANGE_NAME.getText())
                )
            },
            {
                LocalizationKeys.NAME_TOO_LONG.name(),
                "Имя не должно превышать ${max_name_length} символов"
            },
            {
                LocalizationKeys.SUCCESS_NAME_CHANGE.name(),
                "Имя успешно изменено!"
            },
            {
                LocalizationKeys.PROFILE_LEVEL_UP.name(),
                StringNamedTemplate.format(
                    "Есть неизрасходованные очки прокачки! Жми ${level_up_command}",
                    Collections.singletonMap("level_up_command", CommandType.LEVEL_UP.getText())
                )
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
                """
                    Искатель ${level_icon}${initiator_personage_level} ${initiator_personage_name} вызывает на дуэль \
                    искателя  ${level_icon}${accepting_personage_level} ${accepting_personage_name}.
                    
                    Каким будет его ответ?
                    """
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
                """
                    Искатель ${level_icon}${winner_personage_level} ${winner_personage_name} одержал верх над \
                    ${level_icon}${looser_personage_level} ${looser_personage_name}
                    """
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
