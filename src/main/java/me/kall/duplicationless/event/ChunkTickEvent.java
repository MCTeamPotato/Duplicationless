package me.kall.duplicationless.event;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.neoforged.bus.api.Event;

public abstract class ChunkTickEvent extends Event {
    private final LevelChunk chunk;
    private final ServerLevel level;
    private final int randomTickSpeed;

    public ChunkTickEvent(LevelChunk chunk, ServerLevel level, int randomTickSpeed) {
        this.chunk = chunk;
        this.level = level;
        this.randomTickSpeed = randomTickSpeed;
    }

    public LevelChunk getChunk() {
        return chunk;
    }

    public ServerLevel getLevel() {
        return level;
    }

    public int randomTickSpeed() {
        return randomTickSpeed;
    }

    public static final class Pre extends ChunkTickEvent {
        public Pre(LevelChunk chunk, ServerLevel level, int randomTickSpeed) {
            super(chunk, level, randomTickSpeed);
        }
    }

    public static final class Post extends ChunkTickEvent {
        public Post(LevelChunk chunk, ServerLevel level, int randomTickSpeed) {
            super(chunk, level, randomTickSpeed);
        }
    }
}
