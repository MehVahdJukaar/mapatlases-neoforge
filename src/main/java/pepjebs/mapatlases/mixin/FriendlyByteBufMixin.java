package pepjebs.mapatlases.mixin;

import net.mehvahdjukaar.supplementaries.reg.ModRegistry;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;

@Mixin(FriendlyByteBuf.class)
public abstract  class FriendlyByteBufMixin {


    @Shadow public abstract FriendlyByteBuf writeItemStack(ItemStack par1, boolean par2);

    @Inject(method = "writeItemStack", at = @At(value = "HEAD"), remap = false, cancellable = true)
    public void sendCapsFromCreative(ItemStack stack, boolean useShareTag, CallbackInfoReturnable<FriendlyByteBuf> cir) {
        if (!useShareTag && stack.getItem() == MapAtlasesMod.MAP_ATLAS.get()) {
            cir.setReturnValue(this.writeItemStack(stack, true)); // needed for caps syncing in creative inv
        }
    }
}
