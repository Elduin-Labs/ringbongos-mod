package com.ringbongos;

import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The LOG app's backing store, and where each terminal's uptime is counted from.
 *
 * <p>Kept in server memory rather than on a block entity: a terminal's log is a session
 * thing, and it clears when the server restarts, same as its uptime.
 */
public final class TerminalLog {
    private static final int MAX_LINES = 12;

    private record Key(String dimension, BlockPos pos) {
    }

    private static final Map<Key, Deque<String>> LINES = new HashMap<>();
    private static final Map<Key, Long> BOOTED_AT = new HashMap<>();

    private TerminalLog() {
    }

    public static void append(ServerWorld world, BlockPos pos, String line) {
        Deque<String> lines = LINES.computeIfAbsent(key(world, pos), unused -> new ArrayDeque<>());
        lines.addFirst("[" + stamp(world) + "] " + line);
        while (lines.size() > MAX_LINES) {
            lines.removeLast();
        }
    }

    public static List<String> lines(ServerWorld world, BlockPos pos) {
        Deque<String> lines = LINES.get(key(world, pos));
        return lines == null ? List.of() : List.copyOf(new ArrayList<>(lines));
    }

    /** Ticks since this terminal was first talked to in this session. */
    public static long uptime(ServerWorld world, BlockPos pos) {
        long booted = BOOTED_AT.computeIfAbsent(key(world, pos), unused -> world.getTime());
        return Math.max(0L, world.getTime() - booted);
    }

    /** Forgets a terminal, so a rebuilt one boots fresh. */
    public static void forget(ServerWorld world, BlockPos pos) {
        Key key = key(world, pos);
        LINES.remove(key);
        BOOTED_AT.remove(key);
    }

    /** In-game clock, the same "HH:MM" the terminal draws in its status bar. */
    public static String stamp(ServerWorld world) {
        long ticks = world.getTimeOfDay() % 24000L;
        long hours = (ticks / 1000L + 6L) % 24L;
        long minutes = (ticks % 1000L) * 60L / 1000L;
        return String.format("%02d:%02d", hours, minutes);
    }

    private static Key key(ServerWorld world, BlockPos pos) {
        return new Key(world.getRegistryKey().getValue().toString(), pos.toImmutable());
    }
}
