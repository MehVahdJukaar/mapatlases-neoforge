package pepjebs.mapatlases.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.utils.MapAtlasesAccessUtils;
import pepjebs.mapatlases.utils.TriState;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(value = MapItemSavedData.class, priority = 1100)
public class MapItemSavedDataMixin {

    @Shadow
    @Final
    private Map<Player, MapItemSavedData.HoldingPlayer> carriedByPlayers;

    @ModifyExpressionValue(
            method = "tickCarriedBy",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/saveddata/maps/MapItemSavedData;mapMatcher(Lnet/minecraft/world/item/ItemStack;)Ljava/util/function/Predicate;")
    )
    private Predicate<ItemStack> mapAtlases$containsProxy(Predicate<ItemStack> predicate, @Local(argsOnly = true) Player player) {
        TriState state = MapAtlasesMod.containsHack();
        if (state == TriState.SET_FALSE) return stack -> false;
        //needs to call these for some reason... before the rest
        if(state == TriState.SET_TRUE) return stack -> true;
        return  stack -> predicate.test(stack)
                || (MapAtlasesAccessUtils.getAtlasFromCurioOrTrinket(player) == stack);

    }

    @Inject(method = "checkBanners", at = @At("HEAD"), cancellable = true)
    public void mapAtlases$preventCheckingOffThread(BlockGetter world, int x, int z, CallbackInfo ci) {
        if (world instanceof ServerLevel l && !l.getServer().isSameThread()) {
            ci.cancel();
        }
    }

    @Inject(method = "getHoldingPlayer", at = @At("HEAD"), cancellable = true)
    public void mapAtlases$preventModifyingOffThread(Player player,
                                                     CallbackInfoReturnable<MapItemSavedData.HoldingPlayer> cir) {
        if (player.level() instanceof ServerLevel l && !l.getServer().isSameThread()) {
            var value = this.carriedByPlayers.get(player);
            if (value == null) {
                //we cant modify the map so we return a dummy. updateMarkers will update this properly on thread
                value = ((MapItemSavedData) (Object) this).new HoldingPlayer(player);
            }
            cir.setReturnValue(value);
        }

    }
}
