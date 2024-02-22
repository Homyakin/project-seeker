package ru.homyakin.seeker.locale.top;

import ru.homyakin.seeker.game.personage.models.PersonageId;
import ru.homyakin.seeker.game.top.models.TopPosition;
import ru.homyakin.seeker.game.top.models.TopResult;
import ru.homyakin.seeker.locale.Language;

public class TopUtils {
    public static String createTopList(
        Language language,
        PersonageId requestedPersonage,
        TopResult<? extends TopPosition> result
    ) {
        final var positions = result.positions();
        final var positionsToShow = Math.min(MAX_TOP_POSITIONS, positions.size());
        final var personageList = new StringBuilder();
        for (int i = 1; i <= positionsToShow; ++i) {
            personageList.append(positions.get(i - 1).toLocalizedString(language, i));
            if (i != positionsToShow) {
                personageList.append("\n");
            }
        }
        if (positionsToShow < positions.size()) {
            final var requestedIdx = result.findPersonageIndex(requestedPersonage);
            requestedIdx.ifPresent(
                index -> {
                    //Крайний случай, когда персонаж следующий в топе
                    if (index == positionsToShow) {
                        personageList.append("\n").append(positions.get(index).toLocalizedString(language, index + 1));
                    } else if (index > positionsToShow) {
                        personageList
                            .append("\n-----------\n")
                            .append(positions.get(index - 1).toLocalizedString(language, index))
                            .append("\n")
                            .append(positions.get(index).toLocalizedString(language, index + 1));
                        if (positions.size() > index + 1) {
                            personageList.append("\n").append(positions.get(index + 1).toLocalizedString(language, index + 2));
                        }
                    }
                }
            );
        }
        return personageList.toString();
    }

    private static final int MAX_TOP_POSITIONS = 10;
}
