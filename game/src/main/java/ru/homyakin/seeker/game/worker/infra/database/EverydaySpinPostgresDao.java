package ru.homyakin.seeker.game.worker.infra.database;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.worker.entity.WorkerOfDayStorage;

import javax.sql.DataSource;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public class EverydaySpinPostgresDao implements WorkerOfDayStorage {
    private final JdbcClient jdbcClient;

    public EverydaySpinPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public Optional<PersonageId> findPersonageIdByGroupIdAndDate(GroupId groupId, LocalDate date) {
        final var sql = """
            SELECT personage_id FROM everyday_spin WHERE pgroup_id = :pgroup_id AND choose_date = :choose_date 
            """;
        return jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("choose_date", date)
            .query((rs, rowNum) -> PersonageId.from(rs.getLong("personage_id")))
            .optional();
    }

    @Override
    public void save(GroupId groupId, PersonageId personageId, LocalDate date) {
        final var sql = """
            INSERT INTO everyday_spin (pgroup_id, personage_id, choose_date)
            VALUES (:pgroup_id, :personage_id, :choose_date)
            """;

        jdbcClient.sql(sql)
            .param("pgroup_id", groupId.value())
            .param("personage_id", personageId.value())
            .param("choose_date", date)
            .update();
    }
}
