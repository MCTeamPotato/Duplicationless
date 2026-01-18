package me.kall.duplicationless.mixin.data;

import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;
import me.kall.duplicationless.data.EntityTracker;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import java.util.Map;
import java.util.function.Predicate;

@Mixin(Entity.class)
public class EntityMixin implements EntityTracker.Filterable {
    @Unique private ObjectList<Identifier> filter$matched = null;

    @Override
    public ObjectList<Identifier> filter$matched() {
        return this.filter$matched;
    }

    @Override
    public void filter$initialize(@NotNull Object2ObjectMap<Identifier, Predicate<Entity>> filters) {
        if (filters.isEmpty()) {
            this.filter$matched = ObjectLists.emptyList();
            return;
        }
        Entity entity = (Entity) (Object) this;
        for (Map.Entry<Identifier, Predicate<Entity>> entry : filters.entrySet()) {
            if (entry.getValue().test(entity)) {
                if (this.filter$matched == null) this.filter$matched = new ObjectArrayList<>();
                this.filter$matched.add(entry.getKey());
            }
        }
    }

    @Override
    public boolean filter$initialized() {
        return this.filter$matched != null;
    }
}
