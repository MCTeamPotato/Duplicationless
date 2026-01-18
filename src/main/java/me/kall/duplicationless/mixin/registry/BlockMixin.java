package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.Block;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Block.class)
public abstract class BlockMixin implements RegistryEntry {
    @Unique
    private ResourceLocation registry$name;

    @Override
    public ResourceLocation registry$getName() {
        if (this.registry$name == null) {
            ResourceLocation id = BuiltInRegistries.BLOCK.getKeyOrNull((Block) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return this.registry$name;
    }
}
