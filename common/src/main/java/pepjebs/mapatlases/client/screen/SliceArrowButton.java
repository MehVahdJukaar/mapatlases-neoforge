package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;

import java.util.TreeSet;

public class SliceArrowButton extends BookmarkButton {

    private static final int BUTTON_H = 5;
    private static final int BUTTON_W = 8;

    private final SliceBookmarkButton button;
    private final boolean down;
    private final ResourceLocation inactiveSprite;
    private Integer maxSlice;


    protected SliceArrowButton(boolean down, SliceBookmarkButton button, AtlasOverviewScreen screen) {
        super(getpX(button), getpY(down, button),
                BUTTON_W, BUTTON_H,
                screen, down ? MapAtlasesClient.SLICE_DOWN_SPRITE : MapAtlasesClient.SLICE_UP_SPRITE,
                down ? MapAtlasesClient.SLICE_DOWN_HOVERED_SPRITE : MapAtlasesClient.SLICE_UP_HOVERED_SPRITE);
        this.button = button;
        this.down = down;
        this.inactiveSprite = down ? MapAtlasesClient.SLICE_DOWN_INACTIVE_SPRITE : MapAtlasesClient.SLICE_UP_INACTIVE_SPRITE;
        this.setSelected(false);

        maxSlice = down ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    }

    private static int getpX(SliceBookmarkButton button) {
        return button.getX() + button.getWidth() + 6 + (button.compact ? -22 : 0);
    }

    private static int getpY(boolean down, SliceBookmarkButton button) {
        int i = button.getY() - 1 + (down ? button.getHeight() - BUTTON_H + 2 : 0);
        if (button.compact) {
            i += (down ? 7 : -7);
        }
        return i;
    }

    @Override
    public ResourceLocation getSprite() {
        int h = button.getSlice().heightOrTop();
        if (h == maxSlice) return inactiveSprite;

        return super.getSprite();
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();

        if (selected()) {
            pose.translate(0, 0, 2);
        }
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        this.setSelected(this.isHovered);
        pose.popPose();
    }

    @Override
    protected boolean clicked(double mouseX, double mouseY) {
        boolean b = super.clicked(mouseX, mouseY);
        if (b) {
            int h = button.getSlice().heightOrTop();
            if (h == maxSlice) return false;
        }
        return b;
    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        if (down) this.parentScreen.decreaseSlice();
        else this.parentScreen.increaseSlice();
    }

    //@Override
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }


    @Override
    public void playDownSound(SoundManager pHandler) {
        super.playDownSound(pHandler);
        pHandler.play(SimpleSoundInstance.forUI(MapAtlasesMod.ATLAS_PAGE_TURN_SOUND_EVENT.get(), 1.0F,
                (float) (double) MapAtlasesClientConfig.soundScalar.get()));
    }

    public void setMaxSlice(TreeSet<Integer> heightTree) {
        if (heightTree.isEmpty()) return;
        maxSlice = down ? heightTree.first() : heightTree.last();
    }
}
