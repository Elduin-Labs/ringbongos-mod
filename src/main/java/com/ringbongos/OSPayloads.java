package com.ringbongos;

import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Everything Ring Bong OS says over the wire. */
public final class OSPayloads {
    private OSPayloads() {
    }

    /** "Send me a fresh screenful for the terminal at this position." */
    public record Refresh(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<Refresh> ID =
                new CustomPayload.Id<>(Identifier.of(RingBongOS.MOD_ID, "refresh"));
        public static final PacketCodec<RegistryByteBuf, Refresh> CODEC =
                PacketCodec.tuple(BlockPos.PACKET_CODEC, Refresh::pos, Refresh::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** "Ring the bong." Fired by the BONG app. */
    public record RingBong(BlockPos pos) implements CustomPayload {
        public static final CustomPayload.Id<RingBong> ID =
                new CustomPayload.Id<>(Identifier.of(RingBongOS.MOD_ID, "ring_bong"));
        public static final PacketCodec<RegistryByteBuf, RingBong> CODEC =
                PacketCodec.tuple(BlockPos.PACKET_CODEC, RingBong::pos, RingBong::new);

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** "F pressed — flip me between myself and Herobrine." */
    public record ToggleHerobrine() implements CustomPayload {
        public static final CustomPayload.Id<ToggleHerobrine> ID =
                new CustomPayload.Id<>(Identifier.of(RingBongOS.MOD_ID, "toggle_herobrine"));
        public static final PacketCodec<RegistryByteBuf, ToggleHerobrine> CODEC =
                PacketCodec.unit(new ToggleHerobrine());

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /** Server -> everyone: who is currently Herobrine. Sent whenever the set changes. */
    public record HerobrineSet(List<UUID> players) implements CustomPayload {
        public static final CustomPayload.Id<HerobrineSet> ID =
                new CustomPayload.Id<>(Identifier.of(RingBongOS.MOD_ID, "herobrine_set"));
        public static final PacketCodec<RegistryByteBuf, HerobrineSet> CODEC =
                new PacketCodec<RegistryByteBuf, HerobrineSet>() {
                    @Override
                    public void encode(RegistryByteBuf buf, HerobrineSet value) {
                        buf.writeVarInt(value.players().size());
                        for (UUID id : value.players()) {
                            buf.writeUuid(id);
                        }
                    }

                    @Override
                    public HerobrineSet decode(RegistryByteBuf buf) {
                        int count = buf.readVarInt();
                        List<UUID> ids = new ArrayList<>(count);
                        for (int i = 0; i < count; i++) {
                            ids.add(buf.readUuid());
                        }
                        return new HerobrineSet(List.copyOf(ids));
                    }
                };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    /**
     * Server -> client: one screenful of terminal state. Sent when the terminal is opened and
     * again on every refresh, so the apps only ever draw what the server told them.
     *
     * @param nearby "<name>  <distance>m" lines, nearest first
     * @param log    recent events, newest first
     */
    public record TerminalState(BlockPos pos, boolean powered, long uptimeTicks, long timeOfDay,
                                boolean raining, String dimension, List<String> nearby,
                                List<String> log) implements CustomPayload {
        public static final CustomPayload.Id<TerminalState> ID =
                new CustomPayload.Id<>(Identifier.of(RingBongOS.MOD_ID, "terminal_state"));

        /**
         * Hand-written rather than a tuple codec: eight fields, two of them lists, is past the
         * point where the generated tuples stay readable.
         */
        public static final PacketCodec<RegistryByteBuf, TerminalState> CODEC =
                new PacketCodec<RegistryByteBuf, TerminalState>() {
                    @Override
                    public void encode(RegistryByteBuf buf, TerminalState value) {
                        BlockPos.PACKET_CODEC.encode(buf, value.pos());
                        buf.writeBoolean(value.powered());
                        buf.writeVarLong(value.uptimeTicks());
                        buf.writeVarLong(value.timeOfDay());
                        buf.writeBoolean(value.raining());
                        buf.writeString(value.dimension());
                        writeLines(buf, value.nearby());
                        writeLines(buf, value.log());
                    }

                    @Override
                    public TerminalState decode(RegistryByteBuf buf) {
                        return new TerminalState(
                                BlockPos.PACKET_CODEC.decode(buf),
                                buf.readBoolean(),
                                buf.readVarLong(),
                                buf.readVarLong(),
                                buf.readBoolean(),
                                buf.readString(),
                                readLines(buf),
                                readLines(buf));
                    }
                };

        @Override
        public Id<? extends CustomPayload> getId() {
            return ID;
        }
    }

    private static void writeLines(RegistryByteBuf buf, List<String> lines) {
        buf.writeVarInt(lines.size());
        for (String line : lines) {
            buf.writeString(line);
        }
    }

    private static List<String> readLines(RegistryByteBuf buf) {
        int count = buf.readVarInt();
        List<String> lines = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            lines.add(buf.readString());
        }
        return List.copyOf(lines);
    }
}
