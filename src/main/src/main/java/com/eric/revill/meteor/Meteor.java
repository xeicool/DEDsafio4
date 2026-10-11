package com.eric.revill.meteor;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

public class Meteor {
    private final Vec3 start;
    private final Vec3 target;
    private Vec3 position;
    private final Vec3 velocity;
    private final double speed;
    private final Entity display; // puede ser null si no se pudo crear
    private boolean active = true;
    private boolean exploded;

    public Meteor(Vec3 start, Vec3 target, double speed, Entity display) {
        this.start = start;
        this.target = target;
        this.position = start;
        this.speed = speed;
        this.display = display;
        this.velocity = target.subtract(start).normalize().scale(speed);
        updateDisplayPosition();
    }

    public void tick(ServerLevel world) {
        if (!active) return;
        position = position.add(velocity);
        updateDisplayPosition();
        if (position.distanceTo(target) <= 2.5D || collidesWithTerrain(world)) {
            active = false;
            exploded = true;
        }
    }

    public void removeDisplay() {
        if (display != null && !display.isRemoved()) {
            display.discard();
        }
    }

    private void updateDisplayPosition() {
        if (display != null) display.setPos(position.x, position.y, position.z);
    }

    private boolean collidesWithTerrain(ServerLevel world) {
        int radius = 2;
        int minX = (int) Math.floor(position.x - radius);
        int maxX = (int) Math.floor(position.x + radius);
        int minY = (int) Math.floor(position.y - radius);
        int maxY = (int) Math.floor(position.y + radius);
        int minZ = (int) Math.floor(position.z - radius);
        int maxZ = (int) Math.floor(position.z + radius);
        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    BlockPos blockPos = new BlockPos(x, y, z);
                    BlockState state = world.getBlockState(blockPos);
                    if (!state.getCollisionShape(world, blockPos).isEmpty()) return true;
                }
            }
        }
        return false;
    }

    public Vec3 getPosition() { return position; }
    public Vec3 getTarget() { return target; }
    public double getSpeed() { return speed; }
    public boolean isActive() { return active; }
    public boolean hasExploded() { return exploded; }
    public void setExploded(boolean exploded) { this.exploded = exploded; }
}
