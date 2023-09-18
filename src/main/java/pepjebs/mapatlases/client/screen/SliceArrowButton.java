package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

public class SliceArrowButton extends BookmarkButton {

    private static final int BUTTON_H = 7;
    private static final int BUTTON_W = 12;

    private final boolean down;

    protected SliceArrowButton(boolean down, SliceBookmarkButton button, AtlasOverviewScreen screen) {
        super(getpX(button), getpY(down, button),
                BUTTON_W, BUTTON_H, button.getWidth() + (down ? BUTTON_W : 0), AtlasOverviewScreen.IMAGE_HEIGHT + 64,
                screen);
        this.down = down;
        this.setSelected(false);
    }

    private static int getpX(SliceBookmarkButton button) {
        return button.x  + button.getWidth() + 6 + (button.compact ? -22 : 0);
    }

    private static int getpY(boolean down, SliceBookmarkButton button) {
        int i = button.y - 1 + (down ? button.getHeight() - BUTTON_H + 2 : 0);
        if(button.compact){
            i+= ( down ? 7 : -7);
        }
        return i;
    }


    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        pose.pushPose();

        if (selected()) {
            pose.translate(0, 0, 2);
        }
        super.renderButton(pose, pMouseX, pMouseY, pPartialTick);
        this.setSelected(this.isHovered);
        pose.popPose();
    }


    @Override
    public void onClick(double mouseX, double mouseY) {
        if(down) this.parentScreen.decreaseSlice();
        else this.parentScreen.increaseSlice();
    }

    @Override
    public void playDownSound(SoundManager pHandler) {
        super.playDownSound(pHandler);
        pHandler.play(SimpleSoundInstance.forUI( MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), 1.0F,
                (float)(double)   MapAtlasesClientConfig.soundScalar.get()));
    }
}
