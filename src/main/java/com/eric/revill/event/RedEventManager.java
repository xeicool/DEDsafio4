package com.eric.revill.event;

import com.eric.revill.RevillMod;
import com.eric.revill.bunker.Bunker;
import com.eric.revill.bunker.BunkerManager;
import com.eric.revill.config.RevillConfig;
import com.eric.revill.meteor.MeteorManager;
import com.eric.revill.network.RevillNetworking;
import com.eric.revill.waypoint.WaypointManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/** Lógica del Evento Rojo (Momento Revill). */
public class RedEventManager {
    public static final RedEventManager INSTANCE = new RedEventManager();
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final RevillConfig config = RevillMod.CONFIG;
    private final MeteorManager meteorManager = new MeteorManager();
    private final Set<UUID> participatingPlayers = new HashSet<>();
    private final Set<UUID> completedTaskPlayers = new HashSet<>();

    private boolean eventActive;
    private int remainingSeconds = 600;
    private boolean skyActive;
    private boolean announcementActive;
    private boolean eventFinished;
    private int tickCounter;
    private RevillTask activeTask = RevillTask.BUNKER;
    private RevillTask pendingTask = RevillTask.BUNKER;

    public RevillConfig getConfig() {
        return config;
    }

    public void startRedEvent(MinecraftServer server) {
        startRedEvent(server, RevillTask.BUNKER.id(), null);
    }

    public void startRedEvent(MinecraftServer server, String rouletteMessage) {
        startRedEvent(server, RevillTask.BUNKER.id(), rouletteMessage);
    }

    /**
     * ETAPA 3: la ruleta (Dedsafio4Manager.startRevillRoulette) aún no está portada,
     * así que el evento arranca directamente.
     */
    public void startRedEvent(MinecraftServer server, String taskId, String additionalMessage) {
        if (server == null || eventActive) return;
        pendingTask = RevillTask.fromId(taskId);
        if (additionalMessage != null && !additionalMessage.isBlank()) {
            sendGlobalMessage(server, additionalMessage);
        }
        beginRedEvent(server);
    }

    public RevillTask getPendingTask() {
        return pendingTask;
    }

    public void beginRedEvent(MinecraftServer server) {
        if (server == null || eventActive) return;
        eventActive = true;
        eventFinished = false;
        tickCounter = 0;
        remainingSeconds = config.durationSeconds;
        skyActive = true;
        announcementActive = config.announcementEnabled;
        activeTask = pendingTask;
        participatingPlayers.clear();
        completedTaskPlayers.clear();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            participatingPlayers.add(player.getUUID());
        }

        if (activeTask == RevillTask.BUNKER && BunkerManager.isConfigured()) {
            BlockPos first = BunkerManager.getBunker().getFirstCorner();
            BlockPos second = BunkerManager.getBunker().getSecondCorner();
            BlockPos center = new BlockPos(
                    (first.getX() + second.getX()) / 2,
                    (first.getY() + second.getY()) / 2,
                    (first.getZ() + second.getZ()) / 2);
            WaypointManager.setBunkerWaypoint(server, center, BunkerManager.getBunker().getDimension());
        }

