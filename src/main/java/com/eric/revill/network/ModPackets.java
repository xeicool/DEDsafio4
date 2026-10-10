package com.eric.revill.network;

import io.netty.buffer.Unpooled;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Registro y envío de ModPacket (lado común/servidor). El cliente usa ClientPackets. */
public final class ModPackets {
    @FunctionalInterface
    public interface ServerHandler {
        void handle(MinecraftServer server, ServerPlayer player, FriendlyByteBuf buf);
    }

    private static final Map<Identifier, ServerHandler> SERVER_HANDLERS = new HashMap<>();

    private ModPackets() {}

    /** Llamar una vez desde RevillMod.onInitialize(). */
    public static void registerTypes() {
        // NOTA 26.x: si el compilador dice que playS2C()/playC2S() no existen, usar
        // clientboundPlay()/serverboundPlay() (o al revés).
        PayloadTypeRegistry.clientboundPlay().register(ModPacket.TYPE, ModPacket.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(ModPacket.TYPE, ModPacket.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(ModPacket.TYPE, (payload, context) -> {
            ServerHandler handler = SERVER_HANDLERS.get(payload.channel());
            if (handler == null) {
                return;
            }
            byte[] data = payload.data();
            MinecraftServer server = context.server();
            ServerPlayer player = context.player();
            server.execute(() -> handler.handle(server, player, wrap(data)));
        });
    }

    public static void onServer(Identifier channel, ServerHandler handler) {
        SERVER_HANDLERS.put(channel, handler);
    }

    public static FriendlyByteBuf create() {
        return new FriendlyByteBuf(Unpooled.buffer());
    }

    public static FriendlyByteBuf wrap(byte[] data) {
        return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
    }

    public static byte[] bytes(FriendlyByteBuf buf) {
        byte[] out = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), out);
        return out;
    }

    public static void send(ServerPlayer player, Identifier channel, FriendlyByteBuf buf) {
        ServerPlayNetworking.send(player, new ModPacket(channel, bytes(buf)));
    }

    public static void send(ServerPlayer player, Identifier channel) {
        ServerPlayNetworking.send(player, new ModPacket(channel, new byte[0]));
    }
}
