package com.ringbongos;

import com.mojang.serialization.MapCodec;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.HorizontalFacingBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.IntProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.BlockMirror;
import net.minecraft.util.BlockRotation;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.block.WireOrientation;

import org.jetbrains.annotations.Nullable;

/**
 * A wall panel running Ring Bong OS. Right-click it to open the OS; its BONG app rings a
 * two-tone chime and pulses redstone out of the back while the chime lasts.
 *
 * <p>The chime lives entirely in the {@link #STAGE} property and scheduled block ticks, so
 * the block needs no block entity.
 */
public class BongTerminalBlock extends HorizontalFacingBlock {
    public static final MapCodec<BongTerminalBlock> CODEC = createCodec(BongTerminalBlock::new);

    /** 0 idle, 1 "ring", 2 "bong". Anything but idle is putting out redstone. */
    public static final IntProperty STAGE = IntProperty.of("stage", 0, 2);

    private static final int IDLE = 0;
    private static final int RING = 1;
    private static final int BONG = 2;

    /** How far away the OS still accepts your button presses. */
    private static final double REACH = 8.0;

    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(3.0, 3.0, 13.0, 13.0, 13.0, 16.0);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(0.0, 3.0, 3.0, 3.0, 13.0, 13.0);
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(3.0, 3.0, 0.0, 13.0, 13.0, 3.0);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(13.0, 3.0, 3.0, 16.0, 13.0, 13.0);

    public BongTerminalBlock(Settings settings) {
        super(settings);
        setDefaultState(getDefaultState().with(FACING, Direction.NORTH).with(STAGE, IDLE));
    }

    @Override
    protected MapCodec<BongTerminalBlock> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING, STAGE);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        return getDefaultState().with(FACING, ctx.getHorizontalPlayerFacing().getOpposite());
    }

    @Override
    protected VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
            default -> NORTH_SHAPE;
        };
    }

    @Override
    protected BlockState rotate(BlockState state, BlockRotation rotation) {
        return state.with(FACING, rotation.rotate(state.get(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, BlockMirror mirror) {
        return state.rotate(mirror.getRotation(state.get(FACING)));
    }

    /** A freshly placed terminal boots clean, even on a spot where one stood before. */
    @Override
    public void onPlaced(World world, BlockPos pos, BlockState state, @Nullable LivingEntity placer,
                           ItemStack stack) {
        if (world instanceof ServerWorld serverWorld) {
            TerminalLog.forget(serverWorld, pos);
        }
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        if (world instanceof ServerWorld serverWorld && player instanceof net.minecraft.server.network.ServerPlayerEntity serverPlayer) {
            TerminalLog.append(serverWorld, pos, "login " + player.getGameProfile().name());
            RingBongOS.sendState(serverPlayer, pos);
            serverPlayer.playSound(SoundEvents.BLOCK_NOTE_BLOCK_HAT.value(), 0.4F, 1.8F);
        }
        // The screen itself opens client-side, off the back of the state packet.
        return ActionResult.SUCCESS;
    }

    @Override
    protected void neighborUpdate(BlockState state, World world, BlockPos pos, Block sourceBlock,
                                  @Nullable WireOrientation wireOrientation, boolean notify) {
        if (world instanceof ServerWorld serverWorld && state.get(STAGE) == IDLE
                && world.isReceivingRedstonePower(pos)) {
            ring(serverWorld, pos, state, "redstone");
        }
    }

    /** The terminal is its own redstone source while the chime is going. */
    @Override
    protected boolean emitsRedstonePower(BlockState state) {
        return true;
    }

    @Override
    protected int getWeakRedstonePower(BlockState state, BlockView world, BlockPos pos, Direction direction) {
        return state.get(STAGE) == IDLE ? 0 : 15;
    }

    /** Entry point for the BONG app. Ignores presses from too far away, or mid-chime. */
    public void ringFromOS(ServerWorld world, BlockPos pos, PlayerEntity caller) {
        BlockState state = world.getBlockState(pos);
        if (!state.isOf(this) || state.get(STAGE) != IDLE) {
            return;
        }
        if (caller.squaredDistanceTo(Vec3d.ofCenter(pos)) > REACH * REACH) {
            return;
        }
        ring(world, pos, state, caller.getGameProfile().name());
    }

    private void ring(ServerWorld world, BlockPos pos, BlockState state, String source) {
        TerminalLog.append(world, pos, "bong  (" + source + ")");
        enter(world, pos, state, RING);
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        switch (state.get(STAGE)) {
            case RING -> enter(world, pos, state, BONG);
            default -> world.setBlockState(pos, state.with(STAGE, IDLE), Block.NOTIFY_ALL);
        }
    }

    /** Moves to {@code stage}, plays its half of the chime, and schedules what follows. */
    private void enter(ServerWorld world, BlockPos pos, BlockState state, int stage) {
        world.setBlockState(pos, state.with(STAGE, stage), Block.NOTIFY_ALL);
        Vec3d center = Vec3d.ofCenter(pos);

        switch (stage) {
            case RING -> {
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS,
                        1.0F, 1.6F);
                world.spawnParticles(ParticleTypes.NOTE, center.x, center.y + 0.3, center.z, 4,
                        0.2, 0.2, 0.2, 0.0);
                world.scheduleBlockTick(pos, this, 10);
            }
            case BONG -> {
                world.playSound(null, pos, SoundEvents.BLOCK_BELL_RESONATE, SoundCategory.BLOCKS,
                        1.0F, 0.7F);
                world.playSound(null, pos, SoundEvents.BLOCK_NOTE_BLOCK_BELL.value(), SoundCategory.BLOCKS,
                        0.9F, 0.6F);
                world.spawnParticles(ParticleTypes.NOTE, center.x, center.y + 0.3, center.z, 8,
                        0.3, 0.3, 0.3, 0.0);
                world.scheduleBlockTick(pos, this, 20);
            }
            default -> {
            }
        }
    }
}
