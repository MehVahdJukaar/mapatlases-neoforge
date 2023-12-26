package pepjebs.mapatlases.mixin;

import net.minecraft.client.quickplay.QuickPlayLog;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

@Mixin(QuickPlayLog.class)
public class QuickPlayLogHackMixin {

    @Inject(method = "setWorldData", at = @At("HEAD"))
    public void associatedFolderNameWithLevelName(QuickPlayLog.Type pType, String pId, String pName, CallbackInfo ci){
        if(MapAtlasesMod.MOONLIGHT) ClientMarkers.setWorldFolder(pId, pType);
    }
}
