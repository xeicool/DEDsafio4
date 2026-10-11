package com.eric.revill.client.ui;

import com.eric.revill.RevillMod;
import com.eric.revill.client.network.EconomyClientNetworking;
import com.eric.revill.client.render.CurrencyHudRenderer;
import com.eric.revill.economy.CurrencyType;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AtmWithdrawScreen extends Screen {
    private static final Identifier TEXTURE = RevillMod.id("textures/gui/atm/sacar.png");
    private static final int SOURCE_WIDTH = 176;
    private static final int SOURCE_HEIGHT = 166;
    private static final int[] COLUMN_X = {51, 85, 119};
    private static final CurrencyType[] ROW_TYPES = {CurrencyType.ROJA, CurrencyType.VERDE, CurrencyType.AZUL};
    private final int[] selected = new int[3];
    private float scale;
    private int left;
    private int top;
    private int renderWidth;
    private int renderHeight;

    public AtmWithdrawScreen() {
        super(Component.literal("Retirar deditas"));
    }

    @Override
    protected void init() {
        this.scale = Math.min(1.0F, Math.min(this.width / (float) SOURCE_WIDTH, this.height / (float) SOURCE_HEIGHT));
        this.renderWidth = Math.round(SOURCE_WIDTH * this.scale);
        this.renderHeight = Math.round(SOURCE_HEIGHT * this.scale);
        this.left = (this.width - this.renderWidth) / 2;
        this.top = (this.height - this.renderHeight) / 2;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.left, this.top, 0.0F, 0.0F,
                this.renderWidth, this.renderHeight, this.renderWidth, this.renderHeight);
        int[] balances = CurrencyHudRenderer.getBalances();
        for (int index = 0; index < COLUMN_X.length; index++) {
            int amount = Math.min(this.selected[index], balances[ROW_TYPES[index].ordinal()]);
            graphics.centeredText(this.font, Component.literal(Integer.toString(amount)),
                    this.left + Math.round(COLUMN_X[index] * this.scale),
                    this.top + Math.round(102.0F * this.scale), 0xFFFFFFFF);
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double imageX = (event.x() - this.left) / this.scale;
        double imageY = (event.y() - this.top) / this.scale;

        for (int index = 0; index < COLUMN_X.length; index++) {
            if (Math.abs(imageX - COLUMN_X[index]) <= 12.0D) {
                int balance = CurrencyHudRenderer.getBalances()[ROW_TYPES[index].ordinal()];
                if (imageY >= 54.0D && imageY <= 66.0D) {
                    this.selected[index] = Math.min(balance, this.selected[index] + 1);
                    return true;
                }
                if (imageY >= 81.0D && imageY <= 95.0D) {
                    this.selected[index] = Math.max(0, this.selected[index] - 1);
                    return true;
                }
            }
        }

        if (inside(imageX, imageY, 75, 130, 101, 146)) {
            // ROW_TYPES = {ROJA, VERDE, AZUL} -> el servidor espera (azul, roja, verde)
            EconomyClientNetworking.sendWithdrawRequest(this.selected[2], this.selected[0], this.selected[1]);
            this.onClose();
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    private static boolean inside(double x, double y, int left, int top, int right, int bottom) {
        return x >= left && x <= right && y >= top && y <= bottom;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
