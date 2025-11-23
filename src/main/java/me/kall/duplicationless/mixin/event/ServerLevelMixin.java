package me.kall.duplicationless.mixin.event;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import me.kall.duplicationless.event.BlockChangeEvent;
import me.kall.duplicationless.event.ChunkTickEvent;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.LevelChunk;
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

    @WrapMethod(method = "tickChunk")
    private void onChunkTick(LevelChunk chunk, int randomTickSpeed, Operation<Void> original) {
        ServerLevel level = (ServerLevel) (Object) this;
        MinecraftForge.EVENT_BUS.post(new ChunkTickEvent.Pre(chunk, level, randomTickSpeed));
        original.call(chunk, randomTickSpeed);
        MinecraftForge.EVENT_BUS.post(new ChunkTickEvent.Post(chunk, level, randomTickSpeed));
    }
}
