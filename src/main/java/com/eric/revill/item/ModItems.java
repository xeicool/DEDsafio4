package com.eric.revill.item;

import com.eric.revill.RevillMod;
import com.eric.revill.entity.ModEntities;
import java.util.function.Function;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModItems {
    public static final Identifier NUTRITOTEM_GROUP_ID = RevillMod.id("nutritotem_group");

    public static final Item DEDITA = register("dedita", Item::new, new Item.Properties().stacksTo(99));
    public static final Item DEDITA_ROJA = register("dedita_roja", Item::new, new Item.Properties().stacksTo(99));
    public static final Item DEDITA_ROSA = register("dedita_rosa", Item::new, new Item.Properties().stacksTo(99));
    public static final Item DEDITA_VERDE = register("dedita_verde", Item::new, new Item.Properties().stacksTo(99));
    public static final Item CAJERO_SPAWN_EGG = register("cajero_spawn_egg", CajeroSpawnEggItem::new,
            new Item.Properties().spawnEgg(ModEntities.CAJERO));
    public static final Item PALITOMAGICO = register("palitomagico", MagicStickItem::new, new Item.Properties());

    public static final CreativeModeTab NUTRITOTEM_GROUP = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            NUTRITOTEM_GROUP_ID,
            CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                    .title(Component.translatable("itemGroup.dedsafio-4-mechanics.nutritotem_group"))
                    .icon(() -> new ItemStack(Items.TOTEM_OF_UNDYING))
                    .displayItems((parameters, output) -> {
                        output.accept(Items.TOTEM_OF_UNDYING);
                        output.accept(DEDITA);
                        output.accept(DEDITA_ROJA);
                        output.accept(DEDITA_ROSA);
                        output.accept(DEDITA_VERDE);
                        output.accept(CAJERO_SPAWN_EGG);
                        output.accept(PALITOMAGICO);
                        // TODO etapa 2: output.accept(ModMailBlocks.MAILBOX_ITEM);
                    })
                    .build());

    private ModItems() {}

    private static Item register(String name, Function<Item.Properties, Item> factory, Item.Properties properties) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, RevillMod.id(name));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties.setId(key)));
    }

    public static void initialize() {
        // Forzar la carga de la clase (los campos estáticos registran los ítems).
    }
}
