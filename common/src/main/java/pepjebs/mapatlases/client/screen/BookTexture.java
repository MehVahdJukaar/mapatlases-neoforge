package pepjebs.mapatlases.client.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import pepjebs.mapatlases.client.MapAtlasesClient;

record BookTexture(ResourceLocation texture, int width, int height, int textureWidth,
                   int mapWidth, int mapHeight, int mapYOffset, int rightEdgeU, int leftEdgeU) {

    static final BookTexture SMALL = new BookTexture(MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE, 162, 167, 256, 128, 128, 5, 189, 194);
    static final BookTexture BIG = new BookTexture(MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE_BIG, 290, 231, 512, 256, 192, 2, 304, 309);

    void render(GuiGraphics graphics, int screenWidth, int screenHeight) {
        int halfW = width / 2;
        int halfH = height / 2;
        graphics.pose().pushPose();
        graphics.pose().translate(screenWidth / 2f, screenHeight / 2f, 0);

        RenderSystem.enableDepthTest();
        graphics.blit(texture, -halfW, -halfH, 0, 0, width, height, textureWidth, 256);
        graphics.blit(MapAtlasesClient.ATLAS_OVERLAY_TEXTURE, -halfW, -halfH, 0, 0, width, height, textureWidth, 256);

        graphics.pose().translate(0, 0, 1);
        graphics.blit(texture, halfW - 10, -halfH, rightEdgeU, 0, 5, height, textureWidth, 256);
        graphics.blit(texture, -halfW + 5, -halfH, leftEdgeU, 0, 5, height, textureWidth, 256);
        graphics.pose().popPose();
    }
}
