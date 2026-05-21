package me.kall.duplicationless.util;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Block;
import org.jetbrains.annotations.NotNull;
import net.neoforged.neoforge.server.ServerLifecycleHooks;

public class RegistryEntries {
    public static @NotNull Item item(ResourceLocation id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Item: " + id));
    }

    public static @NotNull EntityType<?> entityType(ResourceLocation id) {
        return BuiltInRegistries.ENTITY_TYPE.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in EntityType: " + id));
    }

    public static @NotNull Block block(ResourceLocation id) {
        return BuiltInRegistries.BLOCK.getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Block: " + id));
    }

    public static @NotNull Enchantment enchantment(ResourceLocation id) {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) throw new IllegalStateException("Server unavailable. Cannot read enchantment " + id.toString());
        return server.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getOptional(id).orElseThrow(() -> new IllegalStateException("Missing key in Enchantment: " + id));
    }

    public static @NotNull ParticleType<?> particle(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.PARTICLE_TYPES.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in ParticleType: " + id));
    }

    public static @NotNull MobEffect effect(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.MOB_EFFECTS.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in MobEffect: " + id));
    }

    public static @NotNull Attribute attribute(ResourceLocation id) {
        return Optional.ofNullable(ForgeRegistries.ATTRIBUTES.getValue(id)).orElseThrow(() -> new IllegalStateException("Missing key in Attribute: " + id));
    }
}
