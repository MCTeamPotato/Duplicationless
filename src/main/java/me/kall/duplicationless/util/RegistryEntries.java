package me.kall.duplicationless.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class RegistryEntries {
    public static Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Item: " + id));
    }

    public static EntityType<?> entityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in EntityType: " + id));
    }

    public static Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Block: " + id));
    }

    public static Enchantment enchantment(ResourceLocation id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Server unavailable. Cannot read enchantment " + id.toString());
        return server.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Enchantment: " + id));
    }
}
