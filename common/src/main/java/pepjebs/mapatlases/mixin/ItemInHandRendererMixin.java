package pepjebs.mapatlases.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.AtlasInHandRenderer;

@Mixin(ItemInHandRenderer.class)
public abstract class ItemInHandRendererMixin {

    @Shadow
    @Final
    private Minecraft minecraft;

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    private void mapAtlases$renderAtlasInHand(PoseStack poseStack, SubmitNodeCollector collector, int light, ItemStack stack, CallbackInfo ci) {
        if (stack.is(MapAtlasesMod.MAP_ATLAS.get())) {
            AtlasInHandRenderer.render(poseStack, collector, light, stack, this.minecraft);
            ci.cancel();
        }
    }
}
