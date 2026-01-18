package me.kall.duplicationless.util;

import net.neoforged.fml.loading.FMLLoader;

public class Mods {
    public static boolean isLoaded(String modID) {
        return FMLLoader.getCurrent().getLoadingModList().getModFileById(modID) != null;
    }
}
