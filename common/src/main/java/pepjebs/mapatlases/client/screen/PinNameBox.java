package pepjebs.mapatlases.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkersRenderer;

import java.util.Random;

public class PinNameBox extends EditBox {

    //static so they persist on new screen
    private static float currentIndex = new Random().nextInt(10);
    private static float displayIndex = currentIndex;
    private static float displayIndexO = displayIndex;


    private final Runnable onDone;

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
    public void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTicks) {
        graphics.nextStratum();
        super.extractWidgetRenderState(graphics, mouseX, mouseY, partialTicks);

        this.markerHovered = mouseX >= (double) this.getX() - height - 1 && mouseY >= this.getY() &&
                mouseX < this.getX() && mouseY < (this.getY() + this.height);
        if (MapAtlasesMod.MOONLIGHT) {
            float popIn = Mth.lerp(partialTicks, scrollPopInAnimationO, scrollPopInAnimation) * 3;
            float displayInd = Mth.lerp(partialTicks, displayIndexO, displayIndex);
            float remainder = displayInd % 1;
            int closestInd = (int) displayInd;
            int alphaDecrement = 120;
            int aa = (int) Mth.lerp(Mth.abs(remainder), 255, alphaDecrement);
            ClientMarkersRenderer.renderDecorationPreview(graphics,
                    this.getX() - height / 2f - 2,
                    this.getY() + height / 2f - 1 + remainder * popIn,
                    closestInd, this.markerHovered, aa);

            if (popIn != 0) {
                for (int j = 1; j < 4; j++) {
                    int al = (int) Mth.clamp(255 - (remainder + j) * alphaDecrement, 0, 255);
                    if (al <= 0) break;
                    graphics.nextStratum();
                    ClientMarkersRenderer.renderDecorationPreview(graphics,
                            this.getX() - height / 2f - 2,
                            this.getY() + height / 2f - 1 + remainder * popIn + j * popIn,
                            closestInd - j,
                            false, al);
                }
                for (int j = 1; j < 4; j++) {
                    int al = (int) Mth.clamp(255 - (-remainder + j) * alphaDecrement, 0, 255);
                    if (al <= 0) break;
                    graphics.nextStratum();
                    ClientMarkersRenderer.renderDecorationPreview(graphics,
                            this.getX() - height / 2f - 2,
                            this.getY() + height / 2f - 1 + remainder * popIn - j * popIn,
                            closestInd + j,
                            false, al);
                }
            }
        }
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
    public boolean keyPressed(KeyEvent event) {
        if ((event.key() == GLFW.GLFW_KEY_ENTER || event.key() == GLFW.GLFW_KEY_KP_ENTER) && active && canConsumeInput()) {
            onDone.run();
            scrollVisibleCounter = 0;
            displayIndex = currentIndex;
            displayIndexO = currentIndex;
            scrollPopInAnimation = 0;
            scrollPopInAnimationO = 0;
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        if (this.markerHovered) {
            increasePinIndex();
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            return;
        }
        super.onClick(event, doubleClick);
    }

    public void increasePinIndex() {
        this.currentIndex++;
        this.displayIndex = (int) currentIndex;
        this.displayIndexO = displayIndex;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        scrollVisibleCounter = 40;
        this.currentIndex -= (float) scrollY;
        return true;
    }

    private static float smoothStep(float start, float end, float speed) {
        float delta = end - start;
        return start + delta * speed;
    }
}
