package ru.homyakin.seeker.game.top;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.TopRaidPosition;

@Component
public class TopDao {
    private final JdbcClient jdbcClient;

    public TopDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public List<TopRaidPosition> getUnsortedTopRaid(LocalDate start, LocalDate end) {
        final var sql = """
            WITH event_points AS (
                SELECT
                    pte.personage_id,
                    SUM(CASE WHEN le.status_id = :success_id THEN 1 ELSE 0 END) AS success_count,
                    SUM(CASE WHEN le.status_id = :fail_id THEN 1 ELSE 0 END) AS fail_count
                FROM personage_to_event pte
                LEFT JOIN public.launched_event le on le.id = pte.launched_event_id
                WHERE le.start_date::date >= :start_date AND le.start_date::date <= :end_date
                GROUP BY pte.personage_id
            )
            SELECT p.id personage_id, p.name personage_name, b.code badge_code, success_count, fail_count FROM personage p
            INNER JOIN event_points ep ON p.id = ep.personage_id
            LEFT JOIN public.personage_available_badge pab on p.id = pab.personage_id
            LEFT JOIN public.badge b on b.id = pab.badge_id
            WHERE pab.is_active = true
            """;
        return jdbcClient.sql(sql)
            .param("success_id", EventStatus.SUCCESS.id())
            .param("fail_id", EventStatus.FAILED.id())
            .param("start_date", start)
            .param("end_date", end)
            .query(this::mapRow)
            .list();
    }

    private TopRaidPosition mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new TopRaidPosition(
            PersonageId.from(rs.getLong("personage_id")),
            rs.getString("personage_name"),
            BadgeView.findByCode(rs.getString("badge_code")),
            rs.getInt("success_count"),
            rs.getInt("fail_count")
        );
    }
}
