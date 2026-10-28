package com.aell.zonefocus.zone;

import fi.dy.masa.litematica.selection.Box;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

@Environment(EnvType.CLIENT)
public record ZoneBox(int minX, int minY, int minZ, int maxX, int maxY, int maxZ)
{
    public ZoneBox(BlockPos a, BlockPos b)
    {
        this(Math.min(a.getX(), b.getX()), Math.min(a.getY(), b.getY()), Math.min(a.getZ(), b.getZ()),
                Math.max(a.getX(), b.getX()), Math.max(a.getY(), b.getY()), Math.max(a.getZ(), b.getZ()));
    }

    public static ZoneBox of(Box box)
    {
        BlockPos pos1 = box.getPos1();
        BlockPos pos2 = box.getPos2();

        return new ZoneBox(pos1 != null ? pos1 : BlockPos.ZERO, pos2 != null ? pos2 : BlockPos.ZERO);
    }

    public int sizeX() { return this.maxX - this.minX + 1; }
    public int sizeY() { return this.maxY - this.minY + 1; }
    public int sizeZ() { return this.maxZ - this.minZ + 1; }

    public long volume()
    {
        return (long) this.sizeX() * this.sizeY() * this.sizeZ();
    }

    public boolean contains(BlockPos pos)
    {
        return pos.getX() >= this.minX && pos.getX() <= this.maxX
                && pos.getY() >= this.minY && pos.getY() <= this.maxY
                && pos.getZ() >= this.minZ && pos.getZ() <= this.maxZ;
    }

    @Nullable
    public ZoneBox intersect(ZoneBox other)
    {
        int x1 = Math.max(this.minX, other.minX);
        int y1 = Math.max(this.minY, other.minY);
        int z1 = Math.max(this.minZ, other.minZ);
        int x2 = Math.min(this.maxX, other.maxX);
        int y2 = Math.min(this.maxY, other.maxY);
        int z2 = Math.min(this.maxZ, other.maxZ);

        return x1 <= x2 && y1 <= y2 && z1 <= z2 ? new ZoneBox(x1, y1, z1, x2, y2, z2) : null;
    }

    public ZoneBox union(ZoneBox other)
    {
        return new ZoneBox(
                Math.min(this.minX, other.minX), Math.min(this.minY, other.minY), Math.min(this.minZ, other.minZ),
                Math.max(this.maxX, other.maxX), Math.max(this.maxY, other.maxY), Math.max(this.maxZ, other.maxZ));
    }

    public ZoneBox offset(BlockPos delta)
    {
        return new ZoneBox(
                this.minX + delta.getX(), this.minY + delta.getY(), this.minZ + delta.getZ(),
                this.maxX + delta.getX(), this.maxY + delta.getY(), this.maxZ + delta.getZ());
    }

    public ZoneBox moveFace(net.minecraft.core.Direction face, int amount)
    {
        int x1 = this.minX, y1 = this.minY, z1 = this.minZ;
        int x2 = this.maxX, y2 = this.maxY, z2 = this.maxZ;

        switch (face)
        {
            case WEST  -> x1 = Math.min(x1 - amount, x2);
            case EAST  -> x2 = Math.max(x2 + amount, x1);
            case DOWN  -> y1 = Math.min(y1 - amount, y2);
            case UP    -> y2 = Math.max(y2 + amount, y1);
            case NORTH -> z1 = Math.min(z1 - amount, z2);
            case SOUTH -> z2 = Math.max(z2 + amount, z1);
        }

        return new ZoneBox(Math.min(x1, x2), Math.min(y1, y2), Math.min(z1, z2),
                Math.max(x1, x2), Math.max(y1, y2), Math.max(z1, z2));
    }

    public AABB toAabb()
    {
        return new AABB(this.minX, this.minY, this.minZ, this.maxX + 1.0, this.maxY + 1.0, this.maxZ + 1.0);
    }

    public AABB toAabb(double grow)
    {
        return this.toAabb().inflate(grow);
    }

    public BlockPos min()
    {
        return new BlockPos(this.minX, this.minY, this.minZ);
    }

    public BlockPos max()
    {
        return new BlockPos(this.maxX, this.maxY, this.maxZ);
    }

    public net.minecraft.world.phys.Vec3 center()
    {
        return new net.minecraft.world.phys.Vec3(
                (this.minX + this.maxX + 1) / 2.0,
                (this.minY + this.maxY + 1) / 2.0,
                (this.minZ + this.maxZ + 1) / 2.0);
    }

    public Box toLitematicaBox(String name)
    {
        return new Box(this.min(), this.max(), name);
    }

    public String signature()
    {
        return this.minX + "," + this.minY + "," + this.minZ + "/" + this.maxX + "," + this.maxY + "," + this.maxZ;
    }
}
