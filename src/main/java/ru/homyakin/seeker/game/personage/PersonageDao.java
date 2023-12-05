package ru.homyakin.seeker.game.personage;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageId;

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
    private final NamedParameterJdbcTemplate jdbcTemplate;

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

        this.jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
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
        final var params = new HashMap<String, Object>();
        params.put("id", personage.id().value());
        params.put("name", personage.name());
        params.put("strength", personage.characteristics().strength());
        params.put("agility", personage.characteristics().agility());
        params.put("wisdom", personage.characteristics().wisdom());
        params.put("health", personage.characteristics().health());
        params.put("last_energy_change", personage.energy().lastChange());
        params.put("energy", personage.energy().value());
        params.put("money", personage.money().value());
        jdbcTemplate.update(
            UPDATE,
            params
        );
    }

    public Optional<Personage> getById(PersonageId id) {
        final var result = jdbcTemplate.query(
            GET_BY_ID,
            Collections.singletonMap("id", id.value()),
            this::mapRow
        );
        return result.stream().findFirst();
    }

    public List<Personage> getByLaunchedEvent(Long launchedEventId) {
        final var params = Collections.singletonMap("launched_event_id", launchedEventId);
        return jdbcTemplate.query(
            GET_BY_LAUNCHED_EVENT,
            params,
            this::mapRow
        );
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
