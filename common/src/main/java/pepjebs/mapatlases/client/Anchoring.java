package pepjebs.mapatlases.client;

import java.util.Locale;

public enum Anchoring {
    UPPER_LEFT, UPPER_RIGHT,
    LOWER_LEFT, LOWER_RIGHT;
    public final boolean isLeft;
    public final boolean isUp;

    Anchoring() {
        String name = this.name().toLowerCase(Locale.ROOT);
        isLeft = name.contains("left");
        isUp = name.contains("up");
    }
}