package com.eric.revill.meteor;

import com.eric.revill.RevillMod;
import com.eric.revill.config.RevillConfig;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Display;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class MeteorManager {
    private final List<Meteor> activeMeteorites = new ArrayList<>();
    private int nextSpawnTick = 0;

    public void update(ServerLevel world, int timeRemaining, RevillConfig config) {
        if (world == null || !config.meteoritesEnabled) return;
        MinecraftServer server = world.getServer();

        if (nextSpawnTick == 0 || server.getTickCount() >= nextSpawnTick) {
            int interval = getMeteoriteSpawnInterval(timeRemaining, config);
            if (interval > 0 && activeMeteorites.size() < config.meteoritesMaxActive) {
                spawnMeteorite(world, config);
            }
            nextSpawnTick = server.getTickCount() + interval;
        }

        Iterator<Meteor> iterator = activeMeteorites.iterator();
        while (iterator.hasNext()) {
            Meteor meteor = iterator.next();
            if (!meteor.isActive()) {
                if (meteor.hasExploded()) handleImpact(world, meteor, config);
                iterator.remove();
                continue;
            }
            meteor.tick(world);
            if (!meteor.isActive()) continue;
            Vec3 p = meteor.getPosition();
            world.sendParticles(ParticleTypes.FLAME, p.x, p.y, p.z, 6, 0.15D, 0.15D, 0.15D, 0.02D);
            world.sendParticles(ParticleTypes.SMOKE, p.x, p.y, p.z, 3, 0.12D, 0.12D, 0.12D, 0.01D);
        }
    }

    public int getMeteoriteSpawnInterval(int timeRemaining, RevillConfig config) {
        if (timeRemaining >= 600) return 80;
        if (timeRemaining >= 480) return 60;
        if (timeRemaining >= 300) return 45;
        if (timeRemaining >= 120) return 25;
        if (timeRemaining >= 60) return 15;
        if (timeRemaining >= 30) return 10;
        return 8;
    }

    public void spawnMeteorite(ServerLevel world, RevillConfig config) {
        if (activeMeteorites.size() >= config.meteoritesMaxActive) return;
        List<ServerPlayer> players = world.players();
        if (players.isEmpty()) return;

        ServerPlayer anchor = players.get(world.random.nextInt(players.size()));
        double targetX = anchor.getX() + world.random.nextDouble() * 192.0D - 96.0D;
        double targetZ = anchor.getZ() + world.random.nextDouble() * 192.0D - 96.0D;
        BlockPos surfacePos = world.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING, new BlockPos((int) targetX, 0, (int) targetZ));
        double targetY = surfacePos.getY();

        int minHeight = Math.max(40, config.meteoritesMinHeight);
        int maxHeight = Math.max(minHeight, config.meteoritesMaxHeight);
        double startY = targetY + minHeight + world.random.nextInt(maxHeight - minHeight + 1);
        double startX = targetX + world.random.nextDouble() * 80.0D - 40.0D;
        double startZ = targetZ + world.random.nextDouble() * 80.0D - 40.0D;

        Vec3 start = new Vec3(startX, startY, startZ);
        Vec3 target = new Vec3(targetX, targetY, targetZ);
        double speed = 1.6D + world.random.nextDouble() * 0.8D;

        Entity display = createMeteorDisplay(world, start);
        activeMeteorites.add(new Meteor(start, target, speed, display));
    }

    public void clear() {
        for (Meteor meteor : activeMeteorites) meteor.removeDisplay();
        activeMeteorites.clear();
        nextSpawnTick = 0;
    }

    public int getActiveCount() {
        return activeMeteorites.size();
    }

    private void handleImpact(ServerLevel world, Meteor meteor, RevillConfig config) {
        Vec3 pos = meteor.getPosition();
        int ix = (int) Math.floor(pos.x);
        int iy = (int) Math.floor(pos.y);
        int iz = (int) Math.floor(pos.z);
        meteor.removeDisplay();

        world.sendParticles(ParticleTypes.EXPLOSION_EMITTER, pos.x, pos.y, pos.z, 1, 0.0D, 0.0D, 0.0D, 0.0D);
        world.sendParticles(ParticleTypes.FLAME, pos.x, pos.y, pos.z, 60, 2.0D, 1.5D, 2.0D, 0.08D);
        world.sendParticles(ParticleTypes.LARGE_SMOKE, pos.x, pos.y, pos.z, 120, 3.0D, 1.5D, 3.0D, 0.15D);
        world.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE, pos.x, pos.y + 1.0D, pos.z, 160, 4.0D, 2.5D, 4.0D, 0.05D);
        world.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GENERIC_EXPLODE.value(), SoundSource.BLOCKS, 3.0F, 0.7F);

        // Cráter
        int craterRadius = Math.max(12, config.meteoritesCraterRadius);
        int craterDepth = Math.max(5, craterRadius / 2);
        for (int dx = -craterRadius; dx <= craterRadius; dx++) {
            for (int dz = -craterRadius; dz <= craterRadius; dz++) {
                double dist2d = Math.sqrt(dx * dx + dz * dz);
                if (dist2d <= craterRadius) {
                    int depth = (int) Math.max(1.0D, craterDepth * (1.0D - dist2d / craterRadius));
                    for (int dy = -depth; dy <= 1; dy++) {
                        BlockPos bpos = new BlockPos(ix + dx, iy + dy, iz + dz);
                        BlockState state = world.getBlockState(bpos);
                        if (!state.isAir() && !state.is(Blocks.BEDROCK)) {
                            world.setBlock(bpos, Blocks.AIR.defaultBlockState(), 3);
                        }
                    }
                }
            }
        }

        // Fuego alrededor del cráter
        int fireRadius = craterRadius + 2;
        for (int i = -fireRadius; i <= fireRadius; i++) {
            for (int dz = -fireRadius; dz <= fireRadius; dz++) {
                double dist2d = Math.sqrt(i * i + dz * dz);
                if (dist2d >= craterRadius - 1 && dist2d <= fireRadius && world.random.nextFloat() <= 0.4F) {
                    for (int dy = 2; dy >= -2; dy--) {
                        BlockPos below = new BlockPos(ix + i, iy + dy - 1, iz + dz);
                        BlockPos firePos = new BlockPos(ix + i, iy + dy, iz + dz);
                        if (!world.getBlockState(below).isAir() && world.getBlockState(firePos).isAir()) {
                            world.setBlock(firePos, Blocks.FIRE.defaultBlockState(), 3);
                            break;
                        }
                    }
                }
            }
        }

        if (config.meteoritesCreateExplosion) {
            Level.ExplosionInteraction interaction = config.meteoritesDestroyBlocks
                    ? Level.ExplosionInteraction.BLOCK : Level.ExplosionInteraction.NONE;
            world.explode(null, pos.x, pos.y, pos.z, (float) Math.max(6.0D, config.meteoritesExplosionPower), false, interaction);
        }
    }

    /** Bloque de magma de 5x5x5 invocado con /summon (evita depender del API de NBT de 26.x). */
    private Entity createMeteorDisplay(ServerLevel world, Vec3 position) {
        String tag = "revill_meteor_" + UUID.randomUUID().toString().replace("-", "");
        String snbt = "{block_state:{Name:\"minecraft:magma_block\"},"
                + "transformation:{translation:[-2f,-2f,-2f],left_rotation:[0f,0f,0f,1f],scale:[5f,5f,5f],right_rotation:[0f,0f,0f,1f]},"
                + "view_range:4f,width:5f,height:5f,Glowing:1b,Invulnerable:1b,NoGravity:1b,Tags:[\"" + tag + "\"]}";
        try {
            MinecraftServer server = world.getServer();
            var source = server.createCommandSourceStack().withLevel(world).withPosition(position).withSuppressedOutput();
            server.getCommands().performPrefixedCommand(source,
                    String.format(java.util.Locale.ROOT, "summon minecraft:block_display %f %f %f %s", position.x, position.y, position.z, snbt));
            for (Display.BlockDisplay found : world.getEntitiesOfClass(Display.BlockDisplay.class,
                    new AABB(position, position).inflate(3.0D), e -> e.getTags().contains(tag))) {
                return found;
            }
        } catch (RuntimeException e) {
            RevillMod.LOGGER.warn("No se pudo crear el bloque del meteorito", e);
        }
        return null;
    }
}
