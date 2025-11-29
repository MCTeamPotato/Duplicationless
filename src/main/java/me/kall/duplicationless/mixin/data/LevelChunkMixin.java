package me.kall.duplicationless.mixin.data;

import me.kall.duplicationless.ext.DataRebuilder;

public class LevelChunkMixin implements DataRebuilder {
    private boolean duplicationless$rebuilt;

    @Override
    public boolean duplicationless$rebuilt() {
        return this.duplicationless$rebuilt;
    }

    @Override
    public void duplicationless$setRebuilt() {
        this.duplicationless$rebuilt = true;
    }
}
