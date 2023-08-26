package pebjebs.mapatlases.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pebjebs.mapatlases.MapAtlasesMod;
import pebjebs.mapatlases.utils.AtlasHolder;

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin extends Block {


    protected LecternBlockMixin(Properties arg) {
        super(arg);
    }

    @Inject(
            method = "openScreen",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void injectAtlasScreen(Level level, BlockPos pos, Player player, CallbackInfo ci) {
        if (level.getBlockEntity(pos) instanceof AtlasHolder al && al.mapatlases$hasAtlas()) {
            MapAtlasesMod.MAP_ATLAS.get().openHandledAtlasScreen((ServerPlayer) player);
            ci.cancel();
        }
    }

    @Inject(
            method = "use",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void injectAtlasRemoval(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (player.isSecondaryUseActive() && level.getBlockEntity(pos) instanceof AtlasHolder al && al.mapatlases$hasAtlas()) {
            LecternBlockEntity lbe = (LecternBlockEntity) al;
            ItemStack atlas = lbe.getBook();
            if (!player.getInventory().add(atlas)) {
                player.drop(atlas, false);
            }
            al.mapatlases$setAtlas(false);
            LecternBlock.resetBookState(player, level, pos, state, false);
            cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
        }
    }
}
