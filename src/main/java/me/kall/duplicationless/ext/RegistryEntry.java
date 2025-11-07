package me.kall.duplicationless.ext;

import me.kall.duplicationless.Duplicationless;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;

public interface RegistryEntry {
    ResourceLocation registry$getName();

    ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(Duplicationless.MOD_ID, "none");

    static ResourceLocation getLocation(EntityType<?> type) {
        return ((RegistryEntry)type).registry$getName();
    }
}
