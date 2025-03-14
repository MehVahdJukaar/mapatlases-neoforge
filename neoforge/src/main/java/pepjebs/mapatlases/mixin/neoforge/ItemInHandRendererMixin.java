package pepjebs.mapatlases.mixin.neoforge;

import com.llamalad7.mixinextras.sugar.Local;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.ItemInHandRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
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

    @Shadow
    protected abstract void renderTwoHandedMap(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float pitch, float equippedProgress, float swingProgress);

    @Shadow
    protected abstract void renderOneHandedMap(PoseStack poseStack, MultiBufferSource buffer, int packedLight, float equippedProgress, HumanoidArm hand, float swingProgress, ItemStack stack);

    @Shadow
    private ItemStack offHandItem;
    @Unique
    private boolean mapatlases$renderingAtlas = false;

    @Inject(method = "renderArmWithItem", at = @At(value = "INVOKE",
            shift = At.Shift.BEFORE,
            target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z"), cancellable = true)
    public void renderMapAtlasItem(AbstractClientPlayer player, float partialTicks, float pitch, InteractionHand hand,
                                   float swingProgress, ItemStack stack, float equippedProgress,
                                   PoseStack poseStack, MultiBufferSource buffer, int combinedLight,
                                   CallbackInfo ci, @Local(argsOnly = true) ItemStack pStack,
                                   @Local HumanoidArm humanoidarm, @Local(ordinal = 0) boolean flag) {
        if (pStack.is(MapAtlasesMod.MAP_ATLAS.get()) && MapAtlasesClientConfig.inHandMode.get().isOn(pStack)) {
            if (!MapAtlasItem.getMaps(stack, player.level()).mapsDimension(player.level().dimension())) return;
            mapatlases$renderingAtlas = true;
            if (flag && this.offHandItem.isEmpty()) {
                this.renderTwoHandedMap(poseStack, buffer, combinedLight, pitch, equippedProgress, swingProgress);
            } else {
                this.renderOneHandedMap(poseStack, buffer, combinedLight, equippedProgress, humanoidarm, swingProgress, stack);
            }
            poseStack.popPose();
            ci.cancel();
        }
    }

    @Inject(method = "renderMap", at = @At("HEAD"), cancellable = true)
    public void renderMapAtlasInHand(PoseStack pPoseStack, MultiBufferSource pBuffer, int pCombinedLight, ItemStack pStack, CallbackInfo ci) {
        if (mapatlases$renderingAtlas) {
            AtlasInHandRenderer.render(pPoseStack, pBuffer, pCombinedLight, pStack, this.minecraft);
            mapatlases$renderingAtlas = false;
            ci.cancel();
        }
    }
}
