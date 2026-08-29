package com.ringbongos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.boss.dragon.EnderDragonEntity;
import net.minecraft.entity.boss.dragon.EnderDragonFight;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Set;

/**
 * {@code /dragon} — puts you in the End with a dragon to fight, respawning one if the last is dead.
 *
 * <p>Messages here are plain literals rather than translation keys on purpose: a new key needs the
 * client restarted to render as anything but the raw key, and this command only needed the server.
 */
public final class DragonCommand {
    /** Vanilla's arrival platform: a 5x5 obsidian pad centred here, with air above it. */
    private static final BlockPos PLATFORM = new BlockPos(100, 49, 0);

    private DragonCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("dragon")
                .requires(CommandManager.<ServerCommandSource>requirePermissionLevel(CommandManager.GAMEMASTERS_CHECK))
                .executes(context -> run(context.getSource())));
    }

    private static int run(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ServerWorld end = source.getServer().getWorld(World.END);
        if (end == null) {
            source.sendFeedback(() -> Text.literal("This server has no End"), false);
            return 0;
        }

        List<? extends EnderDragonEntity> dragons =
                end.getEntitiesByType(EntityType.ENDER_DRAGON, dragon -> dragon.isAlive());

        String state;
        if (!dragons.isEmpty()) {
            state = "A dragon is already up";
        } else {
            EnderDragonFight fight = end.getEnderDragonFight();
            if (fight == null) {
                state = "No dragon, and this End has no dragon fight to restart";
            } else {
                fight.respawnDragon();
                state = "No dragon was left, so one is being summoned - watch the pillars";
            }
        }

        BlockPos landing = findLanding(end);
        player.teleport(end, landing.getX() + 0.5, landing.getY(), landing.getZ() + 0.5, Set.of(),
                player.getYaw(), player.getPitch(), true);
        end.playSound(null, landing, SoundEvents.ENTITY_ENDER_DRAGON_GROWL, player.getSoundCategory(), 1.0F, 1.0F);

        String message = state;
        source.sendFeedback(() -> Text.literal(message), false);
        return 1;
    }

    /**
     * Somewhere in the End to stand. The centre island is the interesting place to be, so that is
     * tried first; failing that the arrival platform is built the same way vanilla builds it, since
     * a plain teleport into the End otherwise drops you through the void.
     */
    private static BlockPos findLanding(ServerWorld end) {
        BlockPos island = RespawnSpot.findRoom(end, new BlockPos(0, 70, 0));
        if (island != null) {
            return island;
        }
        return buildPlatform(end);
    }

    private static BlockPos buildPlatform(ServerWorld end) {
        RespawnSpot.load(end, PLATFORM);

        BlockPos floor = PLATFORM.down();
        for (int x = -2; x <= 2; x++) {
            for (int z = -2; z <= 2; z++) {
                end.setBlockState(floor.add(x, 0, z), Blocks.OBSIDIAN.getDefaultState(), Block.NOTIFY_ALL);
                for (int y = 0; y < 3; y++) {
                    end.setBlockState(PLATFORM.add(x, y, z), Blocks.AIR.getDefaultState(), Block.NOTIFY_ALL);
                }
            }
        }
        return PLATFORM;
    }

    /** Unused hook kept out of the way: the nearest living dragon, when one is wanted. */
    static @Nullable EnderDragonEntity nearestDragon(ServerWorld end) {
        List<? extends EnderDragonEntity> dragons =
                end.getEntitiesByType(EntityType.ENDER_DRAGON, dragon -> dragon.isAlive());
        return dragons.isEmpty() ? null : dragons.get(0);
    }
}
