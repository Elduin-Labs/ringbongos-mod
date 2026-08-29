package com.ringbongos;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.passive.VillagerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.world.ServerWorld;

import java.util.List;

/**
 * Villagers tagged {@code ringbongos.follower} trail the nearest player.
 *
 * <p>Vanilla villagers have no reason to follow anybody, so this pushes their navigation once a
 * second rather than adding an AI goal — the same approach the baby ravagers use, and for the same
 * reason: these are ordinary vanilla mobs with nothing custom to hang a goal on.
 *
 * <p>A tagged villager must have {@code NoAI} off. NoAI switches off pathfinding entirely, so a
 * frozen villager will simply ignore everything here.
 */
public final class Followers {
    public static final String TAG = "ringbongos.follower";

    private static final int RETARGET_EVERY = 20;
    private static final double SPEED = 0.9;
    private static final double RANGE = 48.0;

    /** Close enough. Any nearer and they shove you around trying to close the gap. */
    private static final double COMFORTABLE = 3.0;

    private static int ticks;

    private Followers() {
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % RETARGET_EVERY != 0) {
            return;
        }

        for (ServerWorld world : server.getWorlds()) {
            List<? extends VillagerEntity> followers = world.getEntitiesByType(EntityType.VILLAGER,
                    villager -> villager.isAlive() && villager.getCommandTags().contains(TAG));

            for (VillagerEntity follower : followers) {
                PlayerEntity target = world.getClosestPlayer(follower, RANGE);
                if (target == null) {
                    continue;
                }
                if (follower.squaredDistanceTo(target) <= COMFORTABLE * COMFORTABLE) {
                    follower.getNavigation().stop();
                    follower.getLookControl().lookAt(target);
                    continue;
                }
                follower.getNavigation().startMovingTo(target, SPEED);
            }
        }
    }
}
