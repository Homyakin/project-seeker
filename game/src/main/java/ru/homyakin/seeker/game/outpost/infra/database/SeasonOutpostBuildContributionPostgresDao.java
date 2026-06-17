package ru.homyakin.seeker.game.outpost.infra.database;

import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.SeasonOutpostBuildContributionStorage;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;

@Repository
public class SeasonOutpostBuildContributionPostgresDao implements SeasonOutpostBuildContributionStorage {

    private final JdbcClient jdbcClient;

    public SeasonOutpostBuildContributionPostgresDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public void add(
        SeasonNumber seasonNumber,
        GroupId groupId,
        Building building,
        int targetLevel,
        PersonageId personageId,
        int materialsDelta
    ) {
        final var sql = """
            INSERT INTO season_pgroup_outpost_build_contribution
            (season_number, pgroup_id, building_id, target_level, personage_id, materials)
            VALUES (:season_number, :pgroup_id, :building_id, :target_level, :personage_id, :materials)
            ON CONFLICT (season_number, pgroup_id, building_id, target_level, personage_id)
            DO UPDATE SET materials = season_pgroup_outpost_build_contribution.materials + :materials
            """;
        jdbcClient.sql(sql)
            .param("season_number", seasonNumber.value())
            .param("pgroup_id", groupId.value())
            .param("building_id", building.id())
            .param("target_level", targetLevel)
            .param("personage_id", personageId.value())
            .param("materials", materialsDelta)
            .update();
    }
}
