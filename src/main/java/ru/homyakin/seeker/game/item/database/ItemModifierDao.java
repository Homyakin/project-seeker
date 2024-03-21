package ru.homyakin.seeker.game.item.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import javax.sql.DataSource;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.duel.models.Duel;
import ru.homyakin.seeker.game.duel.models.DuelStatus;
import ru.homyakin.seeker.game.item.models.ItemRangeCharacteristics;
import ru.homyakin.seeker.game.item.models.Modifier;
import ru.homyakin.seeker.game.item.models.ModifierLocale;
import ru.homyakin.seeker.game.item.models.ModifierType;
import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingModifier;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.utils.JsonUtils;

@Component
public class ItemModifierDao {
    private final JdbcClient jdbcClient;
    private final JsonUtils jsonUtils;

    public ItemModifierDao(DataSource dataSource, JsonUtils jsonUtils) {
        this.jdbcClient = JdbcClient.create(dataSource);
        this.jsonUtils = jsonUtils;
    }

    public void saveModifier(SavingModifier modifier) {
        final var sql = """
            INSERT INTO item_modifier (code, item_modifier_type_id, characteristics, locale)
            VALUES (:code, :item_modifier_type_id, CAST(:characteristics AS JSON), CAST(:locale AS JSON))
            ON CONFLICT (code) DO
            UPDATE SET item_modifier_type_id = :item_modifier_type_id, characteristics = CAST(:characteristics AS JSON),
            locale = CAST(:locale AS JSON)
            """;

        jdbcClient
            .sql(sql)
            .param("code", modifier.code())
            .param("item_modifier_type_id", modifier.type().id)
            .param("characteristics", jsonUtils.mapToPostgresJson(modifier.characteristics()))
            .param("locale", jsonUtils.mapToPostgresJson(modifier.locales()))
            .update();
    }

    public Modifier getRandomModifier() {
        final var sql = """
            SELECT * FROM item_modifier
            ORDER BY random() LIMIT 1
            """;

        return jdbcClient.sql(sql)
            .query(this::mapRow)
            .optional()
            .orElseThrow();
    }

    @SuppressWarnings("unchecked") // locale парсится как Map без типизации дженериков
    private Modifier mapRow(ResultSet rs, int rowNum) throws SQLException {
        return new Modifier(
            rs.getInt("id"),
            rs.getString("code"),
            ModifierType.findById(rs.getInt("item_modifier_type_id")),
            jsonUtils.fromString(rs.getString("characteristics"), ItemRangeCharacteristics.class),
            (Map<Language, ModifierLocale>) jsonUtils.fromString(rs.getString("locale"), Map.class)
        );
    }
}
