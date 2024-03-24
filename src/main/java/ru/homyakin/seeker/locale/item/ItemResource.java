package ru.homyakin.seeker.locale.item;

public record ItemResource(
    String itemWithoutModifiers,
    String itemWithPrefixModifier,
    String itemWithSuffixModifier,
    String itemWithPrefixAndSuffixModifier,
    String itemWithTwoPrefixModifiers,
    String characteristics,
    String attack
) {
}
