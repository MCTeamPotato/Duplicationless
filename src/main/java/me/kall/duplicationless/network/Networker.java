package me.kall.duplicationless.network;

import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

public class Networker {
    @Contract("_, _ -> new")
    public static @NotNull SimpleChannel create(String modID, String version) {
        return NetworkRegistry.newSimpleChannel(ResourceLocation.parse(modID + ":main"), () -> version, ver -> ver.equals(version), ver -> ver.equals(version));
    }
}
