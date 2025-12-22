package me.kall.duplicationless.mixin.event;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import net.minecraft.core.SectionPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraftforge.common.MinecraftForge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract ChunkPos chunkPosition();
    @Shadow public abstract double getY();

    @Inject(method = "setPosRaw", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/ChunkPos;<init>(Lnet/minecraft/core/BlockPos;)V"))
    private void beforeChunkPosUpdate(CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new EntityChunkChangeEvent.Before((Entity) (Object) this));
    }

    @WrapMethod(method = "setPosRaw")
    private void onChunkUpdate(double x, double y, double z, @NotNull Operation<Void> original) {
        Entity entity = (Entity) (Object) this;
        final long chunkBefore = this.chunkPosition().toLong();

        boolean isSectionChange = SectionPos.blockToSectionCoord(this.getY()) != SectionPos.blockToSectionCoord(y);
        if (isSectionChange) MinecraftForge.EVENT_BUS.post(new EntityChunkChangeEvent.Section.Before(entity));

        original.call(x, y, z);

        if (isSectionChange) MinecraftForge.EVENT_BUS.post(new EntityChunkChangeEvent.Section.After(entity));

        final long chunkAfter = this.chunkPosition().toLong();
        if (chunkBefore == chunkAfter) return;
        MinecraftForge.EVENT_BUS.post(new EntityChunkChangeEvent.After(entity));
    }
}
