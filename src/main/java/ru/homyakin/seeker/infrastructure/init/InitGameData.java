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
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import ru.homyakin.seeker.game.event.service.EventService;
import ru.homyakin.seeker.game.item.ItemService;
import ru.homyakin.seeker.game.item.modifier.ItemModifierService;
import ru.homyakin.seeker.infrastructure.init.saving_models.PersonalQuests;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingPersonalQuest;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingItemObject;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.SavingModifier;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemModifiers;
import ru.homyakin.seeker.infrastructure.init.saving_models.item.ItemObjects;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingBadge;
import ru.homyakin.seeker.game.personage.badge.BadgeService;
import ru.homyakin.seeker.game.rumor.Rumor;
import ru.homyakin.seeker.game.rumor.RumorService;
import ru.homyakin.seeker.game.tavern_menu.menu.MenuService;
import ru.homyakin.seeker.infrastructure.init.saving_models.Badges;
import ru.homyakin.seeker.infrastructure.init.saving_models.Raids;
import ru.homyakin.seeker.infrastructure.init.saving_models.Items;
import ru.homyakin.seeker.infrastructure.init.saving_models.Rumors;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingRaid;
import ru.homyakin.seeker.infrastructure.init.saving_models.SavingMenuItem;
import ru.homyakin.seeker.locale.LocalizationCoverage;
import ru.homyakin.seeker.utils.ResourceUtils;

@Component
public class InitGameData {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ObjectMapper mapper = TomlMapper.builder()
        .addModule(new JavaTimeModule())
        .addModule(new Jdk8Module())
        .build();
    private final EventService eventService;
    private final MenuService menuService;
    private final RumorService rumorService;
    private final BadgeService badgeService;
    private final ItemService itemService;
    private final ItemModifierService itemModifierService;
    private final InitGameDataConfig config;

    public InitGameData(
        EventService eventService,
        MenuService menuService,
        RumorService rumorService,
        BadgeService badgeService,
        ItemService itemService,
        ItemModifierService itemModifierService,
        InitGameDataConfig config
    ) {
        this.eventService = eventService;
        this.menuService = menuService;
        this.rumorService = rumorService;
        this.badgeService = badgeService;
        this.itemService = itemService;
        this.itemModifierService = itemModifierService;
        this.config = config;
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadRaids() {
        logger.info("loading raids");
        ResourceUtils.doAction(
            raidsPath(),
            stream -> {
                final var raids = extractClass(stream, Raids.class);
                LocalizationCoverage.addRaidsInfo(raids);
                raids.raid().forEach(SavingRaid::validateLocale);
                raids.raid().forEach(eventService::saveRaid);
            }
        );
        logger.info("loaded raids");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadPersonalQuests() {
        logger.info("loading personal quests");
        ResourceUtils.doAction(
            personalQuestsPath(),
            stream -> {
                final var personalQuests = extractClass(stream, PersonalQuests.class);
                LocalizationCoverage.addPersonalQuestsInfo(personalQuests);
                personalQuests.quest().forEach(SavingPersonalQuest::validateLocale);
                personalQuests.quest().forEach(eventService::savePersonalQuest);
            }
        );
        logger.info("loaded personal quests");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadMenuItems() {
        logger.info("loading menu items");
        ResourceUtils.doAction(
            menuItemsPath(),
            stream -> {
                final var items = extractClass(stream, Items.class);
                LocalizationCoverage.addMenuItemsInfo(items);
                items.item().forEach(SavingMenuItem::validateLocale);
                items.item().forEach(menuService::saveItem);
            }
        );
        logger.info("loaded menu items");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadRumors() {
        logger.info("loading rumors");
        ResourceUtils.doAction(
            rumorsPath(),
            stream -> {
                final var rumors = extractClass(stream, Rumors.class);
                LocalizationCoverage.addRumorsInfo(rumors);
                rumors.rumor().forEach(Rumor::validateLocale);
                rumors.rumor().forEach(rumorService::save);
            }
        );
        logger.info("loaded rumors");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadBadges() {
        logger.info("loading badges");
        ResourceUtils.doAction(
            BADGES,
            stream -> {
                final var badges = extractClass(stream, Badges.class);
                LocalizationCoverage.addBadgesInfo(badges);
                badges.badge().forEach(SavingBadge::validateLocale);
                badges.badge().forEach(badgeService::save);
            }
        );
        logger.info("loaded badges");
    }

    @EventListener(ApplicationStartedEvent.class)
    public void loadItems() {
        logger.info("loading items");
        ResourceUtils.doAction(
            ITEM_OBJECTS,
            stream -> {
                final var itemObjects = extractClass(stream, ItemObjects.class);
                LocalizationCoverage.addItemObjectsInfo(itemObjects);
                itemObjects.object().forEach(SavingItemObject::validateLocale);
                itemService.saveObjects(itemObjects);
            }
        );
        ResourceUtils.doAction(
            ITEM_MODIFIERS,
            stream -> {
                final var itemModifiers = extractClass(stream, ItemModifiers.class);
                LocalizationCoverage.addIteModifiersInfo(itemModifiers);
                itemModifiers.modifier().forEach(SavingModifier::validateLocale);
                itemModifiers.modifier().forEach(SavingModifier::validateWordForms);
                itemModifierService.saveModifiers(itemModifiers);
            }
        );
        logger.info("loaded items");
    }

    private <T> T extractClass(InputStream stream, Class<T> clazz) {
        try {
            return mapper.readValue(stream, clazz);
        } catch (Exception e) {
            throw new RuntimeException("Can't parse " + clazz.getSimpleName(), e);
        }
    }

    private String raidsPath() {
        return DATA_FOLDER + config.type().folder() + RAIDS;
    }

    private String personalQuestsPath() {
        return PERSONAL_QUESTS;
    }

    private String menuItemsPath() {
        return DATA_FOLDER + config.type().folder() + MENU_ITEMS;
    }

    private String rumorsPath() {
        return DATA_FOLDER + config.type().folder() + RUMORS;
    }

    private static final String DATA_FOLDER = "game-data" + File.separator;
    private static final String RAIDS = File.separator + "raids.toml";
    private static final String PERSONAL_QUESTS = DATA_FOLDER + "personal_quests.toml";
    private static final String MENU_ITEMS = File.separator + "menu_items.toml";
    private static final String RUMORS = File.separator + "rumors.toml";
    private static final String BADGES = DATA_FOLDER + "badges.toml";
    private static final String ITEM_OBJECTS = DATA_FOLDER + "item_objects.toml";
    private static final String ITEM_MODIFIERS = DATA_FOLDER + "item_modifiers.toml";
}
