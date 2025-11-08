package me.kall.duplicationless.network;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.NotNull;

import java.util.Optional;
import java.util.function.Supplier;

public class ParticlePacket {
    private final int entity;
    private final byte particle;

    public ParticlePacket(int entity, byte particle) {
        this.entity = entity;
        this.particle = particle;
    }

    private ParticlePacket(@NotNull FriendlyByteBuf buf) {
        this.entity = buf.readInt();
        this.particle = buf.readByte();
    }

    private void encode(@NotNull FriendlyByteBuf buf) {
        buf.writeInt(this.entity);
        buf.writeByte(this.particle);
    }

    private void handle(@NotNull Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            Minecraft minecraft = Minecraft.getInstance();
            ClientLevel level = minecraft.level;
            if (level == null) return;
            Entity target = level.getEntity(this.entity);
            if (target == null) return;
            ParticleOptions particleOptions = ParticleManager.PARTICLES.get(this.particle);
            if (particleOptions == null) return;
            minecraft.particleEngine.createTrackingEmitter(target, particleOptions);
        });
        context.setPacketHandled(true);
    }

    public static void register(@NotNull SimpleChannel instance, int id) {
        instance.registerMessage(id, ParticlePacket.class, ParticlePacket::encode, ParticlePacket::new, ParticlePacket::handle, Optional.of(NetworkDirection.PLAY_TO_CLIENT));
    }

    public static void send(@NotNull SimpleChannel instance, @NotNull Entity entity, byte particle) {
        instance.send(PacketDistributor.TRACKING_ENTITY.with(() -> entity), new ParticlePacket(entity.getId(), particle));
    }
}
