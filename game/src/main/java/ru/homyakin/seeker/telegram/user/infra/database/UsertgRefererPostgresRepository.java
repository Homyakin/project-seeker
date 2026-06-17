package ru.homyakin.seeker.telegram.user.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.telegram.user.entity.UsertgRefererStorage;
import ru.homyakin.seeker.telegram.user.models.UserId;

import javax.sql.DataSource;
import java.time.ZonedDateTime;

@Repository
public class UsertgRefererPostgresRepository implements UsertgRefererStorage {
    private final JdbcClient jdbcClient;

    public UsertgRefererPostgresRepository(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void saveReferer(UserId userId, String referer, ZonedDateTime dateTime) {
        final var sql = """
            INSERT INTO usertg_referer (usertg_id, referer, date)
            VALUES (:usertg_id, :referer, :date)
            """;
        jdbcClient.sql(sql)
            .param("usertg_id", userId.value())
            .param("referer", referer)
            .param("date", dateTime.toLocalDateTime())
            .update();
    }
}
