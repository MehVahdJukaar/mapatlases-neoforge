package pepjebs.mapatlases.mixin;

import net.minecraft.client.gui.MapRenderer;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.integration.moonlight.MapLightHandler;

@Mixin(value = MapRenderer.MapInstance.class, priority = 1200)
public abstract class MapTextureMixin {


    @Shadow
    private MapItemSavedData data;

    @Shadow
    @Final
    private DynamicTexture texture;

    @Inject(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/renderer/texture/DynamicTexture;upload()V",
            shift = At.Shift.BEFORE), method = "updateTexture")
    public void updateColoredTexture(CallbackInfo ci) {
        if(MapAtlasesMod.MOONLIGHT)
            MapLightHandler.getLightData(this.data).processTexture(this.texture.getPixels(), 0, 0);
    }


}