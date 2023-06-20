package ru.homyakin.seeker.game.battle;

import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.common.CommonLocalization;

public record PersonageResult(
    Personage personage,
    BattlePersonage battlePersonage
) {
    public String statsText(Language language) {
        return CommonLocalization.personageBattleResult(language, this);
    }
}
