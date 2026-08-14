package me.kall.duplicationless.mixin.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.kall.duplicationless.data.ClientEntityTracker;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(Entity.class)
public class EntityClientMixin implements ClientEntityTracker.ClientFilterable {
    @Unique private ObjectList<ResourceLocation> clientFilter$matched = null;

    @Override
    public ObjectList<ResourceLocation> clientFilter$matched() {
        return this.clientFilter$matched;
    }

    @Override
    public void clientFilter$initialize(@NotNull Object2ObjectMap<ResourceLocation, Predicate<Entity>> filters) {
        if (filters.isEmpty()) {
            this.clientFilter$matched = ObjectLists.emptyList();
            return;
        }
        Entity entity = (Entity) (Object) this;
        for (Map.Entry<ResourceLocation, Predicate<Entity>> entry : filters.entrySet()) {
            if (entry.getValue().test(entity)) {
                if (this.clientFilter$matched == null) this.clientFilter$matched = new ObjectArrayList<>();
                this.clientFilter$matched.add(entry.getKey());
            }
        }
    }

    @Override
    public boolean clientFilter$initialized() {
        return this.clientFilter$matched != null;
    }
}
