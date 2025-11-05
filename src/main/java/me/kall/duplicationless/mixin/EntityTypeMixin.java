package me.kall.duplicationless.mixin;

import me.kall.duplicationless.ext.IEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.registries.ForgeRegistries;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Optional;

@Mixin(EntityType.class)
public class EntityTypeMixin implements IEntityType {
    @Unique private ResourceLocation registry$name;

    @Override
    public ResourceLocation registry$getName() {
        if (this.registry$name == null) this.registry$name = Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getKey((EntityType<?>) (Object) this)).orElse(IEntityType.NONE);
        return this.registry$name;
    }
}