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
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.screen.CartographyTableAtlasButton;

@Mixin(CartographyTableScreen.class)
public abstract class CartographyTableScreenMixin extends AbstractContainerScreen<CartographyTableMenu> {

    protected CartographyTableScreenMixin(CartographyTableMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.addRenderableWidget(new CartographyTableAtlasButton(this, true, this.menu));
        this.addRenderableWidget(new CartographyTableAtlasButton(this, false, this.menu));
    }

    @Inject(method = "renderResultingMap", at = @At(value = "HEAD"))
    void renderAtlasMap(GuiGraphics pGuiGraphics, Integer pMapId, MapItemSavedData pMapData, boolean pHasMap, boolean pHasPaper,
                        boolean pHasGlassPane, boolean pIsMaxSize, CallbackInfo ci,
                        @Local LocalRef<Integer> mapid, @Local LocalRef<MapItemSavedData> data) {

        if (pMapData == null && pMapId == null && this.menu.slots.get(0).getItem().is(MapAtlasesMod.MAP_ATLAS.get())) {
            ItemStack item = this.menu.slots.get(2).getItem();
            if (item.is(Items.FILLED_MAP)) {
                mapid.set(MapItem.getMapId(item));
                data.set(MapItem.getSavedData(mapid.get(), this.minecraft.level));
            }
        }
    }

    @Override
    public boolean mouseScrolled(double pMouseX, double pMouseY, double pDelta) {
        int pId = pDelta > 0 ? 4 : 5;
        if (this.menu.clickMenuButton(Minecraft.getInstance().player, pId)) {
            Minecraft.getInstance().gameMode.handleInventoryButtonClick((this.menu).containerId, pId);
            return true;
        }
        return super.mouseScrolled(pMouseX, pMouseY, pDelta);
    }
}
