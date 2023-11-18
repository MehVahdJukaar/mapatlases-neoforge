package pepjebs.mapatlases.client;

import com.google.common.collect.ImmutableList;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;

import java.util.Arrays;

public class CompoundTooltip {

    public static Tooltip create(Tooltip... tooltips) {
        Tooltip first = null;
        var merged = new ImmutableList.Builder<FormattedCharSequence>();
        Minecraft mc = Minecraft.getInstance();
        for (var t : tooltips) {
            if (first == null) first = t;
            merged.addAll(t.toCharSequence(mc));
        }
        if (first == null) throw new AssertionError();
        first.cachedTooltip = merged.build();
        return first;
    }

    public static Tooltip create(Component... components) {
        return create(Arrays.stream(components).map(Tooltip::create).toArray(Tooltip[]::new));
    }
}
