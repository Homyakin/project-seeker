package ru.homyakin.seeker.game.tavern_menu.order;

import org.springframework.boot.context.properties.ConfigurationProperties;
import ru.homyakin.seeker.game.effect.Effect;
import ru.homyakin.seeker.game.effect.EffectCharacteristic;
import ru.homyakin.seeker.game.models.Money;

import java.time.Duration;

@ConfigurationProperties("homyakin.seeker.tavern-menu.order")
public class OrderConfig {
    private Money throwCost;
    private Effect throwDamageEffect;
    private Effect throwToStaffDamageEffect;
    private Duration throwEffectDuration;
    private Duration effectDuration;
    private Duration orderTtl;
    private Duration timeToThrowOrder;
    private Duration throwGroupTimeout;
    private Duration throwTargetGroupTimeout;

    public Money throwCost() {
        return throwCost;
    }

    public Effect throwDamageEffect() {
        return throwDamageEffect;
    }

    public Effect throwToStaffDamageEffect() {
        return throwToStaffDamageEffect;
    }

    public Duration throwEffectDuration() {
        return throwEffectDuration;
    }

    public Duration effectDuration() {
        return effectDuration;
    }

    public Duration orderTtl() {
        return orderTtl;
    }

    public Duration timeToThrowOrder() {
        return timeToThrowOrder;
    }

    public Duration throwGroupTimeout() {
        return throwGroupTimeout;
    }

    public Duration throwTargetGroupTimeout() {
        return throwTargetGroupTimeout;
    }

    public void setThrowDamagePercent(int throwDamagePercent) {
        this.throwDamageEffect = new Effect.MinusMultiplier(throwDamagePercent, EffectCharacteristic.HEALTH);
    }

    public void setThrowToStaffDamagePercent(int throwToStaffDamagePercent) {
        this.throwToStaffDamageEffect = new Effect.MinusMultiplier(throwToStaffDamagePercent, EffectCharacteristic.HEALTH);
    }

    public void setThrowCost(int throwCost) {
        this.throwCost = Money.from(throwCost);
    }

    public void setThrowEffectDuration(Duration throwEffectDuration) {
        this.throwEffectDuration = throwEffectDuration;
    }

    public void setEffectDuration(Duration effectDuration) {
        this.effectDuration = effectDuration;
    }

    public void setOrderTtl(Duration orderTtl) {
        this.orderTtl = orderTtl;
    }

    public void setTimeToThrowOrder(Duration timeToThrowOrder) {
        this.timeToThrowOrder = timeToThrowOrder;
    }

    public void setThrowGroupTimeout(Duration throwGroupTimeout) {
        this.throwGroupTimeout = throwGroupTimeout;
    }

    public void setThrowTargetGroupTimeout(Duration throwTargetGroupTimeout) {
        this.throwTargetGroupTimeout = throwTargetGroupTimeout;
    }
}
