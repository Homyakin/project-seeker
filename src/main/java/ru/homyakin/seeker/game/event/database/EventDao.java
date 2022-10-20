package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.locale.Language;
import org.postgresql.util.PGInterval;

@Component
public class EventDao {
    // На маленьких данных работает быстро. Если понадобится ускорить - https://habr.com/ru/post/242999/
    private static final String GET_RANDOM_EVENT = "SELECT * FROM event ORDER BY random() LIMIT 1";
    private static final String GET_EVENT_BY_ID = "SELECT * FROM event WHERE id = :id";
    private static final String GET_EVENT_LOCALES = "SELECT * FROM event_locale WHERE event_id = :event_id";
    private static final EventRowMapper EVENT_ROW_MAPPER = new EventRowMapper();
    private static final EventLocaleMapper EVENT_LOCALE_ROW_MAPPER = new EventLocaleMapper();
    private final NamedParameterJdbcTemplate jdbcTemplate;

    public EventDao(DataSource dataSource) {
        jdbcTemplate = new NamedParameterJdbcTemplate(dataSource);
    }

    public Event getRandomEvent() {
        final var result = jdbcTemplate.query(
            GET_RANDOM_EVENT,
            EVENT_ROW_MAPPER
        );
        final var eventWithoutLocale = result
            .stream()
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No events in database")); // TODO either
        final var locales = getEventLocales(eventWithoutLocale.id());
        return eventWithoutLocale.toEvent(locales);
    }

    public Optional<Event> getById(Integer eventId) {
        final var params = Collections.singletonMap("id", eventId);
        final var result = jdbcTemplate.query(
            GET_EVENT_BY_ID,
            params,
            EVENT_ROW_MAPPER
        );
        final var eventWithoutLocale = result
            .stream()
            .findFirst();
        if (eventWithoutLocale.isEmpty()) {
            return Optional.empty();
        }
        final var locales = getEventLocales(eventWithoutLocale.get().id());
        return Optional.of(eventWithoutLocale.get().toEvent(locales));
    }

    private List<EventLocale> getEventLocales(int eventId) {
        final var params = Collections.singletonMap("event_id", eventId);
        return jdbcTemplate.query(
            GET_EVENT_LOCALES,
            params,
            EVENT_LOCALE_ROW_MAPPER
        );
    }

    private static class EventRowMapper implements RowMapper<EventWithoutLocale> {
        @Override
        public EventWithoutLocale mapRow(ResultSet rs, int rowNum) throws SQLException {
            final var pgInterval = (PGInterval) rs.getObject("duration");
            final var period = Period.of(pgInterval.getYears(), pgInterval.getMonths(), pgInterval.getDays());
            final var duration = Duration.ofHours(pgInterval.getHours())
                .plus(pgInterval.getMinutes(), ChronoUnit.MINUTES)
                .plus(pgInterval.getWholeSeconds(), ChronoUnit.SECONDS)
                .plus(pgInterval.getMicroSeconds(), ChronoUnit.MICROS);
            return new EventWithoutLocale(
                rs.getInt("id"),
                period,
                duration,
                EventType.get(rs.getInt("type"))
            );
        }
    }

    private static class EventLocaleMapper implements RowMapper<EventLocale> {
        @Override
        public EventLocale mapRow(ResultSet rs, int rowNum) throws SQLException {
            return new EventLocale(
                Language.getOrDefault(rs.getInt("lang")),
                rs.getString("name"),
                rs.getString("description")
            );
        }
    }

    private record EventWithoutLocale(
        Integer id,
        Period period,
        Duration duration,
        EventType type
    ) {
        public Event toEvent(List<EventLocale> locales) {
            return new Event(
                id,
                period,
                duration,
                type,
                locales
            );
        }
    }
}
