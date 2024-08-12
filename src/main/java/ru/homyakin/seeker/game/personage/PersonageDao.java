package ru.homyakin.seeker.game.personage;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class PersonageDao {
    private static final String GET_BY_ID = """
        WITH item_characteristics AS (
            SELECT personage_id,
                SUM(attack) item_attack,
                SUM(health) item_health,
                SUM(defense) item_defense
            FROM item WHERE personage_id in (:id_list) AND is_equipped = true
            GROUP BY personage_id
        )
        SELECT p.*, b.code as badge_code, ic.* FROM personage p
        LEFT JOIN personage_available_badge pab ON p.id = pab.personage_id
        LEFT JOIN badge b ON pab.badge_id = b.id
        LEFT JOIN item_characteristics ic on p.id = ic.personage_id
        WHERE p.id in (:id_list)
        AND pab.is_active = true
        """;

    private static final String GET_BY_LAUNCHED_EVENT = """
        SELECT p.*, b.code as badge_code FROM personage_to_event le
        LEFT JOIN personage p on p.id = le.personage_id
        LEFT JOIN personage_available_badge pab ON p.id = pab.personage_id
        LEFT JOIN badge b on pab.badge_id = b.id
        WHERE le.launched_event_id = :launched_event_id
        AND pab.is_active = true
        """;

    private static final String UPDATE = """
        UPDATE personage
        SET name = :name, strength = :strength, agility = :agility, wisdom = :wisdom,
        health = :health, last_energy_change = :last_energy_change, money = :money,
        energy = :energy, effects = :effects
        WHERE id = :id
        """;

    private final SimpleJdbcInsert jdbcInsert;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public PersonageDao(DataSource dataSource, JsonUtils jsonUtils) {
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
                "energy",
                "effects"
            );
        this.jsonUtils = jsonUtils;
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
        params.put("effects", jsonUtils.mapToPostgresJson(personage.effects()));

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
            .param("effects", jsonUtils.mapToPostgresJson(personage.effects()))
            .update();
    }

    public Optional<Personage> getById(PersonageId id) {
        return jdbcClient.sql(GET_BY_ID)
            .param("id_list", List.of(id.value()))
            .query(this::mapRow)
            .optional();
    }

    public List<Personage> getByLaunchedEvent(Long launchedEventId) {
        // TODO Эффективность неизвестна, данный запрос написан просто чтобы работали предметы.
        // TODO Надо протестировать запросы на проде
        final var personageIdByLaunchedEvent = """
            SELECT * FROM personage_to_event WHERE launched_event_id = :launched_event_id
            """;
        final var idList = jdbcClient.sql(personageIdByLaunchedEvent)
            .param("launched_event_id", launchedEventId)
            .query((rs, rowNum) -> rs.getLong("personage_id"))
            .list();
        if (idList.isEmpty()) {
            return List.of();
        }
        return jdbcClient.sql(GET_BY_ID)
            .param("id_list", idList)
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
            ),
            BadgeView.findByCode(rs.getString("badge_code")),
            new Characteristics(
                DatabaseUtils.getIntOrDefault(rs, "item_health", 0),
                DatabaseUtils.getIntOrDefault(rs, "item_attack", 0),
                DatabaseUtils.getIntOrDefault(rs, "item_defense", 0),
                0,
                0,
                0
            ),
            jsonUtils.fromString(rs.getString("effects"), PersonageEffects.class)
        );
    }
}
