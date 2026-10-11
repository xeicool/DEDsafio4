package com.eric.revill.bunker;

import net.minecraft.core.BlockPos;

public class Bunker {
    private String dimension = "";
    private BlockPos firstCorner;
    private BlockPos secondCorner;

    public boolean isConfigured() {
        return dimension != null && !dimension.isEmpty() && firstCorner != null && secondCorner != null;
    }

    public boolean contains(BlockPos pos) {
        if (!isConfigured() || pos == null) return false;
        int minX = Math.min(firstCorner.getX(), secondCorner.getX());
        int maxX = Math.max(firstCorner.getX(), secondCorner.getX());
        int minY = Math.min(firstCorner.getY(), secondCorner.getY());
        int maxY = Math.max(firstCorner.getY(), secondCorner.getY());
        int minZ = Math.min(firstCorner.getZ(), secondCorner.getZ());
        int maxZ = Math.max(firstCorner.getZ(), secondCorner.getZ());
        return pos.getX() >= minX && pos.getX() <= maxX
                && pos.getY() >= minY && pos.getY() <= maxY
                && pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }

    public String getDimension() { return dimension; }
    public void setDimension(String dimension) { this.dimension = dimension; }
    public BlockPos getFirstCorner() { return firstCorner; }
    public void setFirstCorner(BlockPos firstCorner) { this.firstCorner = firstCorner; }
    public BlockPos getSecondCorner() { return secondCorner; }
    public void setSecondCorner(BlockPos secondCorner) { this.secondCorner = secondCorner; }
}
