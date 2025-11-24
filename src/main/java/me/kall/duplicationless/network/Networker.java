package me.kall.duplicationless.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class Networker {
    @Contract("_, _ -> new")
    public static @NotNull SimpleChannel create(String modID, String version) {
        Predicate<String> equality = ver -> ver.equals(version);
        return NetworkRegistry.newSimpleChannel(ResourceLocation.parse(modID + ":main"), () -> version, equality, equality);
    }
}
