package ru.homyakin.seeker.game.event.database;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Period;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.postgresql.util.PGInterval;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.models.Event;
import ru.homyakin.seeker.game.event.models.EventLocale;
import ru.homyakin.seeker.game.event.models.EventType;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingEvent;
import ru.homyakin.seeker.locale.Language;

@Component
public class EventDao {
    // На маленьких данных работает быстро. Если понадобится ускорить - https://habr.com/ru/post/242999/
    private static final String GET_RANDOM_EVENT = "SELECT * FROM event WHERE is_enabled = true ORDER BY random() LIMIT 1";
    private static final String GET_EVENT_BY_ID = "SELECT * FROM event WHERE id = :id";
    private static final String GET_EVENT_LOCALES = "SELECT * FROM event_locale WHERE event_id = :event_id";
    private static final String SAVE_EVENT = """
        INSERT INTO event (id, type_id, duration, is_enabled)
        VALUES (:id, :type_id, :duration, :is_enabled)
        ON CONFLICT (id)
        DO UPDATE SET type_id = :type_id, duration = :duration, is_enabled = :is_enabled
        """;
    private static final String SAVE_LOCALES = """
        INSERT INTO event_locale (event_id, language_id, intro, description) 
        VALUES (:event_id, :language_id, :intro, :description)
        ON CONFLICT (event_id, language_id)
        DO UPDATE SET intro = :intro, description = :description
        """;
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

    public void save(SavingEvent event) {
        jdbcClient.sql(SAVE_EVENT)
            .param("id", event.id())
            .param("type_id", event.type().id())
            .param("duration", fromDuration(event.duration()))
            .param("is_enabled", event.isEnabled())
            .update();
        saveLocales(event);
    }

    private void saveLocales(SavingEvent event) {
        event.locales().forEach(
            locale -> jdbcClient.sql(SAVE_LOCALES)
                .param("event_id", event.id())
                .param("language_id", locale.language().id())
                .param("intro", locale.intro())
                .param("description", locale.description())
                .update()
        );
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

    private PGInterval fromDuration(Duration duration) {
        final var interval = new PGInterval();
        interval.setHours(duration.toHoursPart());
        interval.setMinutes(duration.toMinutesPart());
        interval.setSeconds(duration.toSecondsPart());
        return interval;
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
