package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.Duplicationless;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Positions;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractEntityTracker<L extends Level> {
    protected static final Logger LOGGER = LogManager.getLogger(AbstractEntityTracker.class);

    protected final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Int2ObjectMap<EntityStorage>>> ENTITIES = new Object2ObjectOpenHashMap<>();
    protected final Object2ObjectMap<ResourceLocation, Predicate<Entity>> FILTERS = new Object2ObjectOpenHashMap<>();
    protected final ConcurrentLinkedQueue<Runnable> UPDATE_TASKS = new ConcurrentLinkedQueue<>();

    public static final ResourceLocation LIVING = new ResourceLocation(Duplicationless.MOD_ID, "living_entity");
    public static final ResourceLocation ENEMY = new ResourceLocation(Duplicationless.MOD_ID, "enemy");

    public void registerFilter(ResourceLocation filterId, Predicate<Entity> filter) {
        if (FILTERS.containsKey(filterId)) LOGGER.error("Duplicate filter ID detected: {}. Overriding.", filterId);
        FILTERS.put(filterId, filter);
    }

    public void registerFilter(ResourceLocation filterId, @NotNull Class<?> entityClass) {
        registerFilter(filterId, entityClass::isInstance);
    }

    protected abstract void assertThread(L level);

    @Deprecated
    public @NotNull IntSet getEntities(@NotNull L level, long chunkPos) {
        return getInternal(level, chunkPos, s -> s.entities);
    }

    @Deprecated
    public @NotNull IntSet getEntities(@NotNull L level, long chunkPos, EntityType<?> type) {
        return getInternal(level, chunkPos, s -> byType(s, type));
    }

    @Deprecated
    public @NotNull IntSet getEntities(@NotNull L level, long chunkPos, ResourceLocation filter) {
        return getInternal(level, chunkPos, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter));
    }

    public ObjectList<IntSet> getEntityList(@NotNull L level, long chunkPos) {
        return getEntityListInternal(level, chunkPos, s -> s.entities);
    }

    public ObjectList<IntSet> getEntityList(@NotNull L level, long chunkPos, EntityType<?> type) {
        return getEntityListInternal(level, chunkPos, s -> byType(s, type));
    }

    public ObjectList<IntSet> getEntityList(@NotNull L level, long chunkPos, ResourceLocation filter) {
        return getEntityListInternal(level, chunkPos, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter));
    }

    private ObjectList<IntSet> getEntityListInternal(@NotNull L level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        assertThread(level);

        Int2ObjectMap<EntityStorage> sections = chunkSections(level, chunkPos);
        if (sections.isEmpty()) return ObjectLists.emptyList();
        ObjectList<IntSet> entities = new ObjectArrayList<>(sections.size());
        for (EntityStorage storage : sections.values()) {
            IntSet set = extractor.apply(storage);
            if (set != null && !set.isEmpty()) entities.add(IntSets.unmodifiable(set));
        }
        return entities.isEmpty() ? ObjectLists.emptyList() : entities;
    }

    //Use getEntityListInternal instead, the addAll call in this logic is expensive
    @Deprecated
    private @NotNull IntSet getInternal(@NotNull L level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        assertThread(level);

        IntSet entities = new IntOpenHashSet();
        for (EntityStorage storage : chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(storage);
            if (set != null) entities.addAll(set);
        }
        return entities.isEmpty() ? IntSets.EMPTY_SET : entities;
    }

    public void forEach(@NotNull L level, long chunkPos, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, s -> s.entities, entityConsumer);
    }

    public void forEach(@NotNull L level, long chunkPos, EntityType<?> type, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, s -> byType(s, type), entityConsumer);
    }

    public void forEach(@NotNull L level, long chunkPos, ResourceLocation filter, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter), entityConsumer);
    }

    private void forEachInternal(@NotNull L level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor, Consumer<Entity> entityConsumer) {
        assertThread(level);

        for (EntityStorage storage : chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(storage);
            if (set != null) {
                IntIterator it = set.iterator();
                while (it.hasNext()) {
                    Entity entity = level.getEntity(it.nextInt());
                    if (entity != null) entityConsumer.accept(entity);
                }
            }
        }
    }

    public @NotNull @UnmodifiableView IntSet getEntities(@NotNull L level, long chunkPos, int sectionIndex) {
        return getInternal(level, chunkPos, sectionIndex, s -> s.entities);
    }

    public @NotNull @UnmodifiableView IntSet getEntities(@NotNull L level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return getInternal(level, chunkPos, sectionIndex, s -> byType(s, type));
    }

    public @NotNull @UnmodifiableView IntSet getEntities(@NotNull L level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return getInternal(level, chunkPos, sectionIndex, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter));
    }

    private @NotNull @UnmodifiableView IntSet getInternal(@NotNull L level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor) {
        assertThread(level);

        EntityStorage storage = chunkSections(level, chunkPos).get(sectionIndex);
        if (storage == null || storage.isEmpty()) return IntSets.EMPTY_SET;

        IntSet set = extractor.apply(storage);
        return (set == null || set.isEmpty()) ? IntSets.EMPTY_SET : IntSets.unmodifiable(set);
    }

    public void forEach(@NotNull L level, long chunkPos, int sectionIndex, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, s -> s.entities, entityConsumer);
    }

    public void forEach(@NotNull L level, long chunkPos, int sectionIndex, EntityType<?> type, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, s -> byType(s, type), entityConsumer);
    }

    public void forEach(@NotNull L level, long chunkPos, int sectionIndex, ResourceLocation filter, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter), entityConsumer);
    }

    private void forEachInternal(@NotNull L level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor, Consumer<Entity> entityConsumer) {
        assertThread(level);

        EntityStorage storage = chunkSections(level, chunkPos).get(sectionIndex);
        if (storage == null || storage.isEmpty()) return;

        IntSet set = extractor.apply(storage);
        if (set == null || set.isEmpty()) return;

        IntIterator it = set.iterator();
        while (it.hasNext()) {
            Entity entity = level.getEntity(it.nextInt());
            if (entity != null) entityConsumer.accept(entity);
        }
    }

    public int count(@NotNull L level, long chunkPos) {
        return countInternal(level, chunkPos, s -> s.entities);
    }

    public int count(@NotNull L level, long chunkPos, EntityType<?> type) {
        return countInternal(level, chunkPos, s -> byType(s, type));
    }

    public int count(@NotNull L level, long chunkPos, ResourceLocation filter) {
        return countInternal(level, chunkPos, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter));
    }

    private int countInternal(@NotNull L level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        assertThread(level);

        int count = 0;
        for (EntityStorage storage : chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(storage);
            if (set != null) count += set.size();
        }
        return count;
    }

    public int count(@NotNull L level, long chunkPos, int sectionIndex) {
        return countInternal(level, chunkPos, sectionIndex, s -> s.entities);
    }

    public int count(@NotNull L level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return countInternal(level, chunkPos, sectionIndex, s -> byType(s, type));
    }

    public int count(@NotNull L level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return countInternal(level, chunkPos, sectionIndex, s -> s.entitiesByFilter == null ? null : s.entitiesByFilter.get(filter));
    }

    private int countInternal(@NotNull L level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor) {
        assertThread(level);

        EntityStorage storage = chunkSections(level, chunkPos).get(sectionIndex);
        if (storage == null || storage.isEmpty()) return 0;

        IntSet set = extractor.apply(storage);
        return set == null ? 0 : set.size();
    }

    private static @Nullable IntSet byType(EntityStorage storage, EntityType<?> type) {
        ResourceLocation id = RegistryEntry.get(type);
        if (id.equals(RegistryEntry.NONE) || storage.entitiesByType == null) return null;
        return storage.entitiesByType.get(id);
    }

    private @NotNull Int2ObjectMap<EntityStorage> chunkSections(@NotNull L level, long chunkPos) {
        Long2ObjectMap<Int2ObjectMap<EntityStorage>> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return Int2ObjectMaps.emptyMap();
        Int2ObjectMap<EntityStorage> sections = chunks.get(chunkPos);
        return (sections == null || sections.isEmpty()) ? Int2ObjectMaps.emptyMap() : sections;
    }

    protected void update(@NotNull Entity entity, @NotNull L level, boolean add, @NotNull ObjectList<ResourceLocation> matchedFilters) {
        final long chunkPos = Positions.toChunk(entity.blockPosition());
        final int sectionIndex = SectionPos.blockToSectionCoord(entity.blockPosition().getY());
        final ResourceLocation dim = level.dimension().location();
        final int id = entity.getId();
        final ResourceLocation entityType = RegistryEntry.get(entity.getType());
        final boolean isNone = entityType.equals(RegistryEntry.NONE);

        UPDATE_TASKS.add(() -> {
            Long2ObjectMap<Int2ObjectMap<EntityStorage>> chunks = ENTITIES.computeIfAbsent(dim, k -> new Long2ObjectOpenHashMap<>());
            Int2ObjectMap<EntityStorage> sections = chunks.computeIfAbsent(chunkPos, k -> new Int2ObjectOpenHashMap<>());
            EntityStorage storage = sections.computeIfAbsent(sectionIndex, k -> new EntityStorage());

            if (add) {
                storage.add(id, entityType, isNone, matchedFilters);
            } else {
                storage.remove(id, entityType, isNone, matchedFilters);
                if (storage.isEmpty()) {
                    sections.remove(sectionIndex);
                    if (sections.isEmpty()) {
                        chunks.remove(chunkPos);
                        if (chunks.isEmpty()) ENTITIES.remove(dim);
                    }
                }
            }
        });
    }

    public void drainUpdateTasks() {
        Runnable task;
        while ((task = UPDATE_TASKS.poll()) != null) task.run();
    }

    public void reset() {
        UPDATE_TASKS.clear();
        ENTITIES.clear();
        FILTERS.clear();
    }

    protected static final class EntityStorage {
        @Nullable IntSet entities;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByType;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByFilter;

        boolean isEmpty() {
            return entities == null && entitiesByType == null && entitiesByFilter == null;
        }

        void add(int entityId, ResourceLocation type, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            updateEntities(entityId, true);

            if (!isNone) {
                if (entitiesByType == null) entitiesByType = new Object2ObjectOpenHashMap<>();
                update(entitiesByType, type, entityId, true);
            }

            if (matched != null && !matched.isEmpty()) {
                if (entitiesByFilter == null) entitiesByFilter = new Object2ObjectOpenHashMap<>();
                for (ResourceLocation filterId : matched) update(entitiesByFilter, filterId, entityId, true);
            }
        }

        void remove(int entityId, ResourceLocation type, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            updateEntities(entityId, false);

            if (!isNone && entitiesByType != null) {
                update(entitiesByType, type, entityId, false);
                if (entitiesByType.isEmpty()) entitiesByType = null;
            }

            if (matched != null && !matched.isEmpty() && entitiesByFilter != null) {
                for (ResourceLocation filterId : matched) update(entitiesByFilter, filterId, entityId, false);
                if (entitiesByFilter.isEmpty()) entitiesByFilter = null;
            }
        }

        private void updateEntities(int entityId, boolean add) {
            if (add) {
                if (entities == null) entities = new IntOpenHashSet();
                entities.add(entityId);
            } else if (entities != null) {
                entities.remove(entityId);
                if (entities.isEmpty()) entities = null;
            }
        }

        private static <K> void update(Object2ObjectMap<K, IntSet> map, K key, int entityId, boolean add) {
            if (add) {
                map.computeIfAbsent(key, k -> new IntOpenHashSet()).add(entityId);
            } else {
                IntSet set = map.get(key);
                if (set != null) {
                    set.remove(entityId);
                    if (set.isEmpty()) map.remove(key);
                }
            }
        }
    }
}