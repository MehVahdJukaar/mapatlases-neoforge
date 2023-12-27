package pepjebs.mapatlases.map_collection.forge;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class CapStuff {
    public static final Capability<IMapCollectionImpl> ATLAS_CAP_TOKEN = CapabilityManager.get(new CapabilityToken<>() {
    });

    public static void register(RegisterCapabilitiesEvent event) {
        event.register(IMapCollectionImpl.class);
    }

    // maybe not needed, could be in cap itself
    public record Provider(
            LazyOptional<IMapCollectionImpl> capInstance) implements ICapabilitySerializable<CompoundTag> {

        public Provider(){
            this(LazyOptional.of(IMapCollectionImpl::new));
        }
        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return ATLAS_CAP_TOKEN.orEmpty(cap, capInstance);
        }

        @Override
        public CompoundTag serializeNBT() {
            return capInstance.resolve().get().serializeNBT();
        }

        @Override
        public void deserializeNBT(CompoundTag nbt) {
            capInstance.resolve().get().deserializeNBT(nbt);
        }
    }


}
