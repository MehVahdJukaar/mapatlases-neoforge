package pepjebs.mapatlases.mixin;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.utils.AtlasLectern;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity implements AtlasLectern {

    @Shadow
    ItemStack book;

    @Shadow abstract void onBookItemRemove();

    @Unique
    private boolean mapatlases$hasAtlas = false;

    protected LecternBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }


    @Inject(method = "saveAdditional", at = @At("TAIL"))
    public void onSave(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (mapatlases$hasAtlas) tag.putBoolean("has_atlas", true);
    }

    @Inject(method = "loadAdditional", at = @At("TAIL"))
    public void onLoad(CompoundTag tag, HolderLookup.Provider registries, CallbackInfo ci) {
        if (tag.contains("has_atlas")) mapatlases$hasAtlas = tag.getBoolean("has_atlas");
    }

    @Override
    public boolean mapatlases$hasAtlas() {
        return mapatlases$hasAtlas;
    }

    @Override
    public boolean mapatlases$setAtlas(Player player, ItemStack atlas) {
        if(LecternBlock.tryPlaceBook(
                player,
                level,
                worldPosition,
                getBlockState(),
                atlas
        )){
            this.mapatlases$hasAtlas = true;
            return true;
        }
        return false;
    }

    @Override
    public ItemStack mapatlases$removeAtlas(){
        this.mapatlases$hasAtlas = false;
        ItemStack atlas = this.book;
        this.book = ItemStack.EMPTY;
        this.onBookItemRemove();
        return atlas;
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return this.saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
