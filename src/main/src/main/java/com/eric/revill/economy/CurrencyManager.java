package com.eric.revill.economy;

import com.eric.revill.RevillMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

/**
 * Cuentas de deditas por jugador. Se guardan en un JSON dentro de la carpeta del mundo
 * (dedsafio4_economy.json) en vez de SavedData, para no depender del API de guardado de 26.x.
 */
public final class CurrencyManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Type MAP_TYPE = new TypeToken<Map<String, Map<String, Integer>>>() {}.getType();

    private static Map<UUID, int[]> balances;
    private static Path file;

    private CurrencyManager() {}

    public static int getCurrency(MinecraftServer server, UUID playerId, CurrencyType type) {
        int[] values = load(server).get(playerId);
        return values == null ? 0 : values[type.ordinal()];
    }

    public static int[] getCurrencies(MinecraftServer server, UUID playerId) {
        int[] values = load(server).get(playerId);
        return values == null ? new int[CurrencyType.values().length] : values.clone();
    }

    public static void addCurrency(MinecraftServer server, UUID playerId, CurrencyType type, int amount) {
        if (amount <= 0) return;
        int[] values = values(server, playerId);
        values[type.ordinal()] = Math.addExact(values[type.ordinal()], amount);
        saveAndSync(server, playerId);
    }

    public static boolean subtractCurrency(MinecraftServer server, UUID playerId, CurrencyType type, int amount) {
        if (amount <= 0) return false;
        int[] values = values(server, playerId);
        if (values[type.ordinal()] < amount) return false;
        values[type.ordinal()] -= amount;
        saveAndSync(server, playerId);
        return true;
    }

    public static boolean subtractCurrencies(MinecraftServer server, UUID playerId, int[] amounts) {
        if (amounts.length != CurrencyType.values().length) return false;
        int[] values = values(server, playerId);
        for (CurrencyType type : CurrencyType.values()) {
            int amount = amounts[type.ordinal()];
            if (amount < 0 || values[type.ordinal()] < amount) return false;
        }
        for (CurrencyType type : CurrencyType.values()) {
            values[type.ordinal()] -= amounts[type.ordinal()];
        }
        saveAndSync(server, playerId);
        return true;
    }

    public static void setCurrency(MinecraftServer server, UUID playerId, CurrencyType type, int amount) {
        values(server, playerId)[type.ordinal()] = Math.max(0, amount);
        saveAndSync(server, playerId);
    }

    public static void sync(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        EconomyNetworking.syncBalances(player, getCurrencies(server, player.getUUID()));
    }

    /** Llamar al parar el servidor para forzar a recargar desde disco en el próximo mundo. */
    public static void reset() {
        balances = null;
        file = null;
    }

    private static void saveAndSync(MinecraftServer server, UUID playerId) {
        save();
        ServerPlayer player = server.getPlayerList().getPlayer(playerId);
        if (player != null) sync(player);
    }

    private static int[] values(MinecraftServer server, UUID playerId) {
        return load(server).computeIfAbsent(playerId, ignored -> new int[CurrencyType.values().length]);
    }

    private static Map<UUID, int[]> load(MinecraftServer server) {
        Path expected = server.getWorldPath(LevelResource.ROOT).resolve("dedsafio4_economy.json");
        if (balances != null && expected.equals(file)) return balances;

        file = expected;
        balances = new HashMap<>();
        if (Files.exists(file)) {
            try (Reader reader = Files.newBufferedReader(file)) {
                Map<String, Map<String, Integer>> raw = GSON.fromJson(reader, MAP_TYPE);
                if (raw != null) {
                    raw.forEach((uuidKey, perType) -> {
                        try {
                            int[] values = new int[CurrencyType.values().length];
                            for (CurrencyType type : CurrencyType.values()) {
                                Integer v = perType.get(type.key());
                                values[type.ordinal()] = v == null ? 0 : Math.max(0, v);
                            }
                            balances.put(UUID.fromString(uuidKey), values);
                        } catch (IllegalArgumentException ignored) {
                        }
                    });
                }
            } catch (IOException | RuntimeException e) {
                RevillMod.LOGGER.error("No se pudo leer {}", file, e);
            }
        }
        return balances;
    }

    private static void save() {
        if (balances == null || file == null) return;
        Map<String, Map<String, Integer>> raw = new HashMap<>();
        balances.forEach((playerId, values) -> {
            Map<String, Integer> perType = new HashMap<>();
            for (CurrencyType type : CurrencyType.values()) {
                perType.put(type.key(), values[type.ordinal()]);
            }
            raw.put(playerId.toString(), perType);
        });
        try (Writer writer = Files.newBufferedWriter(file)) {
            GSON.toJson(raw, writer);
        } catch (IOException e) {
            RevillMod.LOGGER.error("No se pudo guardar {}", file, e);
        }
    }
}
