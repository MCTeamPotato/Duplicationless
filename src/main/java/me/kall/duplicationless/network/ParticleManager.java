package me.kall.duplicationless.network;

import it.unimi.dsi.fastutil.bytes.Byte2ObjectMap;
import it.unimi.dsi.fastutil.bytes.Byte2ObjectOpenHashMap;
import net.minecraft.core.particles.SimpleParticleType;

public class ParticleManager {
    public static final Byte2ObjectMap<SimpleParticleType> PARTICLES = new Byte2ObjectOpenHashMap<>();

    public static void register(byte id, SimpleParticleType particleType) {
        synchronized (PARTICLES) {
            if (PARTICLES.containsKey(id)) {
                SimpleParticleType existing = PARTICLES.get(id);
                throw new RuntimeException("Particle Type ID " + id + " Conflict found. Existing: " + existing + ". Registering: " + particleType);
            }
            PARTICLES.put(id, particleType);
        }
    }
}
