package me.kall.duplicationless.util;

import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public final class ResourceLocations {
    @Contract("_ -> new")
    public static @NotNull ResourceLocation of(String id) {
        return ResourceLocation.parse(id);
    }

    @Contract("_, _ -> new")
    public static @NotNull ResourceLocation of(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }
}
