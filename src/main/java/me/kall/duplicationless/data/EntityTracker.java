package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.kall.duplicationless.Duplicationless;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Duplicationless.MOD_ID)
public final class EntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(EntityTracker.class);
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<EntityStorage>> ENTITIES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Predicate<Entity>> FILTERS = new Object2ObjectOpenHashMap<>();
    private static final ConcurrentLinkedQueue<Runnable> UPDATE_TASKS = new ConcurrentLinkedQueue<>();

    public static final class EntityFilterRegistryEvent extends Event {
        public void register(ResourceLocation filterId, Predicate<Entity> filter) {
            if (FILTERS.containsKey(filterId)) LOGGER.warn("[EntityTracker] Duplicate filter ID detected: {}. Overriding.", filterId.toString());
            FILTERS.put(filterId, filter);
        }

        public void register(ResourceLocation filterId, @NotNull Class<?> entityClass) {
            register(filterId, entityClass::isInstance);
        }
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos) {
        return getInternal(level, chunkPos, entityStorage -> entityStorage.entities);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return getInternal(level, chunkPos, entityStorage -> {
            ResourceLocation id = RegistryEntry.get(type);
            if (id.equals(RegistryEntry.NONE)) return null;
            if (entityStorage.entitiesByType == null) return null;
            return entityStorage.entitiesByType.get(id);
        });
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, Class<?> entityClass) {
        return getInternal(level, chunkPos, entityStorage -> entityStorage.entitiesByClass == null ? null : entityStorage.entitiesByClass.get(entityClass));
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return getInternal(level, chunkPos, entityStorage -> entityStorage.entitiesByFilter == null ? null : entityStorage.entitiesByFilter.get(filter));
    }

    private static @NotNull @UnmodifiableView IntSet getInternal(@NotNull ServerLevel level, long chunkPos, Function<EntityStorage, @Nullable IntSet> extractor) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        Long2ObjectMap<EntityStorage> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return IntSets.emptySet();

        EntityStorage storage = chunks.get(chunkPos);
        if (storage == null) return IntSets.emptySet();

        IntSet set = extractor.apply(storage);
        if (set == null || set.isEmpty()) return IntSets.emptySet();

        return IntSets.unmodifiable(set);
    }

    private static void update(@NotNull Entity entity, @NotNull ServerLevel level, boolean add) {
        final long chunkPos = entity.chunkPosition().toLong();
        final ResourceLocation dim = level.dimension().location();
        final int id = entity.getId();
        final ResourceLocation entityType = RegistryEntry.get(entity.getType());
        boolean isNone = entityType.equals(RegistryEntry.NONE);
        if (entity instanceof Filterable filterable && !filterable.filter$initialized()) filterable.filter$initialize(FILTERS);

        final ObjectList<ResourceLocation> filters = Filterable.getMatched(entity);

        UPDATE_TASKS.add(() -> {
            Long2ObjectMap<EntityStorage> chunks = ENTITIES.computeIfAbsent(dim, key -> new Long2ObjectOpenHashMap<>());
            EntityStorage entityStorage = chunks.computeIfAbsent(chunkPos, key -> new EntityStorage());

            if (add) {
                entityStorage.add(id, entityType, entity.getClass(), isNone, filters);
            } else {
                entityStorage.remove(id, entityType, entity.getClass(), isNone, filters);
                if (entityStorage.isEmpty()) {
                    chunks.remove(chunkPos);
                    if (chunks.isEmpty()) ENTITIES.remove(dim);
                }
            }
        });
    }

    @SubscribeEvent
    public static void filterRegistry(@NotNull ServerAboutToStartEvent event) {
        MinecraftForge.EVENT_BUS.post(new EntityFilterRegistryEvent());
        LOGGER.info("Duplicationless Entity Tracker has initialized successfully.");
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(@NotNull EntityJoinLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level) {
            update(entity, level, true);
        }
    }

    @SubscribeEvent
    public static void onLeave(@NotNull EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level) {
            update(entity, level, false);
        }
    }

    @SubscribeEvent
    public static void beforeChunkChange(EntityChunkChangeEvent.@NotNull Before event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, false);
        }
    }

    @SubscribeEvent
    public static void afterChunkChange(EntityChunkChangeEvent.@NotNull After event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, true);
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
    public static void stopServer(ServerStoppedEvent event) {
        UPDATE_TASKS.clear();
        ENTITIES.clear();
        FILTERS.clear();
    }

    private static final class EntityStorage {
        @Nullable IntSet entities;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByType;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByFilter;
        @Nullable Object2ObjectMap<Class<?>, IntSet> entitiesByClass;

        boolean isEmpty() {
            return entities == null && entitiesByType == null && entitiesByFilter == null && entitiesByClass == null;
        }

        void add(int entityId, ResourceLocation type, Class<?> entityClass, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            updateEntities(entityId, true);

            if (!isNone) {
                if (entitiesByType == null) entitiesByType = new Object2ObjectOpenHashMap<>();
                update(entitiesByType, type, entityId, true);
            }

            if (entitiesByClass == null) entitiesByClass = new Object2ObjectOpenHashMap<>();
            update(entitiesByClass, entityClass, entityId, true);

            if (matched != null && !matched.isEmpty()) {
                if (entitiesByFilter == null) entitiesByFilter = new Object2ObjectOpenHashMap<>();
                for (ResourceLocation filterId : matched) {
                    update(entitiesByFilter, filterId, entityId, true);
                }
            }
        }

        void remove(int entityId, ResourceLocation type, Class<?> entityClass, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            updateEntities(entityId, false);

            if (!isNone && entitiesByType != null) {
                update(entitiesByType, type, entityId, false);
                if (entitiesByType.isEmpty()) entitiesByType = null;
            }

            if (entitiesByClass != null) {
                update(entitiesByClass, entityClass, entityId, false);
                if (entitiesByClass.isEmpty()) entitiesByClass = null;
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
    public interface Filterable {
        ObjectList<ResourceLocation> filter$matched();
        void filter$initialize(Object2ObjectMap<ResourceLocation, Predicate<Entity>> filters);
        boolean filter$initialized();

        static ObjectList<ResourceLocation> getMatched(Entity entity) {
            return ((Filterable)entity).filter$matched();
        }
    }
}
