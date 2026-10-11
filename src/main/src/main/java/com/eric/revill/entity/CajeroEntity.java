package com.eric.revill.entity;

import com.eric.revill.economy.EconomyNetworking;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;

/** Cajero automático: entidad fija, invulnerable salvo para jugadores en creativo. */
public class CajeroEntity extends Mob {
    private boolean spawnFacingInitialized;

    public CajeroEntity(EntityType<? extends CajeroEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    @Override
    public void tick() {
        super.tick();
        this.setNoGravity(true);
        this.noPhysics = true;
        this.setDeltaMovement(Vec3.ZERO);
        this.setXRot(0.0F);

        if (!this.level().isClientSide() && !this.spawnFacingInitialized) {
            Player nearest = null;
            double closest = 256.0D;
            for (Player player : this.level().players()) {
                if (player.isSpectator()) {
                    continue;
                }
                double distance = this.distanceToSqr(player);
                if (distance < closest) {
                    closest = distance;
                    nearest = player;
                }
            }
            initializeSpawnFacing(nearest);
        }
    }

    public void initializeSpawnFacing(Player player) {
        float yaw = this.getYRot();
        if (player != null) {
            double dx = player.getX() - this.getX();
            double dz = player.getZ() - this.getZ();
            if (dx != 0.0D || dz != 0.0D) {
                yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0D);
            }
        }
        yaw = Math.round(yaw / 90.0F) * 90.0F;
        this.setYRot(yaw);
        this.setYHeadRot(yaw);
        this.setYBodyRot(yaw);
        this.spawnFacingInitialized = true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof Player player && player.isCreative()) {
            this.kill(level);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (this.level().isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            EconomyNetworking.openAtm(serverPlayer);
        }
        return InteractionResult.CONSUME;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("SpawnFacingInitialized", this.spawnFacingInitialized);
    }

    @Override
    public void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        this.spawnFacingInitialized = input.getBooleanOr("SpawnFacingInitialized", false);
    }
}
