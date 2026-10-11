package com.eric.revill.network;

import com.eric.revill.RevillMod;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class RevillNetworking {
    public static final Identifier EVENT_STATE = RevillMod.id("event_state", "revill");
    public static final Identifier ANNOUNCEMENT = RevillMod.id("announcement", "revill");
    public static final Identifier STOP_EVENT = RevillMod.id("stop_event", "revill");
    public static final Identifier ROULETTE = RevillMod.id("roulette", "revill");
    public static final Identifier ROULETTE_ANNOUNCEMENT = RevillMod.id("roulette_announcement", "revill");
    public static final Identifier NUTRIA_ANIMATION = RevillMod.id("nutria_animation", "revill");
    public static final Identifier NUTRIA_ANIMATION_COMPLETE = RevillMod.id("nutria_animation_complete", "revill");
    public static final Identifier REVILL_INTRO_DEATH = RevillMod.id("revill_intro_death", "revill");
    public static final Identifier REVILL_INTRO_COMPLETE = RevillMod.id("revill_intro_complete", "revill");

    private RevillNetworking() {}

    public static void sendEventState(MinecraftServer server, boolean active, int remainingSeconds, boolean skyActive, boolean announcementActive) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = ModPackets.create();
            buf.writeBoolean(active);
            buf.writeInt(remainingSeconds);
            buf.writeBoolean(skyActive);
            buf.writeBoolean(announcementActive);
            ModPackets.send(player, EVENT_STATE, buf);
        }
    }

    public static void sendAnnouncement(MinecraftServer server, int durationSeconds, double flashSpeed) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = ModPackets.create();
            buf.writeInt(durationSeconds);
            buf.writeDouble(flashSpeed);
            ModPackets.send(player, ANNOUNCEMENT, buf);
        }
    }

    public static void sendStopEvent(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = ModPackets.create();
            buf.writeBoolean(false);
            ModPackets.send(player, STOP_EVENT, buf);
        }
    }

    public static void sendRoulette(MinecraftServer server, String color, String result) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = ModPackets.create();
            buf.writeUtf(color);
            buf.writeUtf(result);
            ModPackets.send(player, ROULETTE, buf);
        }
    }

    public static void sendRouletteAnnouncement(MinecraftServer server, String color, String message) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            FriendlyByteBuf buf = ModPackets.create();
            buf.writeUtf(color == null ? "naranja" : color, 32);
            buf.writeUtf(message, 32767);
            ModPackets.send(player, ROULETTE_ANNOUNCEMENT, buf);
        }
    }

    public static void sendRouletteAnnouncement(MinecraftServer server, String message) {
        sendRouletteAnnouncement(server, "naranja", message);
    }

    public static void sendNutriaAnimation(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModPackets.send(player, NUTRIA_ANIMATION);
        }
    }

    public static void sendRevillIntroDeath(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModPackets.send(player, REVILL_INTRO_DEATH);
        }
    }
}
