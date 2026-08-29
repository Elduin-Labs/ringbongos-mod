package com.ringbongos;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Who is currently Herobrine. Server-side truth, mirrored to every client so everyone sees
 * the white eyes — not just the person wearing them.
 *
 * <p>Held in memory only: nobody stays Herobrine across a restart.
 */
public final class HerobrineManager {
    private static final Set<UUID> HEROBRINE = new HashSet<>();

    private HerobrineManager() {
    }

    /** Flips a player between themselves and Herobrine. */
    public static void toggle(ServerPlayerEntity player) {
        UUID id = player.getUuid();
        boolean becoming = !HEROBRINE.contains(id);
        if (becoming) {
            HEROBRINE.add(id);
        } else {
            HEROBRINE.remove(id);
        }

        ServerWorld world = (ServerWorld) player.getEntityWorld();
        world.playSound(null, player.getBlockPos(),
                becoming ? SoundEvents.AMBIENT_CAVE.value() : SoundEvents.ENTITY_ENDERMAN_TELEPORT,
                SoundCategory.PLAYERS, 1.0F, becoming ? 0.6F : 1.0F);
        world.spawnParticles(becoming ? ParticleTypes.SMOKE : ParticleTypes.POOF,
                player.getX(), player.getY() + 1.0, player.getZ(), 30, 0.4, 0.8, 0.4, 0.02);

        if (becoming) {
            // Eyes that carry in the dark.
            player.addStatusEffect(new StatusEffectInstance(StatusEffects.NIGHT_VISION, StatusEffectInstance.INFINITE, 0, false, false));
            player.sendMessage(Text.translatable("key.ringbongos.herobrine.on").formatted(Formatting.WHITE), true);
        } else {
            player.removeStatusEffect(StatusEffects.NIGHT_VISION);
            player.sendMessage(Text.translatable("key.ringbongos.herobrine.off").formatted(Formatting.GRAY), true);
        }

        broadcast(player.getEntityWorld().getServer());
    }

    /** Somebody left, so they are nobody's problem any more. */
    public static void forget(ServerPlayerEntity player) {
        if (HEROBRINE.remove(player.getUuid())) {
            broadcast(player.getEntityWorld().getServer());
        }
    }

    /** Brings one client up to date — used when a player joins. */
    public static void sendTo(ServerPlayerEntity player) {
        ServerPlayNetworking.send(player, new OSPayloads.HerobrineSet(List.copyOf(HEROBRINE)));
    }

    private static void broadcast(MinecraftServer server) {
        if (server == null) {
            return;
        }
        OSPayloads.HerobrineSet payload = new OSPayloads.HerobrineSet(List.copyOf(HEROBRINE));
        for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
            ServerPlayNetworking.send(player, payload);
        }
    }
}
