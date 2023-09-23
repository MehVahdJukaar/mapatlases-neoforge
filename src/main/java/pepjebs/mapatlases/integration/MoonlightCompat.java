package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientHandler;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetowrking;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoonlightCompat {


    public static DecorationBookmarkButton makeCustomButton(int px, int py, AtlasOverviewScreen screen, Pair<String, MapItemSavedData> data, Object mapDecoration) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration);
    }

    public static Collection<Pair<Object, Pair<String,MapItemSavedData>>> getCustomDecorations(Pair<String,MapItemSavedData> data) {
        return ((ExpandedMapData) data.getSecond()).getCustomDecorations().values().stream()
                .map(a -> Pair.of((Object) a, data)).toList();
    }

    public static void addDecoration(MapItemSavedData second, BlockPos pos, ResourceLocation name) {
        var type = MapDecorationRegistry.get(name);
        if(type != null){
            ((ExpandedMapData)second).addCustomDecoration(type.getDefaultMarker(pos));
        }
    }

    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
        if (data instanceof ExpandedMapData d) {
            d.getCustomDecorations().entrySet().removeIf(e -> e.getValue().hashCode() == hash);
        }
    }

    public static class CustomDecorationButton extends DecorationBookmarkButton {

        private final CustomMapDecoration decoration;

        public CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, Pair<String, MapItemSavedData> data, CustomMapDecoration mapDecoration) {
            super(px, py, screen, data);
            this.decoration = mapDecoration;
            this.tooltip = (createTooltip());
        }

        @Override
        public double getWorldX() {
            return data.getSecond().x - getDecorationPos(decoration.getX(), data.getSecond());
        }

        @Override
        public double getWorldZ() {
            return data.getSecond().z - getDecorationPos(decoration.getY(), data.getSecond());
        }

        @Override
        public int getBatchGroup() {
            return 1;
        }

        @Override
        public List<Component> createTooltip() {
            Component displayName = decoration.getDisplayName();
            Component mapIconComponent = displayName == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(Utils.getID(decoration.getType()).getPath()
                            .toLowerCase(Locale.ROOT)))
                    : displayName;

            // draw text
            MutableComponent coordsComponent = Component.literal("X: " + decoration.getX() + ", Z: " + decoration.getY());
            MutableComponent formattedCoords = coordsComponent.setStyle(Style.EMPTY.applyFormat(ChatFormatting.GRAY));
            return List.of(mapIconComponent, formattedCoords);
        }

        @Override
        public void renderButton(PoseStack matrices, int pMouseX, int pMouseY, float pPartialTick) {
            matrices.pushPose();
            matrices.translate(0, 0, 0.01 * this.index);
            super.renderButton(matrices, pMouseX, pMouseY, pPartialTick);

            matrices.translate(x + width / 2f, y + height / 2f, 1.0D);

            // de translates by the amount the decoration renderer will translate
            matrices.translate(-(float) decoration.getX() / 2.0F - 64.0F,
                    -(float) decoration.getY() / 2.0F - 64.0F, -0.02F);

            var buffer = Minecraft.getInstance().renderBuffers().bufferSource();

            MapDecorationClientHandler.render(decoration, matrices,
                    buffer, null, false, LightTexture.FULL_BRIGHT, 0);

            buffer.endBatch();
            matrices.popPose();

            setSelected(false);

        }

        @Override
        protected void deleteMarker() {
            Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) data.getSecond()).getCustomDecorations();
            for (var d : decorations.entrySet()) {
                CustomMapDecoration deco = d.getValue();
                if (deco == decoration) {
                    MapAtlasesNetowrking.sendToServer(new C2SRemoveMarkerPacket(data.getFirst(), deco.hashCode()));
                    decorations.remove(d.getKey());
                    return;
                }
            }
        }
    }
}
