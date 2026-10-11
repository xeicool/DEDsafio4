package com.eric.revill.client.network;

import com.eric.revill.client.render.CurrencyHudRenderer;
import com.eric.revill.client.ui.AtmScreen;
import com.eric.revill.client.ui.AtmWithdrawScreen;
import com.eric.revill.client.ui.EconomyMenuScreen;
import com.eric.revill.economy.EconomyNetworking;
import com.eric.revill.network.ModPackets;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.network.FriendlyByteBuf;

public final class EconomyClientNetworking {
    private EconomyClientNetworking() {}

    public static void register() {
        ClientPackets.on(EconomyNetworking.OPEN_ATM, (client, buf) -> client.gui.setScreen(new AtmScreen()));
        ClientPackets.on(EconomyNetworking.OPEN_COINS_MENU, (client, buf) -> client.gui.setScreen(new EconomyMenuScreen()));
        ClientPackets.on(EconomyNetworking.OPEN_WITHDRAW_UI, (client, buf) -> client.gui.setScreen(new AtmWithdrawScreen()));
        ClientPackets.on(EconomyNetworking.BALANCE_SYNC, (client, buf) -> {
            int blue = buf.readVarInt();
            int red = buf.readVarInt();
            int green = buf.readVarInt();
            CurrencyHudRenderer.setBalances(blue, red, green);
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> CurrencyHudRenderer.setBalances(0, 0, 0));
    }

    public static void sendWithdrawRequest(int blue, int red, int green) {
        FriendlyByteBuf buf = ModPackets.create();
        buf.writeVarInt(blue);
        buf.writeVarInt(red);
        buf.writeVarInt(green);
        ClientPackets.send(EconomyNetworking.WITHDRAW, buf);
    }
}
