package pepjebs.mapatlases.item.recipe;

import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

public abstract class AbstractAtlasRecipe extends CustomRecipe {

    //assemble doesnt get a level... weak so the world can still unload
    private WeakReference<Level> lastMatchedLevel = new WeakReference<>(null);

    protected AbstractAtlasRecipe(CraftingBookCategory category) {
        super(category);
    }

    protected void rememberLevel(Level level) {
        lastMatchedLevel = new WeakReference<>(level);
    }

    @Nullable
    protected Level getLevel() {
        return lastMatchedLevel.get();
    }
}
