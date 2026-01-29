package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.sugar.Local;
import com.llamalad7.mixinextras.sugar.ref.LocalRef;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.CartographyTableScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.screen.CartographyTableAtlasButton;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.utils.MapType;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {

    protected CartographyTableScreenMixin(CartographyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Inject(method = "<init>", at = @At("RETURN"))
    public void init(CartographyTableMenu menu, Inventory playerInventory, Component title, CallbackInfo ci) {
        this.addRenderableWidget(new CartographyTableAtlasButton(this, true, this.menu));
        this.addRenderableWidget(new CartographyTableAtlasButton(this, false, this.menu));
    }

    @Inject(method = "renderResultingMap", at = @At(value = "HEAD"))
    void mapAtlases$renderAtlasMap(GuiGraphics guiGraphics, MapId pMapId, MapItemSavedData pMapData, boolean hasMap, boolean hasPaper,
                                   boolean hasGlassPane, boolean isMaxSize, CallbackInfo ci,
                                   @Local(argsOnly = true) LocalRef<MapId> mapid,
                                   @Local(argsOnly = true) LocalRef<MapItemSavedData> data) {

        if (pMapData == null && pMapId == null && this.menu.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) {
            ItemStack item = this.menu.slots.get(2).getItem();
            if (MapType.fromFilledMap(item.getItem()) != null) {
                MapDataHolder holder = MapAtlasesAccessUtils.findMapFromItemStack(this.minecraft.level, item);
                if (holder != null) {
                    mapid.set(holder.id);
                    data.set(holder.data);
                }
            }
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta,double yDelta) {
        int pId = pDelta > 0 ? 4 : 5;
        if (this.menu.clickMenuButton(Minecraft.getInstance().player, pId)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, pId);
            return true;
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta, yDelta);
    }
}
