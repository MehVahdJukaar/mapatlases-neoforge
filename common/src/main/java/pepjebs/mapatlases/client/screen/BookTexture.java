package pepjebs.mapatlases.client.screen;

import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import pepjebs.mapatlases.client.MapAtlasesClient;

record BookTexture(Identifier texture, int width, int height, int textureWidth,
                   int mapWidth, int mapHeight, int mapYOffset, int rightEdgeU, int leftEdgeU) {

    static final BookTexture SMALL = new BookTexture(MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE, 162, 167, 256, 128, 128, 5, 189, 194);
    static final BookTexture BIG = new BookTexture(MapAtlasesClient.ATLAS_BACKGROUND_TEXTURE_BIG, 290, 231, 512, 256, 192, 2, 304, 309);

    void render(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        int halfW = width / 2;
        int halfH = height / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenWidth / 2f, screenHeight / 2f);

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -halfW, -halfH, 0, 0, width, height, textureWidth, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, MapAtlasesClient.ATLAS_OVERLAY_TEXTURE, -halfW, -halfH, 0, 0, width, height, textureWidth, 256);
        graphics.pose().popMatrix();
    }

    //paper strips that hide the inner end of the bookmarks
    void renderEdges(GuiGraphicsExtractor graphics, int screenWidth, int screenHeight) {
        int halfW = width / 2;
        int halfH = height / 2;
        graphics.pose().pushMatrix();
        graphics.pose().translate(screenWidth / 2f, screenHeight / 2f);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, halfW - 10, -halfH, rightEdgeU, 0, 5, height, textureWidth, 256);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, -halfW + 5, -halfH, leftEdgeU, 0, 5, height, textureWidth, 256);
        graphics.pose().popMatrix();
    }
}
