package me.kall.duplicationless.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class RegistryEntries {
    public static @NotNull Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Item: " + id));
    }

    public static @NotNull EntityType<?> entityType(Identifier id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in EntityType: " + id));
    }

    public static @NotNull Block block(Identifier id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Block: " + id));
    }

    public static @NotNull Enchantment enchantment(Identifier id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Server unavailable. Cannot read enchantment " + id.toString());
        return server.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Enchantment: " + id));
    }
}
