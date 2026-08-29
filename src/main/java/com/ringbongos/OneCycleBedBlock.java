package com.ringbongos;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.List;

/**
 * The one cycle bed. Right-click it and it goes off like a bed in the End does, except this one
 * hits the dragon hard enough to end the fight on the spot.
 *
 * <p>The damage goes at {@link EnderDragonEntity#head} on purpose: damage aimed anywhere else on
 * the dragon is quartered, which is why beds have to be timed for the perch in vanilla.
 */
public class OneCycleBedBlock extends Block {
    public static final MapCodec<OneCycleBedBlock> CODEC = createCodec(OneCycleBedBlock::new);

    /** A dragon has 200 health. This leaves no argument about it. */
    private static final float DRAGON_DAMAGE = 250.0F;

    /** How far the blast reaches for dragon-hunting purposes. */
    private static final double REACH = 32.0;

    /** Same power as a vanilla bed going off. */
    private static final float BLAST = 5.0F;

    public OneCycleBedBlock(Settings settings) {
        super(settings);
    }

    @Override
    protected MapCodec<OneCycleBedBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player,
                                 BlockHitResult hit) {
        if (!(world instanceof ServerWorld serverWorld)) {
            return ActionResult.SUCCESS;
        }

        // Out of the way first, or the blast just spends itself breaking this block.
        serverWorld.setBlockState(pos, Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);

        Vec3d center = Vec3d.ofCenter(pos);
        serverWorld.createExplosion(player, center.x, center.y, center.z, BLAST,
                World.ExplosionSourceType.BLOCK);

        int hits = hitDragons(serverWorld, center, player);
        player.sendMessage(Text.translatable(
                hits > 0 ? "block.ringbongos.one_cycle_bed.hit" : "block.ringbongos.one_cycle_bed.miss"), true);
        return ActionResult.SUCCESS;
    }

    /** Puts {@link #DRAGON_DAMAGE} through the head of every dragon in range. */
    private int hitDragons(ServerWorld world, Vec3d center, PlayerEntity player) {
        List<? extends EnderDragonEntity> dragons =
                world.getEntitiesByType(EntityType.ENDER_DRAGON, dragon -> dragon.isAlive());

        DamageSource source = world.getDamageSources().explosion(null, player);
        int hits = 0;
        for (EnderDragonEntity dragon : dragons) {
            if (dragon.squaredDistanceTo(center) > REACH * REACH) {
                continue;
            }
            if (dragon.damagePart(world, dragon.head, source, DRAGON_DAMAGE)) {
                hits++;
            }
        }
        return hits;
    }
}
