package ru.homyakin.seeker.user;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.locale.Language;

@Component
class UserUpdateDao {
    private static final String UPDATE_ACTIVE_PRIVATE_MESSAGES = """
        update tg_user
        set is_active_private_messages = :is_active_private_messages
        where id = :id;
        """;

    private static final String UPDATE_LANGUAGE = """
        update tg_user
        set lang = :lang
        where id = :id;
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserUpdateDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void updateIsActivePrivateMessages(Long userId, boolean isActivePrivateMessages) {
        final var params = new HashMap<String, Object>();
        params.put("id", userId);
        params.put("is_active", isActivePrivateMessages);
        jdbcTemplate.update(
            UPDATE_ACTIVE_PRIVATE_MESSAGES,
            params
        );
    }

    public void updateLanguage(Long userId, Language language) {
        final var params = new HashMap<String, Object>();
        params.put("id", userId);
        params.put("lang", language.id());
        jdbcTemplate.update(
            UPDATE_LANGUAGE,
            params
        );
    }
}
