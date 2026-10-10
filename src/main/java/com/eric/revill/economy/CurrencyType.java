package com.eric.revill.economy;

import com.eric.revill.item.ModItems;
import java.util.Locale;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public enum CurrencyType {
    AZUL("azul", ModItems.DEDITA),
    ROJA("roja", ModItems.DEDITA_ROJA),
    VERDE("verde", ModItems.DEDITA_VERDE);

    private final String key;
    private final Item item;

    CurrencyType(String key, Item item) {
        this.key = key;
        this.item = item;
    }

    public String key() {
        return key;
    }

    public Item item() {
        return item;
    }

    public static CurrencyType fromStack(ItemStack stack) {
        if (stack == null || stack.isEmpty()) return null;
        for (CurrencyType type : values()) {
            if (stack.is(type.item)) return type;
        }
        return null;
    }

    public static CurrencyType parse(String value) {
        if (value == null) return null;
        String normalized = value.toLowerCase(Locale.ROOT);
        for (CurrencyType type : values()) {
            if (type.key.equals(normalized)) return type;
        }
        return null;
    }
}
