package me.kall.duplicationless.mixin.data;

import me.kall.duplicationless.ext.DataRebuilder;
import net.minecraft.world.level.chunk.LevelChunk;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(LevelChunk.class)
public class LevelChunkMixin implements DataRebuilder {
    @Unique private boolean duplicationless$rebuilt;

    @Override
    public boolean duplicationless$rebuilt() {
        return this.duplicationless$rebuilt;
    }

    @Override
    public void duplicationless$setRebuilt() {
        this.duplicationless$rebuilt = true;
    }
}
