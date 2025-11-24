package me.kall.duplicationless;

import me.kall.duplicationless.data.EntityTracker;
import net.minecraftforge.fml.common.Mod;

@Mod(Duplicationless.MOD_ID)
public final class Duplicationless {
    public static final String MOD_ID = "duplicationless";

    public Duplicationless() {
        EntityTracker.register();
    }
}
