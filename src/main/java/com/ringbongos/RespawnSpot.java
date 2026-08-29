package com.ringbongos;

import net.minecraft.registry.tag.StructureTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.Heightmap;
import net.minecraft.world.World;
import net.minecraft.world.poi.PointOfInterestStorage;
import net.minecraft.world.poi.PointOfInterestTypes;

import org.jetbrains.annotations.Nullable;

import java.util.Set;

/**
 * Die, and you wake up somewhere more interesting than your bed.
 *
 * <p>First choice is inside the nearest stronghold; failing that, a villager's house; failing
 * that, wherever you would have respawned anyway. The searches run on the server thread and can
 * generate chunks nobody has visited, so they are capped and only ever run in the overworld.
 */
public final class RespawnSpot {
    /** How far out to look, in chunks. Vanilla's /locate uses 100; this is kinder to the tick. */
    private static final int SEARCH_CHUNKS = 64;

    /** Chunks either side of a find to load before looking inside it. */
    private static final int LOAD_RADIUS = 2;

    /** How far from the village centre a bed still counts as "this village", in blocks. */
    private static final int BED_RADIUS = 48;

    /** How far from the stronghold's marker to hunt for a room to stand in. */
    private static final int ROOM_RADIUS = 12;
    private static final int ROOM_HEIGHT = 20;

    private RespawnSpot() {
    }

    /** Moves a freshly respawned player somewhere worth waking up. */
    public static void afterRespawn(ServerPlayerEntity oldPlayer, ServerPlayerEntity player, boolean alive) {
        // `alive` means they came back through the End portal rather than dying.
        if (alive) {
            return;
        }
        if (!(player.getEntityWorld() instanceof ServerWorld world)
                || world.getRegistryKey() != World.OVERWORLD) {
            return;
        }

        BlockPos from = player.getBlockPos();

        BlockPos stronghold = world.locateStructure(StructureTags.EYE_OF_ENDER_LOCATED, from, SEARCH_CHUNKS, false);
        if (stronghold != null) {
            BlockPos room = findRoom(world, stronghold);
            if (room != null) {
                land(player, world, room, "respawn.ringbongos.stronghold");
                return;
            }
        }

        BlockPos village = world.locateStructure(StructureTags.VILLAGE, from, SEARCH_CHUNKS, false);
        if (village == null) {
            player.sendMessage(Text.translatable("respawn.ringbongos.nowhere"), false);
            return;
        }

        BlockPos bed = findBed(world, village);
        if (bed != null) {
            land(player, world, bed, "respawn.ringbongos.house");
        } else {
            land(player, world,
                    village.withY(world.getTopY(Heightmap.Type.MOTION_BLOCKING_NO_LEAVES, village.getX(), village.getZ())),
                    "respawn.ringbongos.village");
        }
    }

    private static void land(ServerPlayerEntity player, ServerWorld world, BlockPos spot, String message) {
        player.teleport(world, spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5, Set.of(),
                player.getYaw(), player.getPitch(), true);
        world.playSound(null, spot, SoundEvents.ENTITY_VILLAGER_YES, player.getSoundCategory(), 0.8F, 1.0F);
        player.sendMessage(Text.translatable(message, spot.getX(), spot.getY(), spot.getZ()), false);
    }

    /**
     * Somewhere inside the stronghold a player can actually stand: two blocks of air on a solid
     * floor. The position a structure search hands back is a marker, not a room, and is quite
     * happily buried in solid stone.
     */
    static @Nullable BlockPos findRoom(ServerWorld world, BlockPos marker) {
        load(world, marker);

        BlockPos best = null;
        double bestDistance = Double.MAX_VALUE;

        for (int x = -ROOM_RADIUS; x <= ROOM_RADIUS; x++) {
            for (int z = -ROOM_RADIUS; z <= ROOM_RADIUS; z++) {
                for (int y = -ROOM_HEIGHT; y <= ROOM_HEIGHT; y++) {
                    BlockPos pos = marker.add(x, y, z);
                    if (pos.getY() <= world.getBottomY() || pos.getY() + 1 >= world.getTopYInclusive()) {
                        continue;
                    }
                    if (!world.getBlockState(pos).isAir()
                            || !world.getBlockState(pos.up()).isAir()
                            || !world.getBlockState(pos.down()).isSolidBlock(world, pos.down())) {
                        continue;
                    }

                    double distance = pos.getSquaredDistance(marker);
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        best = pos;
                    }
                }
            }
        }
        return best;
    }

    /**
     * The nearest villager bed, which is as close to "inside a house" as the game will tell us.
     * Beds only become points of interest once their chunk has generated, so the chunks around the
     * village are pulled in before asking.
     */
    private static @Nullable BlockPos findBed(ServerWorld world, BlockPos village) {
        load(world, village);
        return world.getPointOfInterestStorage().getNearestPosition(
                        entry -> entry.matchesKey(PointOfInterestTypes.HOME),
                        village,
                        BED_RADIUS,
                        PointOfInterestStorage.OccupationStatus.ANY)
                .orElse(null);
    }

    static void load(ServerWorld world, BlockPos around) {
        int chunkX = around.getX() >> 4;
        int chunkZ = around.getZ() >> 4;
        for (int x = -LOAD_RADIUS; x <= LOAD_RADIUS; x++) {
            for (int z = -LOAD_RADIUS; z <= LOAD_RADIUS; z++) {
                world.getChunk(chunkX + x, chunkZ + z);
            }
        }
    }
}
