package pepjebs.mapatlases.integration;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.saveddata.maps.MapId;
import net.minecraft.world.level.saveddata.maps.MapItemSavedData;
import pepjebs.mapatlases.MapAtlasesMod;
import pepjebs.mapatlases.client.MapAtlasesClient;
import pepjebs.mapatlases.config.MapAtlasesClientConfig;
import pepjebs.mapatlases.config.MapAtlasesConfig;
import pepjebs.mapatlases.item.MapAtlasItem;

import java.io.IOException;
import java.lang.reflect.Method;

public class SupplementariesClientCompat {
    private static final Identifier DAY_TEXTURE_ID = MapAtlasesMod.res("textures/item/lightmap_day.png");
    private static final Identifier NIGHT_TEXTURE_ID = MapAtlasesMod.res("textures/item/lightmap_night.png");

    private static NativeImage dayTexture;
    private static NativeImage nightTexture;
    private static Boolean usingDayTexture;
    private static boolean lightMapActive;
    private static boolean textureLoadFailed;
    private static boolean reflectionFailed;
    private static Method setActiveMethod;
    private static Method setLightMapMethod;

    public static void init() {
    }

    public static void onClientTick(ClientLevel level) {
        boolean active = MapAtlasesConfig.lightMap.get();
        if (lightMapActive != active) {
            lightMapActive = active;
            invokeSetActive(active);
            usingDayTexture = null;
            refreshActiveAtlasMaps(level);
        }
        if (!active || reflectionFailed) {
            return;
        }
        if (!ensureTexturesLoaded()) {
            return;
        }

        boolean isDayTexture = !MapAtlasesClientConfig.nightLightMap.get() || isDay(level);
        if (usingDayTexture == null || usingDayTexture != isDayTexture) {
            usingDayTexture = isDayTexture;
            invokeSetLightMap(isDayTexture ? dayTexture : nightTexture);
            refreshActiveAtlasMaps(level);
        }
    }

    private static boolean ensureTexturesLoaded() {
        if (dayTexture != null && nightTexture != null) {
            return true;
        }
        if (textureLoadFailed) {
            return false;
        }
        try {
            var resourceManager = Minecraft.getInstance().getResourceManager();
            try (var dayStream = resourceManager.getResource(DAY_TEXTURE_ID).orElseThrow().open()) {
                dayTexture = NativeImage.read(dayStream);
            }
            try (var nightStream = resourceManager.getResource(NIGHT_TEXTURE_ID).orElseThrow().open()) {
                nightTexture = NativeImage.read(nightStream);
            }
            return true;
        } catch (IOException e) {
            textureLoadFailed = true;
            MapAtlasesMod.LOGGER.error("Failed to load atlas lightmap textures", e);
            return false;
        }
    }

    private static boolean isDay(ClientLevel level) {
        long timeOfDay = Math.floorMod(level.getOverworldClockTime(), 24000L);
        return timeOfDay < 13000L || timeOfDay > 23000L;
    }

    private static void refreshActiveAtlasMaps(ClientLevel level) {
        var atlas = MapAtlasesClient.getCurrentActiveAtlas();
        if (atlas.isEmpty()) {
            return;
        }
        var mapTextureManager = Minecraft.getInstance().getMapTextureManager();
        var maps = MapAtlasItem.getMaps(atlas, level);
        maps.addNotSynced(level);
        for (int id : maps.getAllIds()) {
            MapItemSavedData data = level.getMapData(new MapId(id));
            if (data != null) {
                mapTextureManager.update(new MapId(id), data);
            }
        }
    }

    private static void invokeSetActive(boolean active) {
        Method method = getSetActiveMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, active);
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            MapAtlasesMod.LOGGER.error("Failed to toggle Supplementaries map light handler", e);
        }
    }

    private static void invokeSetLightMap(NativeImage image) {
        Method method = getSetLightMapMethod();
        if (method == null) {
            return;
        }
        try {
            method.invoke(null, image);
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            MapAtlasesMod.LOGGER.error("Failed to update Supplementaries atlas light map", e);
        }
    }

    private static Method getSetActiveMethod() {
        if (reflectionFailed) {
            return null;
        }
        if (setActiveMethod != null) {
            return setActiveMethod;
        }
        try {
            Class<?> clazz = Class.forName("net.mehvahdjukaar.supplementaries.common.misc.MapLightHandler");
            setActiveMethod = clazz.getMethod("setActive", boolean.class);
            return setActiveMethod;
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            MapAtlasesMod.LOGGER.error("Failed to resolve Supplementaries map light handler", e);
            return null;
        }
    }

    private static Method getSetLightMapMethod() {
        if (reflectionFailed) {
            return null;
        }
        if (setLightMapMethod != null) {
            return setLightMapMethod;
        }
        try {
            Class<?> clazz = Class.forName("net.mehvahdjukaar.supplementaries.common.misc.MapLightHandler");
            setLightMapMethod = clazz.getMethod("setLightMap", NativeImage.class);
            return setLightMapMethod;
        } catch (ReflectiveOperationException e) {
            reflectionFailed = true;
            MapAtlasesMod.LOGGER.error("Failed to resolve Supplementaries lightmap setter", e);
            return null;
        }
    }
}
