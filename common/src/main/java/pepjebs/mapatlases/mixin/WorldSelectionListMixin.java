package pepjebs.mapatlases.mixin;

import net.minecraft.client.gui.screens.worldselection.WorldSelectionList;
import net.minecraft.world.level.storage.LevelSummary;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pepjebs.mapatlases.integration.moonlight.ClientMarkers;

@Mixin(WorldSelectionList.WorldListEntry.class)
public class WorldSelectionListMixin {

    @Shadow @Final
    LevelSummary summary;

    @Inject(method = "doDeleteWorld", at = @At("HEAD"))
    public void mapAtlases$deleteAtlasMarkers(CallbackInfo ci){
        ClientMarkers.deleteAllMarkersData(this.summary.getLevelId());
    }
}
