package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(ParticleType.class)
public abstract class ParticleTypeMixin implements RegistryEntry {
    @Unique private ResourceLocation registry$name;

    @Override
    public ResourceLocation registry$getName() {
        if (this.registry$name == null) {
            ResourceLocation id = ForgeRegistries.PARTICLE_TYPES.getKey((ParticleType<?>) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return null;
    }
}
