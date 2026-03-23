package ru.homyakin.seeker.locale.outpost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class OutpostLocalization {
    private static final Resources<OutpostResource> resources = new Resources<>();

    public static void add(Language language, OutpostResource resource) {
        resources.add(language, resource);
    }

    public static String openInPrivateButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::openInPrivateButton);
    }

    public static String outpostNoGroup(Language language) {
        return resources.getOrDefault(language, OutpostResource::outpostNoGroup);
    }

    public static String startBuildingButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::startBuildingButton);
    }

    public static String chooseBuildingTitle(Language language) {
        return resources.getOrDefault(language, OutpostResource::chooseBuildingTitle);
    }

    public static String confirmStartBuilding(Language language, Building building, int fromLevel, int toLevel) {
        if (fromLevel == 0) {
            return StringNamedTemplate.format(
                resources.getOrDefault(language, OutpostResource::confirmNewBuilding),
                Map.of(
                    "building_name", buildingDisplayName(language, building),
                    "to", toLevel
                )
            );
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::confirmUpgradeBuilding),
            Map.of(
                "building_name", buildingDisplayName(language, building),
                "from", fromLevel,
                "to", toLevel
            )
        );
    }

    public static String buildingButtonLabel(Language language, Building building, int fromLevel, int toLevel) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingButtonLabel),
            Map.of(
                "building_name", buildingDisplayName(language, building),
                "from", fromLevel,
                "to", toLevel
            )
        );
    }

    public static String confirmStartButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::confirmStartButton);
    }

    public static String confirmUpgradeButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::confirmUpgradeButton);
    }

    public static String cancelStartButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::cancelStartButton);
    }

    public static String startBuildingSuccessPrivate(Language language) {
        return resources.getOrDefault(language, OutpostResource::startBuildingSuccessPrivate);
    }

    public static String startBuildingCanceled(Language language) {
        return resources.getOrDefault(language, OutpostResource::startBuildingCanceled);
    }

    public static String groupBuildingStarted(
        Language language,
        Building building,
        Personage initiator,
        int fromLevel,
        int toLevel
    ) {
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, building));
        params.put("initiator", LocaleUtils.personageNameWithBadge(initiator));
        params.put("from", fromLevel);
        params.put("to", toLevel);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::groupBuildingStarted),
            params
        );
    }

    public static String notAdminOutpost(Language language) {
        return resources.getOrDefault(language, OutpostResource::notAdminOutpost);
    }

    public static String startBuildingConflict(Language language) {
        return resources.getOrDefault(language, OutpostResource::startBuildingConflict);
    }

    public static String outpost(Language language, List<OutpostSlot> slots) {
        final var lines = slots.stream()
            .map(slot -> formatSlotLine(language, slot))
            .collect(Collectors.joining("\n"));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::outpost),
            Map.of("slots", lines)
        );
    }

    public static String buildingDisplayName(Language language, Building building) {
        return switch (building) {
            case MONOLITH -> resources.getOrDefault(language, OutpostResource::monolith);
        };
    }

    private static String formatSlotLine(Language language, OutpostSlot slot) {
        return switch (slot) {
            case OutpostSlot.BuildingSlot occupied -> formatBuildingLine(language, occupied);
            case OutpostSlot.EmptySlot _ -> resources.getOrDefault(language, OutpostResource::emptySlot);
        };
    }

    private static String formatBuildingLine(Language language, OutpostSlot.BuildingSlot occupied) {
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, occupied.building()));
        params.put("level", occupied.level());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingSlot),
            params
        );
    }
}
