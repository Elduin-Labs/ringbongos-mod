package com.ringbongos;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Where the baby ravagers go. A thrown villager leg is a lure they run to; once it has gone stale
 * they trail whoever threw it instead.
 *
 * <p>Done by pushing the navigation around once a second rather than by adding AI goals, because
 * the babies are ordinary vanilla ravagers with their scale halved — there is no custom entity to
 * hang a goal on.
 */
public final class BabyRavagers {
    /** Command tag that marks one of ours. */
    public static final String TAG = "baby_ravager";

    /** How long a landed leg keeps their attention. */
    private static final int LURE_TICKS = 300;

    /** Repathing every tick would be wasteful and jittery. */
    private static final int RETARGET_EVERY = 20;

    private static final double SPEED = 1.25;
    private static final double LURE_RANGE = 48.0;
    private static final double FOLLOW_RANGE = 32.0;

    private record Lure(Vec3d pos, @Nullable UUID thrower, int expiresAt) {
    }

    private static final Map<String, Lure> LURES = new HashMap<>();
    private static int ticks;

    private BabyRavagers() {
    }

    /** A leg just landed: everything nearby should come and look at it. */
    public static void legLanded(ServerWorld world, Vec3d pos, @Nullable UUID thrower) {
        LURES.put(key(world), new Lure(pos, thrower, ticks + LURE_TICKS));
    }

    public static void tick(MinecraftServer server) {
        ticks++;
        if (ticks % RETARGET_EVERY != 0) {
            return;
        }

        for (ServerWorld world : server.getWorlds()) {
            List<? extends RavagerEntity> babies = world.getEntitiesByType(EntityType.RAVAGER,
                    ravager -> ravager.isAlive() && ravager.getCommandTags().contains(TAG));
            if (babies.isEmpty()) {
                continue;
            }

            Lure lure = LURES.get(key(world));
            if (lure != null && ticks > lure.expiresAt()) {
                LURES.remove(key(world));
                lure = null;
            }

            for (RavagerEntity baby : babies) {
                // Somebody is driving; leave the navigation alone or it fights the rider.
                if (baby.hasPassengers()) {
                    continue;
                }
                if (lure != null && baby.squaredDistanceTo(lure.pos()) <= LURE_RANGE * LURE_RANGE) {
                    baby.getNavigation().startMovingTo(lure.pos().x, lure.pos().y, lure.pos().z, SPEED);
                    continue;
                }
                follow(world, baby, lure == null ? null : lure.thrower());
            }
        }
    }

    /** Trails the thrower, or whoever is nearest if they have gone. */
    private static void follow(ServerWorld world, RavagerEntity baby, @Nullable UUID thrower) {
        PlayerEntity owner = thrower == null ? null : world.getPlayerByUuid(thrower);
        if (owner == null) {
            owner = world.getClosestPlayer(baby, FOLLOW_RANGE);
        }
        if (owner == null || baby.squaredDistanceTo(owner) > FOLLOW_RANGE * FOLLOW_RANGE) {
            return;
        }

        // A pet that mauls you is not a pet.
        if (baby.getTarget() == owner) {
            baby.setTarget(null);
        }

        if (baby.squaredDistanceTo(owner) > 9.0) {
            baby.getNavigation().startMovingTo(owner, SPEED);
        }
    }

    /** Right-click a baby with an empty hand to get on its back. */
    public static ActionResult interact(net.minecraft.entity.player.PlayerEntity player, World world,
                                        Hand hand, Entity target, @Nullable EntityHitResult hit) {
        if (hand != Hand.MAIN_HAND || world.isClient()) {
            return ActionResult.PASS;
        }
        if (!(target instanceof RavagerEntity baby) || !baby.getCommandTags().contains(TAG)) {
            return ActionResult.PASS;
        }
        if (!player.getStackInHand(hand).isEmpty() || player.isSneaking()) {
            return ActionResult.PASS;
        }

        return player.startRiding(baby) ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    private static String key(World world) {
        return world.getRegistryKey().getValue().toString();
    }
}
