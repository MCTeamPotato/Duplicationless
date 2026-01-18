package me.kall.duplicationless.util;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import me.kall.duplicationless.Duplicationless;
import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@EventBusSubscriber(modid = Duplicationless.MOD_ID)
public class Executor {
    private static final Int2ObjectMap<List<Runnable>> TASKS = new Int2ObjectOpenHashMap<>();
    private static final Logger LOGGER = LogManager.getLogger(Executor.class);

    public static void runAfter(int ticks, Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            logInvalid();
            return;
        }
        server.execute(() -> TASKS.computeIfAbsent(server.getTickCount() + ticks, key -> new ObjectArrayList<>()).add(task));
    }

    public static void run(Runnable task) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            logInvalid();
            return;
        }
        server.execute(task);
    }

    private static void logInvalid() {
        LOGGER.warn("Server unavailable. Skipping task setup.");
    }

    @SubscribeEvent
    @ApiStatus.Internal
    public static void onServerTick(ServerTickEvent.@NotNull Pre event) {
        int tick = event.getServer().getTickCount();
        List<Runnable> tasks = TASKS.get(tick);
        if (tasks == null) return;
        tasks.forEach(Runnable::run);
        TASKS.remove(tick);
    }
}
