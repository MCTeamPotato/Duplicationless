package me.kall.duplicationless.ext;

import me.kall.duplicationless.Duplicationless;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleType;
import net.minecraft.resources.Identifier;
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

    static Identifier get(@NotNull ParticleOptions particleOptions) {
        return get(particleOptions.getType());
    }

    static Identifier get(ParticleType<?> particleType) {
        return ((RegistryEntry)particleType).registry$getName();
    }

    static Identifier get(@NotNull MobEffectInstance effectInstance) {
        return get(effectInstance.getEffect().value());
    }

    static Identifier get(MobEffect effect) {
        return ((RegistryEntry)effect).registry$getName();
    }

    static Identifier get(@NotNull AttributeInstance attributeInstance) {
        return get(attributeInstance.getAttribute().value());
    }

    static Identifier get(Attribute attribute) {
        return ((RegistryEntry)attribute).registry$getName();
    }
}
