package me.kall.duplicationless.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.duplicationless.Duplicationless;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.FORGE, modid = Duplicationless.MOD_ID)
public class Executor {
    private static final Int2ObjectMap<List<Runnable>> TASKS = new Int2ObjectOpenHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(Executor.class);

    public static void runAfter(int ticks, Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            logInvalid();
            return;
        }
        int tickCount = server.getTickCount();
        server.execute(() -> TASKS.computeIfAbsent(tickCount + ticks, key -> new ObjectArrayList<>()).add(task));
    }

    public static void run(Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            logInvalid();
            return;
        }
        if (server.isSameThread()) {
            task.run();
        } else {
            server.execute(task);
        }
    }

    private static void logInvalid() {
        LOGGER.warn("Server unavailable. Skipping task setup.");
    }

    @SubscribeEvent
    @ApiStatus.Internal
    public static void onServerTick(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.START) {
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            int tick = server.getTickCount();
            server.execute(() -> {
                List<Runnable> tasks = TASKS.get(tick);
                if (tasks == null) return;
                tasks.forEach(Runnable::run);
                TASKS.remove(tick);
            });
        }
    }
}
