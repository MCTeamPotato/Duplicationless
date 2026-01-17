package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(EntityType.class)
public abstract class EntityTypeMixin implements RegistryEntry {
    @Unique private ResourceLocation registry$name;

    @Override
    public ResourceLocation registry$getName() {
        if (this.registry$name == null) {
            ResourceLocation id = ForgeRegistries.ENTITIES.getKey((EntityType<?>) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return this.registry$name;
    }
}