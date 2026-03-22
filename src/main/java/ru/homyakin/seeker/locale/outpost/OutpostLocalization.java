package ru.homyakin.seeker.locale.outpost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.locale.Language;
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

    public static String outpost(Language language, List<OutpostSlot> slots) {
        final var lines = slots.stream()
            .map(slot -> formatSlotLine(language, slot))
            .collect(Collectors.joining("\n"));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::outpost),
            Map.of("slots", lines)
        );
    }

    private static String formatSlotLine(Language language, OutpostSlot slot) {
        return switch (slot) {
            case OutpostSlot.BuildingSlot occupied -> formatBuildingLine(language, occupied);
            case OutpostSlot.EmptySlot _ -> resources.getOrDefault(language, OutpostResource::emptySlot);
        };
    }

    private static String formatBuildingLine(Language language, OutpostSlot.BuildingSlot occupied) {
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingName(language, occupied.building()));
        params.put("level", occupied.level());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingSlot),
            params
        );
    }

    private static String buildingName(Language language, Building building) {
        return switch (building) {
            case MONOLITH -> resources.getOrDefault(language, OutpostResource::monolith);
        };
    }
}
