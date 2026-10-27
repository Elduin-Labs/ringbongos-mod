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
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Baby ravagers already in a world trail the nearest player and can be ridden. Nothing makes new
 * ones any more; this just keeps the existing ones tame.
 *
 * <p>Done by pushing the navigation around once a second rather than by adding AI goals, because
 * the babies are ordinary vanilla ravagers with their scale halved — there is no custom entity to
 * hang a goal on.
 */
public final class BabyRavagers {
    /** Command tag that marks one of ours. */
    public static final String TAG = "baby_ravager";

    /** Repathing every tick would be wasteful and jittery. */
    private static final int RETARGET_EVERY = 20;

    private static final double SPEED = 1.25;
    private static final double FOLLOW_RANGE = 32.0;

    private static int ticks;

    private BabyRavagers() {
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

            for (RavagerEntity baby : babies) {
                // Somebody is driving; leave the navigation alone or it fights the rider.
                if (baby.hasPassengers()) {
                    continue;
                }
                follow(world, baby);
            }
        }
    }

    /** Trails whoever is nearest. */
    private static void follow(ServerWorld world, RavagerEntity baby) {
        PlayerEntity owner = world.getClosestPlayer(baby, FOLLOW_RANGE);
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
}
