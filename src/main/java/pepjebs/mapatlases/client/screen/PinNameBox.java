package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarker;

public class PinNameBox extends EditBox {

    private final Runnable onDone;
    private int index = 0;
    private boolean markerHovered;

    public PinNameBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage, Runnable onDone) {
        super(pFont, pX + pHeight /2, pY, pWidth, pHeight, pMessage);
        this.onDone = onDone;
        this.active = false;
        this.visible = false;
        this.setFocused(false);
        this.setCanLoseFocus(true);
        this.setMaxLength(16);
    }

    public int getIndex() {
        return index;
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        PoseStack p = pGuiGraphics.pose();
        p.pushPose();
        p.translate(0, 0, 30);
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);
        // int col = this.isFocused() ? -1 : -6250336;
        //  pGuiGraphics.fill(this.getX() - 1 - this.height, this.getY() - 1,
        //        this.getX() + 1, this.getY() + this.height + 1, col);
         // pGuiGraphics.fill(this.getX() - this.height, this.getY(),
         //       this.getX(), this.getY() + this.height, -16777216);
        this.markerHovered = pMouseX >= (double) this.getX() - height - 1 && pMouseY >= this.getY() &&
                pMouseX < (this.getX()) && pMouseY < (this.getY() + this.height);
        if (MapAtlasesMod.MOONLIGHT) {
            p.pushPose();
            p.translate(this.getX() - height / 2f - 2, this.getY() + height / 2f -1,0);
            p.scale(2,2,0);
            RenderSystem.setShaderColor(1,1,1,1);
            ClientMarker.renderDecorationPreview(pGuiGraphics, 0,0, index, this.markerHovered);
            p.popPose();
        }
        p.popPose();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if ((pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode == GLFW.GLFW_KEY_KP_ENTER) && active && canConsumeInput()) {
            onDone.run();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        if (this.markerHovered) {
            this.index++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return false;
        }
        return super.clicked(pMouseX, pMouseY);
    }

}
