package com.eric.revill;

import com.eric.revill.client.network.ClientPackets;
import com.eric.revill.client.network.EconomyClientNetworking;
import com.eric.revill.client.ui.AtmDepositScreen;
import com.eric.revill.economy.AtmScreenHandler;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;

public class RevillClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        ClientPackets.registerReceiver();
        EconomyClientNetworking.register();
        MenuScreens.register(AtmScreenHandler.TYPE, AtmDepositScreen::new);
        // TODO etapas siguientes: renderers (cajero), HUD, cielo rojo, fusión, zenith, audio, correo.
    }
}
