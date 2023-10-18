package ru.homyakin.seeker.telegram.group.database;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.telegram.group.models.PersonageCount;
import ru.homyakin.seeker.telegram.user.models.UserId;

@Repository
public class EverydaySpinDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EverydaySpinDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(long groupId, UserId userId, LocalDate date) {
        String sql = """
            INSERT INTO everyday_spin_tg (grouptg_id, usertg_id, choose_date)
            VALUES (:grouptg_id, :usertg_id, :choose_date)""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId);
        params.put("usertg_id", userId.value());
        params.put("choose_date", date);

        jdbcTemplate.update(sql, params);
    }

    public Optional<UserId> findUserIdByGrouptgIdAndDate(long grouptgId, LocalDate chooseDate) {
        String sql = """
            SELECT usertg_id FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id AND choose_date = :choose_date""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", grouptgId);
        params.put("choose_date", chooseDate);

        return jdbcTemplate
            .query(sql, params, (rs, rowNum) -> new UserId(rs.getLong("usertg_id")))
            .stream()
            .findFirst();
    }

    public List<PersonageCount> findPersonageCountByGrouptgId(long grouptgId) {
        String sql = """
        WITH personage_count
        AS (
            SELECT personage_id, COUNT(*) as count
            FROM everyday_spin_tg est
            LEFT JOIN usertg u on u.id = est.usertg_id
            WHERE est.grouptg_id = :grouptg_id
            AND personage_id is not null
            GROUP BY u.personage_id
        )
        SELECT p.name, pc.count
        FROM personage_count pc
        LEFT JOIN personage p ON p.id = pc.personage_id""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", grouptgId);

        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new PersonageCount(rs.getString("name"), rs.getInt("count"))
        );
    }
}
