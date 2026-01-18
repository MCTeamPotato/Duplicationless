package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemMixin implements RegistryEntry {
    @Unique
    private ResourceLocation registry$name;

    @Override
    public ResourceLocation registry$getName() {
        if (this.registry$name == null) {
            ResourceLocation id = BuiltInRegistries.ITEM.getKeyOrNull((Item) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return this.registry$name;
    }
}
