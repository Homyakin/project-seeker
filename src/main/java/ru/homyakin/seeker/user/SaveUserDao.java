package ru.homyakin.seeker.user;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
class SaveUserDao {
    private static final String SAVE_USER = """
        insert into tg_user (id, is_active_private_messages, lang, init_date)
        values (:id, :is_active_private_messages, :lang, :init_date);
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public SaveUserDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(User user) {
        final var params = new HashMap<String, Object>();
        params.put("id", user.id());
        params.put("is_active_private_messages", user.isActivePrivateMessages());
        params.put("lang", user.language().id());
        params.put("init_date", TimeUtils.moscowTime());
        jdbcTemplate.update(
            SAVE_USER,
            params
        );
    }
}
