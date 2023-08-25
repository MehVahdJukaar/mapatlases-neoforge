package pepjebs.mapatlases.networking;

import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.listener.PacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.util.Identifier;
import pepjebs.mapatlases.MapAtlasesMod;

public class MapAtlasesOpenGUIC2SPacket implements Packet {

    public static final Identifier MAP_ATLAS_OPEN_GUI = new Identifier(MapAtlasesMod.MOD_ID, "open_gui");

    public ItemStack atlas;

    public MapAtlasesOpenGUIC2SPacket(){}

    public MapAtlasesOpenGUIC2SPacket(ItemStack atlas1) {
        atlas = atlas1;
    }

    public void read(PacketByteBuf buf) {
        atlas = buf.readItemStack();
    }

    public void write(PacketByteBuf buf) {
        buf.writeItemStack(atlas);
    }

    @Override
    public void apply(PacketListener listener) {

    }
}
