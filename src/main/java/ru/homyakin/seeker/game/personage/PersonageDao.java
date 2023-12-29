package ru.homyakin.seeker.game.personage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

@Component
public class PersonageDao {
    private static final String GET_BY_ID = """
        SELECT * FROM personage WHERE id = :id
        """;

    private static final String GET_BY_LAUNCHED_EVENT = """
        SELECT p.* FROM personage_to_event le
        LEFT JOIN personage p on p.id = le.personage_id
        WHERE le.launched_event_id = :launched_event_id
        """;

    private static final String UPDATE = """
        UPDATE personage
        SET name = :name, strength = :strength, agility = :agility, wisdom = :wisdom,
        health = :health, last_energy_change = :last_energy_change, money = :money,
        energy = :energy
        WHERE id = :id
        """;

    private final SimpleJdbcInsert jdbcInsert;
    private final JdbcClient jdbcClient;

    public PersonageDao(DataSource dataSource) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("personage")
            .usingColumns(
                "name",
                "money",
                "attack",
                "defense",
                "health",
                "strength",
                "agility",
                "wisdom",
                "last_energy_change",
                "energy"
            );
        jdbcInsert.setGeneratedKeyName("id");

        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public PersonageId save(Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("name", personage.name());
        params.put("money", personage.money().value());
        params.put("attack", personage.characteristics().attack());
        params.put("defense", personage.characteristics().defense());
        params.put("health", personage.characteristics().health());
        params.put("strength", personage.characteristics().strength());
        params.put("agility", personage.characteristics().agility());
        params.put("wisdom", personage.characteristics().wisdom());
        params.put("last_energy_change", personage.energy().lastChange());
        params.put("energy", personage.energy().value());

        return PersonageId.from(jdbcInsert.executeAndReturnKey(params).longValue());
    }

    public void update(Personage personage) {
        jdbcClient.sql(UPDATE)
            .param("id", personage.id().value())
            .param("name", personage.name())
            .param("strength", personage.characteristics().strength())
            .param("agility", personage.characteristics().agility())
            .param("wisdom", personage.characteristics().wisdom())
            .param("health", personage.characteristics().health())
            .param("last_energy_change", personage.energy().lastChange())
            .param("energy", personage.energy().value())
            .param("money", personage.money().value())
            .update();
    }

    public Optional<Personage> getById(PersonageId id) {
        return jdbcClient.sql(GET_BY_ID)
            .param("id", id.value())
            .query(this::mapRow)
            .optional();
    }

    public List<Personage> getByLaunchedEvent(Long launchedEventId) {
        return jdbcClient.sql(GET_BY_LAUNCHED_EVENT)
            .param("launched_event_id", launchedEventId)
            .query(this::mapRow)
            .list();
    }

    private Personage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Personage(
            PersonageId.from(rs.getLong("id")),
            rs.getString("name"),
            new Money(rs.getInt("money")),
            new Characteristics(
                rs.getInt("health"),
                rs.getInt("attack"),
                rs.getInt("defense"),
                rs.getInt("strength"),
                rs.getInt("agility"),
                rs.getInt("wisdom")
            ),
            new Energy(
                rs.getInt("energy"),
                rs.getTimestamp("last_energy_change").toLocalDateTime()
            )
        );
    }
}
