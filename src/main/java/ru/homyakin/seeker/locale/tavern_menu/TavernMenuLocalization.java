package ru.homyakin.seeker.locale.tavern_menu;

import java.util.Collections;
import java.util.HashMap;
import java.util.Optional;

import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.group.entity.Group;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.personage.models.Personage;
import ru.homyakin.seeker.game.tavern_menu.menu.models.Category;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItem;
import ru.homyakin.seeker.game.tavern_menu.order.models.ConsumeResult;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowOrderError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowToGroupError;
import ru.homyakin.seeker.game.tavern_menu.order.models.ThrowToGroupResult;
import ru.homyakin.seeker.infrastructure.Icons;
import ru.homyakin.seeker.infrastructure.PersonageMention;
import ru.homyakin.seeker.locale.Language;
import ru.homyakin.seeker.locale.LocaleUtils;
import ru.homyakin.seeker.locale.Resources;
import ru.homyakin.seeker.locale.common.CommonLocalization;
import ru.homyakin.seeker.telegram.command.type.CommandType;
import ru.homyakin.seeker.telegram.group.taver_menu.ThrowOrderTgError;
import ru.homyakin.seeker.telegram.group.taver_menu.ThrowResultTg;
import ru.homyakin.seeker.telegram.models.TgPersonageMention;
import ru.homyakin.seeker.utils.StringNamedTemplate;

public class TavernMenuLocalization {
    private static final Resources<TavernMenuResource> resources = new Resources<>();

    public static void add(Language language, TavernMenuResource resource) {
        resources.add(language, resource);
    }

