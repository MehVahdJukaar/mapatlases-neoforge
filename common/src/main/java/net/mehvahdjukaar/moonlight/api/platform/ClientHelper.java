package net.mehvahdjukaar.moonlight.api.platform;

import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;

import java.util.function.Consumer;
import java.util.function.Supplier;

public final class ClientHelper {

    private ClientHelper() {
    }

    public static void addKeyBindRegistration(Consumer<KeyBindEvent> callback) {
    }

    public static void addClientSetup(Runnable callback) {
    }

    public static void addClientReloadListener(Supplier<PreparableReloadListener> listenerFactory, Identifier id) {
    }

    @FunctionalInterface
    public interface KeyBindEvent {
        void register(KeyMapping keyMapping);
    }
}
