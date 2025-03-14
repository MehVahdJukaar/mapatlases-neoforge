package pepjebs.mapatlases.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AtlasInHandRenderer;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.item.MapAtlasItem;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Unique
    private boolean mapatlases$renderingAtlas = false;

    @ModifyExpressionValue(method = "renderArmWithItem", at =  @At(value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/world/item/ItemStack;is(Lnet/minecraft/world/item/Item;)Z"))
    public boolean renderMapAtlasItem(boolean isNormalMap, @Local(argsOnly = true) ItemStack pStack, @Local(argsOnly = true) AbstractClientPlayer player){
        if(pStack.is(MapAtlasesMod.MAP_ATLAS.get()) && MapAtlasesClientConfig.inHandMode.get().isOn(pStack)){
            if (!MapAtlasItem.getMaps(pStack, player.level()).mapsDimension(player.level().dimension())) return isNormalMap;
            mapatlases$renderingAtlas = true;
            return true;
        }
        return isNormalMap;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    public void renderMapAtlasInHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, ItemStack pStack, CallbackInfo ci){
        if(mapatlases$renderingAtlas){
            AtlasInHandRenderer.render(pPoseStack, pBuffer, pCombinedLight, pStack, this.minecraft);
            mapatlases$renderingAtlas = false;
            ci.cancel();
        }
    }
}
