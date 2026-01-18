package me.kall.duplicationless.event;

import net.minecraft.world.entity.Entity;
import net.neoforged.neoforge.event.entity.EntityEvent;

public class EntityChunkChangeEvent extends EntityEvent {
    public EntityChunkChangeEvent(Entity entity) {
        super(entity);
    }

    public static final class Before extends EntityChunkChangeEvent {
        public Before(Entity entity) {
            super(entity);
        }
    }

    public static final class After extends EntityChunkChangeEvent {
        public After(Entity entity) {
            super(entity);
        }
    }

    public static class Section extends EntityChunkChangeEvent {
        public Section(Entity entity) {
            super(entity);
        }

        public static final class Before extends Section {
            public Before(Entity entity) {
                super(entity);
            }
        }

        public static final class After extends Section {
            public After(Entity entity) {
                super(entity);
            }
        }
    }
}