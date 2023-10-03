package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.DecorationRenderer;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientHandler;
import net.mehvahdjukaar.moonlight.api.map.markers.MapBlockMarker;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.vehicle.Minecart;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.jetbrains.annotations.Nullable;
import pepjebs.mapatlases.utils.MapDataHolder;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;
import pepjebs.mapatlases.networking.C2SRemoveMarkerPacket;
import pepjebs.mapatlases.networking.MapAtlasesNetworking;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MoonlightCompat {


    public static DecorationBookmarkButton makeCustomButton(int px, int py, AtlasOverviewScreen screen, MapDataHolder data, Object mapDecoration) {
        return new CustomDecorationButton(px, py, screen, data, (CustomMapDecoration) mapDecoration);
    }

    public static Collection<Pair<Object, MapDataHolder>> getCustomDecorations(MapDataHolder data) {
        return ((ExpandedMapData) data.data()).getCustomDecorations().values().stream()
                .map(a -> Pair.of((Object) a, data)).toList();
    }

    public static void addDecoration(MapItemSavedData second, BlockPos pos, ResourceLocation id, @Nullable Component name) {
        var type = MapDecorationRegistry.get(id);
        if(type != null){
            MapBlockMarker<?> defaultMarker = type.getDefaultMarker(pos);
            var decoration = defaultMarker.createDecorationFromMarker(second);
            if (decoration != null) {
                decoration.setDisplayName(name);
                ExpandedMapData data = (ExpandedMapData) second;
                data.getCustomDecorations().put(defaultMarker.getMarkerId(), decoration);
                data.setCustomDecorationsDirty();
            }
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
            this.tooltip = (createTooltip());
        }

        @Override
        public double getWorldX() {
            return data.data().x - getDecorationPos(decoration.getX(), data.data());
        }

        @Override
        public double getWorldZ() {
            return data.data().z - getDecorationPos(decoration.getY(), data.data());
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
            super.renderButton(matrices, pMouseX, pMouseY, pPartialTick);

            DecorationRenderer<CustomMapDecoration> renderer = MapDecorationClientManager.getRenderer(decoration);

            if(renderer != null) {

                matrices.translate(x + width / 2f, y + height / 2f, 1.0D);

                // de translates by the amount the decoration renderer will translate
                matrices.translate(-(float) decoration.getX() / 2.0F - 64.0F,
                        -(float) decoration.getY() / 2.0F - 64.0F, -0.02F);

                var buffer = pGuiGraphics.bufferSource();

                renderer.render(decoration, matrices,
                        vertexBuilder, buffer, data.data(),
                        false, LightTexture.FULL_BRIGHT, index);


            }
            matrices.popPose();

            setSelected(false);

        }

        @Override
        protected void deleteMarker() {
            Map<String, CustomMapDecoration> decorations = ((ExpandedMapData) data.data()).getCustomDecorations();
            for (var d : decorations.entrySet()) {
                CustomMapDecoration deco = d.getValue();
                if (deco == decoration) {
                    MapAtlasesNetworking.sendToServer(new C2SRemoveMarkerPacket(data.stringId(), deco.hashCode()));
                    decorations.remove(d.getKey());
                    return;
                }
            }
        }
    }
}
