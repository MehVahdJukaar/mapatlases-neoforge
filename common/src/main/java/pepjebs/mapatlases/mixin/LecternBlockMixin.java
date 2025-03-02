package pepjebs.mapatlases.mixin;

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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.AtlasLectern;

import java.util.Optional;

@Mixin(LecternBlock.class)
public abstract class LecternBlockMixin extends Block {


    protected LecternBlockMixin(Properties arg) {
        super(arg);
    }


    //use click events? should really use click events
    @Inject(
            method = "useWithoutItem",
            at = @At(value = "HEAD"),
            cancellable = true
    )
    public void injectAtlasRemoval(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        if (state.getValue(LecternBlock.HAS_BOOK) && level.getBlockEntity(pos) instanceof AtlasLectern al
                && al.mapatlases$hasAtlas()) {
            if (player.isSecondaryUseActive()) {
                LecternBlockEntity lbe = (LecternBlockEntity) al;
                ItemStack atlas = lbe.getBook();
                if (!player.getInventory().add(atlas)) {
                    player.drop(atlas, false);
                }
                al.mapatlases$removeAtlas();
                cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            } else {
                LecternBlockEntity lbe = (LecternBlockEntity) al;
                ItemStack atlas = lbe.getBook();

                if(level.isClientSide) {

                    if(atlas.getItem() instanceof MapAtlasItem) {
                        //MapAtlasesClient.openScreen(atlas, lbe);
                    }
                }else{
                    MapAtlasItem.syncAndOpenGui((ServerPlayer) player, atlas, Optional.ofNullable(pos), false);
                }
                cir.setReturnValue(InteractionResult.sidedSuccess(level.isClientSide));
            }
        }
    }
}
