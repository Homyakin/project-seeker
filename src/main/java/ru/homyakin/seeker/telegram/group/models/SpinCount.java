package ru.homyakin.seeker.telegram.group.models;

import java.util.Comparator;
import java.util.List;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.spin.EverydaySpinLocalization;

public record SpinCount(
    List<PersonageCount> userCounts
) {
    public String text(Language language) {
        if (userCounts.isEmpty()) {
            return EverydaySpinLocalization.noChosenUsers(language);
        } else {
            final var top = new StringBuilder(EverydaySpinLocalization.topChosenUsers(language)).append("\n");
            final var sorted = userCounts.stream().sorted(Comparator.comparing(PersonageCount::count).reversed()).toList();
            //TODO если вызвавшего пользователя не показало, показать место
            for (int i = 1; i <= Math.min(sorted.size(), 50); ++i) {
                final var item = sorted.get(i - 1);
                top.append("\n").append(i).append(". ").append(item.name()).append(": ").append(item.count());
            }
            return top.toString();
        }
    }
}
