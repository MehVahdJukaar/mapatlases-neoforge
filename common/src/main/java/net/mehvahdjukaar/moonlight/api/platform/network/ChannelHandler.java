package net.mehvahdjukaar.moonlight.api.platform.network;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

public final class ChannelHandler {
    private final String channelId;
    private final List<Registration<?>> registrations;
    private final Map<Class<?>, Registration<?>> registrationsByClass;
    private boolean codecsInitialized;
    private boolean serverReceiversInitialized;
    private boolean clientInitialized;

    private ChannelHandler(String channelId, List<Registration<?>> registrations) {
        this.channelId = channelId;
        this.registrations = List.copyOf(registrations);
        this.registrationsByClass = new HashMap<>();
        for (Registration<?> registration : registrations) {
            registrationsByClass.put(registration.messageClass(), registration);
        }
    }

    public static Builder builder(String id) {
        return new Builder(id);
    }

    public void init() {
        initCodecs();
        if (serverReceiversInitialized) {
            return;
        }
        serverReceiversInitialized = true;

        for (Registration<?> registration : registrations) {
            if (registration.direction() != NetworkDir.PLAY_TO_CLIENT) {
                ServerPlayNetworking.registerGlobalReceiver(registration.payloadType(), (payload, context) ->
                        registration.handle(payload.message(), new Context(NetworkDir.PLAY_TO_SERVER, context.player())));
            }
        }
    }

    public void initClient() {
        initCodecs();
        if (clientInitialized) {
            return;
        }
        clientInitialized = true;

        for (Registration<?> registration : registrations) {
            if (registration.direction() != NetworkDir.PLAY_TO_SERVER) {
                ClientPlayNetworking.registerGlobalReceiver(registration.payloadType(), (payload, context) ->
                        registration.handle(payload.message(), new Context(NetworkDir.PLAY_TO_CLIENT, null)));
            }
        }
    }

    private void initCodecs() {
        if (codecsInitialized) {
            return;
        }
        codecsInitialized = true;

        for (Registration<?> registration : registrations) {
            if (registration.direction() != NetworkDir.PLAY_TO_CLIENT) {
                registerServerbound(registration);
            }
            if (registration.direction() != NetworkDir.PLAY_TO_SERVER) {
                registerClientbound(registration);
            }
        }
    }

    private static <T extends Message> void registerServerbound(Registration<T> registration) {
        PayloadTypeRegistry.serverboundPlay().register(registration.payloadType(), registration.codec());
    }

    private static <T extends Message> void registerClientbound(Registration<T> registration) {
        PayloadTypeRegistry.clientboundPlay().register(registration.payloadType(), registration.codec());
    }

    public void sendToServer(Message message) {
        @SuppressWarnings("unchecked")
        Registration<Message> registration = (Registration<Message>) registrationsByClass.get(message.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered message class for channel " + channelId + ": " + message.getClass().getName());
        }
        ClientPlayNetworking.send(registration.wrap(message));
    }

    public void sendToClientPlayer(ServerPlayer player, Message message) {
        @SuppressWarnings("unchecked")
        Registration<Message> registration = (Registration<Message>) registrationsByClass.get(message.getClass());
        if (registration == null) {
            throw new IllegalArgumentException("Unregistered message class for channel " + channelId + ": " + message.getClass().getName());
        }
        ServerPlayNetworking.send(player, registration.wrap(message));
    }

    public static final class Builder {
        private final String channelId;
        private final List<Registration<?>> registrations = new ArrayList<>();

        private Builder(String channelId) {
            this.channelId = channelId;
        }

        public Builder version(int version) {
            return this;
        }

        public <T extends Message> Builder register(NetworkDir dir, Class<T> type, Function<FriendlyByteBuf, T> decoder) {
            String path = camelToSnake(type.getSimpleName());
            registrations.add(new Registration<>(dir, type,
                    new CustomPacketPayload.Type<>(Identifier.fromNamespaceAndPath(channelId, path)),
                    decoder));
            return this;
        }

        public ChannelHandler build() {
            return new ChannelHandler(channelId, registrations);
        }

        private static String camelToSnake(String value) {
            return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2").toLowerCase(Locale.ROOT);
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

    private record PacketPayload<T extends Message>(Registration<T> registration, T message) implements CustomPacketPayload {
        @Override
        public Type<? extends CustomPacketPayload> type() {
            return registration.payloadType();
        }
    }

    private static final class Registration<T extends Message> {
        private final NetworkDir direction;
        private final Class<T> messageClass;
        private final CustomPacketPayload.Type<PacketPayload<T>> payloadType;
        private final Function<FriendlyByteBuf, T> decoder;
        private final StreamCodec<RegistryFriendlyByteBuf, PacketPayload<T>> codec;

        private Registration(NetworkDir direction, Class<T> messageClass,
                             CustomPacketPayload.Type<PacketPayload<T>> payloadType,
                             Function<FriendlyByteBuf, T> decoder) {
            this.direction = direction;
            this.messageClass = messageClass;
            this.payloadType = payloadType;
            this.decoder = decoder;
            this.codec = CustomPacketPayload.codec(this::encode, this::decode);
        }

        private void encode(PacketPayload<T> payload, RegistryFriendlyByteBuf buffer) {
            payload.message().writeToBuffer(buffer);
        }

        private PacketPayload<T> decode(RegistryFriendlyByteBuf buffer) {
            return new PacketPayload<>(this, decoder.apply(buffer));
        }

        private void handle(Message message, Context context) {
            message.handle(context);
        }

        private PacketPayload<T> wrap(Message message) {
            return new PacketPayload<>(this, messageClass.cast(message));
        }

        private NetworkDir direction() {
            return direction;
        }

        private Class<T> messageClass() {
            return messageClass;
        }

        private CustomPacketPayload.Type<PacketPayload<T>> payloadType() {
            return payloadType;
        }

        private StreamCodec<RegistryFriendlyByteBuf, PacketPayload<T>> codec() {
            return codec;
        }
    }
}
