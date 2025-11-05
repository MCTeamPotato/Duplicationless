package me.kall.duplicationless;

import me.kall.duplicationless.data.EntityTracker;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Duplicationless.MOD_ID)
public final class Duplicationless {
    public static final String MOD_ID = "duplicationless";

    public Duplicationless(FMLJavaModLoadingContext context) {
        EntityTracker.register();
    }
}
