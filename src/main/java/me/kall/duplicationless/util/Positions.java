package me.kall.duplicationless.util;

import it.unimi.dsi.fastutil.longs.LongConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

public final class Positions {
    public static long nearestOne(@NotNull final Set<Long> positions, @NotNull final BlockPos target) {
        return nearestOne(positions, target.getX(), target.getY(), target.getZ());
    }

    public static long nearestOne(@NotNull final Set<Long> positions, final long target) {
        return nearestOne(positions, BlockPos.getX(target), BlockPos.getY(target), BlockPos.getZ(target));
    }

    public static long nearestOne(@NotNull final Set<Long> positions, final int targetX, final int targetY, final int targetZ) {
        if (positions.isEmpty()) throw new IllegalArgumentException("Cannot find nearest one from empty positions set.");

        long nearest = 0;
        long minDistSq = Long.MAX_VALUE;

        for (long next : positions) {
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

    public static long toChunk(@NotNull BlockPos pos) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(pos.getX()), SectionPos.blockToSectionCoord(pos.getZ()));
    }

    public static long toChunk(long blockPos) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(BlockPos.getX(blockPos)), SectionPos.blockToSectionCoord(BlockPos.getZ(blockPos)));
    }

    public static long toChunk(int blockX, int blockZ) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(blockX), SectionPos.blockToSectionCoord(blockZ));
    }

    public static long toChunk(double x, double z) {
        return ChunkPos.asLong(SectionPos.blockToSectionCoord(Mth.floor(x)), SectionPos.blockToSectionCoord(Mth.floor(z)));
    }

    public static void iterateAround(@NotNull ChunkPos centerChunk, int radius, LongConsumer action) {
        iterateAround(centerChunk.x, centerChunk.z, radius, action);
    }

    public static void iterateAround(long centerChunk, int radius, LongConsumer action) {
        iterateAround(ChunkPos.getX(centerChunk), ChunkPos.getZ(centerChunk), radius, action);
    }

    public static void iterateAround(int chunkX, int chunkZ, int radius, LongConsumer action) {
        for (int dx = -radius; dx <= radius; dx++) {
            for (int dz = -radius; dz <= radius; dz++) {
                action.accept(ChunkPos.asLong(chunkX + dx, chunkZ + dz));
            }
        }
    }
}
