package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.PlatStuff;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.utils.AtlasCartographyTable;
import pepjebs.mapatlases.utils.MapType;
import pepjebs.mapatlases.utils.Slice;

public class CartographyTableAtlasButton extends AbstractWidget {

    protected final boolean left;
    protected final AbstractContainerMenu menu;
    protected final ResourceLocation hoveredSprite;
    protected final ResourceLocation sprite;
    protected final ResourceLocation selectedSprite;

    public CartographyTableAtlasButton(AbstractContainerScreen<?> screen, boolean left, AbstractContainerMenu menu) {
        super(screen.leftPos + (left ? 71 : 122), screen.topPos + 65, 7, 11, Component.empty());
        this.menu = menu;
        this.left = left;
        if (left) {
            sprite = MapAtlasesClient.CARTOGRAPHY_TABLE_LEFT_SPRITE;
            hoveredSprite = MapAtlasesClient.CARTOGRAPHY_TABLE_LEFT_HOVERED_SPRITE;
            selectedSprite = MapAtlasesClient.CARTOGRAPHY_TABLE_LEFT_SELECTED_SPRITE;
        } else {
            sprite = MapAtlasesClient.CARTOGRAPHY_TABLE_RIGHT_SPRITE;
            hoveredSprite = MapAtlasesClient.CARTOGRAPHY_TABLE_RIGHT_HOVERED_SPRITE;
            selectedSprite = MapAtlasesClient.CARTOGRAPHY_TABLE_RIGHT_SELECTED_SPRITE;
        }
    }

    @Override
    protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
        if (!PlatStuff.isShear(menu.getSlot(CartographyTableMenu.ADDITIONAL_SLOT).getItem()) ||
                !menu.getSlot(CartographyTableMenu.MAP_SLOT).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) return;
        RenderSystem.enableDepthTest();
        if (!visible) return;
        PoseStack pose = pGuiGraphics.pose();
        pose.pushPose();
        pose.translate(0, 0, 5);
        pGuiGraphics.blitSprite(isHovered ? hoveredSprite : sprite,
                this.getX(), this.getY(), this.width, this.height);

        if (this.menu instanceof AtlasCartographyTable at) {
            if (left) {
                pGuiGraphics.drawCenteredString(Minecraft.getInstance().font, Component.translatable("message.map_atlases.map_index",
                                at.mapatlases$getSelectedMapIndex()),
                        this.getX() + 30, this.getY() + 2, -1);
            } else {
                Slice slice = at.mapatlases$getSelectedSlice();
                if (slice != null) {

                    var dim = slice.dimension();
                    int y0 = 0;
                    if (!dim.equals(Level.OVERWORLD)) {
                        pGuiGraphics.drawString(Minecraft.getInstance().font,
                                Component.literal(LangBuilder.getReadableName(dim.location().getPath())),
                                this.getX() - 52, this.getY() - 50, -1);
                        y0 += 8;
                    }
                    MapType type = slice.type();
                    if (type != MapType.VANILLA) {
                        pGuiGraphics.drawString(Minecraft.getInstance().font, type.getName(),
                                this.getX() - 52, y0 + this.getY() - 50, -1);
                        y0 += 8;
                    }
                    var height = slice.height();
                    if (height.isPresent()) {
                        pGuiGraphics.drawString(Minecraft.getInstance().font,
                                Component.translatable("message.map_atlases.slice_height", height.get()),
                                this.getX() - 52, y0 + this.getY() - 50, -1);
                    }
                }
            }
        }
        pose.popPose();
    }

    @Nullable
    @Override
    public Tooltip getTooltip() {
        return null;
    }


    @Override
    protected void updateWidgetNarration(NarrationElementOutput pNarrationElementOutput) {

    }

    @Override
    public void onClick(double mouseX, double mouseY) {
        int pId = left ? 4 : 5;
        if (this.menu.clickMenuButton(Minecraft.getInstance().player, pId)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, pId);
        }
    }

    //@Override
    public void onClick(double mouseX, double mouseY, int button) {
        onClick(mouseX, mouseY);
    }
}
