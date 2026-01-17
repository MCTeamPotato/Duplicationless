package me.kall.duplicationless.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.fml.network.NetworkRegistry;
import net.minecraftforge.fml.network.simple.SimpleChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.function.Predicate;

public class Networker {
    @Contract("_, _ -> new")
    public static @NotNull SimpleChannel create(String modID, String version) {
        Predicate<String> equality = ver -> ver.equals(version);
        return NetworkRegistry.newSimpleChannel(new ResourceLocation(modID + ":main"), () -> version, equality, equality);
    }
}
