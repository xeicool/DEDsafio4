package com.eric.revill.economy;

import com.eric.revill.RevillMod;
import com.eric.revill.network.ModPackets;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public final class EconomyNetworking {
    public static final Identifier OPEN_ATM = RevillMod.id("atm_open");
    public static final Identifier OPEN_WITHDRAW = RevillMod.id("atm_withdraw_open");
    public static final Identifier OPEN_WITHDRAW_UI = RevillMod.id("atm_withdraw_ui");
    public static final Identifier OPEN_DEPOSIT = RevillMod.id("atm_deposit_open");
    public static final Identifier OPEN_COINS_MENU = RevillMod.id("coins_menu_open");
    public static final Identifier COINS_ACTION = RevillMod.id("coins_action");
    public static final Identifier WITHDRAW = RevillMod.id("atm_withdraw");
    public static final Identifier BALANCE_SYNC = RevillMod.id("currency_sync");

    private EconomyNetworking() {}

    public static void registerServer() {
        // El cliente pide abrir el menú de depósito (contenedor con 9 casillas).
        ModPackets.onServer(OPEN_DEPOSIT, (server, player, buf) ->
                player.openMenu(new SimpleMenuProvider(
                        (syncId, inventory, p) -> new AtmScreenHandler(syncId, inventory, new net.minecraft.world.SimpleContainer(9)),
                        Component.literal("Cajero Automático"))));

        // El cliente pide la pantalla de retiro: respondemos con la orden de abrirla y el saldo actual.
        ModPackets.onServer(OPEN_WITHDRAW, (server, player, buf) -> {
            ModPackets.send(player, OPEN_WITHDRAW_UI);
            CurrencyManager.sync(player);
        });

        ModPackets.onServer(COINS_ACTION, (server, player, buf) -> {
            int action = buf.readVarInt();
            int currencyIndex = buf.readVarInt();
            int amount = buf.readVarInt();
            handleCoinsAction(player, action, currencyIndex, amount);
        });

        ModPackets.onServer(WITHDRAW, (server, player, buf) -> {
            int blue = buf.readVarInt();
            int red = buf.readVarInt();
            int green = buf.readVarInt();
            withdraw(player, blue, red, green);
        });
    }

    public static void openAtm(ServerPlayer player) {
        ModPackets.send(player, OPEN_ATM);
    }

    public static void openCoinsMenu(ServerPlayer player) {
        ModPackets.send(player, OPEN_COINS_MENU);
    }

    public static void syncBalances(ServerPlayer player, int[] balances) {
        FriendlyByteBuf buf = ModPackets.create();
        for (int balance : balances) buf.writeVarInt(balance);
        ModPackets.send(player, BALANCE_SYNC, buf);
    }

    private static void say(ServerPlayer player, String text, boolean overlay) {
        if (overlay) {
            player.sendSystemMessage(Component.literal(text), true);
        } else {
            player.sendSystemMessage(Component.literal(text));
        }
    }

    private static void handleCoinsAction(ServerPlayer player, int action, int currencyIndex, int amount) {
        CurrencyType[] types = CurrencyType.values();
        if (currencyIndex < 0 || currencyIndex >= types.length) {
            currencyIndex = 0;
        }
        CurrencyType type = types[currencyIndex];
        var server = player.level().getServer();
        switch (action) {
            case 0 -> {
                int add = Math.max(1, amount);
                CurrencyManager.addCurrency(server, player.getUUID(), type, add);
                say(player, "Añadidas " + add + " deditas " + type.key() + " a tu cuenta.", false);
            }
            case 1 -> {
                int removal = Math.max(1, amount);
                if (!CurrencyManager.subtractCurrency(server, player.getUUID(), type, removal)) {
                    say(player, "No tienes suficientes deditas " + type.key() + " para quitar.", false);
                    return;
                }
                say(player, "Quitadas " + removal + " deditas " + type.key() + " de tu cuenta.", false);
            }
            case 2 -> {
                int[] b = CurrencyManager.getCurrencies(server, player.getUUID());
                say(player, "Tu saldo: Azul " + b[0] + " | Roja " + b[1] + " | Verde " + b[2], false);
            }
            default -> say(player, "Acción no válida.", false);
        }
        CurrencyManager.sync(player);
    }

    private static void withdraw(ServerPlayer player, int blue, int red, int green) {
        var server = player.level().getServer();
        int[] requested = {blue, red, green};
        CurrencyType[] types = CurrencyType.values();
        for (int index = 0; index < requested.length; index++) {
            int amount = requested[index];
            if (amount < 0 || amount > CurrencyManager.getCurrency(server, player.getUUID(), types[index])) {
                say(player, "No tienes suficientes deditas en la cuenta.", true);
                CurrencyManager.sync(player);
                return;
            }
        }
        if (!canFitWithdrawal(player, requested, types)) {
            say(player, "No tienes espacio suficiente en el inventario.", true);
            return;
        }
        if (!CurrencyManager.subtractCurrencies(server, player.getUUID(), requested)) {
            say(player, "No tienes suficientes deditas en la cuenta.", true);
            CurrencyManager.sync(player);
            return;
        }
        for (int index = 0; index < requested.length; index++) {
            int amount = requested[index];
            Item item = types[index].item();
            while (amount > 0) {
                int stackSize = Math.min(amount, item.getDefaultMaxStackSize());
                player.getInventory().add(new ItemStack(item, stackSize));
                amount -= stackSize;
            }
        }
        CurrencyManager.sync(player);
    }

    private static boolean canFitWithdrawal(ServerPlayer player, int[] amounts, CurrencyType[] types) {
        Inventory inventory = player.getInventory();
        int emptySlots = 0;
        for (int slot = 0; slot < 36; slot++) {
            if (inventory.getItem(slot).isEmpty()) emptySlots++;
        }
        int requiredEmptySlots = 0;
        for (int index = 0; index < amounts.length; index++) {
            int remaining = amounts[index];
            Item item = types[index].item();
            for (int i = 0; i < 36 && remaining > 0; i++) {
                ItemStack stack = inventory.getItem(i);
                if (stack.is(item)) {
                    remaining -= stack.getMaxStackSize() - stack.getCount();
                }
            }
            remaining = Math.max(0, remaining);
            int max = item.getDefaultMaxStackSize();
            requiredEmptySlots += (remaining + max - 1) / max;
        }
        return requiredEmptySlots <= emptySlots;
    }
}
