package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.locale.Language;
import org.postgresql.util.PGInterval;

@Component
public class EventDao {
    // На маленьких данных работает быстро. Если понадобится ускорить - https://habr.com/ru/post/242999/
    private static final String GET_RANDOM_EVENT = "SELECT * FROM event WHERE is_enabled = true ORDER BY random() LIMIT 1";
    private static final String GET_EVENT_BY_ID = "SELECT * FROM event WHERE id = :id";
    private static final String GET_EVENT_LOCALES = "SELECT * FROM event_locale WHERE event_id = :event_id";
    private final JdbcClient jdbcClient;

    public EventDao(DataSource dataSource) {
        jdbcClient = JdbcClient.create(dataSource);
    }

    public Optional<Event> getRandomEvent() {
        return jdbcClient.sql(GET_RANDOM_EVENT)
            .query(this::mapEvent)
            .optional()
            .map(
                eventWithoutLocale -> {
                    final var locales = getEventLocales(eventWithoutLocale.id());
                    return eventWithoutLocale.toEvent(locales);
                }
            );

    }

    public Optional<Event> getById(Integer eventId) {
        final var eventWithoutLocale = jdbcClient.sql(GET_EVENT_BY_ID)
            .param("id", eventId)
            .query(this::mapEvent)
            .optional();
        if (eventWithoutLocale.isEmpty()) {
            return Optional.empty();
        }
        final var locales = getEventLocales(eventWithoutLocale.get().id());
        return Optional.of(eventWithoutLocale.get().toEvent(locales));
    }

    private List<EventLocale> getEventLocales(int eventId) {
        return jdbcClient.sql(GET_EVENT_LOCALES)
            .param("event_id", eventId)
            .query(this::mapEventLocale)
            .list();
    }

    private EventWithoutLocale mapEvent(ResultSet rs, int rowNum) throws SQLException {
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
            EventType.get(rs.getInt("type_id"))
        );
    }

    private EventLocale mapEventLocale(ResultSet rs, int rowNum) throws SQLException {
        return new EventLocale(
            Language.getOrDefault(rs.getInt("language_id")),
            rs.getString("intro"),
            rs.getString("description")
        );
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
