package me.kall.duplicationless.data;

import com.mojang.datafixers.util.Pair;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.DynamicOps;
import it.unimi.dsi.fastutil.longs.*;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import me.kall.duplicationless.util.Executor;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.*;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public abstract class ChunkData<DATA, TYPE> extends SavedData {
    public abstract @NotNull Object2ObjectMap<Identifier, Long2ObjectMap<Set<DATA>>> data();

    public abstract boolean dataTrustable();
    public abstract @Nullable Predicate<TYPE> validation();

    public abstract BiFunction<DATA, ServerLevel, TYPE> dataToType();

    public abstract Function<DATA, Tag> dataToTag();
    public abstract Function<Tag, DATA> tagToData();

    private final LongSet rebuiltChunks = new LongOpenHashSet();

    private @NotNull Long2ObjectMap<Set<DATA>> getLevelData(@NotNull Identifier dim) {
        return this.data().computeIfAbsent(dim, d -> new Long2ObjectOpenHashMap<>());
    }

    private @NotNull Identifier dim(@NotNull ServerLevel level) {
        return level.dimension().identifier();
    }

    public void rebuild(ServerLevel level) {
        Predicate<TYPE> validation = this.validation();
        if (this.dataTrustable() || validation == null) return;

        Identifier dim = dim(level);
        Long2ObjectMap<Set<DATA>> map = this.getLevelData(dim);

        Long2ObjectMap<List<DATA>> copy = new Long2ObjectArrayMap<>(map.size());

        ObjectIterator<Long2ObjectMap.Entry<Set<DATA>>> origin = Long2ObjectMaps.fastIterator(map);

        while (origin.hasNext()) {
            Long2ObjectMap.Entry<Set<DATA>> entry = origin.next();
            copy.put(entry.getLongKey(), new ObjectArrayList<>(entry.getValue()));
        }

        map.clear();

        BiFunction<DATA, ServerLevel, TYPE> function = this.dataToType();

        ObjectIterator<Long2ObjectMap.Entry<List<DATA>>> copied = Long2ObjectMaps.fastIterator(copy);

        while (copied.hasNext()) {
            Long2ObjectMap.Entry<List<DATA>> entry = copied.next();
            long chunk = entry.getLongKey();
            List<DATA> dataList = entry.getValue();

            //noinspection ForLoopReplaceableByForEach
            for (int i = 0; i < dataList.size(); i++) {
                DATA data = dataList.get(i);
                TYPE type = function.apply(data, level);

                if (type != null && validation.test(type)) {
                    this.add(level, chunk, data);
                }
            }
        }
    }

    public void rebuildChunk(ServerLevel level, @NotNull ChunkPos chunkPos) {
        int chunkX = chunkPos.x;
        int chunkZ = chunkPos.z;
        long chunk = chunkPos.toLong();

        Predicate<TYPE> validation = this.validation();
        BiFunction<DATA, ServerLevel, TYPE> function = this.dataToType();
        Identifier dim = dim(level);

        if (this.dataTrustable() || validation == null) return;

        Runnable rebuildTask = new Runnable() {
            private int tries;

            @Override
            public void run() {
                if (tries >= 20) return;
                if (level.hasChunk(chunkX, chunkZ)) {
                    if (!rebuiltChunks.contains(chunk)) {
                        rebuiltChunks.add(chunk);
                        Long2ObjectMap<Set<DATA>> chunks = getLevelData(dim);
                        if (chunks.isEmpty()) return;

                        Set<DATA> dataSet = chunks.get(chunk);
                        if (dataSet == null || dataSet.isEmpty()) return;

                        List<DATA> copy = new ObjectArrayList<>(dataSet);

                        dataSet.clear();

                        for (DATA data : copy) {
                            TYPE type = function.apply(data, level);
                            if (type != null && validation.test(type)) {
                                add(level, chunk, data);
                            }
                        }
                    }
                } else {
                    tries++;
                    Executor.runAfter(1, this);
                }
            }
        };

        Executor.run(rebuildTask);
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

        ListTag dimList = tag.getList("dimensions").orElseThrow();
        for (int d = 0; d < dimList.size(); d++) {
            CompoundTag dimTag = dimList.getCompound(d).orElseThrow();
            Identifier dim = Identifier.parse(dimTag.getString("id").orElseThrow());

            Long2ObjectMap<Set<DATA>> map = this.getLevelData(dim);

            ListTag chunkList = dimTag.getList("chunks").orElseThrow();
            for (int i = 0; i < chunkList.size(); i++) {
                CompoundTag chunkTag = chunkList.getCompound(i).orElseThrow();
                long chunk = chunkTag.getLong("chunk").orElseThrow();

                ListTag dataList = chunkTag.getList("data").orElseThrow();
                Set<DATA> set = new ObjectOpenHashSet<>();

                for (Tag dataTag : dataList) set.add(loadFunction.apply(dataTag));
                map.put(chunk, set);
            }
        }

        this.setDirty();
        return this;
    }

    public static <DATA, TYPE> @NotNull ChunkData<DATA, TYPE> get(@NotNull ServerLevel level, Supplier<ChunkData<DATA, TYPE>> constructor, String name) {
        return level.getDataStorage().computeIfAbsent(new SavedDataType<>(name, constructor, new Codec<>() {
            @Override
            public <T> DataResult<Pair<ChunkData<DATA, TYPE>, T>> decode(DynamicOps<T> ops, T input) {
                return DataResult.success(Pair.of(constructor.get().load((CompoundTag) ops.convertTo(NbtOps.INSTANCE, input)), input));
            }

            @Override
            @SuppressWarnings("unchecked")
            public <T> DataResult<T> encode(ChunkData<DATA, TYPE> input, DynamicOps<T> ops, T prefix) {
                return DataResult.success((T) input.save(new CompoundTag()));
            }
        }));
    }

    public static abstract class UUIDData extends ChunkData<UUID, Entity> {
        @Override public abstract @NotNull Object2ObjectMap<Identifier, Long2ObjectMap<Set<UUID>>> data();
        @Override public abstract @Nullable Predicate<Entity> validation();

        @Override public BiFunction<UUID, ServerLevel, Entity> dataToType() {
            return (uuid, level) -> level.getEntity(uuid);
        }

        @Override public Function<UUID, Tag> dataToTag() {
            return uuid -> StringTag.valueOf(uuid.toString());
        }

        @Override public Function<Tag, UUID> tagToData() {
            return tag -> UUID.fromString(tag.asString().orElseThrow());
        }
    }

    public static abstract class IdData extends ChunkData<Integer, Entity> {
        @Override public abstract @NotNull Object2ObjectMap<Identifier, Long2ObjectMap<Set<Integer>>> data();
        @Override public abstract @Nullable Predicate<Entity> validation();

        @Override public BiFunction<Integer, ServerLevel, Entity> dataToType() {
            return (id, level) -> level.getEntity(id);
        }

        @Override public Function<Integer, Tag> dataToTag() {
            return IntTag::valueOf;
        }

        @Override public Function<Tag, Integer> tagToData() {
            return tag -> tag instanceof IntTag ? ((IntTag)tag).intValue() : -1;
        }
    }

    public static abstract class BlockData extends ChunkData<Long, BlockState> {
        @Override public abstract @NotNull Object2ObjectMap<Identifier, Long2ObjectMap<Set<Long>>> data();
        @Override public abstract @Nullable Predicate<BlockState> validation();

        public static final long ZERO = BlockPos.ZERO.asLong();

        @Override public BiFunction<Long, ServerLevel, BlockState> dataToType() {
            return (pos, level) -> level.getBlockState(BlockPos.of(pos));
        }

        @Override public Function<Long, Tag> dataToTag() {
            return LongTag::valueOf;
        }

        @Override public Function<Tag, Long> tagToData() {
            return tag -> tag instanceof LongTag ? ((LongTag)tag).longValue() : ZERO;
        }
    }
}
