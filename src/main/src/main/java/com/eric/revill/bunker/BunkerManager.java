package com.eric.revill.bunker;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class BunkerManager {
    private static final Bunker EMPTY_BUNKER = new Bunker();
    private static Bunker activeBunker = new Bunker();

    private BunkerManager() {}

    public static void setBunker(MinecraftServer server, BlockPos firstCorner, BlockPos secondCorner) {
        if (server == null) return;
        ServerLevel world = server.overworld();
        activeBunker = new Bunker();
        activeBunker.setDimension(world.dimension().identifier().toString());
        activeBunker.setFirstCorner(firstCorner);
        activeBunker.setSecondCorner(secondCorner);
    }

    public static void setBunker(Bunker bunker) {
        activeBunker = bunker == null ? new Bunker() : bunker;
    }

    public static Bunker getBunker() {
        return activeBunker == null ? EMPTY_BUNKER : activeBunker;
    }

    public static void clear() {
        activeBunker = new Bunker();
    }

    public static boolean isConfigured() {
        return getBunker().isConfigured();
    }

    public static boolean isPlayerInside(ServerPlayer player) {
        return player != null && getBunker().contains(player.blockPosition());
    }
}
