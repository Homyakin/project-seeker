package ru.homyakin.seeker.game.personage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.game.badge.entity.BadgeView;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Energy;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.personage.models.effect.PersonageEffects;
import ru.homyakin.seeker.game.personage.models.PersonageId;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import javax.sql.DataSource;

import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.utils.DatabaseUtils;
import ru.homyakin.seeker.utils.JsonUtils;
import ru.homyakin.seeker.utils.TimeUtils;

@Component
public class PersonageDao {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private static final String GET_BY_ID = """
        SELECT p.*,
            b.code as badge_code,
            pg.tag as pgroup_member_tag,
            p.member_pgroup_id
        FROM personage p
        LEFT JOIN personage_available_badge pab ON p.id = pab.personage_id AND pab.is_active = true
        LEFT JOIN badge b ON pab.badge_id = b.id
        LEFT JOIN pgroup pg ON p.member_pgroup_id = pg.id
        WHERE p.id in (:id_list)
        """;

    private static final String UPDATE = """
        UPDATE personage
        SET name = :name, last_energy_change = :last_energy_change, money = :money,
        energy = :energy, effects = :effects,
        energy_recovery_notification_time = CASE
            WHEN NOT :has_full_energy
                THEN :energy_recovery_notification_time
            WHEN energy_recovery_notification_time IS NOT NULL
                AND :last_energy_change < energy_recovery_notification_time
                THEN :last_energy_change
            ELSE energy_recovery_notification_time
        END
        WHERE id = :id
        """;

    private final SimpleJdbcInsert jdbcInsert;
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;
    private final PersonageConfig config;

    public PersonageDao(DataSource dataSource, JsonUtils jsonUtils, PersonageConfig config) {
        jdbcInsert = new SimpleJdbcInsert(dataSource)
            .withTableName("personage")
            .usingColumns(
                "name",
                "money",
                "last_energy_change",
                "energy",
                "effects"
            );
        this.jsonUtils = jsonUtils;
        this.config = config;
        jdbcInsert.setGeneratedKeyName("id");

        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
        this.jdbcClient = JdbcClient.create(jdbcTemplate);
    }

    public PersonageId createDefault(String name) {
        final var params = new HashMap<String, Object>();
        params.put("name", name);
        params.put("money", config.defaultMoney());
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
            .param("last_energy_change", personage.energy().lastChange())
            .param("energy", personage.energy().value())
            .param("money", personage.money().value())
            .param("effects", jsonUtils.mapToPostgresJson(personage.effects()))
            // Если энергия полная, то не нужно обновлять время восстановления энерги в null
            // Если энергия полная и дата изменения энергии меньше времени восстановления энергии,
            // то нужно обновить время восстановления энергии на дату изменения энергии
            .param("has_full_energy", personage.energy().isFull())
            .param("energy_recovery_notification_time", personage.energy().energyRecoveryTime().orElse(null))
            .update();
    }

    public void clearEnergyRecoveryNotificationTime(PersonageId id) {
        jdbcClient.sql("""
            UPDATE personage
            SET energy_recovery_notification_time = NULL
            WHERE id = :id
            """)
            .param("id", id.value())
            .update();
    }

    public void addMoney(Map<PersonageId, Money> moneyMap) {
        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var entry : moneyMap.entrySet()) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("id", entry.getKey().value())
                .addValue("money", entry.getValue().value());
            parameters.add(paramSource);
        }
        final var sql = """
            UPDATE personage
            SET money = money + :money
            WHERE id = :id
            """;
        jdbcTemplate.batchUpdate(sql, parameters.toArray(new SqlParameterSource[0]));
    }

    public Optional<Personage> getById(PersonageId id) {
        return jdbcClient.sql(GET_BY_ID)
            .param("id_list", List.of(id.value()))
            .query(this::mapRow)
            .optional();
    }

    public List<Personage> getByIds(Set<PersonageId> ids) {
        // TODO Эффективность неизвестна, данный запрос написан просто чтобы работали предметы.
        // на 02.09.24 проблем с производительностью нет
        final var now = System.currentTimeMillis();
        if (ids.isEmpty()) {
            return List.of();
        }
        final var result = jdbcClient.sql(GET_BY_ID)
            .param("id_list", ids.stream().map(PersonageId::value).toList())
            .query(this::mapRow)
            .list();
        if (logger.isDebugEnabled()) {
            logger.debug("Finished getting {} participants by ids in {} ms", result.size(), System.currentTimeMillis() - now);
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

    public long getActivePersonagesCount(LocalDateTime start) {
        final var sql = """
            SELECT COUNT(*) FROM personage
            WHERE is_hidden = FALSE
            AND last_online >= :start
            """;
        return jdbcClient.sql(sql)
            .param("start", start)
            .query((rs, _) -> rs.getLong(1))
            .single();
    }

    public List<PersonageId> getPersonagesWithRecoveredEnergy() {
        final var sql = """
        SELECT p.id FROM personage p
        WHERE p.energy_recovery_notification_time < :now
        """;
        return jdbcClient.sql(sql)
            .param("now", TimeUtils.moscowTime())
            .query((rs, _) -> PersonageId.from(rs.getLong("id")))
            .list();
    }

    private Personage mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Personage(
            PersonageId.from(rs.getLong("id")),
            rs.getString("name"),
            Optional.ofNullable(rs.getString("pgroup_member_tag")),
            DatabaseUtils.getLongOrEmpty(rs, "member_pgroup_id").map(GroupId::from),
            new Money(rs.getInt("money")),
            new Energy(
                rs.getInt("energy"),
                rs.getTimestamp("last_energy_change").toLocalDateTime(),
                config.energyFullRecovery()
            ),
            BadgeView.findByCode(rs.getString("badge_code")),
            jsonUtils.fromString(rs.getString("effects"), PersonageEffects.class)
        );
    }
}
