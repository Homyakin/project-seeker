package ru.homyakin.seeker.locale.top;

import ru.homyakin.seeker.game.top.models.TopPosition;
import ru.homyakin.seeker.game.top.models.TopResult;
import ru.homyakin.seeker.locale.Language;

public class TopUtils {
    public static <IdType> String createTopList(
        Language language,
        TopResult<IdType, ? extends TopPosition<?>> result
    ) {
        final var positions = result.positions();
        final var positionsToShow = Math.min(MAX_TOP_POSITIONS, positions.size());
        final var list = new StringBuilder();
        for (int i = 1; i <= positionsToShow; ++i) {
            list.append(positions.get(i - 1).toLocalizedString(language, i));
            if (i != positionsToShow) {
                list.append("\n");
            }
        }
        return list.toString();
    }

    public static <IdType> String createTopList(
        Language language,
        IdType requestedId,
        TopResult<IdType, ? extends TopPosition<?>> result
    ) {
        final var positions = result.positions();
        final var positionsToShow = Math.min(MAX_TOP_POSITIONS, positions.size());
        final var list = new StringBuilder();
        for (int i = 1; i <= positionsToShow; ++i) {
            list.append(positions.get(i - 1).toLocalizedString(language, i));
            if (i != positionsToShow) {
                list.append("\n");
            }
        }
        if (positionsToShow < positions.size()) {
            final var requestedIdx = result.findIdIndex(requestedId);
            requestedIdx.ifPresent(
                index -> {
                    //Крайний случай, когда персонаж следующий в топе
                    if (index == positionsToShow) {
                        list.append("\n").append(positions.get(index).toLocalizedString(language, index + 1));
                    } else if (index > positionsToShow) {
                        list
                            .append("\n-----------\n")
                            .append(positions.get(index - 1).toLocalizedString(language, index))
                            .append("\n")
                            .append(positions.get(index).toLocalizedString(language, index + 1));
                        if (positions.size() > index + 1) {
                            list.append("\n").append(positions.get(index + 1).toLocalizedString(language, index + 2));
                        }
                    }
                }
            );
        }
        return list.toString();
    }

    private static final int MAX_TOP_POSITIONS = 10;
}
