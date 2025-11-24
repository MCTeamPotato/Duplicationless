package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectFunction;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2LongFunction;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class ChunkData<DATA, TYPE> extends SavedData {
    public abstract @NotNull Long2ObjectMap<Set<DATA>> data();
    public abstract boolean dataTrustable();
    public abstract @Nullable Predicate<TYPE> validation();

    public abstract BiFunction<DATA, ServerLevel, TYPE> dataToType();

    public abstract Function<DATA, Tag> dataToTag();
    public abstract Function<Tag, DATA> tagToData();
    public abstract int dataTagType();

    public void rebuild(ServerLevel level) {
        Predicate<TYPE> validation = this.validation();
        if (this.dataTrustable() || validation == null) return;

        Long2ObjectMap<Set<DATA>> copy = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<Set<DATA>> entry : this.data().long2ObjectEntrySet()) {
            copy.put(entry.getLongKey(), new ObjectOpenHashSet<>(entry.getValue()));
        }

        this.clear();

        for (Long2ObjectMap.Entry<Set<DATA>> entry : copy.long2ObjectEntrySet()) {
            long chunk = entry.getLongKey();
            for (DATA data : entry.getValue()) {
                TYPE type = dataToType().apply(data, level);
                if (type == null) return;
                if (!validation.test(type)) continue;
                this.add(chunk, data);
            }
        }
    }

    public void add(long chunk, DATA data) {
        if (this.data().computeIfAbsent(chunk, key -> new ObjectOpenHashSet<>()).add(data)) this.setDirty();
    }

    public void remove(long chunk, DATA data) {
        Set<DATA> dataSet = this.data().get(chunk);
        if (dataSet != null && dataSet.remove(data)) {
            if (dataSet.isEmpty()) this.data().remove(chunk);
            this.setDirty();
        }
    }

    public void clear() {
        this.data().clear();
        this.setDirty();
    }

    public @NotNull @UnmodifiableView Set<DATA> viewChunk(long chunk) {
        Set<DATA> dataSet = this.data().get(chunk);
        return dataSet == null || dataSet.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(dataSet);
    }

    public boolean has(long chunk, DATA data) {
        Set<DATA> dataSet = this.data().get(chunk);
        return dataSet != null && dataSet.contains(data);
    }

    public @NotNull Optional<DATA> pick(long chunk) {
        Set<DATA> dataSet = this.data().get(chunk);
        return dataSet == null || dataSet.isEmpty() ? Optional.empty() : Optional.of(dataSet.iterator().next());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag chunkList = new ListTag();
        for (Long2ObjectMap.Entry<? extends Set<DATA>> entry : this.data().long2ObjectEntrySet()) {
            CompoundTag chunkTag = new CompoundTag();
            chunkTag.putLong("chunk", entry.getLongKey());

            ListTag dataList = new ListTag();
            for (DATA data : entry.getValue()) {
                dataList.add(this.dataToTag().apply(data));
            }
            chunkTag.put("data", dataList);

            chunkList.add(chunkTag);
        }

        tag.put("chunks", chunkList);
        return tag;
    }

    public @NotNull ChunkData<DATA, TYPE> load(@NotNull CompoundTag tag) {
        this.clear();

        ListTag chunkList = tag.getList("chunks", Tag.TAG_COMPOUND);
        for (int i = 0; i < chunkList.size(); i++) {
            CompoundTag chunkTag = chunkList.getCompound(i);
            long chunk = chunkTag.getLong("chunk");

            ListTag dataList = chunkTag.getList("data", this.dataTagType());
            Set<DATA> dataSet = new ObjectOpenHashSet<>();

            for (Tag dataTag : dataList) {
                dataSet.add(this.tagToData().apply(dataTag));
            }

            this.data().put(chunk, dataSet);
        }

        return this;
    }

    public static <DATA, TYPE> @NotNull ChunkData<DATA, TYPE> get(@NotNull ServerLevel level, Supplier<ChunkData<DATA, TYPE>> constructor, String name) {
        return level.getDataStorage().computeIfAbsent(tag -> constructor.get().load(tag), constructor, name);
    }


    public static abstract class UUIDData extends ChunkData<UUID, Entity> {
        @Override public abstract @NotNull Long2ObjectMap<Set<UUID>> data();
        @Override public abstract @Nullable Predicate<Entity> validation();

        @Override
        public BiFunction<UUID, ServerLevel, Entity> dataToType() {
            return (uuid, level) -> level.getEntity(uuid);
        }

        @Override
        public Function<UUID, Tag> dataToTag() {
            return uuid -> StringTag.valueOf(uuid.toString());
        }

        @Override
        public Function<Tag, UUID> tagToData() {
            return tag -> UUID.fromString(tag.getAsString());
        }

        @Override
        public int dataTagType() {
            return Tag.TAG_STRING;
        }
    }

    public static abstract class BlockData extends ChunkData<Long, BlockState> {
        @Override public abstract @NotNull Long2ObjectMap<Set<Long>> data();
        @Override public abstract @Nullable Predicate<BlockState> validation();

        public static final long ZERO = BlockPos.ZERO.asLong();

        @Override
        public BiFunction<Long, ServerLevel, BlockState> dataToType() {
            return (pos, level) -> level.getBlockState(BlockPos.of(pos));
        }

        @Override
        public Long2ObjectFunction<Tag> dataToTag() {
            return LongTag::valueOf;
        }

        @Override
        public Object2LongFunction<Tag> tagToData() {
            return tag -> tag instanceof LongTag longTag ? longTag.getAsLong() : ZERO;
        }

        @Override
        public int dataTagType() {
            return Tag.TAG_LONG;
        }
    }
}
