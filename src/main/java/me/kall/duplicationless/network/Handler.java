package me.kall.duplicationless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Supplier;

public abstract class Handler {
    protected final ThreadLocal<NetworkEvent.Context> context = new ThreadLocal<>();

    public void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        contextSupplier.get().setPacketHandled(true);
        contextSupplier.get().enqueueWork(() -> {
            ServerPlayer player = contextSupplier.get().getSender();
            this.context.set(contextSupplier.get());
            this.handle(player, player == null ? null : player.serverLevel());
            this.context.remove();
        });
    }

    public abstract void save(FriendlyByteBuf buffer);

    public abstract void handle(@Nullable ServerPlayer player, @Nullable ServerLevel level);
}
