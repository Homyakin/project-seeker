package ru.homyakin.seeker.game.badge.entity;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

@Disabled
public class BadgeViewPrintTest {

    @Test
    public void printBadges() {
        for (BadgeView badge : BadgeView.values()) {
            System.out.println(badge.code() + " -> " + badge.icon());
        }
    }
}
