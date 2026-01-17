package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.*;
import me.kall.duplicationless.Duplicationless;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import me.kall.duplicationless.util.Positions;
import net.minecraft.core.SectionPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerAboutToStartEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Duplicationless.MOD_ID)
public final class EntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(EntityTracker.class);
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<Int2ObjectMap<EntityStorage>>> ENTITIES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Predicate<Entity>> FILTERS = new Object2ObjectOpenHashMap<>();
    private static final ConcurrentLinkedQueue<Runnable> UPDATE_TASKS = new ConcurrentLinkedQueue<>();

    public static final ResourceLocation LIVING = new ResourceLocation(Duplicationless.MOD_ID, "living_entity");
    public static final ResourceLocation ENEMY = new ResourceLocation(Duplicationless.MOD_ID, "enemy");

    public static final class EntityFilterRegistryEvent extends Event {
        public void register(ResourceLocation filterId, Predicate<Entity> filter) {
            if (FILTERS.containsKey(filterId)) LOGGER.warn("[EntityTracker] Duplicate filter ID detected: {}. Overriding.", filterId);
            FILTERS.put(filterId, filter);
        }

        public void register(ResourceLocation filterId, @NotNull Class<?> entityClass) {
            register(filterId, entityClass::isInstance);
        }
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos) {
        return getInternal(level, chunkPos, entityStorage -> entityStorage.entities);
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return getInternal(level, chunkPos, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        });
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return getInternal(level, chunkPos, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter));
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos) {
        return getEntityListInternal(level, chunkPos, entityStorage -> entityStorage.entities);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return getEntityListInternal(level, chunkPos, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        });
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return getEntityListInternal(level, chunkPos, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter));
    }

    private static ObjectList<IntSet> getEntityListInternal(@NotNull ServerLevel level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        Int2ObjectMap<EntityStorage> sections = EntityTracker.chunkSections(level, chunkPos);
        if (sections.isEmpty()) return ObjectLists.emptyList();
        ObjectList<IntSet> entities = new ObjectArrayList<>(sections.size());
        for (EntityStorage entityStorage : sections.values()) {
            IntSet set = extractor.apply(entityStorage);
            if (set != null && !set.isEmpty()) entities.add(IntSets.unmodifiable(set));
        }
        if (entities.isEmpty()) return ObjectLists.emptyList();
        return entities;
    }

    @Deprecated
    private static @NotNull IntSet getInternal(@NotNull ServerLevel level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        IntSet entities = new IntOpenHashSet();
        for (EntityStorage entityStorage : EntityTracker.chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(entityStorage);
            if (set != null) entities.addAll(set);
        }
        if (entities.isEmpty()) return IntSets.EMPTY_SET;
        return entities;
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos,  Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, entityStorage -> entityStorage.entities, entityConsumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos,  EntityType<?> type, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        }, entityConsumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter), entityConsumer);
    }

    private static void forEachInternal(@NotNull ServerLevel level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor, Consumer<Entity> entityConsumer) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        for (EntityStorage entityStorage : EntityTracker.chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(entityStorage);
            if (set != null) {
                IntIterator entities = set.iterator();
                while (entities.hasNext()) {
                    Entity entity = level.getEntity(entities.nextInt());
                    if (entity != null) {
                        entityConsumer.accept(entity);
                    }
                }
            }
        }
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex) {
        return getInternal(level, chunkPos, sectionIndex, entityStorage -> entityStorage.entities);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return getInternal(level, chunkPos, sectionIndex, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        });
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return getInternal(level, chunkPos, sectionIndex, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter));
    }

    private static @NotNull @UnmodifiableView IntSet getInternal(@NotNull ServerLevel level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        EntityStorage entityStorage = EntityTracker.chunkSections(level, chunkPos).get(sectionIndex);
        if (entityStorage == null || entityStorage.isEmpty()) return IntSets.EMPTY_SET;

        IntSet set = extractor.apply(entityStorage);
        if (set == null || set.isEmpty()) return IntSets.EMPTY_SET;

        return IntSets.unmodifiable(set);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, entityStorage -> entityStorage.entities, entityConsumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        }, entityConsumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter, Consumer<Entity> entityConsumer) {
        forEachInternal(level, chunkPos, sectionIndex, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter), entityConsumer);
    }

    private static void forEachInternal(@NotNull ServerLevel level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor, Consumer<Entity> entityConsumer) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        EntityStorage entityStorage = EntityTracker.chunkSections(level, chunkPos).get(sectionIndex);
        if (entityStorage == null || entityStorage.isEmpty()) return;

        IntSet set = extractor.apply(entityStorage);
        if (set == null || set.isEmpty()) return;

        IntIterator entities = set.iterator();
        while (entities.hasNext()) {
            Entity entity = level.getEntity(entities.nextInt());
            if (entity == null) continue;
            entityConsumer.accept(entity);
        }
    }

    private static @NotNull Int2ObjectMap<EntityStorage> chunkSections(@NotNull ServerLevel level, long chunkPos) {
        Long2ObjectMap<Int2ObjectMap<EntityStorage>> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return Int2ObjectMaps.emptyMap();
        Int2ObjectMap<EntityStorage> sections = chunks.get(chunkPos);
        if (sections == null || sections.isEmpty()) return Int2ObjectMaps.emptyMap();
        return sections;
    }

    public static int count(@NotNull ServerLevel level, long chunkPos) {
        return countInternal(level, chunkPos, storage -> storage.entities);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return countInternal(level, chunkPos, storage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (storage.entitiesByType == null) return null;
            return storage.entitiesByType.get(id);
        });
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return countInternal(level, chunkPos, storage -> storage.entitiesByFilter == null ? null : storage.entitiesByFilter.get(filter));
    }

    private static int countInternal(@NotNull ServerLevel level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        int count = 0;
        for (EntityStorage storage : EntityTracker.chunkSections(level, chunkPos).values()) {
            IntSet set = extractor.apply(storage);
            if (set != null) count += set.size();
        }
        return count;
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex) {
        return countInternal(level, chunkPos, sectionIndex, storage -> storage.entities);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return countInternal(level, chunkPos, sectionIndex, storage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (storage.entitiesByType == null) return null;
            return storage.entitiesByType.get(id);
        });
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return countInternal(level, chunkPos, sectionIndex, storage -> storage.entitiesByFilter == null ? null : storage.entitiesByFilter.get(filter));
    }

    private static int countInternal(@NotNull ServerLevel level, long chunkPos, int sectionIndex, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        EntityStorage storage = EntityTracker.chunkSections(level, chunkPos).get(sectionIndex);
        if (storage == null || storage.isEmpty()) return 0;

        IntSet set = extractor.apply(storage);
        return set == null ? 0 : set.size();
    }

    private static void update(@NotNull Entity entity, @NotNull ServerLevel level, boolean add) {
        final long chunkPos = Positions.toChunk(entity.blockPosition());
        final int sectionIndex = SectionPos.blockToSectionCoord(Mth.floor(entity.getY()));
        final ResourceLocation dim = level.dimension().location();
        final int id = entity.getId();
        final ResourceLocation entityType = RegistryEntry.get(entity.getType());
        boolean isNone = entityType.equals(RegistryEntry.NONE);
        final ObjectList<ResourceLocation> filters = Filterable.getMatched(entity);

        UPDATE_TASKS.add(() -> {
            Long2ObjectMap<Int2ObjectMap<EntityStorage>> chunks = ENTITIES.computeIfAbsent(dim, key -> new Long2ObjectOpenHashMap<>());
            Int2ObjectMap<EntityStorage> sections = chunks.computeIfAbsent(chunkPos, key -> new Int2ObjectOpenHashMap<>());
            EntityStorage entityStorage = sections.computeIfAbsent(sectionIndex, key -> new EntityStorage());

            if (add) {
                entityStorage.add(id, entityType, isNone, filters);
            } else {
                entityStorage.remove(id, entityType, isNone, filters);
                if (entityStorage.isEmpty()) {
                    sections.remove(sectionIndex);
                    if (sections.isEmpty()) {
                        chunks.remove(chunkPos);
                        if (chunks.isEmpty()) ENTITIES.remove(dim);
                    }
                }
            }
        });
    }

    @SubscribeEvent
    public static void filterRegistry(@NotNull FMLServerAboutToStartEvent event) {
        MinecraftForge.EVENT_BUS.post(new EntityFilterRegistryEvent());
        LOGGER.info("Duplicationless Entity Tracker has initialized successfully.");
    }

    @SubscribeEvent
    public static void listenLiving(@NotNull EntityFilterRegistryEvent event) {
        event.register(LIVING, LivingEntity.class);
        event.register(ENEMY, Enemy.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(@NotNull EntityJoinWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld() instanceof ServerLevel) {
            update(entity, (ServerLevel) event.getWorld(), true);
        }
    }

    @SubscribeEvent
    public static void onLeave(@NotNull EntityLeaveWorldEvent event) {
        Entity entity = event.getEntity();
        if (event.getWorld() instanceof ServerLevel) {
            update(entity, (ServerLevel) event.getWorld(), false);
        }
    }

    @SubscribeEvent
    public static void beforeChunkChange(EntityChunkChangeEvent.@NotNull Before event) {
        Entity entity = event.getEntity();
        if (entity.level instanceof ServerLevel) {
            update(entity, (ServerLevel) entity.level, false);
        }
    }

    @SubscribeEvent
    public static void afterChunkChange(EntityChunkChangeEvent.@NotNull After event) {
        Entity entity = event.getEntity();
        if (entity.level instanceof ServerLevel) {
            update(entity, (ServerLevel) entity.level, true);
        }
    }

    @SubscribeEvent
    public static void beforeSectionChange(EntityChunkChangeEvent.Section.@NotNull Before event) {
        Entity entity = event.getEntity();
        if (entity.level instanceof ServerLevel) {
            update(entity, (ServerLevel) entity.level, false);
        }
    }

    @SubscribeEvent
    public static void afterSectionChange(EntityChunkChangeEvent.Section.@NotNull After event) {
        Entity entity = event.getEntity();
        if (entity.level instanceof ServerLevel) {
            update(entity, (ServerLevel) entity.level, true);
        }
    }

    @SubscribeEvent
    public static void taskUpdate(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            Runnable task;
            while ((task = EntityTracker.UPDATE_TASKS.poll()) != null) task.run();
        }
    }

    @SubscribeEvent
    public static void stopServer(FMLServerStoppedEvent event) {
        UPDATE_TASKS.clear();
        ENTITIES.clear();
        FILTERS.clear();
    }

    private static final class EntityStorage {
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
                for (ResourceLocation filterId : matched) {
                    update(entitiesByFilter, filterId, entityId, true);
                }
            }
        }

        void remove(int entityId, ResourceLocation type, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            updateEntities(entityId, false);

            if (!isNone && entitiesByType != null) {
                update(entitiesByType, type, entityId, false);
                if (entitiesByType.isEmpty()) entitiesByType = null;
            }

            if (matched != null && !matched.isEmpty() && entitiesByFilter != null) {
                for (ResourceLocation filterId : matched) {
                    update(entitiesByFilter, filterId, entityId, false);
                }
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

    @ApiStatus.Internal
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public interface Filterable {
        ObjectList<ResourceLocation> filter$matched();
        void filter$initialize(Object2ObjectMap<ResourceLocation, Predicate<Entity>> filters);
        boolean filter$initialized();

        static ObjectList<ResourceLocation> getMatched(Entity entity) {
            Filterable filterable = (Filterable) entity;
            if (!filterable.filter$initialized()) filterable.filter$initialize(FILTERS);
            return filterable.filter$matched();
        }
    }
}
