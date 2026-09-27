package pepjebs.mapatlases.utils;

import net.mehvahdjukaar.moonlight.api.platform.network.NetworkHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.data.AtlasIds;
import net.minecraft.util.Mth;
import net.minecraft.world.level.saveddata.maps.MapDecoration;
import org.joml.Matrix3x2fStack;
import pepjebs.mapatlases.client.AtlasGuiUtils;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;

import java.util.Map;
import java.util.Optional;

public final class VanillaDecorationHolder extends DecorationHolder {
    private final MapDecoration deco;

    VanillaDecorationHolder(MapDecoration deco, String id, AtlasMap data) {
        super(id, data, deco.type().unwrapKey().get().identifier(), deco.name().orElse(null));
        this.deco = deco;
    }

    public MapDecoration deco() {
        return deco;
    }

    @Override
    protected int decoX() {
        return deco.x();
    }

    @Override
    protected int decoY() {
        return deco.y();
    }

    @Override
    public void renderDecoration(GuiGraphicsExtractor graphics, float centerX, float centerY) {
        TextureAtlasSprite sprite = Minecraft.getInstance().getAtlasManager()
                .getAtlasOrThrow(AtlasIds.MAP_DECORATIONS).getSprite(deco.getSpriteLocation());
        Matrix3x2fStack pose = graphics.pose();
        pose.pushMatrix();
        pose.translate(centerX, centerY);
        pose.rotate((deco.rot() * 360) / 16.0F * Mth.DEG_TO_RAD);
        pose.scale(4);
        AtlasGuiUtils.drawMarkerSprite(graphics, sprite, -1, false);
        pose.popMatrix();
    }

    @Override
    public void deleteMarker(Optional<BlockPos> lecternPos) {
        Map<String, MapDecoration> decorations = data.data.decorations;
        var d = decorations.get(id);
        if (d != null) {
            NetworkHelper.sendToServer(new C2SRemoveMarkerPacket(data.id, data.type, d.hashCode(), false, lecternPos));
            decorations.remove(id);
        }
    }

    @Override
    public boolean canDeleteMarker() {
        return !deco.type().value().explorationMapElement();
    }
}