package com.ringbongos;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;

/**
 * A trampoline. Land on it and you go back up harder than you came down, and never take the
 * fall damage. Crouch on the way down if you would rather just stand on it.
 */
public class TrampolineBlock extends Block {
    public static final MapCodec<TrampolineBlock> CODEC = createCodec(TrampolineBlock::new);

    /** How much of the landing speed comes back. Above 1 it climbs, which is the fun part. */
    private static final double LIVING_BOUNCE = 1.35;
    private static final double ITEM_BOUNCE = 0.9;

    /** Without a ceiling on it a few bounces will fling you out of the world. */
    private static final double MAX_BOUNCE = 1.6;

    public TrampolineBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<TrampolineBlock> getCodec() {
        return CODEC;
    }

    /** Landing on a trampoline never hurts. */
    @Override
    public void onLandedUpon(World world, BlockState state, BlockPos pos, Entity entity, double fallDistance) {
        if (entity.isSneaking()) {
            super.onLandedUpon(world, state, pos, entity, fallDistance);
        }
    }

    @Override
    public void onEntityLand(BlockView world, Entity entity) {
        if (entity.isSneaking()) {
            super.onEntityLand(world, entity);
            return;
        }

        Vec3d velocity = entity.getVelocity();
        if (velocity.y >= 0.0) {
            return;
        }

        double bounce = entity instanceof LivingEntity ? LIVING_BOUNCE : ITEM_BOUNCE;
        entity.setVelocity(velocity.x, Math.min(-velocity.y * bounce, MAX_BOUNCE), velocity.z);
    }
}
