package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.util.List;

public class SliceBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 21;
    private static final int BUTTON_W = 27;

    protected final boolean compact =  MapAtlasesClientConfig.worldMapCompactSliceIndicator.get();

    private Integer slice;

    protected SliceBookmarkButton(int pX, int pY, @Nullable Integer slice, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, 0, AtlasOverviewScreen.IMAGE_HEIGHT + 64, screen);
        this.slice = slice;
        this.selected = false;
        this.tooltip =(createTooltip());
    }

    @Override
    public List<Component> createTooltip() {
        return List.of(slice == null ? Component.translatable("item.map_atlases.atlas.tooltip_slice_default") :
                Component.translatable("item.map_atlases.atlas.tooltip_slice", slice));
    }

    public Integer getSlice() {
        return slice;
    }

    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        if (!active || !visible) return;
        pose.pushPose();

        pose.translate(0, 0, 2);
        RenderSystem.enableDepthTest();

        super.renderButton(pose, pMouseX, pMouseY, pPartialTick);

        pose.translate(0, 0, 1);
        Component text = slice != null ? Component.literal(String.valueOf(slice)) :
                Component.translatable("message.map_atlases.atlas.slice_default");
        GuiComponent.drawCenteredString(pose, parentScreen.getMinecraft().font,
                text, this.x     + (compact ? 15: 39), this.y + 7, -1);


        pose.popPose();
    }

    public void setSlice(Integer slice) {
        this.slice = slice;
    }

    @Override
    protected boolean isValidClickButton(int pButton) {
        return false; //cant be clicked
    }
}
