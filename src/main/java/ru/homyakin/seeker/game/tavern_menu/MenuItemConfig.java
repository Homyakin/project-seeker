package ru.homyakin.seeker.game.tavern_menu;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.models.MenuItemRarity;

@ConfigurationProperties("homyakin.seeker.tavern-menu")
public class MenuItemConfig {
    private Money commonPrice;
    private Money uncommonPrice;
    private Money rarePrice;

    public Money priceByRarity(MenuItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonPrice;
            case UNCOMMON -> uncommonPrice;
            case RARE -> rarePrice;
        };
    }

    public void setCommonPrice(int commonPrice) {
        this.commonPrice = Money.from(commonPrice);
    }

    public void setUncommonPrice(int uncommonPrice) {
        this.uncommonPrice = Money.from(uncommonPrice);
    }

    public void setRarePrice(int rarePrice) {
        this.rarePrice = Money.from(rarePrice);
    }
}
