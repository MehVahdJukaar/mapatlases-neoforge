package pepjebs.mapatlases.mixin.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.map_collection.forge.IMapCollectionImpl;

@Mixin(MapAtlasItem.class)
public abstract class SelfMapAtlasItemMixin extends Item {

    @Unique
    private static final String SHARE_TAG = "map_cap";

    protected SelfMapAtlasItemMixin(Properties arg) {
        super(arg);
    }

    @Nullable
    @Override
    public ICapabilityProvider initCapabilities(ItemStack stack, @Nullable CompoundTag nbt) {
        return new IMapCollectionImpl. Provider();
    }


    // I hate this
    @Nullable
    @Override
    public CompoundTag getShareTag(ItemStack stack) {
        CompoundTag baseTag = stack.getTag();
        var cap = stack.getCapability(IMapCollectionImpl.ATLAS_CAP_TOKEN, null).resolve().get();
        if (baseTag == null) baseTag = new CompoundTag();
        baseTag = baseTag.copy();
        baseTag.put(SHARE_TAG, cap.serializeNBT());
        return baseTag;
    }

    @Override
    public void readShareTag(ItemStack stack, @Nullable CompoundTag tag) {
        if (tag != null && tag.contains(SHARE_TAG)) {
            CompoundTag capTag = tag.getCompound(SHARE_TAG);
            tag.remove(SHARE_TAG);
            var cap = stack.getCapability(IMapCollectionImpl.ATLAS_CAP_TOKEN, null).resolve().get();
            cap.deserializeNBT(capTag);
        }
        stack.setTag(tag);
    }


}
