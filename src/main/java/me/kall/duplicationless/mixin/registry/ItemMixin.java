package me.kall.duplicationless.mixin.registry;

import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(Item.class)
public abstract class ItemMixin implements RegistryEntry {
    @Unique private Identifier registry$name;

    @Override
    public Identifier registry$getName() {
        if (this.registry$name == null) {
            Identifier id = BuiltInRegistries.ITEM.getKeyOrNull((Item) (Object) this);
            this.registry$name = id == null ? RegistryEntry.NONE : id;
        }
        return this.registry$name;
    }
}
