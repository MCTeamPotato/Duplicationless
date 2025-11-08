package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.ints.IntSets;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import me.kall.duplicationless.ext.RegistryEntry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Function;
import java.util.function.Predicate;

public final class EntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(EntityTracker.class);
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<EntityStorage>> ENTITIES = new Object2ObjectOpenHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Predicate<Entity>> FILTERS = new Object2ObjectOpenHashMap<>();
    private static final ConcurrentLinkedQueue<Runnable> UPDATE_TASKS = new ConcurrentLinkedQueue<>();

    private EntityTracker() {}

    public static final class EntityFilterRegistryEvent extends Event {
        private final MinecraftServer server;

        private EntityFilterRegistryEvent(MinecraftServer server) {
            this.server = server;
        }

        public void register(ResourceLocation filterId, Predicate<Entity> filter) {
            this.server.execute(() -> {
                if (FILTERS.containsKey(filterId)) throw new RuntimeException("[EntityTracker] Duplicate filter ID detected: " + filterId.toString());
                FILTERS.put(filterId, filter);
            });
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
        ObjectList<ResourceLocation> matched = null;
        if (!FILTERS.isEmpty()) {
            for (Map.Entry<ResourceLocation, Predicate<Entity>> entry : FILTERS.entrySet()) {
                if (entry.getValue().test(entity)) {
                    if (matched == null) matched = new ObjectArrayList<>();
                    matched.add(entry.getKey());
                }
            }
        }

        final ObjectList<ResourceLocation> filters = matched;

        UPDATE_TASKS.add(() -> {
            Long2ObjectMap<EntityStorage> chunks = ENTITIES.computeIfAbsent(dim, key -> new Long2ObjectOpenHashMap<>());
            EntityStorage entityStorage = chunks.computeIfAbsent(chunkPos, key -> new EntityStorage());

            if (add) {
                entityStorage.add(id, entityType, isNone, filters);
            } else {
                entityStorage.remove(id, entityType, isNone, filters);
                if (entityStorage.isEmpty()) {
                    chunks.remove(chunkPos);
                    if (chunks.isEmpty()) ENTITIES.remove(dim);
                }
            }
        });
    }

    @ApiStatus.Internal
    public static void register() {
        IEventBus bus = MinecraftForge.EVENT_BUS;
        bus.addListener(EventPriority.LOWEST, EntityTracker::onJoin);
        bus.addListener(EntityTracker::onLeave);
        bus.addListener(EntityTracker::beforeChunkChange);
        bus.addListener(EntityTracker::afterChunkChange);
        bus.addListener(EntityTracker::taskUpdate);
        bus.addListener(EntityTracker::onServerStart);
        LOGGER.info("[EntityTracker] Initialized successfully.");
    }

    private static void onServerStart(@NotNull ServerAboutToStartEvent event) {
        MinecraftForge.EVENT_BUS.post(new EntityFilterRegistryEvent(event.getServer()));
    }

    private static void onJoin(@NotNull EntityJoinLevelEvent event) {
        if (event.isCanceled()) return;
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level) {
            update(entity, level, true);
        }
    }

    private static void onLeave(@NotNull EntityLeaveLevelEvent event) {
        Entity entity = event.getEntity();
        if (event.getLevel() instanceof ServerLevel level) {
            update(entity, level, false);
        }
    }

    private static void beforeChunkChange(EntityChunkChangeEvent.@NotNull Before event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, false);
        }
    }

    private static void afterChunkChange(EntityChunkChangeEvent.@NotNull After event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, true);
        }
    }

    private static void taskUpdate(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            Runnable task;
            while ((task = EntityTracker.UPDATE_TASKS.poll()) != null) task.run();
        }
    }

    private static final class EntityStorage {
        @Nullable IntSet entities;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByType;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByFilter;

        boolean isEmpty() {
            return this.entities == null && this.entitiesByType == null && this.entitiesByFilter == null;
        }

        void add(int entityId, ResourceLocation entityType, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            if (this.entities == null) this.entities = new IntOpenHashSet();
            this.entities.add(entityId);

            if (!isNone) {
                if (this.entitiesByType == null) this.entitiesByType = new Object2ObjectOpenHashMap<>();
                this.entitiesByType.computeIfAbsent(entityType, key -> new IntOpenHashSet()).add(entityId);
            }

            if (matched != null && !matched.isEmpty()) {
                if (this.entitiesByFilter == null) this.entitiesByFilter = new Object2ObjectOpenHashMap<>();
                for (ResourceLocation filterId : matched) {
                    this.entitiesByFilter.computeIfAbsent(filterId, key -> new IntOpenHashSet()).add(entityId);
                }
            }
        }

        void remove(int entityId, ResourceLocation entityType, boolean isNone, @Nullable ObjectList<ResourceLocation> matched) {
            if (this.entities != null) {
                this.entities.remove(entityId);
                if (this.entities.isEmpty()) this.entities = null;
            }

            if (!isNone && this.entitiesByType != null) {
                IntSet entitiesOfType = this.entitiesByType.get(entityType);
                if (entitiesOfType != null) {
                    entitiesOfType.remove(entityId);
                    if (entitiesOfType.isEmpty()) this.entitiesByType.remove(entityType);
                }
                if (this.entitiesByType.isEmpty()) this.entitiesByType = null;
            }

            if (matched != null && !matched.isEmpty() && this.entitiesByFilter != null) {
                for (ResourceLocation filterId : matched) {
                    IntSet filtered = this.entitiesByFilter.get(filterId);
                    if (filtered != null) {
                        filtered.remove(entityId);
                        if (filtered.isEmpty()) this.entitiesByFilter.remove(filterId);
                    }
                }
                if (this.entitiesByFilter.isEmpty()) this.entitiesByFilter = null;
            }
        }
    }
}
