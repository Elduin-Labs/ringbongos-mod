package com.ringbongos;

import net.minecraft.block.entity.HopperBlockEntity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * An arrow that makes a delivery. Hit a container with it and whatever the shooter is holding
 * in their off hand goes into that container, from wherever they shot it.
 *
 * <p>It extends {@link ArrowEntity} so the vanilla arrow renderer can draw it unchanged.
 */
public class CourierArrowEntity extends ArrowEntity {
    public CourierArrowEntity(EntityType<? extends CourierArrowEntity> type, World world) {
        super(type, world);
    }

    public CourierArrowEntity(World world, LivingEntity shooter, ItemStack stack, ItemStack weapon) {
        super(world, shooter, stack, weapon);
    }

    public CourierArrowEntity(World world, double x, double y, double z, ItemStack stack, ItemStack weapon) {
        super(world, x, y, z, stack, weapon);
    }

    @Override
    protected ItemStack getDefaultItemStack() {
        return new ItemStack(RingBongOS.COURIER_ARROW);
    }

    @Override
    protected void onBlockHit(BlockHitResult hit) {
        super.onBlockHit(hit);
        if (getEntityWorld() instanceof ServerWorld world && getOwner() instanceof PlayerEntity shooter) {
            deliver(world, hit.getBlockPos(), shooter);
        }
    }

    /** Moves as much of the shooter's cargo as fits into the container that was hit. */
    private void deliver(ServerWorld world, BlockPos pos, PlayerEntity shooter) {
        ItemStack cargo = shooter.getStackInHand(Hand.OFF_HAND);
        if (cargo.isEmpty()) {
            shooter.sendMessage(Text.translatable("item.ringbongos.courier_arrow.empty_handed"), true);
            return;
        }

        Inventory target = HopperBlockEntity.getInventoryAt(world, pos);
        if (target == null) {
            shooter.sendMessage(Text.translatable("item.ringbongos.courier_arrow.no_container"), true);
            return;
        }

        int before = cargo.getCount();
        ItemStack leftover = HopperBlockEntity.transfer(null, target, cargo.copy(), null);
        int moved = before - leftover.getCount();
        if (moved <= 0) {
            shooter.sendMessage(Text.translatable("item.ringbongos.courier_arrow.full"), true);
            return;
        }

        // Read the name before decrementing: delivering the last of a stack empties the hand.
        Text name = cargo.getName();
        cargo.decrement(moved);
        target.markDirty();
        world.playSound(null, pos, SoundEvents.ENTITY_ITEM_PICKUP, SoundCategory.BLOCKS, 0.6F, 1.2F);
        shooter.sendMessage(Text.translatable("item.ringbongos.courier_arrow.delivered", moved, name), true);
    }
}
