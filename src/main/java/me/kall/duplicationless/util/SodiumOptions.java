package me.kall.duplicationless.util;

import com.google.common.collect.ImmutableList;
import me.kall.duplicationless.Duplicationless;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.embeddedt.embeddium.api.options.OptionIdentifier;
import org.embeddedt.embeddium.api.options.control.ControlValueFormatter;
import org.embeddedt.embeddium.api.options.control.CyclingControl;
import org.embeddedt.embeddium.api.options.control.SliderControl;
import org.embeddedt.embeddium.api.options.control.TickBoxControl;
import org.embeddedt.embeddium.api.options.structure.*;
import org.embeddedt.embeddium.impl.gui.EmbeddiumOptions;
import org.embeddedt.embeddium.impl.gui.options.storage.EmbeddiumOptionsStorage;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SodiumOptions {
    private static final OptionStorage<EmbeddiumOptions> STORAGE = new EmbeddiumOptionsStorage();
    private static final Component EMPTY_COMPONENT = Component.empty();

    public static <T extends Enum<T>> OptionImpl<EmbeddiumOptions, T> enumOption(Class<T> enumType, String nameKey, Supplier<T> getter, Consumer<T> setter) {
        return SodiumOptions.enumOption(enumType, nameKey, false, getter, setter, null);
    }

    public static <T extends Enum<T>> OptionImpl<EmbeddiumOptions, T> enumOption(Class<T> enumType, String nameKey, boolean tooltip, Supplier<T> getter, Consumer<T> setter, @Nullable OptionImpact impact) {
        OptionImpl.Builder<EmbeddiumOptions, T> builder = OptionImpl.createBuilder(enumType, STORAGE)
                .setId(SodiumOptions.loc(nameKey))
                .setName(Component.translatable(nameKey))
                .setTooltip(tooltip ? Component.translatable(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(option -> new CyclingControl<>(option, enumType))
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionImpl<EmbeddiumOptions, Boolean> boolOption(String nameKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return SodiumOptions.boolOption(nameKey, false, getter, setter, null);
    }

    public static OptionImpl<EmbeddiumOptions, Boolean> boolOption(@NotNull String nameKey, boolean tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter, @Nullable OptionImpact impact) {
        OptionImpl.Builder<EmbeddiumOptions, Boolean> builder = OptionImpl.createBuilder(Boolean.TYPE, STORAGE)
                .setId(SodiumOptions.loc(nameKey))
                .setName(Component.translatable(nameKey))
                .setTooltip(tooltip ? Component.translatable(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(TickBoxControl::new)
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionImpl<EmbeddiumOptions, Integer> intOption(String nameKey, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max, int interval) {
        return SodiumOptions.intOption(nameKey, false, getter, setter, min, max, interval, null);
    }

    public static OptionImpl<EmbeddiumOptions, Integer> intOption(@NotNull String nameKey, boolean tooltip, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max, int interval, @Nullable OptionImpact impact) {
        OptionImpl.Builder<EmbeddiumOptions, Integer> builder = OptionImpl.createBuilder(Integer.TYPE, STORAGE)
                .setId(SodiumOptions.loc(nameKey))
                .setName(Component.translatable(nameKey))
                .setTooltip(tooltip ? Component.translatable(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(option -> new SliderControl(option, min, max, interval, ControlValueFormatter.number()))
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionGroup newGroup(String groupID, OptionImpl<?, ?> @NotNull ... options) {
        OptionGroup.Builder builder = OptionGroup.createBuilder().setId(SodiumOptions.loc(groupID));
        for (OptionImpl<?, ?> option : options) builder.add(option);
        return builder.build();
    }

    @Contract("_, _ -> new")
    public static @NotNull OptionPage newPage(String nameKey, OptionGroup... groups) {
        return new OptionPage(OptionIdentifier.create(SodiumOptions.loc(nameKey)), Component.translatable(nameKey), ImmutableList.copyOf(groups));
    }

    @Contract("_ -> new")
    private static @NotNull ResourceLocation loc(@NotNull String nameKey) {
        return ResourceLocation.fromNamespaceAndPath(Duplicationless.MOD_ID, nameKey.replace('.', '_'));
    }
}
