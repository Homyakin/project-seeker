package ru.homyakin.seeker.telegram.group.taver_menu;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowResult;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;

public sealed interface ThrowResultTg {
    Money cost();

    Category category();

    record ThrowToNone(ThrowResult.ThrowToNone domain) implements ThrowResultTg {
        @Override
        public Money cost() {
            return domain.cost();
        }

        @Override
        public Category category() {
            return domain.category();
        }
    }

    record ThrowToOtherPersonage(Money cost, TgPersonageMention personage, Effect effect, Category category) implements ThrowResultTg {
    }

    record SelfThrow(ThrowResult.SelfThrow domain) implements ThrowResultTg {
        @Override
        public Money cost() {
            return domain.cost();
        }

        @Override
        public Category category() {
            return domain.category();
        }
    }

    record ThrowToStaff(ThrowResult.ThrowToStaff domain) implements ThrowResultTg {
        @Override
        public Money cost() {
            return domain.cost();
        }

        @Override
        public Category category() {
            return domain.category();
        }
    }
}
