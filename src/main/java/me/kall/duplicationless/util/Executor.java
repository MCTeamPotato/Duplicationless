package me.kall.duplicationless.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import me.kall.duplicationless.Duplicationless;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Duplicationless.MOD_ID)
public class Executor {
    private static final Int2ObjectMap<List<Runnable>> TASKS = new Int2ObjectOpenHashMap<>();

    public static void runAfter(int ticks, Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        int tickCount = server.getTickCount();
        server.execute(() -> TASKS.computeIfAbsent(tickCount + ticks, key -> new ArrayList<>()).add(task));
    }

    @SubscribeEvent
    @ApiStatus.Internal
    public static void onServerTick(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            int tick = event.getServer().getTickCount();
            event.getServer().execute(() -> {
                List<Runnable> tasks = TASKS.get(tick);
                if (tasks == null) return;
                tasks.forEach(Runnable::run);
                TASKS.remove(tick);
            });
        }
    }
}
