package me.kall.duplicationless.ext;

import me.kall.duplicationless.Duplicationless;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface RegistryEntry {
    Identifier registry$getName();

    Identifier NONE = Identifier.fromNamespaceAndPath(Duplicationless.MOD_ID, "none");

    static Identifier get(@NotNull Entity entity) {
        return get(entity.getType());
    }

    static Identifier get(EntityType<?> type) {
        return ((RegistryEntry)type).registry$getName();
    }

    static Identifier get(@NotNull ItemStack stack) {
        return get(stack.getItem());
    }

    static Identifier get(Item item) {
        return ((RegistryEntry)item).registry$getName();
    }

    static Identifier get(@NotNull BlockState state) {
        return get(state.getBlock());
    }

    static Identifier get(Block block) {
        return ((RegistryEntry)block).registry$getName();
    }
}
