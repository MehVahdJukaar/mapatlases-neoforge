package net.mehvahdjukaar.moonlight.api.platform.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.Function;

public final class ChannelHandler {

    public static Builder builder(String id) {
        return new Builder();
    }

    public void sendToServer(Message message) {
    }

    public void sendToClientPlayer(ServerPlayer player, Message message) {
    }

    public static final class Builder {
        public Builder version(int version) {
            return this;
        }

        public <T extends Message> Builder register(NetworkDir dir, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
            return this;
        }

        public ChannelHandler build() {
            return new ChannelHandler();
        }
    }

    public static final class Context {
        private final NetworkDir direction;
        private final ServerPlayer sender;

        public Context(NetworkDir direction, ServerPlayer sender) {
            this.direction = direction;
            this.sender = sender;
        }

        public NetworkDir getDirection() {
            return direction;
        }

        public ServerPlayer getSender() {
            return sender;
        }
    }
}
