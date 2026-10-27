package com.ringbongos;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * The BABY app: find your own babies from the Babies mod, and jump to the nearest one.
 *
 * <p>Looked up by entity id rather than compiled against, the same way the item group picks up
 * Bing Bong's items, so Ring Bong OS still loads on its own when Babies isn't installed. A baby
 * is a vanilla {@link TameableEntity} underneath, which is all this needs to tell whose it is.
 */
public final class Babies {
    private static final Identifier BABY_ID = Identifier.of("babies", "baby");

    /** How many babies the app lists. More than this and the card stops fitting the screen. */
    private static final int LISTED = 5;

    /** Sideways offsets tried when the baby's own spot is too tight for a player. */
    private static final Vec3d[] NUDGES = {
            Vec3d.ZERO,
            new Vec3d(1.0, 0.0, 0.0), new Vec3d(-1.0, 0.0, 0.0),
            new Vec3d(0.0, 0.0, 1.0), new Vec3d(0.0, 0.0, -1.0),
    };

    private Babies() {
    }

    /** "Pip  23m" per baby of this player's, nearest first. Empty when there are none. */
    public static List<String> lines(ServerWorld world, ServerPlayerEntity player) {
        List<TameableEntity> babies = mine(world, player);
        List<String> lines = new ArrayList<>();
        for (TameableEntity baby : babies.subList(0, Math.min(babies.size(), LISTED))) {
            lines.add(baby.getName().getString() + "  "
                    + Math.round(Math.sqrt(baby.squaredDistanceTo(player))) + "m");
        }
        return List.copyOf(lines);
    }

    /**
     * Jumps the player to their nearest baby.
     *
     * @return the baby's name, or {@code null} if they haven't got one here
     */
    public static @Nullable String goToNearest(ServerWorld world, ServerPlayerEntity player) {
        List<TameableEntity> babies = mine(world, player);
        if (babies.isEmpty()) {
            return null;
        }

        TameableEntity baby = babies.get(0);
        Vec3d landing = landingSpot(world, player, baby);

        world.playSound(null, player.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                player.getSoundCategory(), 0.6F, 1.4F);
        player.teleport(world, landing.x, landing.y, landing.z, Set.of(),
                player.getYaw(), player.getPitch(), true);
        world.playSound(null, baby.getBlockPos(), SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                player.getSoundCategory(), 0.6F, 1.4F);

        return baby.getName().getString();
    }

    /** This player's living babies in this world, nearest first. */
    private static List<TameableEntity> mine(ServerWorld world, ServerPlayerEntity player) {
        EntityType<?> type = Registries.ENTITY_TYPE.getOptionalValue(BABY_ID).orElse(null);
        if (type == null) {
            return List.of();
        }

        List<TameableEntity> found = new ArrayList<>();
        for (Entity entity : world.getEntitiesByType(type, Entity::isAlive)) {
            if (entity instanceof TameableEntity baby && baby.isOwner(player)) {
                found.add(baby);
            }
        }
        found.sort(Comparator.comparingDouble(baby -> baby.squaredDistanceTo(player)));
        return found;
    }

    /**
     * Somewhere next to the baby a player actually fits. A baby is small enough to stand in a
     * gap a player can't, so landing right on top of one can mean landing inside a wall.
     */
    private static Vec3d landingSpot(ServerWorld world, ServerPlayerEntity player, TameableEntity baby) {
        Vec3d at = baby.getEntityPos();
        for (Vec3d nudge : NUDGES) {
            Vec3d candidate = at.add(nudge);
            Box box = player.getBoundingBox().offset(candidate.subtract(player.getEntityPos()));
            if (world.isSpaceEmpty(player, box)) {
                return candidate;
            }
        }
        return at;
    }
}
