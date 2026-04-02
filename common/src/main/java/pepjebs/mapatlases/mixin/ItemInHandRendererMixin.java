package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
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

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow @Final private Minecraft minecraft;
    @Unique
    private boolean mapatlases$renderingAtlas = false;

    @ModifyExpressionValue(method = "renderArmWithItem", at =  @At(value = "INVOKE",
            ordinal = 0,
            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    public boolean renderMapAtlasItem(boolean isNormalMap, @Local ItemStack pStack){
        if(pStack.is(MapAtlasesMod.MAP_ATLAS.get()) && MapAtlasesClientConfig.inHandMode.get().isOn(pStack)){
            mapatlases$renderingAtlas = true;
            return true;
        }
        return isNormalMap;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    public void renderMapAtlasInHand(PoseStack pPoseStack, SubmitNodeCollector submitNodeCollector, int pCombinedLight,
                                     ItemStack pStack, CallbackInfo ci){
        if(mapatlases$renderingAtlas){
            AtlasInHandRenderer.render(pPoseStack, submitNodeCollector, pCombinedLight, pStack, this.minecraft);
            mapatlases$renderingAtlas = false;
            ci.cancel();
        }
    }
}
