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
import me.kall.duplicationless.ext.IEntityType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.EntityLeaveLevelEvent;
import net.minecraftforge.event.server.ServerAboutToStartEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnmodifiableView;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Predicate;

public final class EntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(EntityTracker.class);
    private static final Object2ObjectMap<ResourceLocation, Long2ObjectMap<EntityStorage>> ENTITIES = new Object2ObjectOpenHashMap<>();
    private static final ConcurrentLinkedQueue<Runnable> TASKS = new ConcurrentLinkedQueue<>();
    private static final ConcurrentHashMap<ResourceLocation, Predicate<Entity>> FILTERS = new ConcurrentHashMap<>();
    private static final Object2ObjectMap<ResourceLocation, Predicate<Entity>> FINAL_FILTERS = new Object2ObjectOpenHashMap<>();
    private static boolean filterFrozen;

    private EntityTracker() {}

    public static void filter(ResourceLocation filterId, Predicate<Entity> filter) {
        if (filterFrozen) throw new UnsupportedOperationException("EntityTracker filter registry should be done before server starting!");
        FILTERS.put(filterId, filter);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        Long2ObjectMap<EntityStorage> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return IntSets.emptySet();

        EntityStorage entityStorage = chunks.get(chunkPos);
        if (entityStorage == null || entityStorage.entities == null || entityStorage.entities.isEmpty()) return IntSets.emptySet();

        return IntSets.unmodifiable(entityStorage.entities);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        Long2ObjectMap<EntityStorage> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return IntSets.emptySet();

        EntityStorage entityStorage = chunks.get(chunkPos);
        if (entityStorage == null || entityStorage.entitiesByType == null) return IntSets.emptySet();

        ResourceLocation registryName = ((IEntityType) type).registry$getName();
        if (registryName.equals(IEntityType.NONE)) return IntSets.emptySet();

        IntSet entitiesByType = entityStorage.entitiesByType.get(registryName);
        if (entitiesByType == null || entitiesByType.isEmpty()) return IntSets.emptySet();

        return IntSets.unmodifiable(entitiesByType);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");

        Long2ObjectMap<EntityStorage> chunks = ENTITIES.get(level.dimension().location());
        if (chunks == null || chunks.isEmpty()) return IntSets.emptySet();

        EntityStorage entityStorage = chunks.get(chunkPos);
        if (entityStorage == null || entityStorage.entitiesByFilter == null) return IntSets.emptySet();

        IntSet filtered = entityStorage.entitiesByFilter.get(filter);
        if (filtered == null || filtered.isEmpty()) return IntSets.emptySet();

        return IntSets.unmodifiable(filtered);
    }

    private static void update(@NotNull Entity entity, @NotNull ServerLevel level, boolean add) {
        final long chunkPos = entity.chunkPosition().toLong();
        final ResourceLocation dim = level.dimension().location();
        final int id = entity.getId();
        final ResourceLocation entityType = ((IEntityType) entity.getType()).registry$getName();
        boolean isNone = entityType.equals(IEntityType.NONE);
        ObjectList<ResourceLocation> updatable = null;
        for (Map.Entry<ResourceLocation, Predicate<Entity>> entry : FINAL_FILTERS.entrySet()) {
            if (entry.getValue().test(entity)) {
                if (updatable == null) updatable = new ObjectArrayList<>();
                updatable.add(entry.getKey());
            }
        }

        ObjectList<ResourceLocation> filters = updatable;
        TASKS.add(() -> {
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
        bus.addListener(EntityTracker::onUpdatePre);
        bus.addListener(EntityTracker::onUpdatePost);
        bus.addListener(EntityTracker::onTick);
        bus.addListener(EntityTracker::onServerStart);
        LOGGER.info("[EntityTracker] Initialized successfully.");
    }

    private static void onServerStart(@NotNull ServerAboutToStartEvent event) {
        event.getServer().execute(() -> {
            filterFrozen = true;
            FINAL_FILTERS.putAll(FILTERS);
            FILTERS.clear();
            LOGGER.info("[EntityTracker] Entity Filters Registry is now frozen.");
        });
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

    private static void onUpdatePre(EntityChunkChangeEvent.@NotNull Before event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, false);
        }
    }

    private static void onUpdatePost(EntityChunkChangeEvent.@NotNull After event) {
        Entity entity = event.getEntity();
        if (entity.level() instanceof ServerLevel level) {
            update(entity, level, true);
        }
    }

    private static void onTick(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) {
            event.getServer().execute(() -> {
                Runnable task;
                while ((task = EntityTracker.TASKS.poll()) != null) task.run();
            });
        }
    }

    private static final class EntityStorage {
        @Nullable IntSet entities;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByType;
        @Nullable Object2ObjectMap<ResourceLocation, IntSet> entitiesByFilter;

        boolean isEmpty() {
            return (this.entities == null || this.entities.isEmpty()) && (this.entitiesByType == null || this.entitiesByType.isEmpty()) && (this.entitiesByFilter == null || this.entitiesByFilter.isEmpty());
        }

        void add(int entityId, ResourceLocation entityType, boolean isNone, @Nullable ObjectList<ResourceLocation> updatable) {
            if (this.entities == null) this.entities = new IntOpenHashSet();
            this.entities.add(entityId);

            if (!isNone) {
                if (this.entitiesByType == null) this.entitiesByType = new Object2ObjectOpenHashMap<>();
                this.entitiesByType.computeIfAbsent(entityType, key -> new IntOpenHashSet()).add(entityId);
            }

            if (updatable !=null && !updatable.isEmpty()) {
                if (this.entitiesByFilter == null) this.entitiesByFilter = new Object2ObjectOpenHashMap<>();
                updatable.forEach(filterId -> this.entitiesByFilter.computeIfAbsent(filterId, key -> new IntOpenHashSet()).add(entityId));
            }
        }

        void remove(int entityId, ResourceLocation entityType, boolean isNone, @Nullable ObjectList<ResourceLocation> updatable) {
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

            if (updatable != null && !updatable.isEmpty() && this.entitiesByFilter != null) {
                updatable.forEach(filterId -> {
                    IntSet filtered = this.entitiesByFilter.get(filterId);
                    if (filtered != null) {
                        filtered.remove(entityId);
                        if (filtered.isEmpty()) this.entitiesByFilter.remove(filterId);
                    }
                    if (this.entitiesByFilter.isEmpty()) this.entitiesByFilter = null;
                });
            }
        }
    }
}
