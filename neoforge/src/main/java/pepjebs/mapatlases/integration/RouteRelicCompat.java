package pepjebs.mapatlases.integration;

import com.avacuoss.RouteRelic.item.RouteRelicItem;
import com.avacuoss.RouteRelic.map.RouteMapPainter;
import com.avacuoss.RouteRelic.route.RouteAccess;
import com.avacuoss.RouteRelic.route.RouteSession;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.LecternBlockEntity;
import net.neoforged.neoforge.event.ItemStackedOnOtherEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.item.MapAtlasItem;
import pepjebs.mapatlases.utils.AtlasMap;

//route relics only changes maps in item frames for some reason
public class RouteRelicCompat {

    public static void onEntityInteract(PlayerInteractEvent.EntityInteract event) {
        Player player = event.getEntity();
        ItemStack relic = event.getItemStack();
        if (player.level().isClientSide || !(relic.getItem() instanceof RouteRelicItem)){
            return;
        }
        if (!(event.getTarget() instanceof ItemFrame frame)) return;
        ItemStack atlas = frame.getItem();
        if (!atlas.is(MapAtlasesMod.MAP_ATLAS.get())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        drawRouteWithFeedback(player, atlas, RouteRelicItem.getRelicColor(relic));
    }

    public static void onRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        ItemStack relic = event.getItemStack();
        if (player.level().isClientSide || !(relic.getItem() instanceof RouteRelicItem)) return;
        if (!(player.level().getBlockEntity(event.getPos()) instanceof LecternBlockEntity lectern)) return;
        ItemStack atlas = lectern.getBook();
        if (!atlas.is(MapAtlasesMod.MAP_ATLAS.get())) return;

        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        drawRouteWithFeedback(player, atlas, RouteRelicItem.getRelicColor(relic));
    }

    // bundle style
    public static void onItemStackedOnOther(ItemStackedOnOtherEvent event) {
        ItemStack relic = event.getCarriedItem();
        ItemStack atlas = event.getStackedOnItem();
        if (event.getClickAction() != ClickAction.SECONDARY || !(relic.getItem() instanceof RouteRelicItem)) return;
        if (!atlas.is(MapAtlasesMod.MAP_ATLAS.get())) return;

        event.setCanceled(true);
        Player player = event.getPlayer();
        if (!player.level().isClientSide) {
            drawRouteWithFeedback(player, atlas, RouteRelicItem.getRelicColor(relic));
        }
    }

    private static void drawRouteWithFeedback(Player player, ItemStack atlas, String color) {
        RouteSession session = RouteAccess.get(player).getSession();
        if (!session.hasFinishedRoute()) {
            player.displayClientMessage(Component.translatable("message.routerelic.no_finished_route"), true);
            return;
        }
        boolean baked = bake(player, atlas, session, color);
        player.displayClientMessage(Component.translatable(baked ?
                "message.routerelic.route_baked_to_map" : "message.routerelic.route_not_on_this_map"), true);
    }

    private static boolean bake(Player player, ItemStack atlas, RouteSession session, String color) {
        String routeDimension = session.getLastFinishedDimension();
        boolean baked = false;
        for (AtlasMap holder : MapAtlasItem.getMaps(atlas, player.level()).getAllFound()) {
            if (!holder.data.dimension.location().toString().equals(routeDimension)) continue;
            ItemStack mapStack = new ItemStack(Items.FILLED_MAP);
            mapStack.set(DataComponents.MAP_ID, holder.id);
            baked |= RouteMapPainter.bakeLastRouteToMap(mapStack, holder.data, session, color);
        }
        return baked;
    }
}
