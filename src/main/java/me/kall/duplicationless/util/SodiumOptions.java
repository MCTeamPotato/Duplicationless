package me.kall.duplicationless.util;

import com.google.common.collect.ImmutableList;
import me.jellysquid.mods.sodium.client.gui.SodiumGameOptions;
import me.jellysquid.mods.sodium.client.gui.options.OptionGroup;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpact;
import me.jellysquid.mods.sodium.client.gui.options.OptionImpl;
import me.jellysquid.mods.sodium.client.gui.options.OptionPage;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.CyclingControl;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import me.jellysquid.mods.sodium.client.gui.options.control.TickBoxControl;
import me.jellysquid.mods.sodium.client.gui.options.storage.SodiumOptionsStorage;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.network.chat.TranslatableComponent;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Supplier;

public class SodiumOptions {
    private static final SodiumOptionsStorage STORAGE = new SodiumOptionsStorage();
    private static final Component EMPTY_COMPONENT = TextComponent.EMPTY;

    public static <T extends Enum<T>> OptionImpl<SodiumGameOptions, T> enumOption(Class<T> enumType, String nameKey, Supplier<T> getter, Consumer<T> setter) {
        return SodiumOptions.enumOption(enumType, nameKey, false, getter, setter, null);
    }

    public static <T extends Enum<T>> OptionImpl<SodiumGameOptions, T> enumOption(Class<T> enumType, String nameKey, boolean tooltip, Supplier<T> getter, Consumer<T> setter, @Nullable OptionImpact impact) {
        OptionImpl.Builder<SodiumGameOptions, T> builder = OptionImpl.createBuilder(enumType, STORAGE)
                .setName(new TranslatableComponent(nameKey))
                .setTooltip(tooltip ? new TranslatableComponent(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(option -> new CyclingControl<>(option, enumType))
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionImpl<SodiumGameOptions, Boolean> boolOption(String nameKey, Supplier<Boolean> getter, Consumer<Boolean> setter) {
        return SodiumOptions.boolOption(nameKey, false, getter, setter, null);
    }

    public static OptionImpl<SodiumGameOptions, Boolean> boolOption(@NotNull String nameKey, boolean tooltip, Supplier<Boolean> getter, Consumer<Boolean> setter, @Nullable OptionImpact impact) {
        OptionImpl.Builder<SodiumGameOptions, Boolean> builder = OptionImpl.createBuilder(Boolean.TYPE, STORAGE)
                .setName(new TranslatableComponent(nameKey))
                .setTooltip(tooltip ? new TranslatableComponent(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(TickBoxControl::new)
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionImpl<SodiumGameOptions, Integer> intOption(String nameKey, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max, int interval) {
        return SodiumOptions.intOption(nameKey, false, getter, setter, min, max, interval, null);
    }

    public static OptionImpl<SodiumGameOptions, Integer> intOption(@NotNull String nameKey, boolean tooltip, Supplier<Integer> getter, Consumer<Integer> setter, int min, int max, int interval, @Nullable OptionImpact impact) {
        OptionImpl.Builder<SodiumGameOptions, Integer> builder = OptionImpl.createBuilder(Integer.TYPE, STORAGE)
                .setName(new TranslatableComponent(nameKey))
                .setTooltip(tooltip ? new TranslatableComponent(nameKey + ".tooltip") : EMPTY_COMPONENT)
                .setControl(option -> new SliderControl(option, min, max, interval, ControlValueFormatter.number()))
                .setBinding((options, value) -> setter.accept(value), options -> getter.get());
        if (impact != null) builder.setImpact(impact);
        return builder.build();
    }

    public static OptionGroup newGroup(String groupID, OptionImpl<?, ?> @NotNull ... options) {
        OptionGroup.Builder builder = OptionGroup.createBuilder();
        for (OptionImpl<?, ?> option : options) builder.add(option);
        return builder.build();
    }

    @Contract("_, _ -> new")
    public static @NotNull OptionPage newPage(String nameKey, OptionGroup... groups) {
        return new OptionPage(new TranslatableComponent(nameKey), ImmutableList.copyOf(groups));
    }
}
