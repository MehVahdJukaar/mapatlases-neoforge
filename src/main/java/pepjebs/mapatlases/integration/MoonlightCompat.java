package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.datafixers.util.Pair;
import net.mehvahdjukaar.moonlight.api.map.CustomMapDecoration;
import net.mehvahdjukaar.moonlight.api.map.ExpandedMapData;
import net.mehvahdjukaar.moonlight.api.map.MapDecorationRegistry;
import net.mehvahdjukaar.moonlight.api.map.client.MapDecorationClientManager;
import net.mehvahdjukaar.moonlight.api.util.Utils;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.client.screen.AtlasOverviewScreen;
import pepjebs.mapatlases.client.screen.DecorationBookmarkButton;

import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class MoonlightCompat {


    public static DecorationBookmarkButton makeCustomButton(int px, int py, Object mapDecoration, AtlasOverviewScreen screen) {
        return new CustomDecorationButton(px, py, (CustomMapDecoration) mapDecoration, screen);
    }

    public static Collection<Pair<Object, MapItemSavedData>> getCustomDecorations(MapItemSavedData data) {
        return ((ExpandedMapData) data).getCustomDecorations().values().stream()
                .map(a -> Pair.of((Object) a, data)).toList();
    }

    public static class CustomDecorationButton extends DecorationBookmarkButton {

        private final CustomMapDecoration decoration;

        public CustomDecorationButton(int px, int py, CustomMapDecoration mapDecoration, AtlasOverviewScreen screen) {
            super(px, py, screen);
            this.decoration = mapDecoration;
            this.tooltip = (createTooltip());
        }

        @Override
        public double getWorldX(MapItemSavedData data) {
            return data.x - getDecorationPos(decoration.getX(), data);
        }

        @Override
        public double getWorldZ(MapItemSavedData data) {
            return data.z - getDecorationPos(decoration.getY(), data);
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
                    AtlasOverviewScreen.getReadableName(MapDecorationRegistry.hackyGetRegistry().getKey(decoration.getType()).getPath()
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
            VertexConsumer vertexBuilder = buffer.getBuffer(MapDecorationClientManager.MAP_MARKERS_RENDER_TYPE);

            MapDecorationClientManager.render(decoration, matrices,
                    vertexBuilder, buffer, null, false, LightTexture.FULL_BRIGHT, 0);

            matrices.popPose();

            setSelected(false);

        }
    }
}
