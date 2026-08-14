package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.kall.duplicationless.Duplicationless;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
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
import org.jetbrains.annotations.UnmodifiableView;

import java.util.function.Consumer;
import java.util.function.Predicate;

@Mod.EventBusSubscriber(modid = Duplicationless.MOD_ID)
public final class EntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(EntityTracker.class);

    private static final AbstractEntityTracker<ServerLevel> INSTANCE = new AbstractEntityTracker<>() {
        @Override
        protected void assertThread(@NotNull ServerLevel level) {
            if (!level.getServer().isSameThread()) throw new UnsupportedOperationException("EntityTracker is only available on the server thread!");
        }
    };

    public static final ResourceLocation LIVING = AbstractEntityTracker.LIVING;
    public static final ResourceLocation ENEMY = AbstractEntityTracker.ENEMY;

    private EntityTracker() {}

    public static final class EntityFilterRegistryEvent extends Event {
        public void register(ResourceLocation filterId, Predicate<Entity> filter) {
            INSTANCE.registerFilter(filterId, filter);
        }

        public void register(ResourceLocation filterId, @NotNull Class<?> entityClass) {
            INSTANCE.registerFilter(filterId, entityClass);
        }
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos) {
        return INSTANCE.getEntities(level, chunkPos);
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.getEntities(level, chunkPos, type);
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.getEntities(level, chunkPos, filter);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos) {
        return INSTANCE.getEntityList(level, chunkPos);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.getEntityList(level, chunkPos, type);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.getEntityList(level, chunkPos, filter);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, consumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, EntityType<?> type, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, type, consumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, filter, consumer);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex, type);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex, filter);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, consumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, type, consumer);
    }

    public static void forEach(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, filter, consumer);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos) {
        return INSTANCE.count(level, chunkPos);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.count(level, chunkPos, type);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.count(level, chunkPos, filter);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex) {
        return INSTANCE.count(level, chunkPos, sectionIndex);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return INSTANCE.count(level, chunkPos, sectionIndex, type);
    }

    public static int count(@NotNull ServerLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return INSTANCE.count(level, chunkPos, sectionIndex, filter);
    }

    private static void update(@NotNull Entity entity, @NotNull ServerLevel level, boolean add) {
        INSTANCE.update(entity, level, add, Filterable.getMatched(entity));
    }

    @SubscribeEvent
    public static void filterRegistry(@NotNull ServerAboutToStartEvent event) {
        MinecraftForge.EVENT_BUS.post(new EntityFilterRegistryEvent());
        LOGGER.info("Duplicationless Entity Tracker has initialized successfully.");
    }

    @SubscribeEvent
    public static void listenLiving(@NotNull EntityFilterRegistryEvent event) {
        event.register(LIVING, LivingEntity.class);
        event.register(ENEMY, Enemy.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(@NotNull EntityJoinLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) update(event.getEntity(), level, true);
    }

    @SubscribeEvent
    public static void onLeave(@NotNull EntityLeaveLevelEvent event) {
        if (event.getLevel() instanceof ServerLevel level) update(event.getEntity(), level, false);
    }

    @SubscribeEvent
    public static void beforeChunkChange(EntityChunkChangeEvent.@NotNull Before event) {
        if (event.getEntity().level() instanceof ServerLevel level) update(event.getEntity(), level, false);
    }

    @SubscribeEvent
    public static void afterChunkChange(EntityChunkChangeEvent.@NotNull After event) {
        if (event.getEntity().level() instanceof ServerLevel level) update(event.getEntity(), level, true);
    }

    @SubscribeEvent
    public static void beforeSectionChange(EntityChunkChangeEvent.Section.@NotNull Before event) {
        if (event.getEntity().level() instanceof ServerLevel level) update(event.getEntity(), level, false);
    }

    @SubscribeEvent
    public static void afterSectionChange(EntityChunkChangeEvent.Section.@NotNull After event) {
        if (event.getEntity().level() instanceof ServerLevel level) update(event.getEntity(), level, true);
    }

    @SubscribeEvent
    public static void taskUpdate(TickEvent.@NotNull ServerTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) INSTANCE.drainUpdateTasks();
    }

    @SubscribeEvent
    public static void stopServer(ServerStoppedEvent event) {
        INSTANCE.reset();
    }

    @ApiStatus.Internal
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public interface Filterable {
        ObjectList<ResourceLocation> filter$matched();
        void filter$initialize(Object2ObjectMap<ResourceLocation, Predicate<Entity>> filters);
        boolean filter$initialized();

        static ObjectList<ResourceLocation> getMatched(Entity entity) {
            Filterable filterable = (Filterable) entity;
            if (!filterable.filter$initialized()) filterable.filter$initialize(EntityTracker.INSTANCE.FILTERS);
            return filterable.filter$matched();
        }
    }
}