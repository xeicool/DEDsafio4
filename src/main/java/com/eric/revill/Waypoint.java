package com.eric.revill.waypoint;

import net.minecraft.core.BlockPos;

public class Waypoint {
    private final String id;
    private final String name;
    private final BlockPos pos;
    private final String icon;
    private final int color;
    private final String dimension;

    public Waypoint(String id, String name, BlockPos pos, String icon, int color, String dimension) {
        this.id = id;
        this.name = name;
        this.pos = pos;
        this.icon = icon;
        this.color = color;
        this.dimension = dimension;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BlockPos getPos() { return pos; }
    public String getIcon() { return icon; }
    public int getColor() { return color; }
    public String getDimension() { return dimension; }
}
