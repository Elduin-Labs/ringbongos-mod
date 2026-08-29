package com.ringbongos;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

/** {@code /givetrampoline} — puts a trampoline in your hands, wherever you are. */
public final class TrampolineCommand {
    private TrampolineCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("givetrampoline")
                .executes(context -> run(context.getSource())));
    }

    private static int run(ServerCommandSource source) throws CommandSyntaxException {
        ServerPlayerEntity player = source.getPlayerOrThrow();
        ItemStack stack = new ItemStack(RingBongOS.TRAMPOLINE_ITEM);

        if (!player.giveItemStack(stack)) {
            player.dropItem(stack, false);
        }

        player.playSound(SoundEvents.BLOCK_WOOL_PLACE, 0.8F, 1.4F);
        source.sendFeedback(() -> Text.translatable("commands.ringbongos.trampoline.given"), false);
        return 1;
    }
}
