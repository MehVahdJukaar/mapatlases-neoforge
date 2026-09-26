package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.mehvahdjukaar.moonlight.api.resources.assets.LangBuilder;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.Optional;

public abstract sealed class DecorationHolder permits VanillaDecorationHolder, CustomDecorationHolder {
    protected final String id;
    protected final AtlasMap data;
    protected final String sortingString;
    private final ResourceLocation typeId;
    @Nullable
    private final Component name;

    protected DecorationHolder(String id, AtlasMap data, ResourceLocation typeId, @Nullable Component name) {
        this.id = id;
        this.data = data;
        this.typeId = typeId;
        this.name = name!=null ? name : Component.literal(LangBuilder.getReadableName(
                typeId.getPath().toLowerCase(Locale.ROOT)));
        this.sortingString = name == null ? typeId.getPath() : typeId.getPath() + " " + name.getString();
    }

    public String id() {
        return id;
    }

    public AtlasMap data() {
        return data;
    }

    public String sortingString() {
        return sortingString;
    }

    protected abstract int decoX();

    protected abstract int decoY();

    public double getWorldX() {
        return data.data.centerX + (1 << data.data.scale) * decoX() / 2.0;
    }

    public double getWorldZ() {
        return data.data.centerZ + (1 << data.data.scale) * decoY() / 2.0;
    }

    public double decorationDistSq(double px, double pz) {
        return Mth.square(getWorldX() - px) + Mth.square(getWorldZ() - pz);
    }

    public Component getDecorationName() {
        return name;
    }

    public abstract void renderDecoration(GuiGraphics graphics, float centerX, float centerY);

    public abstract void deleteMarker(Optional<BlockPos> lecternPos);

    public boolean canDeleteMarker() {
        return true;
    }

    public boolean canFocusMarker() {
        return false;
    }

    public void focusMarker() {
    }

    public static VanillaDecorationHolder vanilla(MapDecoration deco, String id, AtlasMap data) {
        return new VanillaDecorationHolder(deco, id, data);
    }

    public static CustomDecorationHolder custom(MLMapDecoration deco, String id, AtlasMap data) {
        return new CustomDecorationHolder(deco, id, data);
    }
}