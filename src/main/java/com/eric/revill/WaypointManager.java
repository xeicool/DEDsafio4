package com.eric.revill.waypoint;

import com.eric.revill.RevillMod;
import com.eric.revill.network.ModPackets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

public final class WaypointManager {
    public static final Identifier SYNC_WAYPOINTS = RevillMod.id("sync_waypoints", "revill");
    public static final Identifier ADD_WAYPOINT = RevillMod.id("add_waypoint", "revill");
    public static final Identifier REMOVE_WAYPOINT = RevillMod.id("remove_waypoint", "revill");
    public static final Identifier EDIT_WAYPOINT = RevillMod.id("edit_waypoint", "revill");

    private static final Map<String, Waypoint> WAYPOINTS = new LinkedHashMap<>();

    private WaypointManager() {}

    public static void addWaypoint(MinecraftServer server, Waypoint waypoint) {
        if (waypoint == null) return;
        WAYPOINTS.put(waypoint.getId(), waypoint);
        syncToAll(server);
    }

    public static void removeWaypoint(MinecraftServer server, String idOrName) {
        if (idOrName == null || idOrName.isBlank()) return;
        boolean removed = WAYPOINTS.remove(idOrName) != null;
        if (!removed) {
            String targetId = null;
            for (Waypoint wp : WAYPOINTS.values()) {
                if (wp.getName().equalsIgnoreCase(idOrName)) {
                    targetId = wp.getId();
                    break;
                }
            }
            if (targetId != null) {
                WAYPOINTS.remove(targetId);
                removed = true;
            }
        }
        if (removed) syncToAll(server);
    }

    public static List<Waypoint> getAllWaypoints() {
        return Collections.unmodifiableList(new ArrayList<>(WAYPOINTS.values()));
    }

    public static void setBunkerWaypoint(MinecraftServer server, BlockPos pos, String dimension) {
        addWaypoint(server, new Waypoint("bunker", "BÚNKER", pos, "bunker", -56798, dimension));
    }

    public static void syncToPlayer(ServerPlayer player) {
        if (player == null) return;
        ModPackets.send(player, SYNC_WAYPOINTS, encodeWaypoints());
    }

    public static void syncToAll(MinecraftServer server) {
        if (server == null) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            ModPackets.send(player, SYNC_WAYPOINTS, encodeWaypoints());
        }
    }

    private static FriendlyByteBuf encodeWaypoints() {
        FriendlyByteBuf buf = ModPackets.create();
        buf.writeInt(WAYPOINTS.size());
        for (Waypoint wp : WAYPOINTS.values()) {
            buf.writeUtf(wp.getId());
            buf.writeUtf(wp.getName());
            buf.writeBlockPos(wp.getPos());
            buf.writeUtf(wp.getIcon());
            buf.writeInt(wp.getColor());
            buf.writeUtf(wp.getDimension());
        }
        return buf;
    }

    public static List<Waypoint> decodeWaypoints(FriendlyByteBuf buf) {
        List<Waypoint> list = new ArrayList<>();
        int count = buf.readInt();
        for (int i = 0; i < count; i++) {
            String id = buf.readUtf();
            String name = buf.readUtf();
            BlockPos pos = buf.readBlockPos();
            String icon = buf.readUtf();
            int color = buf.readInt();
            String dimension = buf.readUtf();
            list.add(new Waypoint(id, name, pos, icon, color, dimension));
        }
        return list;
    }
}
