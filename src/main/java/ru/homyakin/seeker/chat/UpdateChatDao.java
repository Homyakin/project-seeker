package ru.homyakin.seeker.chat;

import java.util.HashMap;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class UpdateChatDao {
    private static final String UPDATE_ACTIVE = """
        update chat
        set is_active = :is_active
        where id = :id;
        """;

    private final NamedParameterJdbcTemplate jdbcTemplate;

    public UpdateChatDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void updateIsActive(Long chatId, boolean isActive) {
        final var params = new HashMap<String, Object>();
        params.put("id", chatId);
        params.put("is_active", isActive);
        jdbcTemplate.update(
            UPDATE_ACTIVE,
            params
        );
    }
}
