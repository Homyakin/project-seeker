package ru.homyakin.seeker.telegram.command.user.item;

public enum InventorySection {
    EQUIPMENT,
    BAG,
    LOADOUTS,
    ;

    public static InventorySection findForce(String section) {
        for (InventorySection inventorySection : InventorySection.values()) {
            if (inventorySection.name().equals(section)) {
                return inventorySection;
            }
        }
        throw new IllegalStateException("Unknown inventory section " + section);
    }
}
