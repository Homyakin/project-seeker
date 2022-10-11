package ru.homyakin.seeker.telegram.user;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.telegram.user.model.User;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
class UserSaveDao {
    private static final String SAVE_USER = """
        insert into tg_user (id, is_active_private_messages, lang, init_date, character_id)
        values (:id, :is_active_private_messages, :lang, :init_date, :character_id);
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UserSaveDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(User user) {
        final var params = new HashMap<String, Object>();
        params.put("id", user.id());
        params.put("is_active_private_messages", user.isActivePrivateMessages());
        params.put("lang", user.language().id());
        params.put("init_date", TimeUtils.moscowTime());
        params.put("character_id", user.characterId());
        jdbcTemplate.update(
            SAVE_USER,
            params
        );
    }
}
