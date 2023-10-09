package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDataRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;
import pepjebs.mapatlases.utils.MapDataHolder;

import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class MoonlightCompat {
//TODO: fix

    public static void onLogin(MinecraftServer server){
        String name = server.getWorldData().getLevelName()  ;
        long id = server.overworld().getSeed();
        int hash = Objects.hash(name, id);
    }

    public static DecorationBookmarkButton makeCustomButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration);
    }

    public static Collection<Pair<Object, MapDataHolder>> getCustomDecorations(MapDataHolder map) {
        return ((ExpandedMapData) map.data).getCustomDecorations().values().stream()
                .map(a -> Pair.of((Object) a, map)).toList();
    }

    public static void addDecoration(MapItemSavedData data, BlockPos pos, ResourceLocation id, @Nullable Component name) {
        var type = MapDataRegistry.get(id);
        if (type != null) {
            MapBlockMarker<?> defaultMarker = type.createEmptyMarker();
            defaultMarker.setPos( pos);
            defaultMarker.setName(name);
            ((ExpandedMapData)data).addCustomMarker(defaultMarker);
        }
    }

    public static void removeCustomDecoration(MapItemSavedData data, int hash) {
        if (data instanceof ExpandedMapData d) {
            d.getCustomDecorations().entrySet().removeIf(e -> e.getValue().hashCode() == hash);
        }
    }

    public static class CustomDecorationButton extends DecorationBookmarkButton {

        private final CustomMapDecoration decoration;

        public CustomDecorationButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, CustomMapDecoration mapDecoration) {
            super(px, py, screen, data);
            this.decoration = mapDecoration;
            this.setTooltip(createTooltip());
        }

        @Override
        public double getWorldX() {
            return mapData.data.centerX - getDecorationPos(decoration.getX(), mapData.data);
        }

        @Override
        public double getWorldZ() {
            return mapData.data.centerZ - getDecorationPos(decoration.getY(), mapData.data);
        }

        @Override
        public int getBatchGroup() {
            return 1;
        }

        @Override
        public Component getDecorationName() {
            Component displayName = decoration.getDisplayName();
            return displayName == null
                    ? Component.literal(
                    AtlasOverviewScreen.getReadableName(Utils.getID(decoration.getType()).getPath()
                            .toLowerCase(Locale.ROOT)))
                    : displayName;
        }

        @Override
        protected void renderWidget(GuiGraphics pGuiGraphics, int pMouseX, int pMouseY, float pPartialTick) {
            PoseStack matrices = pGuiGraphics.pose();
            matrices.pushPose();
            super.renderWidget(pGuiGraphics, pMouseX, pMouseY, pPartialTick);

            DecorationRenderer<CustomMapDecoration> renderer = MapDecorationClientManager.getRenderer(decoration);

            if (renderer != null) {

                matrices.translate(getX() + width / 2f, getY() + height / 2f, 1.0D);

                // de translates by the amount the decoration renderer will translate
                matrices.translate(-(float) decoration.getX() / 2.0F - 64.0F,
                        -(float) decoration.getY() / 2.0F - 64.0F, -0.02F);

                var buffer = pGuiGraphics.bufferSource();

                VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);

                renderer.render(decoration, matrices,
                        vertexBuilder, buffer, mapData.data,
                        false, LightTexture.FULL_BRIGHT, index);


            }
            matrices.popPose();

            setSelected(false);

        }

        @Override
        protected void deleteMarker() {
            Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) mapData.data).getCustomDecorations();
            for (var d : decorations.entrySet()) {
                CustomMapDecoration deco = d.getValue();
                if (deco == decoration) {
                    MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(mapData.stringId, deco.hashCode()));
                    decorations.remove(d.getKey());
                    return;
                }
            }
        }
    }
}
