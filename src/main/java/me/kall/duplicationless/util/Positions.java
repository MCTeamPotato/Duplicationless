package me.kall.duplicationless.util;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import org.jetbrains.annotations.NotNull;

public final class Positions {
    public static long nearestOne(@NotNull final LongSet positions, @NotNull final BlockPos target) {
        return nearestOne(positions, target.getX(), target.getY(), target.getZ());
    }

    public static long nearestOne(@NotNull final LongSet positions, final long target) {
        return nearestOne(positions, BlockPos.getX(target), BlockPos.getY(target), BlockPos.getZ(target));
    }

    public static long nearestOne(@NotNull final LongSet positions, final int targetX, final int targetY, final int targetZ) {
        if (positions.isEmpty()) throw new IllegalArgumentException("Cannot find nearest one from empty positions set.");

        long nearest = 0;
        long minDistSq = Long.MAX_VALUE;

        final LongIterator iterator = positions.iterator();
        while (iterator.hasNext()) {
            final long next = iterator.nextLong();

            final int x = BlockPos.getX(next);
            final int y = BlockPos.getY(next);
            final int z = BlockPos.getZ(next);

            final long distX = x - targetX;
            final long distY = y - targetY;
            final long distZ = z - targetZ;

            final long distSq = distX * distX + distY * distY + distZ * distZ;

            if (distSq < minDistSq) {
                minDistSq = distSq;
                nearest = next;
            }
        }

        return nearest;
    }
}