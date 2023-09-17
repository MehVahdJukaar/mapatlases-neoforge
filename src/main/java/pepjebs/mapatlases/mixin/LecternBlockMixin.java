package pepjebs.mapatlases.mixin;

import net.minecraft.core.BlockPos;
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
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.AtlasHolder;

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

    }

    //use click events?
    @Inject(
            method = "use",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void injectAtlasRemoval(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit, CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(LecternBlock.HAS_BOOK) && level.getBlockEntity(pos) instanceof AtlasHolder al
                && al.mapatlases$hasAtlas()) {
            if (player.isSecondaryUseActive()) {
                LecternBlockEntity lbe = (LecternBlockEntity) al;
                ItemStack atlas = lbe.getBook();
                if (!player.getInventory().add(atlas)) {
                    player.drop(atlas, false);
                }
                al.mapatlases$setAtlas(false);
                LecternBlock.resetBookState( level, pos, state, false);
                cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            } else {
                if(level.isClientSide) {
                    LecternBlockEntity lbe = (LecternBlockEntity) al;
                    ItemStack atlas = lbe.getBook();
                    MapAtlasesClient.openScreen(atlas, lbe);
                }
                cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            }
        }
    }
}