    public static String menuHeader(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::menuHeader);
    }

    public static String drinks(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::drinks);
    }

    public static String itemNotInMenu(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::itemNotInMenu);
    }

    public static String mainDishes(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::mainDishes);
    }

    public static String notEnoughMoneyDrink(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            resources.getOrDefaultRandom(language, TavernMenuResource::notEnoughMoneyDrink),
            itemCost,
            personageMoneyValue
        );
    }

    public static String notEnoughMoneyMainDish(Language language, Money itemCost, Money personageMoneyValue) {
        return notEnoughMoney(
            resources.getOrDefaultRandom(language, TavernMenuResource::notEnoughMoneyMainDish),
            itemCost,
            personageMoneyValue
        );
    }

    public static String orderGiftToDifferentBot(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::orderGiftToDifferentBot);
    }

    public static String orderDrinkToThisBot(Language language, Category category) {
        return switch (category) {
            case DRINK -> resources.getOrDefaultRandom(language, TavernMenuResource::orderDrinkToThisBot);
            case MAIN_DISH -> resources.getOrDefaultRandom(language, TavernMenuResource::orderMainDishToThisBot);
        };
    }

    public static String order(Language language, MenuItem item, TgPersonageMention giver, TgPersonageMention acceptor) {
        final var params = new HashMap<String, Object>();
        params.put("item_name", item.name(language));
        params.put("mention_acceptor_icon_with_name", acceptor.value());
        final String text;
        if (!giver.equals(acceptor)) {
            params.put("mention_giver_icon_with_name", giver.value());
            text = resources.getOrDefaultRandom(language, TavernMenuResource::orderGift);
        } else {
            text = resources.getOrDefaultRandom(language, TavernMenuResource::order);
        }
        return StringNamedTemplate.format(text, params);
    }

    private static String notEnoughMoney(String template, Money itemCost, Money personageMoneyValue) {
        final var params = new HashMap<String, Object>();
        params.put("money_icon", Icons.MONEY);
        params.put("not_enough_money_value", itemCost.value() - personageMoneyValue.value());
        params.put("item_cost", itemCost.value());
        return StringNamedTemplate.format(
            template,
            params
        );
    }

    public static String consumeDrinkButton(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeDrinkButton);
    }

    public static String consumeMainDishButton(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeMainDishButton);
    }

    public static String wrongConsumer(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::wrongConsumer);
    }

    public static String consumeAlreadyInFinalStatus(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::consumeAlreadyInFinalStatus);
    }

    public static String consumed(Language language, ConsumeResult result) {
        final var params = new HashMap<String, Object>();
        params.put("consumed_item_text", result.item().consumeText(language, result.personage()));
        params.put("effect", CommonLocalization.effect(language, result.effect().effect()));

        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::consumed),
            params
        );
    }

    public static String expiredOrder(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::expiredOrder);
    }

    public static String orderToUnknownUser(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::orderToUnknownUser);
    }

    public static String orderIsLocked(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::orderIsLocked);
    }

    public static String throwOrderError(Language language, ThrowOrderTgError error) {
        return switch (error) {
            case ThrowOrderTgError.Domain domain -> throwOrderError(language, domain.error());
            case ThrowOrderTgError.UserNotFound _ -> userNotFoundToThrow(language);
        };
    }

    private static String userNotFoundToThrow(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::userNotFoundToThrow),
            Collections.singletonMap("get_profile_command", CommandType.GET_PROFILE.getText())
        );
    }

    private static String throwOrderError(Language language, ThrowOrderError error) {
        return switch (error) {
            case ThrowOrderError.NoOrders _ -> noOrdersToThrow(language);
            case ThrowOrderError.NotEnoughMoney _ -> notEnoughMoneyToThrow(language);
            case ThrowOrderError.OnlyCreatedOrders onlyCreatedOrders -> onlyCreatedOrder(language, onlyCreatedOrders);
            case ThrowOrderError.OrderLocked _ -> orderIsLocked(language);
        };
    }

    public static String throwResult(Language language, ThrowResultTg result) {
        final var text = throwResultText(language, result);
        final var effect = switch (result) {
            case ThrowResultTg.SelfThrow selfThrow -> Optional.of(selfThrow.domain().effect());
            case ThrowResultTg.ThrowToNone _ -> Optional.<Effect>empty();
            case ThrowResultTg.ThrowToOtherPersonage throwToOtherPersonage ->
                Optional.of(throwToOtherPersonage.effect());
            case ThrowResultTg.ThrowToStaff throwToStaff -> Optional.of(throwToStaff.domain().effect());
        };
        final var params = new HashMap<String, Object>();
        params.put("throw_text", text);
        params.put("money_icon", Icons.MONEY);
        params.put("money_value", result.cost().value());
        if (effect.isEmpty()) {
            params.put("optional_effect", "");
        } else {
            params.put("optional_effect", throwEffect(language, effect.get()));
        }
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwResult),
            params
        );
    }

    public static String throwToGroupMissingGroup(Language language) {
        final var params = new HashMap<String, Object>();
        params.put("throw_to_group_command", CommandType.THROW_ORDER_TO_GROUP.getText());
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwToGroupMissingGroup),
            params
        );
    }

    public static String throwToGroupError(Language language, ThrowToGroupError error) {
        return switch (error) {
            case ThrowOrderError throwOrderError -> throwOrderError(language, throwOrderError);
            case ThrowToGroupError.InternalError _ -> CommonLocalization.internalError(language);
            case ThrowToGroupError.NotGroupMember _ -> throwToGroupNotMember(language);
            case ThrowToGroupError.NotRegisteredGroup _ -> CommonLocalization.onlyForRegisteredGroup(language);
            case ThrowToGroupError.TargetGroupIsEmpty _ -> throwToGroupNoPersonage(language);
            case ThrowToGroupError.TargetGroupNotFound _ -> throwToGroupNotExists(language);
            case ThrowToGroupError.TargetGroupTimeout _ -> throwToGroupTargetTimeout(language);
            case ThrowToGroupError.ThrowingGroupTimeout throwingGroupTimeout ->
                throwToGroupTimeout(language, throwingGroupTimeout);
            case ThrowToGroupError.ThrowToThisGroup _ -> throwToThisGroup(language);
        };
    }

    public static String throwToGroupResult(Language language, ThrowToGroupResult result) {
        final var throwResult = switch (result.category()) {
            case DRINK -> throwDrinkToGroup(language, result.targetGroup(), result.target());
            case MAIN_DISH -> throwDishToGroup(language, result.targetGroup(), result.target());
        };
        final var params = new HashMap<String, Object>();
        params.put("throw_text", throwResult);
        params.put("money_value", result.cost().value());
        params.put("money_icon", Icons.MONEY);
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwToGroupResult),
            params
        );
    }

    public static String throwFromGroupResult(
        Language language,
        Group fromGroup,
        PersonageMention mention,
        Effect effect,
        Category category
    ) {
        final var throwResult = switch (category) {
            case DRINK -> throwDrinkFromGroup(language, fromGroup, mention);
            case MAIN_DISH -> throwDishFromGroup(language, fromGroup, mention);
        };
        final var params = new HashMap<String, Object>();
        params.put("throw_text", throwResult);
        params.put("effect", CommonLocalization.effect(language, effect));
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwFromGroupResult),
            params
        );
    }

    private static String throwDrinkToGroup(Language language, Group group, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("to_group", LocaleUtils.groupNameWithBadge(group));
        params.put("personage", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDrinkToGroup),
            params
        );
    }

    private static String throwDishToGroup(Language language, Group group, Personage personage) {
        final var params = new HashMap<String, Object>();
        params.put("to_group", LocaleUtils.groupNameWithBadge(group));
        params.put("personage", LocaleUtils.personageNameWithBadge(personage));
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDishToGroup),
            params
        );
    }

    private static String throwDrinkFromGroup(Language language, Group group, PersonageMention mention) {
        final var params = new HashMap<String, Object>();
        params.put("from_group", LocaleUtils.groupNameWithBadge(group));
        params.put("mention_personage", mention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDrinkFromGroup),
            params
        );
    }

    private static String throwDishFromGroup(Language language, Group group, PersonageMention mention) {
        final var params = new HashMap<String, Object>();
        params.put("from_group", LocaleUtils.groupNameWithBadge(group));
        params.put("mention_personage", mention.value());
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDishFromGroup),
            params
        );
    }

    private static String throwToGroupNotMember(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::throwToGroupNotMember);
    }

    private static String throwToGroupNoPersonage(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::throwToGroupNoPersonage);
    }

    private static String throwToGroupNotExists(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::throwToGroupNotExists);
    }

    private static String throwToThisGroup(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::throwToThisGroup);
    }

    private static String throwToGroupTimeout(Language language, ThrowToGroupError.ThrowingGroupTimeout error) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwToGroupTimeout),
            Collections.singletonMap("duration", CommonLocalization.duration(language, error.timeout()))
        );
    }

    private static String throwToGroupTargetTimeout(Language language) {
        return resources.getOrDefault(language, TavernMenuResource::throwToGroupTargetTimeout);
    }

    private static String throwResultText(Language language, ThrowResultTg result) {
        return switch (result.category()) {
            case DRINK -> throwDrinkText(language, result);
            case MAIN_DISH -> throwDishText(language, result);
        };
    }

    private static String throwDishText(Language language, ThrowResultTg result) {
        return switch (result) {
            case ThrowResultTg.SelfThrow _ -> selfThrowDish(language);
            case ThrowResultTg.ThrowToNone _ -> throwDishToNone(language);
            case ThrowResultTg.ThrowToOtherPersonage throwToOtherPersonage ->
                throwDishToPersonage(language, throwToOtherPersonage.personage());
            case ThrowResultTg.ThrowToStaff _ -> throwToStaff(language);
        };
    }

    private static String throwDrinkText(Language language, ThrowResultTg result) {
        return switch (result) {
            case ThrowResultTg.SelfThrow _ -> selfThrowDrink(language);
            case ThrowResultTg.ThrowToNone _ -> throwDrinkToNone(language);
            case ThrowResultTg.ThrowToOtherPersonage throwToOtherPersonage ->
                throwDrinkToPersonage(language, throwToOtherPersonage.personage());
            case ThrowResultTg.ThrowToStaff _ -> throwToStaff(language);
        };
    }

    private static String throwDishToNone(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::throwDishToNone);
    }

    private static String throwDrinkToNone(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::throwDrinkToNone);
    }

    private static String throwDishToPersonage(Language language, TgPersonageMention mention) {
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDishToPersonage),
            Collections.singletonMap("mention_target_icon_with_name", mention.value())
        );
    }

    private static String throwDrinkToPersonage(Language language, TgPersonageMention mention) {
        return StringNamedTemplate.format(
            resources.getOrDefaultRandom(language, TavernMenuResource::throwDrinkToPersonage),
            Collections.singletonMap("mention_target_icon_with_name", mention.value())
        );
    }

    private static String selfThrowDish(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::selfThrowDish);
    }

    private static String selfThrowDrink(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::selfThrowDrink);
    }

    private static String throwToStaff(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::throwToStaff);
    }

    private static String noOrdersToThrow(Language language) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::noOrdersToThrow),
            Collections.singletonMap("menu_command", CommandType.TAVERN_MENU.getText())
        );
    }

    private static String onlyCreatedOrder(Language language, ThrowOrderError.OnlyCreatedOrders error) {
        return switch (error.category()) {
            case DRINK -> resources.getOrDefaultRandom(language, TavernMenuResource::onlyCreatedDrink);
            case MAIN_DISH -> resources.getOrDefaultRandom(language, TavernMenuResource::onlyCreatedDish);
        };
    }

    private static String notEnoughMoneyToThrow(Language language) {
        return resources.getOrDefaultRandom(language, TavernMenuResource::notEnoughMoneyToThrow);
    }

    private static String throwEffect(Language language, Effect effect) {
        return StringNamedTemplate.format(
            resources.getOrDefault(language, TavernMenuResource::throwEffect),
            Collections.singletonMap("effect", CommonLocalization.effect(language, effect))
        );
    }
}
