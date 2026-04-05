package ru.homyakin.seeker.locale.outpost;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import ru.homyakin.seeker.game.outpost.entity.Building;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildOffer;
import ru.homyakin.seeker.game.outpost.entity.OutpostBuildingProgress;
import ru.homyakin.seeker.game.outpost.entity.OutpostSlot;
import ru.homyakin.seeker.locale.item.ItemLocalization;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.TextConstants;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.game.item.models.Item;
import ru.homyakin.seeker.game.outpost.entity.OutpostDonateError;
import ru.homyakin.seeker.game.outpost.entity.OutpostDonateSuccess;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.utils.AsciiProgressBar;
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

    /**
     * Picker message: title plus one line per offer with required materials (icons stay in text, not on buttons).
     */
    public static String chooseBuildingPicker(Language language, List<OutpostBuildOffer> offers) {
        final var lineTemplate = resources.getOrDefault(language, OutpostResource::chooseBuildingOfferLine);
        final var lines = offers.stream()
            .map(offer -> {
                final var required = offer.materialsRequired();
                final var params = new HashMap<String, Object>();
                params.put("building_name", buildingDisplayName(language, offer.building()));
                params.put("from", offer.fromLevel());
                params.put("to", offer.toLevel());
                params.put("materials_icon", Icons.OUTPOST_MATERIALS);
                params.put("required", required);
                return StringNamedTemplate.format(lineTemplate, params);
            })
            .collect(Collectors.joining("\n"));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::chooseBuildingTitle),
            Map.of("offers_lines", lines)
        );
    }

    public static String confirmStartBuilding(
        Language language,
        Building building,
        int fromLevel,
        int toLevel,
        int materialsRequired
    ) {
        final var materialsLabel = resources.getOrDefault(language, OutpostResource::materialsResource);
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, building));
        params.put("materials", materialsLabel);
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("required", materialsRequired);
        params.put("target_description", buildingLevelDescription(language, building, toLevel));
        if (fromLevel == 0) {
            params.put("to", toLevel);
            return StringNamedTemplate.format(
                resources.getOrDefault(language, OutpostResource::confirmNewBuilding),
                params
            );
        }
        params.put("from", fromLevel);
        params.put("to", toLevel);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::confirmUpgradeBuilding),
            params
        );
    }

    public static String buildingButtonLabel(
        Language language,
        Building building,
        int fromLevel,
        int toLevel
    ) {
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

    public static String backToOutpostMenuButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::backToOutpostMenuButton);
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
        int toLevel,
        int materialsRequired
    ) {
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, building));
        params.put("initiator", LocaleUtils.personageNameWithBadge(initiator));
        params.put("from", fromLevel);
        params.put("to", toLevel);
        params.put("materials", resources.getOrDefault(language, OutpostResource::materialsResource));
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("required", materialsRequired);
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

    public static String buildingMenuUnknownBuilding(Language language) {
        return resources.getOrDefault(language, OutpostResource::buildingMenuUnknownBuilding);
    }

    public static String buildingMenu(Language language, Building building, OutpostSlot slot) {
        return switch (slot) {
            case OutpostSlot.EmptySlot _ -> StringNamedTemplate.format(
                resources.getOrDefault(language, OutpostResource::buildingMenuEmptySlot),
                Map.of("building_name", buildingDisplayName(language, building))
            );
            case OutpostSlot.BuildingSlot occupied -> occupied.progress()
                .map(p -> formatBuildingMenuInProgress(language, occupied, p, occupied.materialsRequired()))
                .orElseGet(() -> formatBuildingMenuIdle(language, occupied));
        };
    }

    public static String outpost(
        Language language,
        List<OutpostSlot> slots,
        boolean showOpenBuildingCommand
    ) {
        final var lines = slots.stream()
            .map(slot -> formatSlotLine(language, slot, showOpenBuildingCommand))
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

    private static String buildingLevelDescription(Language language, Building building, int level) {
        return switch (building) {
            case MONOLITH -> switch (level) {
                case 0 -> resources.getOrDefault(language, OutpostResource::monolithLevel0);
                case 1 -> resources.getOrDefault(language, OutpostResource::monolithLevel1);
                case 2 -> resources.getOrDefault(language, OutpostResource::monolithLevel2);
                case 3 -> resources.getOrDefault(language, OutpostResource::monolithLevel3);
                case 4 -> resources.getOrDefault(language, OutpostResource::monolithLevel4);
                case 5 -> resources.getOrDefault(language, OutpostResource::monolithLevel5);
                default -> "";
            };
        };
    }

    private static String formatSlotLine(
        Language language,
        OutpostSlot slot,
        boolean showOpenBuildingCommand
    ) {
        return switch (slot) {
            case OutpostSlot.BuildingSlot occupied -> formatBuildingLine(
                language,
                occupied,
                showOpenBuildingCommand
            );
            case OutpostSlot.EmptySlot _ -> resources.getOrDefault(language, OutpostResource::emptySlot);
        };
    }

    private static String formatBuildingLine(
        Language language,
        OutpostSlot.BuildingSlot occupied,
        boolean showOpenBuildingCommand
    ) {
        return occupied.progress()
            .map(p -> formatBuildingLineInProgress(
                language,
                occupied,
                p,
                showOpenBuildingCommand,
                occupied.materialsRequired()
            ))
            .orElseGet(() -> {
                final var params = new HashMap<String, Object>();
                params.put("building_name", buildingDisplayName(language, occupied.building()));
                params.put("level", occupied.level());
                params.put(
                    "current_description",
                    buildingLevelDescription(language, occupied.building(), occupied.level())
                );
                return StringNamedTemplate.format(
                    resources.getOrDefault(language, OutpostResource::buildingSlot),
                    params
                );
            });
    }

    private static String formatBuildingLineInProgress(
        Language language,
        OutpostSlot.BuildingSlot occupied,
        OutpostBuildingProgress progress,
        boolean showOpenBuildingCommand,
        int materialsRequired
    ) {
        final var targetLevel = occupied.level() == 0 ? 1 : occupied.level() + 1;
        final var delivered = progress.materialsDelivered();
        final var required = materialsRequired;
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, occupied.building()));
        params.put("level", occupied.level());
        params.put("target_level", targetLevel);
        if (showOpenBuildingCommand) {
            var command = CommandType.OPEN_OUTPOST_BUILDING.getText()
                + TextConstants.TG_COMMAND_DELIMITER
                + occupied.building().id();
            params.put("open_building_command", command);
        }
        params.put("progress_bar", AsciiProgressBar.bracketedBar(delivered, required, AsciiProgressBar.DEFAULT_WIDTH));
        params.put("percent", AsciiProgressBar.percent100(delivered, required));
        final var template = showOpenBuildingCommand
            ? resources.getOrDefault(language, OutpostResource::buildingSlotInProgress)
            : resources.getOrDefault(language, OutpostResource::buildingSlotInProgressGroup);
        return StringNamedTemplate.format(template, params);
    }

    private static String formatBuildingMenuIdle(Language language, OutpostSlot.BuildingSlot occupied) {
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, occupied.building()));
        params.put("level", occupied.level());
        params.put(
            "current_description",
            buildingLevelDescription(language, occupied.building(), occupied.level())
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingMenuIdle),
            params
        );
    }

    private static String formatBuildingMenuInProgress(
        Language language,
        OutpostSlot.BuildingSlot occupied,
        OutpostBuildingProgress progress,
        int materialsRequired
    ) {
        final var targetLevel = occupied.level() == 0 ? 1 : occupied.level() + 1;
        final var delivered = progress.materialsDelivered();
        final var required = materialsRequired;
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, occupied.building()));
        params.put("level", occupied.level());
        params.put("target_level", targetLevel);
        params.put("materials", resources.getOrDefault(language, OutpostResource::materialsResource));
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("delivered", delivered);
        params.put("required", required);
        params.put("progress_bar", AsciiProgressBar.bracketedBar(delivered, required, AsciiProgressBar.DEFAULT_WIDTH));
        params.put("percent", AsciiProgressBar.percent100(delivered, required));
        params.put(
            "current_description",
            buildingLevelDescription(language, occupied.building(), occupied.level())
        );
        params.put(
            "target_description",
            buildingLevelDescription(language, occupied.building(), targetLevel)
        );
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingMenuInProgress),
            params
        );
    }

    public static String makeContributeButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::makeContributeButton);
    }

    public static String contributeRefreshButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::contributeRefreshButton);
    }

    public static String contributeBackButton(Language language) {
        return resources.getOrDefault(language, OutpostResource::contributeBackButton);
    }

    public static String buildingContributeItemLine(
        Language language,
        Item item,
        int materialsValue,
        String donateCommand
    ) {
        final var params = new HashMap<String, Object>();
        params.put("full_item", ItemLocalization.fullItem(language, item));
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("materials_value", materialsValue);
        params.put("donate_command", donateCommand);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingContributeItemLine),
            params
        );
    }

    public static String buildingContributePicker(
        Language language,
        OutpostSlot.BuildingSlot occupied,
        OutpostBuildingProgress progress,
        String itemsBlock
    ) {
        final var building = occupied.building();
        final var targetLevel = occupied.level() == 0 ? 1 : occupied.level() + 1;
        final var delivered = progress.materialsDelivered();
        final var required = occupied.materialsRequired();
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, building));
        params.put("level", occupied.level());
        params.put("target_level", targetLevel);
        params.put("materials", resources.getOrDefault(language, OutpostResource::materialsResource));
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("delivered", delivered);
        params.put("required", required);
        params.put("progress_bar", AsciiProgressBar.bracketedBar(delivered, required, AsciiProgressBar.DEFAULT_WIDTH));
        params.put("percent", AsciiProgressBar.percent100(delivered, required));
        params.put("items_block", itemsBlock);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::buildingContributePicker),
            params
        );
    }

    public static String buildingContributeEmptyBag(Language language) {
        return resources.getOrDefault(language, OutpostResource::buildingContributeEmptyBag);
    }

    public static String donateItemSuccessPrivate(Language language, OutpostDonateSuccess success) {
        final var params = new HashMap<String, Object>();
        params.put("added", success.materialsFromItem());
        params.put("delivered", success.deliveredAfter());
        params.put("required", success.materialsRequired());
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("materials", resources.getOrDefault(language, OutpostResource::materialsResource));
        final var successLine = StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::donateItemSuccess),
            params
        );
        if (!success.completed()) {
            return successLine;
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::donateItemBuildingCompletePrivate),
            Map.of(
                "donate_item_success", successLine,
                "new_level", success.newLevel()
            )
        );
    }

    public static String donateBuildingNotInProgress(Language language) {
        return resources.getOrDefault(language, OutpostResource::donateBuildingNotInProgress);
    }

    public static String donateItemError(Language language, OutpostDonateError error) {
        return switch (error) {
            case OutpostDonateError.NoGroup _ -> outpostNoGroup(language);
            case OutpostDonateError.BuildingNotInProgress _ -> donateBuildingNotInProgress(language);
            case OutpostDonateError.ItemNotFound _ ->
                resources.getOrDefault(language, OutpostResource::donateItemNotFound);
            case OutpostDonateError.ItemEquipped _ ->
                resources.getOrDefault(language, OutpostResource::donateItemEquipped);
            case OutpostDonateError.Busy _ -> resources.getOrDefault(language, OutpostResource::donateBusy);
            case OutpostDonateError.StateConflict _ ->
                resources.getOrDefault(language, OutpostResource::donateStateConflict);
        };
    }

    public static String groupBuildingCompletedContributorLine(Language language, String displayName, int materials) {
        final var params = new HashMap<String, Object>();
        params.put("name", displayName);
        params.put("materials", materials);
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::groupBuildingCompletedContributorLine),
            params
        );
    }

    public static String groupBuildingCompletedWithTop(
        Language language,
        Building building,
        int newLevel,
        List<String> contributorLines
    ) {
        final var lines = contributorLines.isEmpty() ? "—" : String.join("\n", contributorLines);
        final var params = new HashMap<String, Object>();
        params.put("building_name", buildingDisplayName(language, building));
        params.put("new_level", newLevel);
        params.put("materials_icon", Icons.OUTPOST_MATERIALS);
        params.put("materials", resources.getOrDefault(language, OutpostResource::materialsResource));
        params.put("contributors_lines", lines);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, OutpostResource::groupBuildingCompletedTop),
            params
        );
    }

}
