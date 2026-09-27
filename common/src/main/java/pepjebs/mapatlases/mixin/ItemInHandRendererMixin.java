package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
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
import pepjebs.mapatlases.item.MapAtlasItem;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;
    @Unique
    private boolean mapatlases$renderingAtlas = false;

    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean mapAtlases$renderAtlasAsMap(boolean isMap, @Local(argsOnly = true) ItemStack stack,
                                                @Local(argsOnly = true) AbstractClientPlayer player) {
        if (stack.is(MapAtlasesMod.MAP_ATLAS.get()) && MapAtlasesClientConfig.inHandMode.get().isOn(stack)) {
            if (!MapAtlasItem.getMaps(stack, player.level()).hasDimension(player.level().dimension())) return isMap;
            mapatlases$renderingAtlas = true;
            return true;
        }
        return isMap;
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void mapAtlases$renderAtlasInHand(PoseStack poseStack, SubmitNodeCollector collector, int light, ItemStack stack, CallbackInfo ci) {
        if (mapatlases$renderingAtlas) {
            AtlasInHandRenderer.render(poseStack, collector, light, stack, this.minecraft);
            mapatlases$renderingAtlas = false;
            ci.cancel();
        }
    }
}
