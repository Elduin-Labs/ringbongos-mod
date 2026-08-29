package com.ringbongos;

import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * The giant pickaxe's appetite. One swing takes a sphere of world with it — but eating tens of
 * thousands of blocks inside a single tick is exactly how a server gets killed by its own
 * watchdog, so a swing becomes a queue that is chewed through a slice at a time.
 *
 * <p>The sphere is ordered by distance from the middle, so it visibly grows outward instead of
 * vanishing all at once. Nothing is exempt — bedrock included — so a swing near the floor of the
 * world opens a hole straight into the void.
 */
public final class GiantPickaxe {
    /** Radius of a single swing, in blocks. */
    private static final int RADIUS = 24;

    /** Blocks removed per server tick. The whole sphere takes about two seconds. */
    private static final int PER_TICK = 1500;

    /** Digs currently in progress, oldest first. */
    private static final List<Dig> DIGS = new ArrayList<>();

    private GiantPickaxe() {
    }

    /** Not a record: the haul grows as the dig eats its way outwards. */
    private static final class Dig {
        final ServerWorld world;
        final BlockPos center;
        final UUID digger;
        final Iterator<BlockPos> remaining;
        /** What every block dropped, merged by item so one swing is a handful of stacks. */
        final Map<Item, Integer> haul = new LinkedHashMap<>();
        /** The loot tables are rolled against the pickaxe itself, so ores behave normally. */
        final ItemStack tool = new ItemStack(RingBongOS.GIANT_PICKAXE);

        Dig(ServerWorld world, BlockPos center, UUID digger, Iterator<BlockPos> remaining) {
            this.world = world;
            this.center = center;
            this.digger = digger;
            this.remaining = remaining;
        }
    }

    /** Queues up a swing centred on {@code center}. */
    public static void swing(ServerWorld world, BlockPos center, UUID digger) {
        List<BlockPos> targets = new ArrayList<>();
        for (int x = -RADIUS; x <= RADIUS; x++) {
            for (int y = -RADIUS; y <= RADIUS; y++) {
                for (int z = -RADIUS; z <= RADIUS; z++) {
                    if (x * x + y * y + z * z <= RADIUS * RADIUS) {
                        targets.add(center.add(x, y, z));
                    }
                }
            }
        }
        // Nearest first, so the hole opens outwards from where you swung.
        targets.sort(Comparator.comparingDouble((BlockPos pos) -> pos.getSquaredDistance(center)));

        DIGS.add(new Dig(world, center, digger, targets.iterator()));

        world.playSound(null, center, SoundEvents.ENTITY_GENERIC_EXPLODE.value(), SoundCategory.BLOCKS,
                1.0F, 0.4F);
    }

    /** Chews through {@link #PER_TICK} blocks of the oldest dig still going. */
    public static void tick(MinecraftServer server) {
        if (DIGS.isEmpty()) {
            return;
        }

        Dig dig = DIGS.get(0);
        BlockState air = Blocks.AIR.getDefaultState();
        int budget = PER_TICK;

        while (budget > 0 && dig.remaining.hasNext()) {
            BlockPos pos = dig.remaining.next();
            if (pos.getY() < dig.world.getBottomY() || pos.getY() >= dig.world.getTopYInclusive()) {
                continue;
            }

            BlockState state = dig.world.getBlockState(pos);
            if (state.isAir()) {
                continue;
            }

            // Roll what the block would have dropped and bank it, rather than letting tens of
            // thousands of item entities loose at once.
            BlockEntity blockEntity = state.hasBlockEntity() ? dig.world.getBlockEntity(pos) : null;
            for (ItemStack drop : Block.getDroppedStacks(state, dig.world, pos, blockEntity, null, dig.tool)) {
                if (!drop.isEmpty()) {
                    dig.haul.merge(drop.getItem(), drop.getCount(), Integer::sum);
                }
            }

            // The block itself goes without dropping anything: the haul is already banked.
            dig.world.setBlockState(pos, air,
                    Block.NOTIFY_LISTENERS | Block.FORCE_STATE | Block.SKIP_DROPS);
            budget--;
        }

        if (!dig.remaining.hasNext()) {
            Vec3d center = Vec3d.ofCenter(dig.center);
            dig.world.spawnParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 12,
                    RADIUS / 3.0, RADIUS / 3.0, RADIUS / 3.0, 0.0);
            dig.world.playSound(null, dig.center, SoundEvents.BLOCK_ANVIL_LAND, SoundCategory.BLOCKS,
                    1.0F, 0.5F);
            payOut(server, dig);
            DIGS.remove(0);
        }
    }

    /** Hands the digger the whole haul, up to whatever their inventory will take. */
    private static void payOut(MinecraftServer server, Dig dig) {
        ServerPlayerEntity player = server.getPlayerManager().getPlayer(dig.digger);
        if (player == null) {
            return;
        }

        int collected = 0;
        int leftBehind = 0;
        boolean full = false;

        for (Map.Entry<Item, Integer> entry : dig.haul.entrySet()) {
            int owed = entry.getValue();
            int perStack = new ItemStack(entry.getKey()).getMaxCount();

            while (owed > 0) {
                int size = Math.min(owed, perStack);
                owed -= size;

                if (full) {
                    leftBehind += size;
                    continue;
                }

                ItemStack stack = new ItemStack(entry.getKey(), size);
                player.giveItemStack(stack);
                // giveItemStack leaves whatever did not fit in the stack it was handed.
                int stuck = stack.getCount();
                collected += size - stuck;
                if (stuck > 0) {
                    leftBehind += stuck;
                    full = true;
                }
            }
        }

        if (collected == 0 && leftBehind == 0) {
            player.sendMessage(Text.translatable("item.ringbongos.giant_pickaxe.nothing"), true);
            return;
        }

        int diamonds = dig.haul.getOrDefault(Items.DIAMOND, 0);
        player.playSound(SoundEvents.ENTITY_PLAYER_LEVELUP, 0.7F, 1.4F);
        player.sendMessage(leftBehind > 0
                ? Text.translatable("item.ringbongos.giant_pickaxe.haul_full", collected, diamonds, leftBehind)
                : Text.translatable("item.ringbongos.giant_pickaxe.haul", collected, diamonds), false);
    }
}
