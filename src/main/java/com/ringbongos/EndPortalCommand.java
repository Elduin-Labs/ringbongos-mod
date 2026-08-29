package com.ringbongos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.Blocks;
import net.minecraft.command.argument.ColumnPosArgumentType;
import net.minecraft.registry.tag.StructureTags;
import net.minecraft.util.math.ColumnPos;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * {@code /end} (and its longer name {@code /endportal}) — drops you in the end portal room of the
 * nearest stronghold, from wherever you are, the End included.
 *
 * <p>Neither {@code /locate} nor an ender eye will do this: both point at the stronghold's start
 * piece, and the portal room is somewhere else inside the maze. So this loads the stronghold and
 * goes looking for the portal frames themselves.
 */
public final class EndPortalCommand {
    /** How far out to look for a stronghold, in chunks. */
    private static final int SEARCH_CHUNKS = 64;

    /**
     * Chunks either side of the stronghold marker to load and sweep. A stronghold sprawls about
     * 112 blocks from its start piece, and at ±64 the portal room was often outside the box.
     */
    private static final int SWEEP_CHUNKS = 7;

    /** Strongholds live well below this; anything higher is wasted work. */
    private static final int SWEEP_TOP = 20;

    private EndPortalCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(build("end"));
        dispatcher.register(build("endportal"));
    }

    /** {@code /<name>} searches from where you are; {@code /<name> <x> <z>} searches from there. */
    private static LiteralArgumentBuilder<ServerCommandSource> build(String name) {
        return CommandManager.literal(name)
                // 1.21.11 replaced hasPermissionLevel with named checks; GAMEMASTERS is level 2.
                .requires(CommandManager.<ServerCommandSource>requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(context -> run(context.getSource(), null))
                .then(CommandManager.argument("from", ColumnPosArgumentType.columnPos())
                        .executes(context -> run(context.getSource(),
                                ColumnPosArgumentType.getColumnPos(context, "from"))));
    }

    private static int run(ServerCommandSource source, @Nullable ColumnPos from) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld overworld = source.getServer().getOverworld();

        // Given coordinates, search from those. Otherwise the player's own position - except in
        // the End or the Nether, where it means nothing, so world spawn stands in instead.
        BlockPos anchor;
        if (from != null) {
            anchor = new BlockPos(from.x(), 64, from.z());
        } else if (player.getEntityWorld().getRegistryKey() == World.OVERWORLD) {
            anchor = player.getBlockPos();
        } else {
            anchor = overworld.getSpawnPoint().getPos();
        }

        source.sendFeedback(() -> Text.translatable("commands.ringbongos.endportal.searching"), false);

        BlockPos stronghold = overworld.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, anchor,
                SEARCH_CHUNKS, false);
        if (stronghold == null) {
            source.sendFeedback(() -> Text.translatable("commands.ringbongos.endportal.no_stronghold"), false);
            return 0;
        }

        BlockPos frame = findPortalFrame(overworld, stronghold);
        if (frame == null) {
            source.sendFeedback(() -> Text.translatable("commands.ringbongos.endportal.no_portal",
                    stronghold.getX(), stronghold.getZ()), false);
            return 0;
        }

        // Stand somewhere real: the frames themselves are solid, and the lava pit is right there.
        BlockPos landing = RespawnSpot.findRoom(overworld, frame.up(2));
        BlockPos spot = landing != null ? landing : frame.up();

        player.teleport(overworld, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(),
                player.getYaw(), player.getPitch(), true);
        overworld.playSound(null, spot, SoundEvents.BLOCK_END_PORTAL_FRAME_FILL, player.getSoundCategory(),
                0.9F, 1.0F);
        source.sendFeedback(() -> Text.translatable("commands.ringbongos.endportal.found",
                frame.getX(), frame.getY(), frame.getZ()), false);
        return 1;
    }

    /** Sweeps the loaded stronghold for the nearest end portal frame. */
    private static @Nullable BlockPos findPortalFrame(ServerWorld world, BlockPos marker) {
        for (int x = -SWEEP_CHUNKS; x <= SWEEP_CHUNKS; x++) {
            for (int z = -SWEEP_CHUNKS; z <= SWEEP_CHUNKS; z++) {
                world.getChunk((marker.getX() >> 4) + x, (marker.getZ() >> 4) + z);
            }
        }

        int reach = SWEEP_CHUNKS * 16;
        int bottom = world.getBottomY();
        int top = Math.min(SWEEP_TOP, world.getTopYInclusive());

        BlockPos.Mutable cursor = new BlockPos.Mutable();
        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = marker.getX() - reach; x <= marker.getX() + reach; x++) {
            for (int z = marker.getZ() - reach; z <= marker.getZ() + reach; z++) {
                for (int y = bottom; y <= top; y++) {
                    cursor.set(x, y, z);
                    if (!world.getBlockState(cursor).isOf(Blocks.END_PORTAL_FRAME)) {
                        continue;
                    }
                    double distance = cursor.getSquaredDistance(marker);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = cursor.toImmutable();
                    }
                }
            }
        }
        return best;
    }
}
