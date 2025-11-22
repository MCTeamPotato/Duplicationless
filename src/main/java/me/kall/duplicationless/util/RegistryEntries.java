package me.kall.duplicationless.util;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.Optional;

public class RegistryEntries {
    public static Item item(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Item: " + id));
    }

    public static EntityType<?> entityType(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ENTITY_TYPES.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in EntityType: " + id));
    }

    public static Block block(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Block: " + id));
    }

    public static Holder<Enchantment> enchantment(ResourceKey<Enchantment> id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Minecraft server is not running, cannot get Enchantment: " + id);
        return server.registryAccess().registry(Registries.ENCHANTMENT).flatMap(registry -> registry.getHolder(id)).orElseThrow(() -> new IllegalStateException("Missing key in Enchantment: " + id));
    }
}
