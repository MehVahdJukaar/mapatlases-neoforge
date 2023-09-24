package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraftforge.common.Tags;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.AtlasCartographyTable;

public class CartographyTableAtlasButton extends AbstractWidget {

    private static final ResourceLocation TEXTURE = MapAtlasesMod.res(
            "textures/gui/screen/cartography_table_buttons.png");

    protected final boolean left;
    protected final AbstractContainerMenu menu;

    public CartographyTableAtlasButton(AbstractContainerScreen<?> screen, boolean left, AbstractContainerMenu menu) {
        super(screen.getGuiLeft()+(left ? 71 : 122), screen.getGuiTop()+ 65, 7, 11, Component.empty());
        this.menu = menu;
        this.left = left;
    }

    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        if( !menu.getSlot(CartographyTableMenu.ADDITIONAL_SLOT).getItem().is(Tags.Items.SHEARS) ||
        !menu.getSlot(CartographyTableMenu.MAP_SLOT).getItem().is(MapAtlasesMod.MAP_ATLAS.get()))return;
        RenderSystem.enableDepthTest();
        if (!visible) return;
        pose.pushPose();
        pose.translate(0, 0,5);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pose,
                this.x, this.y, left ? 9 : 0, isHovered ? height : 0,
                this.width, this.height, 32, 32);

        if(left){
            GuiComponent.drawCenteredString(pose,Minecraft.getInstance().font, Component.translatable("message.map_atlases.map_index",
                            ((AtlasCartographyTable) this.menu).mapatlases$getSelectedMapIndex()),
                    this.x + 30, this.y+2, -1);
        }
        pose.popPose();
    }


    @Override
    public void updateNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int pId = left ? 4 : 5;
        if( this.menu.clickMenuButton(Minecraft.getInstance().player, pId)){
           Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, pId);
       }
    }
}
