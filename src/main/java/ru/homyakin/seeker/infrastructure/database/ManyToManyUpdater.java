package ru.homyakin.seeker.infrastructure.database;

import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

@Repository
public class ManyToManyUpdater {
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public ManyToManyUpdater(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    // Обнволение связей id в таблицах many-to-many, например item_object_to_personage_slot
    // Данный код нельзя вызывать с пользовательскими данными
    @Transactional
    public void update(
        String table,
        String objectColumn,
        String relationColumn,
        Integer objectId,
        List<Integer> relationsIds
    ) {
        // Удалить предыдущие связи
        final var deleteSql = String.format("DELETE FROM %s WHERE %s = :object_id", table, objectColumn);
        final var deleteParams = new HashMap<String, Object>();
        deleteParams.put("object_id", objectId);
        jdbcTemplate.update(deleteSql, deleteParams);

        // Добавить новые связи
        final var insertSql = String.format(
            "INSERT INTO %s (%s, %s) VALUES (:object_id, :relation_id)", table, objectColumn, relationColumn
        );

        final var parameters = new ArrayList<SqlParameterSource>();
        for (final var relationId: relationsIds) {
            MapSqlParameterSource paramSource = new MapSqlParameterSource()
                .addValue("object_id", objectId)
                .addValue("relation_id", relationId);
            parameters.add(paramSource);
        }
        jdbcTemplate.batchUpdate(insertSql, parameters.toArray(new SqlParameterSource[0]));
    }
}
