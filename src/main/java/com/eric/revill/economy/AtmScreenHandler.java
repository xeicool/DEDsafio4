package com.eric.revill.economy;

import com.eric.revill.RevillMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Menú de depósito del cajero: 9 casillas de monedas + inventario del jugador. */
public final class AtmScreenHandler extends AbstractContainerMenu {
    public static final int CONFIRM_DEPOSIT_BUTTON = 0;

    public static final MenuType<AtmScreenHandler> TYPE = Registry.register(
            BuiltInRegistries.MENU,
            ResourceKey.create(Registries.MENU, RevillMod.id("atm_deposit")),
            new MenuType<>((syncId, inventory) -> new AtmScreenHandler(syncId, inventory, new SimpleContainer(9)),
                    FeatureFlags.VANILLA_SET));

    private final Inventory playerInventory;
    private final Container depositInventory;

    public static MenuType<AtmScreenHandler> initialize() {
        return TYPE;
    }

    public AtmScreenHandler(int syncId, Inventory playerInventory, Container depositInventory) {
        super(TYPE, syncId);
        this.playerInventory = playerInventory;
        this.depositInventory = depositInventory;
        checkContainerSize(depositInventory, 9);

        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 3; i++) {
                this.addSlot(new CurrencySlot(depositInventory, row * 3 + i, 0, 0));
            }
        }
        for (int row = 0; row < 3; row++) {
            for (int i = 0; i < 9; i++) {
                this.addSlot(new Slot(playerInventory, i + row * 9 + 9, 0, 0));
            }
        }
        for (int column = 0; column < 9; column++) {
            this.addSlot(new Slot(playerInventory, column, 0, 0));
        }
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (id != CONFIRM_DEPOSIT_BUTTON || !(player instanceof ServerPlayer serverPlayer)) {
            return false;
        }
        int[] deposited = getDepositAmounts();
        for (int slot = 0; slot < depositInventory.getContainerSize(); slot++) {
            if (CurrencyType.fromStack(depositInventory.getItem(slot)) != null) {
                depositInventory.setItem(slot, ItemStack.EMPTY);
            }
        }
        var server = serverPlayer.level().getServer();
        for (CurrencyType type : CurrencyType.values()) {
            CurrencyManager.addCurrency(server, serverPlayer.getUUID(), type, deposited[type.ordinal()]);
        }
        serverPlayer.sendSystemMessage(Component.literal("Deditas ingresadas en la cuenta."), true);
    }

    /** 100 deditas azules = 1 verde; el resto se queda como azul. */
    public int[] getDepositAmounts() {
        int[] deposited = new int[CurrencyType.values().length];
        for (int slot = 0; slot < depositInventory.getContainerSize(); slot++) {
            ItemStack stack = depositInventory.getItem(slot);
            CurrencyType type = CurrencyType.fromStack(stack);
            if (type != null) {
                deposited[type.ordinal()] += stack.getCount();
            }
        }
        int blueAmount = deposited[CurrencyType.AZUL.ordinal()];
        deposited[CurrencyType.AZUL.ordinal()] = blueAmount % 100;
        deposited[CurrencyType.VERDE.ordinal()] += blueAmount / 100;
        return deposited;
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        if (slotIndex < 0 || slotIndex >= this.slots.size()) return ItemStack.EMPTY;
        Slot slot = this.slots.get(slotIndex);
        if (!slot.hasItem()) return ItemStack.EMPTY;
        ItemStack original = slot.getItem();
        ItemStack result = original.copy();
        if (slotIndex < 9) {
            if (!this.moveItemStackTo(original, 9, this.slots.size(), true)) return ItemStack.EMPTY;
        } else if (CurrencyType.fromStack(original) == null || !this.moveItemStackTo(original, 0, 9, false)) {
            return ItemStack.EMPTY;
        }
        if (original.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return result;
    }

    @Override
    public boolean stillValid(Player player) {
        return true;
    }

    @Override
    public void removed(Player player) {
        super.removed(player);
        if (player.level().isClientSide()) return;
        for (int index = 0; index < depositInventory.getContainerSize(); index++) {
            ItemStack stack = depositInventory.removeItemNoUpdate(index);
            if (!stack.isEmpty() && !playerInventory.add(stack)) {
                playerInventory.placeItemBackInInventory(stack);
            }
        }
    }

    /** El cliente recoloca las casillas según el dibujo de la pantalla (45 posiciones x,y). */
    public void layoutSlots(int[][] positions) {
        int expected = 45;
        if (positions.length != expected || this.slots.size() != expected) return;
        for (int index = 0; index < expected; index++) {
            Slot old = this.slots.get(index);
            Slot replacement = index < 9
                    ? new CurrencySlot(depositInventory, index, positions[index][0], positions[index][1])
                    : new Slot(playerInventory, old.getContainerSlot(), positions[index][0], positions[index][1]);
            replacement.index = old.index;
            this.slots.set(index, replacement);
        }
    }

    private static final class CurrencySlot extends Slot {
        private CurrencySlot(Container container, int index, int x, int y) {
            super(container, index, x, y);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return CurrencyType.fromStack(stack) != null;
        }

        @Override
        public int getMaxStackSize() {
            return 99;
        }

        @Override
        public int getMaxStackSize(ItemStack stack) {
            return CurrencyType.fromStack(stack) == null ? super.getMaxStackSize(stack) : stack.getMaxStackSize();
        }
    }
}
