package me.kall.duplicationless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Handler implements CustomPacketPayload {
    private final ThreadLocal<IPayloadContext> context = new ThreadLocal<>();

    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            this.context.set(context);
            if (context.player() instanceof ServerPlayer player) {
                this.handle(player, player.serverLevel());
            } else {
                this.handle(null, null);
            }
            this.context.remove();
        });
    }

    public abstract void save(FriendlyByteBuf buffer);

    public abstract void handle(@Nullable ServerPlayer player, @Nullable ServerLevel level);
}
