package ru.homyakin.seeker.telegram.group.database;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.PersonageCount;

@Repository
public class EverydaySpinDao {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EverydaySpinDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public void save(GroupId groupId, PersonageId personageId, LocalDate date) {
        String sql = """
            INSERT INTO everyday_spin_tg (grouptg_id, personage_id, choose_date)
            VALUES (:grouptg_id, :personage_id, :choose_date)""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", groupId.value());
        params.put("personage_id", personageId.value());
        params.put("choose_date", date);

        jdbcTemplate.update(sql, params);
    }

    public Optional<PersonageId> findPersonageIdByGrouptgIdAndDate(GroupId grouptgId, LocalDate chooseDate) {
        String sql = """
            SELECT personage_id FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id AND choose_date = :choose_date""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", grouptgId.value());
        params.put("choose_date", chooseDate);

        return jdbcTemplate
            .query(sql, params, (rs, rowNum) -> PersonageId.from(rs.getLong("personage_id")))
            .stream()
            .findFirst();
    }

    public List<PersonageCount> findPersonageCountByGrouptgId(GroupId grouptgId) {
        String sql = """
        WITH personage_count
        AS (
            SELECT personage_id, COUNT(*) as count
            FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id
            GROUP BY personage_id
        )
        SELECT p.name, pc.count
        FROM personage_count pc
        LEFT JOIN personage p ON p.id = pc.personage_id""";

        final var params = new HashMap<String, Object>();
        params.put("grouptg_id", grouptgId.value());

        return jdbcTemplate.query(
            sql,
            params,
            (rs, rowNum) -> new PersonageCount(rs.getString("name"), rs.getInt("count"))
        );
    }
}
