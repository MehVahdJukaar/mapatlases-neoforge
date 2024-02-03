package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiComponent;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

public class CartographyTableAtlasButton extends AbstractWidget {

    private static final ResourceLocation TEXTURE = MapAtlasesMod.res(
            "textures/gui/screen/cartography_table_buttons.png");

    protected final boolean left;
    protected final AbstractContainerMenu menu;

    public CartographyTableAtlasButton(AbstractContainerScreen<?> screen, boolean left, AbstractContainerMenu menu) {
        super(screen.leftPos+(left ? 71 : 122), screen.topPos+ 65, 7, 11, Component.empty());
        this.menu = menu;
        this.left = left;
    }

    @Override
    public void renderButton(PoseStack pose, int pMouseX, int pMouseY, float pPartialTick) {
        if (!PlatStuff.isShear(menu.getSlot(CartographyTableMenu.ADDITIONAL_SLOT).getItem()) ||
                !menu.getSlot(CartographyTableMenu.MAP_SLOT).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) return;
        RenderSystem.enableDepthTest();
        if (!visible) return;
        pose.pushPose();
        pose.translate(0, 0, 5);
        RenderSystem.setShaderTexture(0, TEXTURE);
        blit(pose,
                this.x, this.y, left ? 9 : 0, isHovered ? height : 0,
                this.width, this.height, 32, 32);

        if (this.menu instanceof AtlasCartographyTable at) {
            if (left) {
                GuiComponent.drawCenteredString(pose, Minecraft.getInstance().font, Component.translatable("message.map_atlases.map_index",
                                at.mapatlases$getSelectedMapIndex()),
                        this.x + 30, this.y + 2, -1);
            } else {
                Slice slice = at.mapatlases$getSelectedSlice();
                if (slice != null) {

                    var dim = slice.dimension();
                    int y0 = 0;
                    if (!dim.equals(Level.OVERWORLD)) {
                        GuiComponent.drawString(pose, Minecraft.getInstance().font,
                                Component.literal(LangBuilder.getReadableName(dim.location().getPath())),
                                this.x - 52, this.y - 50, -1);
                        y0 += 8;
                    }
                    MapType type = slice.type();
                    if (type != MapType.VANILLA) {
                        GuiComponent.drawString(pose, Minecraft.getInstance().font, Component.translatable(type.translationKey),
                                this.x - 52, y0 + this.y - 50, -1);
                        y0 += 8;
                    }
                    Integer height = slice.height();
                    if (height != null) {
                        GuiComponent.drawString(pose, Minecraft.getInstance().font, Component.translatable("message.map_atlases.slice_height", height),
                                this.x - 52, y0 + this.y - 50, -1);
                    }
                }
            }
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