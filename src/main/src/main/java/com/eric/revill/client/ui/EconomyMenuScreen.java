package com.eric.revill.client.ui;

import com.eric.revill.client.network.ClientPackets;
import com.eric.revill.economy.EconomyNetworking;
import com.eric.revill.network.ModPackets;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;

public final class EconomyMenuScreen extends Screen {
    private static final String[] ACTIONS = {"Añadir", "Quitar", "Ver"};
    private static final String[] TYPES = {"Azul", "Roja", "Verde"};
    private int selectedAction = 0;
    private int selectedType = 0;
    private int amount = 1;

    public EconomyMenuScreen() {
        super(Component.literal("Coins"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int centerY = this.height / 2;

        for (int i = 0; i < ACTIONS.length; i++) {
            final int action = i;
            this.addRenderableWidget(Button.builder(Component.literal(ACTIONS[i]), button -> {
                this.selectedAction = action;
                sendAction();
            }).bounds(centerX - 170, centerY - 80 + i * 30, 120, 20).build());
        }

        // Los botones de tipo y cantidad redibujan la pantalla para actualizar sus etiquetas.
        this.addRenderableWidget(Button.builder(Component.literal("Tipo: " + TYPES[this.selectedType]), button -> {
            this.selectedType = (this.selectedType + 1) % TYPES.length;
            this.rebuildWidgets();
        }).bounds(centerX + 10, centerY - 80, 170, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
            this.amount = Math.max(1, this.amount - 1);
            this.rebuildWidgets();
        }).bounds(centerX + 10, centerY - 50, 40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
            this.amount = Math.min(10000, this.amount + 1);
            this.rebuildWidgets();
        }).bounds(centerX + 60, centerY - 50, 40, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cantidad: " + this.amount), button -> {})
                .bounds(centerX + 110, centerY - 50, 70, 20).build());

        this.addRenderableWidget(Button.builder(Component.literal("Cerrar"), button -> this.onClose())
                .bounds(centerX - 60, centerY + 30, 120, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        graphics.fill(0, 0, this.width, this.height, 0xFF000000);
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        int centerX = this.width / 2;
        int centerY = this.height / 2;
        graphics.centeredText(this.font, Component.literal("Menú de monedas"), centerX, centerY - 120, 0xFFFFFFFF);
        graphics.centeredText(this.font, Component.literal("Acción actual: " + ACTIONS[this.selectedAction]), centerX, centerY - 100, 0xFFFFFFFF);
    }

    private void sendAction() {
        FriendlyByteBuf buf = ModPackets.create();
        buf.writeVarInt(this.selectedAction);
        buf.writeVarInt(this.selectedType);
        buf.writeVarInt(this.amount);
        ClientPackets.send(EconomyNetworking.COINS_ACTION, buf);
        this.onClose();
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
