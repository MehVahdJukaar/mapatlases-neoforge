package pepjebs.mapatlases.mixin;

import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.LecternBlock;
import net.minecraft.block.entity.LecternBlockEntity;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.Property;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;

@Mixin(LecternBlock.class)
public class LecternBlockMixin extends Block {

    public LecternBlockMixin(Settings settings) {
        super(settings);
    }

    @Inject(method = "<init>(Lnet/minecraft/block/AbstractBlock$Settings;)V", at = @At(value = "RETURN"))
    public void lecternAtlasConstructorMixin(AbstractBlock.Settings settings, CallbackInfo ci) {
        this.setDefaultState(this.getDefaultState().with(MapAtlasItem.HAS_ATLAS, false));
    }

    @Inject(
            method = "appendProperties(Lnet/minecraft/state/StateManager$Builder;)V",
            at = @At(value = "RETURN")
    )
    public void injectAtlasProperty(StateManager.Builder<Block, BlockState> builder, CallbackInfo ci) {
        builder.add(new Property[]{MapAtlasItem.HAS_ATLAS});
    }

    @Inject(
            method = "openScreen(Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;)V",
            at = @At(value = "INVOKE"),
            cancellable = true
    )
    public void injectAtlasScreen(World world, BlockPos pos, PlayerEntity player, CallbackInfo ci) {
        BlockState blockState = world.getBlockState(pos);
        if (blockState.contains(MapAtlasItem.HAS_ATLAS) && blockState.get(MapAtlasItem.HAS_ATLAS)) {
            MapAtlasesMod.MAP_ATLAS.openHandledAtlasScreen(world, player);
            ci.cancel();
        }
    }

    @Inject(
            method = "onUse(Lnet/minecraft/block/BlockState;Lnet/minecraft/world/World;Lnet/minecraft/util/math/BlockPos;Lnet/minecraft/entity/player/PlayerEntity;Lnet/minecraft/util/Hand;Lnet/minecraft/util/hit/BlockHitResult;)Lnet/minecraft/util/ActionResult;",
            at = @At(value = "INVOKE"),
            cancellable = true
    )
    public void injectAtlasRemoval(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                   Hand hand, BlockHitResult hit, CallbackInfoReturnable<ActionResult> ci) {
        if (state.contains(MapAtlasItem.HAS_ATLAS) && state.get(MapAtlasItem.HAS_ATLAS) && player.getPose()
                == EntityPose.CROUCHING) {
            LecternBlockEntity lbe = (LecternBlockEntity) world.getBlockEntity(pos);
            if (lbe == null) return;
            ItemStack atlas = lbe.getBook();
            if (!player.getInventory().insertStack(atlas)) {
                player.dropItem(atlas, false);
            }
            LecternBlock.setHasBook(player, world, pos, state.with(MapAtlasItem.HAS_ATLAS, false), false);
            ci.setReturnValue(ActionResult.success(world.isClient));
        }
    }
}
