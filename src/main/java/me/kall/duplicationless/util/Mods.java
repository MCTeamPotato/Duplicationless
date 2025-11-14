package me.kall.duplicationless.util;

import net.minecraftforge.fml.loading.FMLLoader;

public class Mods {
    public static boolean isLoaded(String modID) {
        return FMLLoader.getLoadingModList().getModFileById(modID) != null;
    }
}
