package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import static pepjebs.mapatlases.client.MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE;

public class DimensionBookmarkButton extends BookmarkButton {

    private static final int BUTTON_H = 18;
    private static final int BUTTON_W = 24;

    private final ResourceKey<Level> dimension;
    private final ResourceLocation sprite;


    protected DimensionBookmarkButton(int pX, int pY, ResourceKey<Level> dimension, AtlasOverviewScreen screen) {
        super(pX, pY, BUTTON_W, BUTTON_H, screen,
                MapAtlasesClient.BOOKMARK_RIGHT_SPRITE, MapAtlasesClient.BOOKMARK_RIGHT_SELECTED_SPRITE);
        this.dimension = dimension;
        this.setTooltip(createTooltip());
        ResourceLocation res = MapAtlasesMod.res("dimensions/" + dimension.location().getPath());
        //check for missing
        if (Minecraft.getInstance().getGuiSprites().getSprite(res) ==
                Minecraft.getInstance().getGuiSprites().getSprite(MapAtlasesMod.res("missing"))) {
            res = MapAtlasesMod.res("dimension/overworld");
        }
        this.sprite = res;
    }

    @Override
    public Tooltip createTooltip() {
        return Tooltip.create(Component.literal(AtlasOverviewScreen.getReadableName(dimension.location())));
    }

    public ResourceKey<Level> getDimension() {
        return dimension;
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        if (selected()) {
            pose.translate(0, 0, 2);
        }
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        pGuiGraphics.blitSprite(sprite,
                this.getX() + 4, this.getY() , 16, 16);
        pose.popPose();

    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        this.setSelected(true);
        parentScreen.selectDimension(dimension);
    }

    //@Override
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
        //  super.playDownSound(pHandler);
        pHandler.play(SimpleSoundInstance.forUI(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), 1.0F,
                (float) (double) MapAtlasesClientConfig.soundScalar.get()));
    }
}
