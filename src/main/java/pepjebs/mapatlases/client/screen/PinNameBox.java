package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class PinNameBox extends EditBox {

    private final Runnable onDone;

    public PinNameBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage, Runnable onDone) {
        super(pFont, pX, pY, pWidth, pHeight, pMessage);
        this.onDone = onDone;
        this.active = false;
        this.visible = false;
        this.setFocused(false);
        this.setCanLoseFocus(true);
    }

    @Override
    public void renderButton(PoseStack p, int pMouseX, int pMouseY, float pPartialTick) {
        p.pushPose();
        p.translate(0, 0, 30);
        super.renderButton(p, pMouseX, pMouseY, pPartialTick);
        p.popPose();
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if((pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode ==GLFW.GLFW_KEY_KP_ENTER) && active && canConsumeInput()){
            onDone.run();
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }
}
