package ru.homyakin.seeker.game.season.infra.postgres;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import ru.homyakin.seeker.game.season.entity.SeasonNumber;
import ru.homyakin.seeker.game.season.entity.SeasonStorage;

import javax.sql.DataSource;

@Repository
public class SeasonDao implements SeasonStorage {
    private final JdbcClient jdbcClient;

    public SeasonDao(DataSource dataSource) {
        this.jdbcClient = JdbcClient.create(dataSource);
    }

    @Override
    public SeasonNumber currentSeason() {
        final var sql = "SELECT * FROM season WHERE end_date IS NULL";
        return jdbcClient.sql(sql)
            .query((rs, _) -> SeasonNumber.of(rs.getInt("number")))
            .single();
    }
}
