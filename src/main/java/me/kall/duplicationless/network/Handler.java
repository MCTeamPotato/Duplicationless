package me.kall.duplicationless.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class Handler implements CustomPacketPayload {
    public void handle(@NotNull IPayloadContext context) {
        context.enqueueWork(() -> {
            if (context.player() instanceof ServerPlayer player) {
                this.handle(player, player.level());
            } else {
                this.handle(null, null);
            }
        });
    }

    public abstract void save(FriendlyByteBuf buffer);

    public abstract void handle(@Nullable ServerPlayer player, @Nullable ServerLevel level);
}
