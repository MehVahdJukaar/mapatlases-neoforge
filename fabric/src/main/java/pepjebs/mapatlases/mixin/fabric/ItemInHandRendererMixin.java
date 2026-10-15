package pepjebs.mapatlases.mixin.fabric;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import pepjebs.mapatlases.client.AtlasInHandRenderer;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @ModifyExpressionValue(method = "renderArmWithItem", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;has(Lnet/minecraft/core/component/DataComponentType;)Z"))
    private boolean mapAtlases$renderAtlasAsMap(boolean isMap, @Local(argsOnly = true) ItemStack stack,
                                                @Local(argsOnly = true) AbstractClientPlayer player) {
        return isMap || AtlasInHandRenderer.shouldRenderAsMap(stack, player);
    }
}