        sendGlobalMessage(server, activeTask.description());
        showAnnouncement(server);
        RevillNetworking.sendEventState(server, true, remainingSeconds, true, true);
        saveState(server);
    }

    public void stopRedEvent(MinecraftServer server, boolean cancelled) {
        if (!eventActive && !eventFinished) return;
        eventActive = false;
        eventFinished = true;
        skyActive = false;
        announcementActive = false;
        completedTaskPlayers.clear();
        meteorManager.clear();
        if (server != null) {
            WaypointManager.removeWaypoint(server, "bunker");
            RevillNetworking.sendEventState(server, false, 0, false, false);
            if (cancelled) sendGlobalMessage(server, "El Evento Rojo ha sido cancelado.");
        }
        saveState(server);
    }

    public void tick(MinecraftServer server) {
        if (server == null || !eventActive) return;
        tickCounter++;
        if (tickCounter % 20 == 0) {
            remainingSeconds = Math.max(0, remainingSeconds - 1);
            sendTimeWarnings(server, remainingSeconds);
            RevillNetworking.sendEventState(server, true, remainingSeconds, skyActive, announcementActive);
            if (remainingSeconds <= 0) {
                performFinalCheck(server);
                return;
            }
        }
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            if (!player.isSpectator() && activeTask.isComplete(player) && completedTaskPlayers.add(player.getUUID())) {
                sendGlobalMessage(server, activeTask.completionMessage(player.getName().getString()));
            }
        }
        if (server.overworld() != null) {
            meteorManager.update(server.overworld(), remainingSeconds, config);
        }
    }

    public void displayStatus(CommandSourceStack source) {
        source.sendSuccess(() -> Component.literal(
                "Evento activo: " + (eventActive ? "sí" : "no")
                        + "\nTiempo restante: " + formatSeconds(remainingSeconds)
                        + "\nMeteoritos activos: " + meteorManager.getActiveCount()
                        + "\nBúnker configurado: " + (BunkerManager.isConfigured() ? "sí" : "no")
                        + "\nCielo rojo: " + (skyActive ? "activo" : "inactivo")), false);
    }

    // ---------------- persistencia (JSON en la carpeta del mundo) ----------------

    private static final class Saved {
        boolean active;
        int remainingSeconds;
        boolean skyActive;
        boolean announcementActive;
        String taskId = RevillTask.BUNKER.id();
        String bunkerDimension = "";
        int[] bunkerPos1;
        int[] bunkerPos2;
    }

    private static Path stateFile(MinecraftServer server) {
        return server.getWorldPath(LevelResource.ROOT).resolve("revill_event.json");
    }

    public void loadState(MinecraftServer server) {
        if (server == null) return;
        Path file = stateFile(server);
        if (!Files.exists(file)) return;
        try (Reader reader = Files.newBufferedReader(file)) {
            Saved s = GSON.fromJson(reader, Saved.class);
            if (s == null) return;
            eventActive = s.active;
            remainingSeconds = s.remainingSeconds;
            skyActive = s.skyActive;
            announcementActive = s.announcementActive;
            activeTask = RevillTask.fromId(s.taskId);
            pendingTask = activeTask;

            Bunker bunker = new Bunker();
            bunker.setDimension(s.bunkerDimension == null ? "" : s.bunkerDimension);
            if (s.bunkerPos1 != null && s.bunkerPos1.length == 3) {
                bunker.setFirstCorner(new BlockPos(s.bunkerPos1[0], s.bunkerPos1[1], s.bunkerPos1[2]));
            }
            if (s.bunkerPos2 != null && s.bunkerPos2.length == 3) {
                bunker.setSecondCorner(new BlockPos(s.bunkerPos2[0], s.bunkerPos2[1], s.bunkerPos2[2]));
            }
            BunkerManager.setBunker(bunker);

            if (eventActive) {
                RevillNetworking.sendEventState(server, true, remainingSeconds, skyActive, announcementActive);
            }
        } catch (IOException | RuntimeException e) {
            RevillMod.LOGGER.error("No se pudo leer {}", file, e);
        }
    }

    public void saveState(MinecraftServer server) {
        if (server == null) return;
        Saved s = new Saved();
        s.active = eventActive;
        s.remainingSeconds = remainingSeconds;
        s.skyActive = skyActive;
        s.announcementActive = announcementActive;
        s.taskId = activeTask.id();
        Bunker bunker = BunkerManager.getBunker();
        s.bunkerDimension = bunker.getDimension();
        if (bunker.getFirstCorner() != null) {
            BlockPos p = bunker.getFirstCorner();
            s.bunkerPos1 = new int[] {p.getX(), p.getY(), p.getZ()};
        }
        if (bunker.getSecondCorner() != null) {
            BlockPos p = bunker.getSecondCorner();
            s.bunkerPos2 = new int[] {p.getX(), p.getY(), p.getZ()};
        }
        try (Writer writer = Files.newBufferedWriter(stateFile(server))) {
            GSON.toJson(s, writer);
        } catch (IOException e) {
            RevillMod.LOGGER.error("No se pudo guardar el estado del evento", e);
        }
    }

    public void shutdown(MinecraftServer server) {
        saveState(server);
    }

    // ---------------- internos ----------------

    private void showAnnouncement(MinecraftServer server) {
        if (server == null || !config.announcementEnabled) return;
        announcementActive = true;
        RevillNetworking.sendAnnouncement(server, config.announcementDurationSeconds, config.announcementFlashSpeed);
    }

    private void sendTimeWarnings(MinecraftServer server, int seconds) {
        String text = switch (seconds) {
            case 600 -> "Quedan 10 minutos del Momento Revill.";
            case 300 -> "Quedan 5 minutos del Momento Revill.";
            case 120 -> "Quedan 2 minutos del Momento Revill.";
            case 60 -> "Queda 1 minuto del Momento Revill.";
            case 30 -> "Quedan 30 segundos del Momento Revill.";
            case 10 -> "Quedan 10 segundos del Momento Revill.";
            default -> "";
        };
        if (!text.isEmpty()) sendGlobalMessage(server, text);
    }

    private void performFinalCheck(MinecraftServer server) {
        eventActive = false;
        eventFinished = true;
        skyActive = false;
        announcementActive = false;
        meteorManager.clear();
        if (server != null) {
            for (ServerPlayer player : server.getPlayerList().getPlayers()) {
                if (!player.isSpectator() && !activeTask.isComplete(player) && !completedTaskPlayers.contains(player.getUUID())) {
                    player.kill(player.level());
                }
            }
            sendGlobalMessage(server, "Final del Momento Revill. Objetivo: " + activeTask.shortTitle()
                    + ". Quienes no lo completaron han sido eliminados.");
            WaypointManager.removeWaypoint(server, "bunker");
            RevillNetworking.sendEventState(server, false, 0, false, false);
        }
        saveState(server);
    }

    private void sendGlobalMessage(MinecraftServer server, String message) {
        if (server == null) return;
        Component component = Component.literal(message);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            player.sendSystemMessage(component);
        }
    }

    private String formatSeconds(int seconds) {
        return String.format("%02d:%02d", seconds / 60, seconds % 60);
    }
}
