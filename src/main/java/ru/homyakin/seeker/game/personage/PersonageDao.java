package ru.homyakin.seeker.game.personage;

import io.vavr.CheckedFunction0;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.EventStatus;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.game.personage.badge.BadgeView;
import ru.homyakin.seeker.game.personage.models.Characteristics;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.CurrentEvent;
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

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class PersonageDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String GET_BY_ID = """
        WITH item_characteristics AS (
            SELECT personage_id,
                SUM(attack) item_attack,
                SUM(health) item_health,
                SUM(defense) item_defense
            FROM item WHERE personage_id in (:id_list) AND is_equipped = true
            GROUP BY personage_id
        ),
        active_launched_event AS (
            SELECT pte.personage_id,
                le.end_date as launched_event_end_date,
                e.type_id as event_type,
                ROW_NUMBER() OVER (PARTITION BY pte.personage_id) AS rn
            FROM personage_to_event pte
                LEFT JOIN launched_event le ON pte.launched_event_id = le.id
                LEFT JOIN event e ON le.event_id = e.id
            WHERE le.status_id = :active_status_id AND personage_id in (:id_list)
        )
        SELECT p.*,
            b.code as badge_code,
            ic.*,
            ale.launched_event_end_date,
            ale.event_type
        FROM personage p
        LEFT JOIN personage_available_badge pab ON p.id = pab.personage_id AND pab.is_active = true
        LEFT JOIN badge b ON pab.badge_id = b.id
        LEFT JOIN item_characteristics ic on p.id = ic.personage_id
        LEFT JOIN active_launched_event ale ON p.id = ale.personage_id AND ale.rn = 1
        WHERE p.id in (:id_list)
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
    private final PersonageConfig config;

    public PersonageDao(DataSource dataSource, JsonUtils jsonUtils, PersonageConfig config) {
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
        this.config = config;
        jdbcInsert.setGeneratedKeyName("id");

        this.jdbcClient = JdbcClient.create(dataSource);
    }

    public PersonageId createDefault(String name) {
        final var params = new HashMap<String, Object>();
        final var defaultCharacteristics = Characteristics.createDefault();
        params.put("name", name);
        params.put("money", config.defaultMoney());
        params.put("attack", defaultCharacteristics.attack());
        params.put("defense", defaultCharacteristics.defense());
        params.put("health", defaultCharacteristics.health());
        params.put("strength", defaultCharacteristics.strength());
        params.put("agility", defaultCharacteristics.agility());
        params.put("wisdom", defaultCharacteristics.wisdom());
        params.put("last_energy_change", TimeUtils.moscowTime());
        params.put("energy", config.defaultEnergy());
        params.put("effects", jsonUtils.mapToPostgresJson(PersonageEffects.EMPTY));

        return PersonageId.from(jdbcInsert.executeAndReturnKey(params).longValue());
    }

    public PersonageId createDefault() {
        return createDefault(TextConstants.DEFAULT_PERSONAGE_NAME);
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
            .param("active_status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .optional();
    }

    public List<Personage> getByLaunchedEvent(Long launchedEventId) {
        // TODO Эффективность неизвестна, данный запрос написан просто чтобы работали предметы.
        // на 02.09.24 проблем с производительностью нет
        final var now = System.currentTimeMillis();
        final var personageIdByLaunchedEvent = """
            SELECT * FROM personage_to_event WHERE launched_event_id = :launched_event_id
            """;
        final var idList = jdbcClient.sql(personageIdByLaunchedEvent)
            .param("launched_event_id", launchedEventId)
            .query((rs, _) -> rs.getLong("personage_id"))
            .list();
        if (idList.isEmpty()) {
            return List.of();
        }
        final var result = jdbcClient.sql(GET_BY_ID)
            .param("id_list", idList)
            .param("active_status_id", EventStatus.LAUNCHED.id())
            .query(this::mapRow)
            .list();
        if (logger.isDebugEnabled()) {
            logger.debug("Finished getting {} personages by launched event in {} ms", result.size(), System.currentTimeMillis() - now);
        }
        return result;
    }

    public boolean toggleIsHidden(PersonageId id) {
        final var sql = "UPDATE personage SET is_hidden = NOT is_hidden WHERE id = :id RETURNING is_hidden";
        return jdbcClient.sql(sql)
            .param("id", id.value())
            .query((rs, _) -> rs.getBoolean("is_hidden"))
            .single();
    }

    private Personage mapRow(ResultSet rs, int rowNum) throws SQLException {
        final var eventType = Optional.ofNullable(DatabaseUtils.getIntOrNull(rs, "event_type")).map(EventType::get);
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
                rs.getTimestamp("last_energy_change").toLocalDateTime(),
                config.energyFullRecovery()
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
            jsonUtils.fromString(rs.getString("effects"), PersonageEffects.class),
            eventType.map(
                it -> CheckedFunction0.of(
                    () -> new CurrentEvent(it, rs.getTimestamp("launched_event_end_date").toLocalDateTime())
                ).unchecked().apply()
            )
        );
    }
}
