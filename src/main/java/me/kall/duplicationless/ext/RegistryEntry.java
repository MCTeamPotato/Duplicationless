package me.kall.duplicationless.ext;

import me.kall.duplicationless.Duplicationless;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.NotNull;

public interface RegistryEntry {
    ResourceLocation registry$getName();

    ResourceLocation NONE = new ResourceLocation(Duplicationless.MOD_ID, "none");

    static ResourceLocation get(@NotNull Entity entity) {
        return get(entity.getType());
    }

    static ResourceLocation get(EntityType<?> type) {
        return ((RegistryEntry)type).registry$getName();
    }

    static ResourceLocation get(@NotNull ItemStack stack) {
        return get(stack.getItem());
    }

    static ResourceLocation get(Item item) {
        return ((RegistryEntry)item).registry$getName();
    }

    static ResourceLocation get(@NotNull BlockState state) {
        return get(state.getBlock());
    }

    static ResourceLocation get(Block block) {
        return ((RegistryEntry)block).registry$getName();
    }

    static ResourceLocation get(@NotNull ParticleOptions particleOptions) {
        return get(particleOptions.getType());
    }

    static ResourceLocation get(ParticleType<?> particleType) {
        return ((RegistryEntry)particleType).registry$getName();
    }

    static ResourceLocation get(@NotNull MobEffectInstance effectInstance) {
        return get(effectInstance.getEffect());
    }

    static ResourceLocation get(MobEffect effect) {
        return ((RegistryEntry)effect).registry$getName();
    }

    static ResourceLocation get(@NotNull AttributeInstance attributeInstance) {
        return get(attributeInstance.getAttribute());
    }

    static ResourceLocation get(Attribute attribute) {
        return ((RegistryEntry)attribute).registry$getName();
    }
}
