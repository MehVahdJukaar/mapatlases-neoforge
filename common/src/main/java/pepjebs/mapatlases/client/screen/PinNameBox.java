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
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

public class PinNameBox extends EditBox {

    private final Runnable onDone;
    private float displayIndex = 0;
    private float displayIndexO = 0;
    private float currentIndex = 0;
    private boolean markerHovered = false;

    private int scrollVisibleCounter = 0;

    private float scrollPopInAnimation;
    private float scrollPopInAnimationO;

    public PinNameBox(Font pFont, int pX, int pY, int pWidth, int pHeight, Component pMessage, Runnable onDone) {
        super(pFont, pX + pHeight / 2, pY, pWidth, pHeight, pMessage);
        this.onDone = onDone;
        this.active = false;
        this.visible = false;
        this.setFocused(false);
        this.setCanLoseFocus(true);
        this.setMaxLength(16);
    }

    public int getIndex() {
        return (int) currentIndex;
    }

    @Override
    public void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float partialTicks) {
        PoseStack p = pGuiGraphics.pose();
        p.pushPose();
        p.translate(0, 0, 30);
        super.renderWidget(pGuiGraphics, pMouseX, pMouseY, partialTicks);
        // int col = this.isFocused() ? -1 : -6250336;
        //  pGuiGraphics.fill(this.getX() - 1 - this.height, this.getY() - 1,
        //        this.getX() + 1, this.getY() + this.height + 1, col);
        // pGuiGraphics.fill(this.getX() - this.height, this.getY(),
        //       this.getX(), this.getY() + this.height, -16777216);
        this.markerHovered = pMouseX >= (double) this.getX() - height - 1 && pMouseY >= this.getY() &&
                pMouseX < (this.getX()) && pMouseY < (this.getY() + this.height);
        if (MapAtlasesMod.MOONLIGHT) {
            p.pushPose();
            p.translate(this.getX() - height / 2f - 2, this.getY() + height / 2f - 1, 0);
            p.scale(2, 2, 1);
            RenderSystem.setShaderColor(1, 1, 1, 1);


            float popIn = Mth.lerp(partialTicks, scrollPopInAnimationO, scrollPopInAnimation) * 3;
            float displayInd = Mth.lerp(partialTicks, displayIndexO, displayIndex);

            //displayInd = 2.3f;
            float remainder = displayInd % 1;
            int closestInd = (int) displayInd;

            p.translate(0, remainder * popIn, 0);
            int alphaDecrement = 120;
            int aa = (int) Mth.lerp(Mth.abs(remainder), 255, alphaDecrement);
            ClientMarkers.renderDecorationPreview(pGuiGraphics, 0, 0,
                    closestInd, this.markerHovered, aa);

            if(popIn!=0) {
                p.pushPose();
                for (int j = 1; j < 4; j++) {
                    int al = (int) Mth.clamp(255 - (remainder + j) * alphaDecrement, 0, 255);
                    p.translate(0, 0, -0.01);
                    if (al <= 0) break;
                    ClientMarkers.renderDecorationPreview(pGuiGraphics, 0, j * popIn, closestInd - j,
                            false, al);
                }
                p.popPose();
                p.pushPose();
                for (int j = 1; j < 4; j++) {
                    int al = (int) Mth.clamp(255 - (-remainder + j) * alphaDecrement, 0, 255);
                    p.translate(0, 0, -0.01);
                    if (al <= 0) break;
                    ClientMarkers.renderDecorationPreview(pGuiGraphics, 0, -j * popIn, closestInd + j,
                            false, al);
                }
                p.popPose();
            }
            p.popPose();
        }
        p.popPose();
    }

    @Override
    public void tick() {
        super.tick();
        scrollPopInAnimationO = scrollPopInAnimation;
        displayIndexO = displayIndex;
        int index = getIndex();
        double scrollInSpeed = 0.4;
        if (displayIndex < index) {
            displayIndex = (float) Math.min(index, displayIndex + scrollInSpeed);
        }
        if (displayIndex > index) {
            displayIndex = (float) Math.max(index, displayIndex - scrollInSpeed);
        }
        float popInSpeed = 0.2f;
        float popOutSpeed = 0.4f;
        if (scrollVisibleCounter < 10 && index != displayIndex) {
        } else if (scrollVisibleCounter > 0) scrollVisibleCounter--;
        if (scrollVisibleCounter == 0 && scrollPopInAnimation > 0) {
            scrollPopInAnimation = smoothStep(scrollPopInAnimation, 0, popInSpeed); // Smoothly interpolate towards 0
        } else if (scrollVisibleCounter != 0 && scrollPopInAnimation < 1) {
            scrollPopInAnimation = smoothStep(scrollPopInAnimation, 1.0f, popOutSpeed); // Smoothly interpolate back towards 1
        }
    }

    @Override
    public boolean keyPressed(int pKeyCode, int pScanCode, int pModifiers) {
        if ((pKeyCode == GLFW.GLFW_KEY_ENTER || pKeyCode == GLFW.GLFW_KEY_KP_ENTER) && active && canConsumeInput()) {
            onDone.run();
            scrollVisibleCounter = 0;
            displayIndex = currentIndex;
            displayIndexO = currentIndex;
            scrollPopInAnimation = 0;
            scrollPopInAnimationO = 0;
            return true;
        }
        return super.keyPressed(pKeyCode, pScanCode, pModifiers);
    }

    @Override
    protected boolean clicked(double pMouseX, double pMouseY) {
        if (this.markerHovered) {
            this.currentIndex++;
            this.displayIndex = (int) currentIndex;
            this.displayIndexO = displayIndex;
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return false;
        }
        return super.clicked(pMouseX, pMouseY);
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        scrollVisibleCounter = 40;
        this.currentIndex -= (float) pDelta;
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }

    private static float smoothStep(float start, float end, float speed) {
        float delta = end - start;
        return start + delta * speed;
    }
}
