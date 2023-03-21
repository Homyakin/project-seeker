package ru.homyakin.seeker.telegram.group.database;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class EverydaySpinDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EverydaySpinDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(long groupId, long userId, LocalDate date) {
        String sql = """
            INSERT INTO everyday_spin_tg (grouptg_id, usertg_id, choose_date)
            VALUES (:grouptg_id, :usertg_id, :choose_date)""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId);
        params.put("usertg_id", userId);
        params.put("choose_date", date);

        jdbcTemplate.update(sql, params);
    }

    public Optional<Long> findUserIdByGrouptgIdAndDate(long grouptgId, LocalDate chooseDate) {
        String sql = """
            SELECT usertg_id FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id AND choose_date = :choose_date""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", grouptgId);
        params.put("choose_date", chooseDate);

        return jdbcTemplate
            .query(sql, params, (rs, rowNum) -> rs.getLong("usertg_id"))
            .stream()
            .findFirst();
    }
}
