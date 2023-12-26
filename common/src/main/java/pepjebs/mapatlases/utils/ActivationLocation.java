package pepjebs.mapatlases.utils;


public enum ActivationLocation {
    MAIN_HAND, HOTBAR, HANDS, HOTBAR_AND_HANDS, INVENTORY;

    public boolean hasOffhand() {
        return this == HANDS || this == HOTBAR_AND_HANDS || this == INVENTORY;
    }

    public boolean hasHotbar() {
        return this == INVENTORY || this == HOTBAR || this == HOTBAR_AND_HANDS;
    }

    public boolean scanAll() {
        return this == INVENTORY;
    }



}

