package ru.homyakin.seeker.game.top.models;

import ru.homyakin.seeker.common.models.GroupId;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.top.TopLocalization;

import java.util.List;

public record GroupTopRaidLevelResult(
    List<GroupTopRaidLevelPosition> positions
) implements GroupTopResult<GroupTopRaidLevelPosition> {
    @Override
    public String toLocalizedString(Language language, GroupId requestedId) {
        if (positions.isEmpty()) {
            return TopLocalization.topRaidLevelEmpty(language);
        } else {
            return TopLocalization.topGroupRaidLevel(language, requestedId, this);
        }
    }
}
