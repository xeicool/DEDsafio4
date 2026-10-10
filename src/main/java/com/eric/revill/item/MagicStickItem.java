package com.eric.revill.item;

import com.eric.revill.RevillMod;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** PALITOMAGICO: teletransporta al jugador golpeado a su punto de reaparición. */
public final class MagicStickItem extends Item {
    private static final SoundEvent TELEPORT_SOUND = SoundEvent.createVariableRangeEvent(RevillMod.id("ding", "revill"));
    private static final DustParticleOptions BLACK_DUST = new DustParticleOptions(0x030303, 1.5F);

    public MagicStickItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player user, LivingEntity entity, InteractionHand hand) {
        if (!(entity instanceof Player)) {
            return InteractionResult.PASS;
        }
        if (user.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!(entity instanceof ServerPlayer serverTarget)) {
            return InteractionResult.PASS;
        }
        teleportToRespawn(serverTarget);
        return InteractionResult.SUCCESS;
    }

    private static void teleportToRespawn(ServerPlayer target) {
        ServerLevel sourceLevel = target.level();
        double sx = target.getX();
        double sy = target.getY();
        double sz = target.getZ();

        // TODO(26.3): si el compilador se queja aquí, el API de "respawn" cambió; ver docs de ServerPlayer.
        ServerPlayer.RespawnConfig respawn = target.getRespawnConfig();
        ServerLevel destLevel = target.level().getServer().overworld();
        BlockPos destPos = null;
        if (respawn != null) {
            ServerLevel respawnLevel = target.level().getServer().getLevel(respawn.respawnData().dimension());
            if (respawnLevel != null) {
                destLevel = respawnLevel;
                destPos = respawn.respawnData().pos();
            }
        }
        if (destPos == null) {
            destPos = destLevel.getRespawnData().pos();
        }

        sourceLevel.playSound(null, sx, sy, sz, TELEPORT_SOUND, SoundSource.PLAYERS, 1.0F, 1.0F);
        sourceLevel.sendParticles(ParticleTypes.EXPLOSION, sx, sy + 0.5D, sz, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        sourceLevel.sendParticles(BLACK_DUST, sx, sy + 0.5D, sz, 48, 0.35D, 0.45D, 0.35D, 0.08D);

        target.teleportTo(destLevel, destPos.getX() + 0.5D, destPos.getY(), destPos.getZ() + 0.5D,
                Set.of(), target.getYRot(), target.getXRot(), true);
        target.displayClientMessage(
                Component.literal("Has sido teletransportado a tu último punto de reaparición establecido.")
                        .withStyle(ChatFormatting.GREEN),
                true);
    }
}
