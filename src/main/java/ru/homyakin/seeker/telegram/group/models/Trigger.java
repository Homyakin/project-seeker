package ru.homyakin.seeker.telegram.group.models;

import jakarta.annotation.Nullable;

public record Trigger(
        GroupId groupId,
        String textToTrigger,
        @Nullable
        String triggerText
) {

    public static Trigger from(Group group, String textToTrigger, String triggerText) {
        return new Trigger(group.id(), textToTrigger, triggerText);
    }

}
