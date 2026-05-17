package ru.homyakin.seeker.locale.personal;

public record PersonalResource(
    MenuResource menu,
    ChangeNameResource changeName,
    BadgeResource badges,
    BulletinBoardResource bulletinBoard,
    SettingsResource settings
) {
}
