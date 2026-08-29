package com.ringbongos;

import com.mojang.brigadier.CommandDispatcher;

import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;

import java.util.List;

/**
 * {@code /pants} — takes off everything you are wearing. Armour goes back into your inventory,
 * or on the floor if there is no room for it.
 */
public final class PantsCommand {
    private static final List<EquipmentSlot> WORN =
            List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET);

    private PantsCommand() {
    }

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(CommandManager.literal("pants")
                .executes(context -> strip(context.getSource().getPlayerOrThrow())));
    }

    private static int strip(ServerPlayerEntity player) {
        int taken = 0;
        for (EquipmentSlot slot : WORN) {
            ItemStack worn = player.getEquippedStack(slot);
            if (worn.isEmpty()) {
                continue;
            }
            player.equipStack(slot, ItemStack.EMPTY);
            if (!player.giveItemStack(worn.copy())) {
                player.dropItem(worn.copy(), false);
            }
            taken++;
        }

        if (taken == 0) {
            player.sendMessage(Text.translatable("commands.ringbongos.pants.already_bare"), false);
            return 0;
        }

        player.playSound(SoundEvents.ITEM_ARMOR_EQUIP_LEATHER.value(), 0.8F, 0.9F);
        player.sendMessage(Text.translatable("commands.ringbongos.pants.stripped", taken), false);
        return taken;
    }
}
