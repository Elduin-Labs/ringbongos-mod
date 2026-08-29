package com.ringbongos.client;

import net.minecraft.util.Identifier;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Client-side mirror of who the server says is Herobrine. */
public final class Herobrine {
    public static final Identifier SKIN =
            Identifier.of("ringbongos", "textures/entity/herobrine.png");

    private static final Set<UUID> WEARING = new HashSet<>();

    private Herobrine() {
    }

    public static void set(Iterable<UUID> players) {
        WEARING.clear();
        for (UUID id : players) {
            WEARING.add(id);
        }
    }

    public static boolean is(UUID id) {
        return id != null && WEARING.contains(id);
    }
}
