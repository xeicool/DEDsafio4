package com.eric.revill.client.network;

import com.eric.revill.network.ModPacket;
import com.eric.revill.network.ModPackets;
import java.util.HashMap;
import java.util.Map;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;

/** Recepción/envío de ModPacket en el cliente. */
public final class ClientPackets {
    @FunctionalInterface
    public interface ClientHandler {
        void handle(Minecraft client, FriendlyByteBuf buf);
    }

    private static final Map<Identifier, ClientHandler> HANDLERS = new HashMap<>();

    private ClientPackets() {}

    public static void registerReceiver() {
        ClientPlayNetworking.registerGlobalReceiver(ModPacket.TYPE, (payload, context) -> {
            ClientHandler handler = HANDLERS.get(payload.channel());
            if (handler == null) {
                return;
            }
            byte[] data = payload.data();
            Minecraft client = context.client();
            client.execute(() -> handler.handle(client, ModPackets.wrap(data)));
        });
    }

    public static void on(Identifier channel, ClientHandler handler) {
        HANDLERS.put(channel, handler);
    }

    public static void send(Identifier channel, FriendlyByteBuf buf) {
        ClientPlayNetworking.send(new ModPacket(channel, ModPackets.bytes(buf)));
    }

    public static void send(Identifier channel) {
        ClientPlayNetworking.send(new ModPacket(channel, new byte[0]));
    }
}
