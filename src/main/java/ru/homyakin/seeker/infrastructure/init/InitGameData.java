package ru.homyakin.seeker.infrastructure.init;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.File;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationStartedEvent;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.tavern_menu.MenuService;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItem;
import ru.homyakin.seeker.utils.ResourceUtils;

@Configuration
@ConfigurationProperties("homyakin.seeker.init-game-data")
public class InitGameData {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = TomlMapper.builder()
        .addModule(new JavaTimeModule())
        .addModule(new Jdk8Module())
        .build();
    private final EventService eventService;
    private final MenuService menuService;
    private InitGameDataType type;

    public InitGameData(EventService eventService, MenuService menuService) {
        this.eventService = eventService;
        this.menuService = menuService;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadEvents() {
        logger.info("loading events");
        ResourceUtils.getResourcePath(eventsPath())
            .map(stream -> extractClass(stream, Events.class))
            .ifPresent(events -> {
                events.event().forEach(SavingEvent::validateLocale);
                events.event().forEach(eventService::save);
            });
        logger.info("loaded events");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadMenuItems() {
        logger.info("loading menu items");
        ResourceUtils.getResourcePath(menuItemsPath())
            .map(stream -> extractClass(stream, Items.class))
            .ifPresent(items -> {
                items.item().forEach(MenuItem::validateLocale);
                items.item().forEach(menuService::saveItem);
            });
        logger.info("loaded menu items");
    }

    private <T> T extractClass(InputStream stream, Class<T> clazz) {
        try {
            return mapper.readValue(stream, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse " + clazz.getSimpleName(), e);
        }
    }

    private String eventsPath() {
        return DATA_FOLDER + type.folder() + EVENTS;
    }

    private String menuItemsPath() {
        return DATA_FOLDER + type.folder() + MENU_ITEMS;
    }

    public void setType(InitGameDataType type) {
        this.type = type;
    }

    private static final String DATA_FOLDER = "game-data" + File.separator;
    private static final String EVENTS = File.separator + "events.toml";
    private static final String MENU_ITEMS = File.separator + "menu_items.toml";
}
