package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.ClientMarker;

public class PinNameBox extends EditBox {

    private final Runnable onDone;
    private int index = 0;

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
    public void renderButton(PoseStack p, int pMouseX, int pMouseY, float pPartialTick) {
        p.pushPose();
        p.translate(0, 0, 30);
        super.renderButton(p, pMouseX, pMouseY, pPartialTick);
        int col = this.isFocused() ? -1 : -6250336;
      //  pGuiGraphics.fill(this.getX() - 1 - this.height, this.getY() - 1,
        //        this.getX() + 1, this.getY() + this.height + 1, col);
       // pGuiGraphics.fill(this.getX() - this.height, this.getY(),
         //       this.getX(), this.getY() + this.height, -16777216);
        if (MapAtlasesMod.MOONLIGHT) {
            p.pushPose();
            p.translate(this.getX() - height / 2f -1, this.getY() + height / 2f -1,0);
            p.scale(2,2,0);
            ClientMarker.renderPin(pGuiGraphics, 0,0, index);
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
        if (pMouseX >= (double) this.getX() - height && pMouseY >= (double) this.getY() &&
                pMouseX < (double) (this.getX()) && pMouseY < (double) (this.getY() + this.height)) {
            this.index++;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return false;
        }
        return super.clicked(pMouseX, pMouseY);
    }

}
