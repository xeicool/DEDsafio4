package com.eric.revill.network;

import com.eric.revill.RevillMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * Paquete genérico del mod: lleva un "canal" (el id antiguo, p. ej. revill:event_state) y los bytes
 * que antes se escribían en el buffer. Así toda la lógica de red original se conserva casi igual.
 */
public record ModPacket(Identifier channel, byte[] data) implements CustomPacketPayload {
    public static final CustomPacketPayload.Type<ModPacket> TYPE = new CustomPacketPayload.Type<>(RevillMod.id("packet"));

    public static final StreamCodec<FriendlyByteBuf, ModPacket> CODEC = StreamCodec.of(
            (buf, packet) -> {
                buf.writeUtf(packet.channel().toString());
                buf.writeByteArray(packet.data());
            },
            buf -> new ModPacket(Identifier.parse(buf.readUtf()), buf.readByteArray()));

    @Override
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
