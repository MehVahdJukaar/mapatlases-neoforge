package pepjebs.mapatlases.client;

import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public class CompoundTooltip {

    public static Tooltip create(Component... components) {
        if (components.length == 0) {
            throw new IllegalArgumentException("Expected at least one tooltip component");
        }
        MutableComponent merged = Component.empty().append(components[0]);
        for (int i = 1; i < components.length; i++) {
            merged.append("\n").append(components[i]);
        }
        return Tooltip.create(merged);
    }
}
