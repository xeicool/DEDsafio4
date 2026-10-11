package com.eric.revill.client.ui;

import com.eric.revill.RevillMod;
import com.eric.revill.client.network.ClientPackets;
import com.eric.revill.economy.EconomyNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class AtmScreen extends Screen {
    private static final Identifier TEXTURE = RevillMod.id("textures/gui/atm/cajero.png");
    private static final int SOURCE_WIDTH = 176;
    private static final int SOURCE_HEIGHT = 166;
    private float scale;
    private int left;
    private int top;
    private int renderWidth;
    private int renderHeight;

    public AtmScreen() {
        super(Component.literal("Cajero Automático"));
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
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() != 0) {
            return super.mouseClicked(event, doubleClick);
        }
        double imageX = (event.x() - this.left) / this.scale;
        double imageY = (event.y() - this.top) / this.scale;
        if (inside(imageX, imageY, 25, 120, 82, 141)) {
            ClientPackets.send(EconomyNetworking.OPEN_DEPOSIT);
            return true;
        }
        if (inside(imageX, imageY, 90, 120, 150, 141)) {
            ClientPackets.send(EconomyNetworking.OPEN_WITHDRAW);
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
