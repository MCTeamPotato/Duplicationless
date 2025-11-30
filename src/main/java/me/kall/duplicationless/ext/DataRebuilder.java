package me.kall.duplicationless.ext;

import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
public interface DataRebuilder {
    boolean duplicationless$rebuilt();
    void duplicationless$setRebuilt();
}
