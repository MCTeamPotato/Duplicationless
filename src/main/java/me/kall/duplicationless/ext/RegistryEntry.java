package me.kall.duplicationless.ext;

import me.kall.duplicationless.Duplicationless;
import net.minecraft.resources.ResourceLocation;

public interface RegistryEntry {
    ResourceLocation registry$getName();

    ResourceLocation NONE = ResourceLocation.fromNamespaceAndPath(Duplicationless.MOD_ID, "none");
}
