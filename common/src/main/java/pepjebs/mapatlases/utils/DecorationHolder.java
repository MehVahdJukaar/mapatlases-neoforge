package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.map.decoration.MLMapDecoration;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.saveddata.maps.MapDecoration;

public abstract sealed class DecorationHolder permits VanillaDecorationHolder, CustomDecorationHolder {
    protected final String id;
    protected final MapDataHolder data;
    protected final String sortingString;

    protected DecorationHolder(String id, MapDataHolder data, String sortingString) {
        this.id = id;
        this.data = data;
        this.sortingString = sortingString;
    }

    public String id() {
        return id;
    }

    public MapDataHolder data() {
        return data;
    }

    public String sortingString() {
        return sortingString;
    }

    public abstract double decorationDistSq(double px, double pz);

    public abstract double getWorldX();

    public abstract double getWorldZ();

    public abstract Component getDecorationName();

    public abstract void renderDecoration(GuiGraphics graphics, float centerX, float centerY);

    public abstract void deleteMarker();

    public boolean canDeleteMarker() {
        return true;
    }

    public boolean canFocusMarker() {
        return false;
    }

    public void focusMarker() {
    }

    public static VanillaDecorationHolder vanilla(MapDecoration deco, String id, MapDataHolder data) {
        return new VanillaDecorationHolder(deco, id, data);
    }

    public static CustomDecorationHolder custom(MLMapDecoration deco, String id, MapDataHolder data) {
        return new CustomDecorationHolder(deco, id, data);
    }
}