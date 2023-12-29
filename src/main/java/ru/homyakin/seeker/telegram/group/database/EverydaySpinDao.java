package ru.homyakin.seeker.telegram.group.database;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupId;
import ru.homyakin.seeker.telegram.group.models.PersonageCount;

@Repository
public class EverydaySpinDao {
    private final JdbcClient jdbcClient;

    public EverydaySpinDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupId groupId, PersonageId personageId, LocalDate date) {
        String sql = """
            INSERT INTO everyday_spin_tg (grouptg_id, personage_id, choose_date)
            VALUES (:grouptg_id, :personage_id, :choose_date)""";

        jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .param("personage_id", personageId.value())
            .param("choose_date", date)
            .update();
    }

    public Optional<PersonageId> findPersonageIdByGrouptgIdAndDate(GroupId grouptgId, LocalDate chooseDate) {
        String sql = """
            SELECT personage_id FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id AND choose_date = :choose_date""";

        return jdbcClient.sql(sql)
            .param("grouptg_id", grouptgId.value())
            .param("choose_date", chooseDate)
            .query((rs, rowNum) -> PersonageId.from(rs.getLong("personage_id")))
            .optional();
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

        return jdbcClient.sql(sql)
            .param("grouptg_id", grouptgId.value())
            .query((rs, rowNum) -> new PersonageCount(rs.getString("name"), rs.getInt("count")))
            .list();
    }
}
