package me.kall.duplicationless.util;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Optional;

public class RegistryEntries {
    public static Item item(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ITEMS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Item: " + id));
    }

    public static EntityType<?> entityType(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ENTITIES.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in EntityType: " + id));
    }

    public static Block block(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.BLOCKS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Block: " + id));
    }

    public static Enchantment enchantment(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ENCHANTMENTS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Enchantment: " + id));
    }
}
