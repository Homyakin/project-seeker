package ru.homyakin.seeker.locale.top;

import ru.homyakin.seeker.game.top.models.TopPosition;
import ru.homyakin.seeker.game.top.models.TopResult;
import ru.homyakin.seeker.locale.Language;

import java.util.List;

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
            final var position = positions.get(i - 1);
            final var positionText = position.toLocalizedString(language, i);
            if (position.id().equals(requestedId)) {
                list.append("<b>").append(positionText).append("</b>");
            } else {
                list.append(positionText);
            }
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
                        list.append("\n").append(positionToString(positions, index, language));
                    } else if (index > positionsToShow) {
                        final var userPositionText = positions.get(index - 1).toLocalizedString(language, index);
                        list
                                .append(SEPARATOR)
                                .append("<b>").append(userPositionText).append("</b>")
                                .append("\n")
                                .append(positionToString(positions, index, language));
                        if (positions.size() > index + 1) {
                            list.append("\n").append(positionToString(positions, index + 1, language));
                        }
                    }
                }
            );
        }
        return list.toString();
    }

    public static <IdType> String createTwoSideTopList(
        Language language,
        IdType requestedId,
        TopResult<IdType, ? extends TopPosition<?>> result
    ) {
        final var positions = result.positions();
        int maxInTop = 5;
        int maxInBottom = 5;

        if (positions.size() <= maxInTop + maxInBottom) {
            final var list = new StringBuilder();
            for (int i = 0; i < positions.size(); ++i) {
                final var position = positions.get(i);
                final var positionText = positionToString(positions, i, language);
                if (position.id().equals(requestedId)) {
                    list.append("<b>").append(positionText).append("</b>");
                } else {
                    list.append(positionText);
                }
                if (i != positions.size() - 1) {
                    list.append("\n");
                }
            }
            return list.toString();
        }

        final var topList = new StringBuilder();
        for (int i = 0; i < maxInTop; ++i) {
            final var position = positions.get(i);
            final var positionText = positionToString(positions, i, language);

            if (position.id().equals(requestedId)) {
                topList.append("<b>").append(positionText).append("</b>");
            } else {
                topList.append(positionText);
            }
            if (i != maxInTop - 1) {
                topList.append("\n");
            }
        }

        final var bottomStart = positions.size() - maxInBottom;
        final var bottomList = new StringBuilder();
        for (int i = bottomStart; i < positions.size(); ++i) {
            final var position = positions.get(i);
            final var positionText = positionToString(positions, i, language);
            if (position.id().equals(requestedId)) {
                bottomList.append("<b>").append(positionText).append("</b>");
            } else {
                bottomList.append(positionText);
            }
            if (i != positions.size() - 1) {
                bottomList.append("\n");
            }
        }

        final var requestedIdxOptional = result.findIdIndex(requestedId);
        final var list = new StringBuilder();
        if (
            requestedIdxOptional.isEmpty()
                || requestedIdxOptional.get() < maxInTop
                || requestedIdxOptional.get() >= bottomStart
        ) {
            list.append(topList).append(SEPARATOR).append(bottomList);
            return list.toString();
        }

        final int requestedIdx = requestedIdxOptional.get();
        list.append(topList);
        final var position = positionToString(positions, requestedIdx, language);
        final var boldPositionText = "<b>" + position + "</b>";

        if (requestedIdx == maxInTop) {
            list.append("\n").append(boldPositionText);
        } else {
            list.append(SEPARATOR).append(boldPositionText);
        }
        if (requestedIdx == bottomStart - 1) {
            list.append("\n");
        } else {
            list.append(SEPARATOR);
        }
        list.append(bottomList);

        return list.toString();
    }

    private static String positionToString(
        List<? extends TopPosition<?>> positions,
        int index,
        Language language
    ) {
        return positions.get(index).toLocalizedString(language, index + 1);
    }

    private static final int MAX_TOP_POSITIONS = 10;
    private static final String SEPARATOR = "\n-----------\n";
}
