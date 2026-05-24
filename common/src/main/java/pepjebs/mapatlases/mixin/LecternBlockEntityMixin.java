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
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.AtlasLectern;

@Mixin(LecternBlockEntity.class)
public abstract class LecternBlockEntityMixin extends BlockEntity implements AtlasLectern {

    @Shadow
    ItemStack book;

    @Shadow abstract void onBookItemRemove();

    protected LecternBlockEntityMixin(BlockEntityType<?> pType, BlockPos pPos, BlockState pBlockState) {
        super(pType, pPos, pBlockState);
    }

    @Override
    public boolean mapatlases$hasAtlas() {
        return book.is(MapAtlasesMod.MAP_ATLAS.get());
    }

    @Override
    public boolean mapatlases$setAtlas(Player player, ItemStack atlas) {
        // Flag must be set before tryPlaceBook because that triggers sendBlockUpdated,
        // which serialises the block entity and sends it to the client. Setting it after
        // would mean the client receives has_atlas=false in that packet.
        if (LecternBlock.tryPlaceBook(player, level, worldPosition, getBlockState(), atlas)) {
            return true;
        }
        return false;
    }

    @Override
    public ItemStack mapatlases$removeAtlas(){
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
