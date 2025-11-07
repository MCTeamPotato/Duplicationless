package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.longs.*;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.OptionalLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class BlockStorage extends SavedData {
    protected abstract @NotNull Long2ObjectMap<LongSet> positions();
    protected abstract boolean dataTrustable();
    protected abstract @Nullable Predicate<Block> validation();

    public void rebuild(@NotNull ServerLevel level) {
        Predicate<Block> validation = this.validation();
        if (this.dataTrustable() || validation == null) return;

        Long2ObjectMap<LongSet> copy = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<LongSet> entry : this.positions().long2ObjectEntrySet()) {
            copy.put(entry.getLongKey(), new LongOpenHashSet(entry.getValue()));
        }

        this.clear();

        for (Long2ObjectMap.Entry<LongSet> entry : copy.long2ObjectEntrySet()) {
            long chunk = entry.getLongKey();
            for (long pos : entry.getValue()) {
                if (!validation.test(level.getBlockState(BlockPos.of(pos)).getBlock())) continue;
                this.add(chunk, pos);
            }
        }
    }

    public void add(long chunk, long block) {
        if (this.positions().computeIfAbsent(chunk, c -> new LongOpenHashSet()).add(block)) this.setDirty();
    }

    public void remove(long chunk, long block) {
        LongSet blocks = this.positions().get(chunk);
        if (blocks == null || blocks.isEmpty()) return;
        if (!blocks.remove(block)) return;
        if (blocks.isEmpty()) this.positions().remove(chunk);
        this.setDirty();
    }

    public void clear() {
        this.positions().clear();
        this.setDirty();
    }

    public @NotNull @UnmodifiableView LongSet viewChunk(long chunk) {
        LongSet blocks = this.positions().get(chunk);
        return blocks == null || blocks.isEmpty() ? LongSets.emptySet() : LongSets.unmodifiable(blocks);
    }

    public boolean has(long chunk, long block) {
        return this.positions().getOrDefault(chunk, LongSets.emptySet()).contains(block);
    }

    public @NotNull OptionalLong pick(long chunk) {
        LongSet blocks = this.positions().get(chunk);
        return blocks == null ? OptionalLong.empty() : OptionalLong.of(blocks.iterator().nextLong());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag chunkList = new ListTag();

        for (Long2ObjectMap.Entry<LongSet> entry : this.positions().long2ObjectEntrySet()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("chunk", entry.getLongKey());
            chunkTag.putLongArray("blocks", entry.getValue().toLongArray());
            chunkList.add(chunkTag);
        }

        tag.put("positions", chunkList);
        return tag;
    }

    public @NotNull BlockStorage load(@NotNull CompoundTag tag) {
        this.clear();

        ListTag chunkList = tag.getList("positions", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkList.size(); i++) {
            CompoundTag chunkTag = chunkList.getCompound(i);
            this.positions().put(chunkTag.getLong("chunk"), new LongOpenHashSet(chunkTag.getLongArray("blocks")));
        }

        return this;
    }

    public static @NotNull BlockStorage get(@NotNull ServerLevel level, Supplier<BlockStorage> constructor, String name) {
        return level.getDataStorage().computeIfAbsent(tag -> constructor.get().load(tag), constructor, name);
    }
}
