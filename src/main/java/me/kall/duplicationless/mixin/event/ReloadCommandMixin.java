package me.kall.duplicationless.mixin.event;

import me.kall.duplicationless.event.ReloadCommandEvent;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.commands.ReloadCommand;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collection;

@Mixin(ReloadCommand.class)
public abstract class ReloadCommandMixin {
    @Inject(method = "reloadPacks", at = @At("RETURN"))
    private static void reload(Collection<String> selectedIds, @NotNull CommandSourceStack source, CallbackInfo ci) {
        NeoForge.EVENT_BUS.post(new ReloadCommandEvent(source.getServer()));
    }
}
