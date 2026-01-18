package me.kall.duplicationless.mixin.event;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import me.kall.duplicationless.util.Positions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(Entity.class)
public abstract class EntityMixin {
    @Shadow public abstract double getY();
    @Shadow public abstract BlockPos blockPosition();

    @WrapMethod(method = "setPosRaw")
    private void onChunkUpdate(double x, double y, double z, @NotNull Operation<Void> original) {
        final Entity entity = (Entity) (Object) this;
        try {
            final long previousChunk = Positions.toChunk(this.blockPosition());
            final long nextChunk = Positions.toChunk(x, z);
            final boolean differentChunk = previousChunk != nextChunk;

            boolean isSectionChange = SectionPos.blockToSectionCoord(Mth.floor(this.getY())) != SectionPos.blockToSectionCoord(Mth.floor(y));

            if (isSectionChange) NeoForge.EVENT_BUS.post(new EntityChunkChangeEvent.Section.Before(entity));
            if (differentChunk) NeoForge.EVENT_BUS.post(new EntityChunkChangeEvent.Before(entity));
            original.call(x, y, z);

            if (isSectionChange) NeoForge.EVENT_BUS.post(new EntityChunkChangeEvent.Section.After(entity));
            if (differentChunk) NeoForge.EVENT_BUS.post(new EntityChunkChangeEvent.After(entity));
        } catch (Throwable ignored) {}
    }
}