package me.kall.duplicationless.mixin;

import me.kall.duplicationless.event.BlockChangeEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerLevel.class)
public abstract class ServerLevelMixin {
    @Inject(method = "onBlockStateChange", at = @At("TAIL"))
    private void onBlockChange(BlockPos pos, BlockState blockState, BlockState newState, CallbackInfo ci) {
        MinecraftForge.EVENT_BUS.post(new BlockChangeEvent((ServerLevel) (Object) this, blockState, newState, pos));
    }
}
