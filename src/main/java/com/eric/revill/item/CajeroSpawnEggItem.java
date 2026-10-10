package com.eric.revill.item;

import com.eric.revill.entity.CajeroEntity;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.phys.AABB;

/** Huevo del cajero: al usarlo, el cajero nace mirando hacia el jugador (a 90°). */
public final class CajeroSpawnEggItem extends SpawnEggItem {
    public CajeroSpawnEggItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getLevel().isClientSide() || context.getPlayer() == null) {
            return super.useOn(context);
        }

        AABB area = new AABB(context.getClickedPos()).inflate(3.0D);
        Set<UUID> existing = new HashSet<>();
        for (CajeroEntity entity : context.getLevel().getEntitiesOfClass(CajeroEntity.class, area)) {
            existing.add(entity.getUUID());
        }

        InteractionResult result = super.useOn(context);
        if (!result.consumesAction()) {
            return result;
        }

        Player spawner = context.getPlayer();
        for (CajeroEntity entity : context.getLevel().getEntitiesOfClass(
                CajeroEntity.class, area, candidate -> !existing.contains(candidate.getUUID()))) {
            entity.initializeSpawnFacing(spawner);
            break;
        }
        return result;
    }
}
