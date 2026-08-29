package com.ringbongos;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.world.World;

/** Throw it. Whatever it hits, a baby ravager comes out of it. */
public class VillagerLegItem extends Item {
    public VillagerLegItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        world.playSound(null, user.getX(), user.getY(), user.getZ(), SoundEvents.ENTITY_SNOWBALL_THROW,
                SoundCategory.PLAYERS, 0.6F, 0.5F);

        if (world instanceof ServerWorld serverWorld) {
            ThrownVillagerLegEntity leg = new ThrownVillagerLegEntity(world, user, stack.copyWithCount(1));
            leg.setVelocity(user, user.getPitch(), user.getYaw(), 0.0F, 1.5F, 1.0F);
            serverWorld.spawnEntity(leg);
        }

        if (!user.isCreative()) {
            stack.decrement(1);
        }
        return ActionResult.SUCCESS;
    }
}
