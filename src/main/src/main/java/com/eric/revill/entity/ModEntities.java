package com.eric.revill.entity;

import com.eric.revill.RevillMod;
import net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

public final class ModEntities {
    public static final ResourceKey<EntityType<?>> CAJERO_KEY =
            ResourceKey.create(Registries.ENTITY_TYPE, RevillMod.id("cajero"));

    public static final EntityType<CajeroEntity> CAJERO = Registry.register(
            BuiltInRegistries.ENTITY_TYPE,
            CAJERO_KEY,
            EntityType.Builder.of(CajeroEntity::new, MobCategory.MISC)
                    .sized(0.75F, 1.8F)
                    .clientTrackingRange(10)
                    .updateInterval(3)
                    .build(CAJERO_KEY));

    private ModEntities() {}

    public static void initialize() {
        FabricDefaultAttributeRegistry.register(CAJERO, Mob.createMobAttributes());
    }
}
