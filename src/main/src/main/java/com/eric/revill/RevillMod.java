package com.eric.revill;

import com.eric.revill.command.EconomyCommand;
import com.eric.revill.command.RevillCommand;
import com.eric.revill.event.RedEventManager;
import com.eric.revill.waypoint.WaypointManager;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import com.eric.revill.config.RevillConfig;
import com.eric.revill.economy.AtmScreenHandler;
import com.eric.revill.economy.CurrencyManager;
import com.eric.revill.economy.EconomyNetworking;
import com.eric.revill.entity.ModEntities;
import com.eric.revill.item.ModItems;
import com.eric.revill.network.ModPackets;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RevillMod implements ModInitializer {
    public static final String MOD_ID = "dedsafio-4-mechanics";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final RevillConfig CONFIG = RevillConfig.load();

    public static boolean FUSION_ACTIVE = false;

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(MOD_ID, path);
    }

    /** Para los recursos que viven en el namespace "revill" (sonidos, texturas del evento, canales). */
    public static Identifier id(String path, String namespace) {
        return Identifier.fromNamespaceAndPath(namespace, path);
    }

    @Override
    public void onInitialize() {
        LOGGER.info("Iniciando DEDSAFIO 4 Mechanics para Minecraft 26.3");
        ModPackets.registerTypes();
        ModEntities.initialize();
        ModItems.initialize();
        AtmScreenHandler.initialize();
        EconomyNetworking.registerServer();

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            EconomyCommand.register(dispatcher);
            RevillCommand.register(dispatcher);
        });

        ServerLifecycleEvents.SERVER_STARTED.register(RedEventManager.INSTANCE::loadState);
        ServerLifecycleEvents.SERVER_STOPPING.register(RedEventManager.INSTANCE::shutdown);
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> CurrencyManager.reset());
        ServerTickEvents.END_SERVER_TICK.register(RedEventManager.INSTANCE::tick);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            WaypointManager.syncToPlayer(handler.getPlayer());
            CurrencyManager.sync(handler.getPlayer());
        });
        // TODO siguiente tanda: correo, Dedsafio4Command/ruletas, mixins y todo el cliente de eventos.
    }
}
