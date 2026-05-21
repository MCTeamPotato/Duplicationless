package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ai.attributes.Attribute;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Attribute.class)
public abstract class AttributeMixin implements RegistryEntry {
    @Unique private Identifier registry$name;

    @Override
    public Identifier registry$getName() {
        if (this.registry$name == null) {
            Identifier id = BuiltInRegistries.ATTRIBUTE.getKey((Attribute) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return this.registry$name;
    }
}
