package pebjebs.mapatlases.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class DummyFilledMap extends Item {

    public DummyFilledMap(Properties settings) {
        super(settings);
    }

    @Override
    public void appendHoverText(ItemStack stack, @Nullable Level level, List<Component> tooltip, TooltipFlag isAdvanced) {
        super.appendHoverText(stack, level, tooltip, isAdvanced);
        tooltip.add(Component.translatable("item.map_atlases.dummy_filled_map.dummy")
                .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("item.map_atlases.dummy_filled_map.desc")
                .withStyle(ChatFormatting.ITALIC).withStyle(ChatFormatting.GRAY));
    }
}
