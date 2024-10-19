package ru.homyakin.seeker.telegram.group.database;

import java.time.LocalDate;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.telegram.group.models.GroupTgId;

@Repository
public class EverydaySpinDao {
    private final JdbcClient jdbcClient;

    public EverydaySpinDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public void save(GroupTgId groupId, PersonageId personageId, LocalDate date) {
        String sql = """
            INSERT INTO everyday_spin_tg (grouptg_id, personage_id, choose_date)
            VALUES (:grouptg_id, :personage_id, :choose_date)""";

        jdbcClient.sql(sql)
            .param("grouptg_id", groupId.value())
            .param("personage_id", personageId.value())
            .param("choose_date", date)
            .update();
    }

    public Optional<PersonageId> findPersonageIdByGrouptgIdAndDate(GroupTgId grouptgId, LocalDate chooseDate) {
        String sql = """
            SELECT personage_id FROM everyday_spin_tg
            WHERE grouptg_id = :grouptg_id AND choose_date = :choose_date""";

        return jdbcClient.sql(sql)
            .param("grouptg_id", grouptgId.value())
            .param("choose_date", chooseDate)
            .query((rs, rowNum) -> PersonageId.from(rs.getLong("personage_id")))
            .optional();
    }
}
