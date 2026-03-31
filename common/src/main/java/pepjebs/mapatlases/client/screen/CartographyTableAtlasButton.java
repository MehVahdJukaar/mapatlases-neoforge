package pepjebs.mapatlases.client.screen;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.level.Level;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

public class CartographyTableAtlasButton extends AbstractWidget {

    private static final Identifier TEXTURE = MapAtlasesMod.res(
            "textures/gui/screen/cartography_table_buttons.png");

    protected final boolean left;
    protected final AbstractContainerMenu menu;

    public CartographyTableAtlasButton(int leftPos, int topPos, boolean left, AbstractContainerMenu menu) {
        super(leftPos + (left ? 71 : 122), topPos + 65, 7, 11, Component.empty());
        this.menu = menu;
        this.left = left;
    }

    @Override
    protected void extractWidgetRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float partialTick) {
        if (!PlatStuff.isShear(menu.getSlot(CartographyTableMenu.ADDITIONAL_SLOT).getItem()) ||
                !menu.getSlot(CartographyTableMenu.MAP_SLOT).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) return;
        if (!visible) return;
        graphics.nextStratum();
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE,
                this.getX(), this.getY(), left ? 9 : 0, isHovered ? height : 0,
                this.width, this.height, 32, 32);

        if (this.menu instanceof AtlasCartographyTable at) {
            if (left) {
                graphics.centeredText(Minecraft.getInstance().font, Component.translatable("message.map_atlases.map_index",
                                at.mapatlases$getSelectedMapIndex()),
                        this.getX() + 30, this.getY() + 2, -1);
            } else {
                Slice slice = at.mapatlases$getSelectedSlice();
                if (slice != null) {

                    var dim = slice.dimension();
                    int y0 = 0;
                    if (!dim.equals(Level.OVERWORLD)) {
                        graphics.text(Minecraft.getInstance().font,
                                Component.literal(AtlasOverviewScreen.getReadableName(dim.identifier().getPath())),
                                this.getX() - 52, this.getY() - 50, -1);
                        y0 += 8;
                    }
                    MapType type = slice.type();
                    if (type != MapType.VANILLA) {
                        graphics.text(Minecraft.getInstance().font, Component.translatable(type.translationKey),
                                this.getX() - 52, y0 + this.getY() - 50, -1);
                        y0 += 8;
                    }
                    Integer height = slice.height();
                    if (height != null) {
                        graphics.text(Minecraft.getInstance().font, Component.translatable("message.map_atlases.slice_height", height),
                                this.getX() - 52, y0 + this.getY() - 50, -1);
                    }
                }
            }
        }
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    @Override
    public void onClick(MouseButtonEvent event, boolean doubleClick) {
        int pId = left ? 4 : 5;
        if (this.menu.clickMenuButton(Minecraft.getInstance().player, pId)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, pId);
        }
    }
}
