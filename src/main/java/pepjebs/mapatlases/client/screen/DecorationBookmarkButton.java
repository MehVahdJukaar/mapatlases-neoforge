package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.util.Locale;

public class DecorationBookmarkButton extends BookmarkButton {

    public static final ResourceLocation MAP_ICON_TEXTURE = new ResourceLocation("textures/map/map_icons.png");

    private static final int BUTTON_H = 14;
    private static final int BUTTON_W = 24;

    private final MapDecoration mapIcon;
    private final AtlasOverviewScreen parentScreen;

    protected DecorationBookmarkButton(int pX, int pY, MapDecoration mapIcon, AtlasOverviewScreen parentScreen) {
        super(pX - BUTTON_W, pY, BUTTON_W, BUTTON_H, 0, AtlasOverviewScreen.IMAGE_HEIGHT + 36);
        this.mapIcon = mapIcon;
        this.parentScreen = parentScreen;
        this.setTooltip(createTooltip());
    }

    @Override
    public Tooltip createTooltip() {
        Component mapIconComponent = mapIcon.getName() == null
                ? Component.literal(
                AtlasOverviewScreen.getReadableName(mapIcon.getType().name().toLowerCase(Locale.ROOT)))
                : mapIcon.getName();

        // draw text
        MutableComponent coordsComponent = Component.literal("X: " + mapIcon.getX() + ", Z: " + mapIcon.getY());
        MutableComponent formattedCoords = coordsComponent.setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));
        this.setTooltip(Tooltip.create(mapIconComponent));
        return Tooltip.create(mapIconComponent);
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        PoseStack matrices = pGuiGraphics.pose();
        matrices.pushPose();
        byte b = mapIcon.getImage();

        int u = (b % 16) * 8;
        int v = (b / 16) * 8;

        matrices.translate(getX() + width / 2f, getY() + height / 2f, 1.0D);
        matrices.mulPose(Axis.ZP.rotationDegrees((mapIcon.getRot() * 360) / 16.0F));
        matrices.scale(-1, -1, 1);

        pGuiGraphics.blit(MAP_ICON_TEXTURE, -4, -4, u, v, 8, 8, 128, 128);

        matrices.popPose();

        //hide waiting to be activated by mapWidget
        setSelected(false);
    }

    public MapDecoration getDecoration() {
        return mapIcon;
    }

    @Override
    public void onClick(double mouseX, double mouseY, int button) {
        this.setSelected(true);
        parentScreen.focusDecoration(this);
    }
}
