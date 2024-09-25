package ru.homyakin.seeker.game.tavern_menu.menu;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.models.Money;
import ru.homyakin.seeker.game.tavern_menu.menu.models.MenuItemRarity;

@ConfigurationProperties("homyakin.seeker.tavern-menu.menu")
public class MenuConfig {
    private Money commonPrice;
    private Money uncommonPrice;
    private Money rarePrice;
    private int addEffectBaseValue;
    private int multiplyPercentEffectBaseValue;

    public Money priceByRarity(MenuItemRarity rarity) {
        return switch (rarity) {
            case COMMON -> commonPrice;
            case UNCOMMON -> uncommonPrice;
            case RARE -> rarePrice;
        };
    }

    public int addValueByRarity(MenuItemRarity rarity) {
        return (int) (addEffectBaseValue * rarity.effectMultiplier());
    }

    public int multiplyPercentByRarity(MenuItemRarity rarity) {
        return (int) (multiplyPercentEffectBaseValue * rarity.effectMultiplier());
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

    public void setAddEffectBaseValue(int addEffectBaseValue) {
        this.addEffectBaseValue = addEffectBaseValue;
    }

    public void setMultiplyPercentEffectBaseValue(int multiplyPercentEffectBaseValue) {
        this.multiplyPercentEffectBaseValue = multiplyPercentEffectBaseValue;
    }

}
