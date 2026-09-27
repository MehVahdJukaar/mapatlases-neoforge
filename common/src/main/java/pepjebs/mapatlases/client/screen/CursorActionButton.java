package pepjebs.mapatlases.client.screen;

import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import pepjebs.mapatlases.client.CompoundTooltip;
import pepjebs.mapatlases.client.MapAtlasesClient;

public class CursorActionButton extends AtlasButton {

    private final CursorAction action;
    private final String translationKey;

    private CursorActionButton(int x, int y, AtlasOverviewScreen screen, CursorAction action, String translationKey,
                               Identifier sprite, Identifier hoveredSprite) {
        super(x, y, 16, 16, screen, sprite, hoveredSprite);
        this.action = action;
        this.translationKey = translationKey;
        this.setTooltip(createTooltip());
    }

    static CursorActionButton pin(int x, int y, AtlasOverviewScreen screen) {
        return new CursorActionButton(x, y, screen, CursorAction.PLACING_PIN, "message.map_atlases.pin",
                MapAtlasesClient.PIN_BUTTON_SPRITE, MapAtlasesClient.PIN_BUTTON_HOVERED_SPRITE);
    }

    static CursorActionButton shear(int x, int y, AtlasOverviewScreen screen) {
        return new CursorActionButton(x, y, screen, CursorAction.SHEARING, "message.map_atlases.shear",
                MapAtlasesClient.SHEAR_BUTTON_SPRITE, MapAtlasesClient.SHEAR_BUTTON_HOVERED_SPRITE);
    }

    @Override
    public Tooltip createTooltip() {
        Tooltip tooltip = Tooltip.create(Component.translatable(translationKey));
        if (!Minecraft.getInstance().options.advancedItemTooltips) return tooltip;
        Tooltip info = Tooltip.create(Component.translatable(translationKey + ".info").withStyle(ChatFormatting.GRAY));
        return CompoundTooltip.create(tooltip, info);
    }

    @Override
    public Identifier getSprite() {
        return isHovered ? highlightedSprite : sprite;
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        parentScreen.toggleCursorAction(action);
    }
}
