package com.ringbongos;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Riding other players. Put a saddle on someone, then right-click them with a carrot on a stick
 * and you climb on; sneak to get off.
 *
 * <p>Vanilla's {@code /ride} refuses with "Players can't be ridden", but that check lives in the
 * command, not the engine — {@code Entity#startRiding} has no player case at all, so mod code can
 * simply do it.
 *
 * <p>Saddles are held in server memory, so everyone is unsaddled again after a restart.
 */
public final class RideablePlayers {
    private static final Set<UUID> SADDLED = new HashSet<>();

    private RideablePlayers() {
    }

    /** Right-click handling for saddling somebody, and for getting on them afterwards. */
    public static ActionResult interact(PlayerEntity player, World world, Hand hand, Entity target,
                                        @Nullable EntityHitResult hit) {
        if (hand != Hand.MAIN_HAND || !(world instanceof ServerWorld serverWorld)) {
            return ActionResult.PASS;
        }
        if (!(player instanceof ServerPlayerEntity rider) || !(target instanceof ServerPlayerEntity ridden)) {
            return ActionResult.PASS;
        }
        if (rider == ridden) {
            return ActionResult.PASS;
        }

        ItemStack held = player.getStackInHand(hand);
        if (held.isOf(Items.SADDLE)) {
            return saddle(serverWorld, rider, ridden, held);
        }
        if (held.isOf(Items.CARROT_ON_A_STICK)) {
            return mount(serverWorld, rider, ridden);
        }
        return ActionResult.PASS;
    }

    private static ActionResult saddle(ServerWorld world, ServerPlayerEntity rider, ServerPlayerEntity ridden,
                                       ItemStack saddleStack) {
        if (!SADDLED.add(ridden.getUuid())) {
            rider.sendMessage(Text.translatable("ride.ringbongos.already_saddled", ridden.getGameProfile().name()), true);
            return ActionResult.SUCCESS;
        }

        if (!rider.isCreative()) {
            saddleStack.decrement(1);
        }

        world.playSound(null, ridden.getBlockPos(), SoundEvents.ENTITY_HORSE_SADDLE.value(), SoundCategory.PLAYERS, 1.0F, 1.0F);
        rider.sendMessage(Text.translatable("ride.ringbongos.saddled", ridden.getGameProfile().name()), true);
        ridden.sendMessage(Text.translatable("ride.ringbongos.you_are_saddled", rider.getGameProfile().name()), false);
        return ActionResult.SUCCESS;
    }

    private static ActionResult mount(ServerWorld world, ServerPlayerEntity rider, ServerPlayerEntity ridden) {
        if (!SADDLED.contains(ridden.getUuid())) {
            rider.sendMessage(Text.translatable("ride.ringbongos.no_saddle", ridden.getGameProfile().name()), true);
            return ActionResult.SUCCESS;
        }
        if (ridden.hasVehicle() && ridden.getVehicle() == rider) {
            rider.sendMessage(Text.translatable("ride.ringbongos.already_carrying"), true);
            return ActionResult.SUCCESS;
        }

        if (rider.startRiding(ridden)) {
            world.playSound(null, ridden.getBlockPos(), SoundEvents.ENTITY_HORSE_ARMOR.value(), SoundCategory.PLAYERS, 0.8F, 1.2F);
            rider.sendMessage(Text.translatable("ride.ringbongos.mounted", ridden.getGameProfile().name()), true);
            ridden.sendMessage(Text.translatable("ride.ringbongos.carrying", rider.getGameProfile().name()), false);
        } else {
            rider.sendMessage(Text.translatable("ride.ringbongos.mount_failed"), true);
        }
        return ActionResult.SUCCESS;
    }

    /** Somebody left, so their saddle goes with them. */
    public static void forget(ServerPlayerEntity player) {
        SADDLED.remove(player.getUuid());
    }
}
