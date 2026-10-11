package com.eric.revill.event;

import com.eric.revill.bunker.BunkerManager;
import net.minecraft.server.level.ServerPlayer;

public enum RevillTask {
    BUNKER("bunker", "Ir al búnker", "el búnker",
            "REVILL ha liberado un gas tóxico en el exterior. Reuníos en el búnker configurado antes de que termine la cuenta atrás y permaneced dentro hasta el final. CONSEJO: seguid el waypoint del búnker y preparad una ruta segura. ADVERTENCIA: quien termine fuera del búnker será eliminado."),
    REACH_Y_250("subir_250", "Subir hasta Y=250", "la capa Y=250",
            "REVILL ha alterado la gravedad y el peligro aumenta a ras de suelo. Subid hasta la capa Y=250 antes de que termine la cuenta atrás y permaneced a esa altura hasta el final. CONSEJO: llevad bloques y comida para construir una plataforma segura. ADVERTENCIA: quien no alcance Y=250 será eliminado."),
    REACH_Y_MINUS_250("bajar_-250", "Bajar hasta Y=-250", "la capa Y=-250",
            "REVILL ha abierto una grieta bajo el mundo. Descended hasta la capa Y=-250 antes de que termine la cuenta atrás y alcanzad esa altura para completar la tarea. CONSEJO: preparaos para descender con cuidado y vigilad vuestra posición. ADVERTENCIA: quien no alcance Y=-250 será eliminado.");

    private final String id;
    private final String label;
    private final String shortTitle;
    private final String description;

    RevillTask(String id, String label, String shortTitle, String description) {
        this.id = id;
        this.label = label;
        this.shortTitle = shortTitle;
        this.description = description;
    }

    public String id() { return id; }
    public String label() { return label; }
    public String shortTitle() { return shortTitle; }
    public String description() { return description; }

    public String buildAnnouncement(String additionalMessage) {
        if (additionalMessage == null || additionalMessage.isBlank()) return description;
        return additionalMessage.trim() + " " + description;
    }

    public boolean isComplete(ServerPlayer player) {
        return switch (this) {
            case BUNKER -> BunkerManager.isPlayerInside(player);
            case REACH_Y_250 -> player.getY() >= 250.0D;
            case REACH_Y_MINUS_250 -> player.getY() <= -250.0D;
        };
    }

    public String completionMessage(String playerName) {
        return switch (this) {
            case BUNKER -> playerName + " ha llegado al búnker.";
            case REACH_Y_250 -> playerName + " ha alcanzado la capa Y=250.";
            case REACH_Y_MINUS_250 -> playerName + " ha alcanzado la capa Y=-250.";
        };
    }

    public static RevillTask fromId(String id) {
        if (id != null) {
            for (RevillTask task : values()) {
                if (task.id.equals(id)) return task;
            }
        }
        return BUNKER;
    }
}
