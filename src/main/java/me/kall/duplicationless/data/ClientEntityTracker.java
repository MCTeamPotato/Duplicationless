package me.kall.duplicationless.data;

import it.unimi.dsi.fastutil.ints.IntSet;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectList;
import me.kall.duplicationless.Duplicationless;
import me.kall.duplicationless.event.EntityChunkChangeEvent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinWorldEvent;
import net.minecraftforge.event.entity.EntityLeaveWorldEvent;
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

@Mod.EventBusSubscriber(modid = Duplicationless.MOD_ID, value = Dist.CLIENT)
public final class ClientEntityTracker {
    private static final Logger LOGGER = LogManager.getLogger(ClientEntityTracker.class);

    private static final AbstractEntityTracker<ClientLevel> INSTANCE = new AbstractEntityTracker<ClientLevel>() {
        @Override
        protected void assertThread(ClientLevel level) {
            if (!Minecraft.getInstance().isSameThread()) throw new UnsupportedOperationException("ClientEntityTracker is only available on the client thread!");
        }
    };

    public static final ResourceLocation LIVING = AbstractEntityTracker.LIVING;
    public static final ResourceLocation ENEMY = AbstractEntityTracker.ENEMY;

    private ClientEntityTracker() {}

    public static final class ClientEntityFilterRegistryEvent extends Event {
        public void register(ResourceLocation filterId, Predicate<Entity> filter) {
            INSTANCE.registerFilter(filterId, filter);
        }

        public void register(ResourceLocation filterId, @NotNull Class<?> entityClass) {
            INSTANCE.registerFilter(filterId, entityClass);
        }
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ClientLevel level, long chunkPos) {
        return INSTANCE.getEntities(level, chunkPos);
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ClientLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.getEntities(level, chunkPos, type);
    }

    @Deprecated
    public static @NotNull IntSet getEntities(@NotNull ClientLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.getEntities(level, chunkPos, filter);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ClientLevel level, long chunkPos) {
        return INSTANCE.getEntityList(level, chunkPos);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ClientLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.getEntityList(level, chunkPos, type);
    }

    public static ObjectList<IntSet> getEntityList(@NotNull ClientLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.getEntityList(level, chunkPos, filter);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, consumer);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, EntityType<?> type, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, type, consumer);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, ResourceLocation filter, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, filter, consumer);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ClientLevel level, long chunkPos, int sectionIndex) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ClientLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex, type);
    }

    public static @NotNull @UnmodifiableView IntSet getEntities(@NotNull ClientLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return INSTANCE.getEntities(level, chunkPos, sectionIndex, filter);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, int sectionIndex, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, consumer);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, int sectionIndex, EntityType<?> type, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, type, consumer);
    }

    public static void forEach(@NotNull ClientLevel level, long chunkPos, int sectionIndex, ResourceLocation filter, Consumer<Entity> consumer) {
        INSTANCE.forEach(level, chunkPos, sectionIndex, filter, consumer);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos) {
        return INSTANCE.count(level, chunkPos);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos, EntityType<?> type) {
        return INSTANCE.count(level, chunkPos, type);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos, ResourceLocation filter) {
        return INSTANCE.count(level, chunkPos, filter);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos, int sectionIndex) {
        return INSTANCE.count(level, chunkPos, sectionIndex);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos, int sectionIndex, EntityType<?> type) {
        return INSTANCE.count(level, chunkPos, sectionIndex, type);
    }

    public static int count(@NotNull ClientLevel level, long chunkPos, int sectionIndex, ResourceLocation filter) {
        return INSTANCE.count(level, chunkPos, sectionIndex, filter);
    }

    private static void update(@NotNull Entity entity, @NotNull ClientLevel level, boolean add) {
        INSTANCE.update(entity, level, add, ClientFilterable.getMatched(entity));
    }

    @SubscribeEvent
    public static void filterRegistry(ClientPlayerNetworkEvent.LoggedInEvent event) {
        MinecraftForge.EVENT_BUS.post(new ClientEntityFilterRegistryEvent());
        LOGGER.info("Duplicationless Client Entity Tracker has initialized successfully.");
    }

    @SubscribeEvent
    public static void listenLiving(@NotNull ClientEntityFilterRegistryEvent event) {
        event.register(LIVING, LivingEntity.class);
        event.register(ENEMY, Enemy.class);
    }

    @SubscribeEvent(priority = EventPriority.LOWEST)
    public static void onJoin(@NotNull EntityJoinWorldEvent event) {
        if (event.getWorld() instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getWorld(), true);
    }

    @SubscribeEvent
    public static void onLeave(@NotNull EntityLeaveWorldEvent event) {
        if (event.getWorld() instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getWorld(), false);
    }

    @SubscribeEvent
    public static void beforeChunkChange(EntityChunkChangeEvent.@NotNull Before event) {
        if (event.getEntity().level instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getEntity().level, false);
    }

    @SubscribeEvent
    public static void afterChunkChange(EntityChunkChangeEvent.@NotNull After event) {
        if (event.getEntity().level instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getEntity().level, true);
    }

    @SubscribeEvent
    public static void beforeSectionChange(EntityChunkChangeEvent.Section.@NotNull Before event) {
        if (event.getEntity().level instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getEntity().level, false);
    }

    @SubscribeEvent
    public static void afterSectionChange(EntityChunkChangeEvent.Section.@NotNull After event) {
        if (event.getEntity().level instanceof ClientLevel) update(event.getEntity(), (ClientLevel) event.getEntity().level, true);
    }

    @SubscribeEvent
    public static void taskUpdate(TickEvent.@NotNull ClientTickEvent event) {
        if (event.phase.equals(TickEvent.Phase.START)) INSTANCE.drainUpdateTasks();
    }

    @SubscribeEvent
    public static void stop(ClientPlayerNetworkEvent.LoggedOutEvent event) {
        INSTANCE.reset();
    }

    @ApiStatus.Internal
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public interface ClientFilterable {
        ObjectList<ResourceLocation> clientFilter$matched();
        void clientFilter$initialize(Object2ObjectMap<ResourceLocation, Predicate<Entity>> filters);
        boolean clientFilter$initialized();

        static ObjectList<ResourceLocation> getMatched(Entity entity) {
            ClientFilterable filterable = (ClientFilterable) entity;
            if (!filterable.clientFilter$initialized()) filterable.clientFilter$initialize(collectFilters());
            return filterable.clientFilter$matched();
        }

        static Object2ObjectMap<ResourceLocation, Predicate<Entity>> collectFilters() {
            return ClientEntityTracker.INSTANCE.FILTERS;
        }
    }
}