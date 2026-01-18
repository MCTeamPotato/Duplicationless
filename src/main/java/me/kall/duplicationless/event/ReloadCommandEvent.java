package me.kall.duplicationless.event;

import net.minecraft.server.MinecraftServer;
import net.neoforged.bus.api.Event;

public class ReloadCommandEvent extends Event {
    public final MinecraftServer server;

    public ReloadCommandEvent(MinecraftServer server) {
        this.server = server;
    }
}
