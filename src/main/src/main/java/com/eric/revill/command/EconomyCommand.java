package com.eric.revill.command;

import com.eric.revill.economy.CurrencyManager;
import com.eric.revill.economy.CurrencyType;
import com.eric.revill.economy.EconomyNetworking;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /dedsafio4 coins [add|remove|see] ... */
public final class EconomyCommand {
    private EconomyCommand() {}

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        registerRoot(dispatcher, "dedsafio4");
    }

    private static void registerRoot(CommandDispatcher<CommandSourceStack> dispatcher, String root) {
        // NOTA 26.x: si "hasPermission(LEVEL_GAMEMASTERS)" no compila, probar
        // .requires(source -> source.hasPermission(2)) o el nuevo sistema de permisos.
        dispatcher.register(Commands.literal(root)
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                .then(currencyBranch("coins")));
    }

    private static LiteralArgumentBuilder<CommandSourceStack> currencyBranch(String name) {
        return Commands.literal(name)
                .executes(context -> {
                    ServerPlayer player = context.getSource().getPlayer();
                    if (player == null) {
                        context.getSource().sendSuccess(() -> Component.literal("Este comando solo puede usarlo un jugador."), false);
                        return 1;
                    }
                    EconomyNetworking.openCoinsMenu(player);
                    return 1;
                })
                .then(Commands.literal("add")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("tipo", StringArgumentType.word())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 1000000))
                                                .executes(context -> change(context.getSource(),
                                                        EntityArgument.getPlayer(context, "jugador"),
                                                        StringArgumentType.getString(context, "tipo"),
                                                        IntegerArgumentType.getInteger(context, "cantidad"), true))))))
                .then(Commands.literal("remove")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .then(Commands.argument("tipo", StringArgumentType.word())
                                        .then(Commands.argument("cantidad", IntegerArgumentType.integer(1, 1000000))
                                                .executes(context -> change(context.getSource(),
                                                        EntityArgument.getPlayer(context, "jugador"),
                                                        StringArgumentType.getString(context, "tipo"),
                                                        IntegerArgumentType.getInteger(context, "cantidad"), false))))))
                .then(Commands.literal("see")
                        .then(Commands.argument("jugador", EntityArgument.player())
                                .executes(context -> show(context.getSource(), EntityArgument.getPlayer(context, "jugador")))));
    }

    private static int change(CommandSourceStack source, ServerPlayer player, String typeName, int amount, boolean add)
            throws CommandSyntaxException {
        CurrencyType type = CurrencyType.parse(typeName);
        if (type == null) {
            throw new SimpleCommandExceptionType(Component.literal("Tipo inválido. Usa: azul, roja o verde.")).create();
        }
        var server = source.getServer();
        if (add) {
            CurrencyManager.addCurrency(server, player.getUUID(), type, amount);
        } else if (!CurrencyManager.subtractCurrency(server, player.getUUID(), type, amount)) {
            throw new SimpleCommandExceptionType(
                    Component.literal("El jugador no tiene suficientes monedas de ese tipo.")).create();
        }
        String who = player.getName().getString();
        source.sendSuccess(() -> Component.literal((add ? "Añadidas " : "Retiradas ") + amount
                + " deditas " + type.key() + (add ? " a " : " de ") + who + "."), false);
        return 1;
    }

    private static int show(CommandSourceStack source, ServerPlayer player) {
        int[] balances = CurrencyManager.getCurrencies(source.getServer(), player.getUUID());
        source.sendSuccess(() -> Component.literal("Cuenta de " + player.getName().getString()
                + " | Azul: " + balances[0] + " | Roja: " + balances[1] + " | Verde: " + balances[2]), false);
        return 1;
    }
}
