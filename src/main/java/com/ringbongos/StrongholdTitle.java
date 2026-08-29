package com.ringbongos;

import net.minecraft.network.packet.s2c.play.TitleS2CPacket;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Walk into a stronghold and the screen tells you that you beat the game.
 *
 * <p>Asks the structure accessor whether the player is standing inside a stronghold's bounding box,
 * rather than hunting for stone bricks — which means it fires on the real structure and nowhere
 * else. The check runs once a second; every tick would be a lot of structure lookups for something
 * nobody can walk into that fast.
 *
 * <p>Plain text rather than a translation key on purpose: a new key needs the client restarted to
 * render, and this way the servers alone carry the change.
 */
public final class StrongholdTitle {
    private static final int CHECK_EVERY = 20;

    /** Who is inside one right now, so leaving and going back in says it again. */
    private static final Set<UUID> INSIDE = new HashSet<>();

    private static int ticks;

    private StrongholdTitle() {
    }

    public static void tick(MinecraftServer server) {
        if (++ticks % CHECK_EVERY != 0) {
            return;
        }

        for (ServerWorld world : server.getWorlds()) {
            for (ServerPlayerEntity player : world.getPlayers()) {
                boolean inside = world.getStructureAccessor()
                        .getStructureContaining(player.getBlockPos(), StructureTags.EYE_OF_ENDER_LOCATED)
                        .hasChildren();

                if (inside) {
                    if (INSIDE.add(player.getUuid())) {
                        announce(player);
                    }
                } else {
                    INSIDE.remove(player.getUuid());
                }
            }
        }
    }

    private static void announce(ServerPlayerEntity player) {
        player.networkHandler.sendPacket(new TitleS2CPacket(
                Text.literal("Beat the game").formatted(Formatting.GOLD, Formatting.BOLD)));
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.8F, 1.0F);
    }

    /** Somebody left, so they get told again next time. */
    public static void forget(ServerPlayerEntity player) {
        INSIDE.remove(player.getUuid());
    }
}
