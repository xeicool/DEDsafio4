package com.eric.revill.command;

import com.eric.revill.RevillMod;
import com.eric.revill.bunker.Bunker;
import com.eric.revill.bunker.BunkerManager;
import com.eric.revill.config.RevillConfig;
import com.eric.revill.event.RedEventManager;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;

/** /revill start|stop|status|fusion|bunker|meteorites|config */
public final class RevillCommand {
    private RevillCommand() {}

    private static void msg(CommandSourceStack source, String text) {
        source.sendSuccess(() -> Component.literal(text), false);
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("revill")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(Commands.literal("start").executes(context -> {
                    RedEventManager.INSTANCE.startRedEvent(context.getSource().getServer());
                    return 1;
                }))
                .then(Commands.literal("stop").executes(context -> {
                    RedEventManager.INSTANCE.stopRedEvent(context.getSource().getServer(), true);
                    msg(context.getSource(), "Evento Rojo detenido.");
                    return 1;
                }))
                .then(Commands.literal("status").executes(context -> {
                    RedEventManager.INSTANCE.displayStatus(context.getSource());
                    return 1;
                }))
                .then(Commands.literal("fusion")
                        .executes(context -> {
                            setFusion(context.getSource(), !RevillMod.FUSION_ACTIVE);
                            return 1;
                        })
                        .then(Commands.literal("entrar").executes(context -> {
                            setFusion(context.getSource(), true);
                            return 1;
                        }))
                        .then(Commands.literal("salir").executes(context -> {
                            setFusion(context.getSource(), false);
                            return 1;
                        })))
                .then(Commands.literal("bunker")
                        .then(Commands.literal("set")
                                .then(Commands.argument("x1", IntegerArgumentType.integer())
                                .then(Commands.argument("y1", IntegerArgumentType.integer())
                                .then(Commands.argument("z1", IntegerArgumentType.integer())
                                .then(Commands.argument("x2", IntegerArgumentType.integer())
                                .then(Commands.argument("y2", IntegerArgumentType.integer())
                                .then(Commands.argument("z2", IntegerArgumentType.integer())
                                        .executes(context -> {
                                            BlockPos pos1 = new BlockPos(
                                                    IntegerArgumentType.getInteger(context, "x1"),
                                                    IntegerArgumentType.getInteger(context, "y1"),
                                                    IntegerArgumentType.getInteger(context, "z1"));
                                            BlockPos pos2 = new BlockPos(
                                                    IntegerArgumentType.getInteger(context, "x2"),
                                                    IntegerArgumentType.getInteger(context, "y2"),
                                                    IntegerArgumentType.getInteger(context, "z2"));
                                            BunkerManager.setBunker(context.getSource().getServer(), pos1, pos2);
                                            RedEventManager.INSTANCE.saveState(context.getSource().getServer());
                                            msg(context.getSource(), "Búnker configurado entre " + pos1.toShortString()
                                                    + " y " + pos2.toShortString() + ".");
                                            return 1;
                                        }))))))))
                        .then(Commands.literal("show").executes(context -> {
                            if (BunkerManager.isConfigured()) {
                                Bunker b = BunkerManager.getBunker();
                                msg(context.getSource(), "Búnker: " + b.getFirstCorner().toShortString()
                                        + " -> " + b.getSecondCorner().toShortString() + " (" + b.getDimension() + ")");
                            } else {
                                msg(context.getSource(), "No hay búnker configurado.");
                            }
                            return 1;
                        }))
                        .then(Commands.literal("clear").executes(context -> {
                            BunkerManager.clear();
                            RedEventManager.INSTANCE.saveState(context.getSource().getServer());
                            msg(context.getSource(), "Búnker borrado.");
                            return 1;
                        })))
                .then(Commands.literal("meteorites")
                        .then(Commands.literal("on").executes(context -> {
                            RedEventManager.INSTANCE.getConfig().meteoritesEnabled = true;
                            msg(context.getSource(), "Meteoritos activados.");
                            return 1;
                        }))
                        .then(Commands.literal("off").executes(context -> {
                            RedEventManager.INSTANCE.getConfig().meteoritesEnabled = false;
                            msg(context.getSource(), "Meteoritos desactivados.");
                            return 1;
                        })))
                .then(Commands.literal("config").executes(context -> {
                    RevillConfig c = RedEventManager.INSTANCE.getConfig();
                    msg(context.getSource(), "Duración: " + c.durationSeconds + "s | Meteoritos: "
                            + (c.meteoritesEnabled ? "sí" : "no") + " (máx " + c.meteoritesMaxActive + ")"
                            + " | Cielo rojo: " + (c.skyEnabled ? "sí" : "no"));
                    return 1;
                })));
    }

    /** ETAPA 3: la fusión visual completa llega con el cliente; por ahora solo cambia el estado. */
    public static void setFusion(CommandSourceStack source, boolean active) {
        RevillMod.FUSION_ACTIVE = active;
        msg(source, active ? "Fusión activada." : "Fusión desactivada.");
    }
}
