package com.eric.revill.client.ui;

import com.eric.revill.RevillMod;
import com.eric.revill.economy.AtmScreenHandler;
import com.eric.revill.economy.CurrencyType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/** Pantalla de depósito. Versión sin escalado: el fondo es 176x166 centrado. */
public final class AtmDepositScreen extends AbstractContainerScreen<AtmScreenHandler> {
    private static final Identifier TEXTURE = RevillMod.id("textures/gui/atm/ingresar.png");

    public AtmDepositScreen(AtmScreenHandler menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        this.imageWidth = 176;
        this.imageHeight = 166;
        this.titleLabelX = 0;
        this.titleLabelY = 0;
    }

    @Override
    protected void init() {
        super.init();
        this.menu.layoutSlots(makeSlotPositions());
    }

    private static int[][] makeSlotPositions() {
        int[][] positions = new int[45][2];
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 3; i++) {
                int index = row * 3 + i;
                positions[index][0] = 39 + i * 18;
                positions[index][1] = 15 + row * 18;
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                int index = 9 + row * 9 + i;
                positions[index][0] = 8 + i * 18;
                positions[index][1] = 85 + row * 18;
            }
        }
        for (int column = 0; column < 9; column++) {
            int index = 36 + column;
            positions[index][0] = 8 + column * 18;
            positions[index][1] = 141;
        }
        return positions;
    }

    // NOTA 26.x: si el compilador dice que este método no existe, el nombre del "fondo" del menú
    // cambió (renderBg -> extractBg / extractBackground...). Pegar el error.
    @Override
    protected void extractBg(GuiGraphicsExtractor graphics, float delta, int mouseX, int mouseY) {
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, this.leftPos, this.topPos, 0.0F, 0.0F, 176, 166, 176, 166);
        drawDepositPreview(graphics);
    }

    @Override
    protected void extractLabels(GuiGraphicsExtractor graphics, int mouseX, int mouseY) {}

    private void drawDepositPreview(GuiGraphicsExtractor graphics) {
        int baseX = this.leftPos + 135;
        graphics.centeredText(this.font, Component.literal("Se ingresará:"), baseX, this.topPos + 56, 0xFFFFFFFF);

        int[] amounts = this.menu.getDepositAmounts();
        int y = this.topPos + 66;
        int x = this.leftPos + 108;
        for (CurrencyType type : CurrencyType.values()) {
            int amount = amounts[type.ordinal()];
            if (amount > 0) {
                graphics.item(new ItemStack(type.item()), x, y);
                graphics.text(this.font, Integer.toString(amount), x + 17, y + 4, 0xFFFFFFFF);
                y += 17;
            }
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        if (event.button() == 0) {
            double x = event.x() - this.leftPos;
            double y = event.y() - this.topPos;
            if (x >= 105.0D && x <= 145.0D && y >= 36.0D && y <= 55.0D) {
                Minecraft client = Minecraft.getInstance();
                if (client.gameMode != null) {
                    client.gameMode.handleInventoryButtonClick(this.menu.containerId, AtmScreenHandler.CONFIRM_DEPOSIT_BUTTON);
                }
                return true;
            }
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
