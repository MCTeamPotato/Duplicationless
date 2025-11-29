package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class ChunkData<DATA, TYPE> extends SavedData {
    public abstract @NotNull Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<DATA>>> data();

    public abstract boolean dataTrustable();
    public abstract @Nullable Predicate<TYPE> validation();

    public abstract BiFunction<DATA, ServerLevel, TYPE> dataToType();

    public abstract Function<DATA, Tag> dataToTag();
    public abstract Function<Tag, DATA> tagToData();
    public abstract int dataTagType();

    private @NotNull Long2ObjectMap<Set<DATA>> getLevelData(@NotNull ResourceLocation dim) {
        return this.data().computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
    }

    private @NotNull ResourceLocation dim(@NotNull ServerLevel level) {
        return level.dimension().location();
    }

    public void rebuild(ServerLevel level) {
        Predicate<TYPE> validation = this.validation();
        if (this.dataTrustable() || validation == null) return;

        ResourceLocation dim = dim(level);
        Long2ObjectMap<Set<DATA>> map = this.getLevelData(dim);

        Long2ObjectMap<List<DATA>> copy = new Long2ObjectOpenHashMap<>();
        for (Long2ObjectMap.Entry<Set<DATA>> entry : map.long2ObjectEntrySet()) {
            copy.put(entry.getLongKey(), new ObjectArrayList<>(entry.getValue()));
        }

        map.clear();

        BiFunction<DATA, ServerLevel, TYPE> function = this.dataToType();

        for (Long2ObjectMap.Entry<List<DATA>> entry : copy.long2ObjectEntrySet()) {
            long chunk = entry.getLongKey();
            for (DATA data : entry.getValue()) {
                TYPE type = function.apply(data, level);
                if (type == null) continue;
                if (!validation.test(type)) continue;
                this.add(level, chunk, data);
            }
        }
    }

    public void add(ServerLevel level, long chunk, DATA data) {
        if (this.getLevelData(dim(level)).computeIfAbsent(chunk, k -> new ObjectOpenHashSet<>()).add(data)) this.setDirty();
    }

    public void remove(ServerLevel level, long chunk, DATA data) {
        Long2ObjectMap<Set<DATA>> map = this.getLevelData(dim(level));
        Set<DATA> s = map.get(chunk);
        if (s != null && s.remove(data)) {
            if (s.isEmpty()) map.remove(chunk);
            this.setDirty();
        }
    }

    public boolean has(ServerLevel level, long chunk, DATA data) {
        Set<DATA> s = this.getLevelData(dim(level)).get(chunk);
        return s != null && s.contains(data);
    }

    public @NotNull Set<DATA> viewChunk(ServerLevel level, long chunk) {
        Set<DATA> s = this.getLevelData(dim(level)).get(chunk);
        return s == null || s.isEmpty() ? Collections.emptySet() : Collections.unmodifiableSet(s);
    }

    public @NotNull Optional<DATA> pick(ServerLevel level, long chunk) {
        Set<DATA> s = this.getLevelData(dim(level)).get(chunk);
        return s == null || s.isEmpty() || !s.iterator().hasNext() ? Optional.empty() : Optional.of(s.iterator().next());
    }

    @Override
    public @NotNull CompoundTag save(@NotNull CompoundTag tag) {
        ListTag dimList = new ListTag();

        Function<DATA, Tag> saveFunction = this.dataToTag();

        for (var dimEntry : this.data().object2ObjectEntrySet()) {
            CompoundTag dimTag = new CompoundTag();
            dimTag.putString("id", dimEntry.getKey().toString());

            ListTag chunkList = new ListTag();
            for (Long2ObjectMap.Entry<? extends Set<DATA>> entry : dimEntry.getValue().long2ObjectEntrySet()) {
                CompoundTag chunkTag = new CompoundTag();
                chunkTag.putLong("chunk", entry.getLongKey());

                ListTag dataList = new ListTag();
                for (DATA data : entry.getValue()) {
                    dataList.add(saveFunction.apply(data));
                }
                chunkTag.put("data", dataList);

                chunkList.add(chunkTag);
            }

            dimTag.put("chunks", chunkList);
            dimList.add(dimTag);
        }

        tag.put("dimensions", dimList);
        return tag;
    }

    public @NotNull ChunkData<DATA, TYPE> load(@NotNull CompoundTag tag) {
        this.data().clear();

        Function<Tag, DATA> loadFunction = this.tagToData();

        ListTag dimList = tag.getList("dimensions", Tag.TAG_COMPOUND);
        for (int d = 0; d < dimList.size(); d++) {
            CompoundTag dimTag = dimList.getCompound(d);
            ResourceLocation dim = ResourceLocation.parse(dimTag.getString("id"));

            Long2ObjectMap<Set<DATA>> map = this.getLevelData(dim);

            ListTag chunkList = dimTag.getList("chunks", Tag.TAG_COMPOUND);
            for (int i = 0; i < chunkList.size(); i++) {
                CompoundTag chunkTag = chunkList.getCompound(i);
                long chunk = chunkTag.getLong("chunk");

                ListTag dataList = chunkTag.getList("data", this.dataTagType());
                Set<DATA> set = new ObjectOpenHashSet<>();

                for (Tag dataTag : dataList) set.add(loadFunction.apply(dataTag));
                map.put(chunk, set);
            }
        }

        this.setDirty();
        return this;
    }

    public static <DATA, TYPE> @NotNull ChunkData<DATA, TYPE> get(@NotNull ServerLevel level, Supplier<ChunkData<DATA, TYPE>> constructor, String name) {
        return level.getDataStorage().computeIfAbsent(tag -> constructor.get().load(tag), constructor, name);
    }

    public static abstract class UUIDData extends ChunkData<UUID, Entity> {
        @Override public abstract @NotNull Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<UUID>>> data();
        @Override public abstract @Nullable Predicate<Entity> validation();

        @Override public BiFunction<UUID, ServerLevel, Entity> dataToType() {
            return (uuid, level) -> level.getEntity(uuid);
        }

        @Override public Function<UUID, Tag> dataToTag() {
            return uuid -> StringTag.valueOf(uuid.toString());
        }

        @Override public Function<Tag, UUID> tagToData() {
            return tag -> UUID.fromString(tag.getAsString());
        }

        @Override public int dataTagType() {
            return Tag.TAG_STRING;
        }
    }

    public static abstract class BlockData extends ChunkData<Long, BlockState> {
        @Override public abstract @NotNull Object2ObjectMap<ResourceLocation, Long2ObjectMap<Set<Long>>> data();
        @Override public abstract @Nullable Predicate<BlockState> validation();

        public static final long ZERO = BlockPos.ZERO.asLong();

        @Override public BiFunction<Long, ServerLevel, BlockState> dataToType() {
            return (pos, level) -> level.getBlockState(BlockPos.of(pos));
        }

        @Override public Function<Long, Tag> dataToTag() {
            return LongTag::valueOf;
        }

        @Override public Function<Tag, Long> tagToData() {
            return tag -> tag instanceof LongTag lg ? lg.getAsLong() : ZERO;
        }

        @Override public int dataTagType() {
            return Tag.TAG_LONG;
        }
    }
}
