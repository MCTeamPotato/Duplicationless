package me.kall.duplicationless.event;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.Event;
import org.jetbrains.annotations.NotNull;

public class BlockChangeEvent extends Event {
    private final ServerLevel level;
    private final BlockState oldState;
    private final BlockState newState;
    private final Identifier dim;
    private final long chunkPos;
    private final long blockPos;

    public BlockChangeEvent(@NotNull ServerLevel level, BlockState oldState, BlockState newState, @NotNull BlockPos blockPos) {
        this.level = level;
        this.oldState = oldState;
        this.newState = newState;
        this.dim = level.dimension().identifier();
        this.chunkPos = ChunkPos.asLong(blockPos.getX() >> 4, blockPos.getZ() >> 4);
        this.blockPos = blockPos.asLong();
    }

    public long blockPos() {
        return this.blockPos;
    }

    public long chunkPos() {
        return this.chunkPos;
    }

    public Identifier dim() {
        return this.dim;
    }

    public BlockState newState() {
        return this.newState;
    }

    public BlockState oldState() {
        return this.oldState;
    }

    public ServerLevel level() {
        return this.level;
    }
}