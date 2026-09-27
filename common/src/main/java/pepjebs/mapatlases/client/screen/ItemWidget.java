package pepjebs.mapatlases.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

public class ItemWidget extends AbstractWidget {

    private final ItemStack item;

    protected ItemWidget(int pX, int pY, ItemStack item) {
        super(pX, pY, 16, 16, Component.empty());
        this.item = item;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        graphics.item(Minecraft.getInstance().player, item, this.getX(), this.getY(), 0);
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {
    }
}
