package com.ringbongos;

import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.ProjectileEntity;
import net.minecraft.item.ArrowItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Position;
import net.minecraft.world.World;

import java.util.function.Consumer;

/** Shoots a {@link CourierArrowEntity}. Works from a bow, a crossbow, or a dispenser. */
public class CourierArrowItem extends ArrowItem {
    public CourierArrowItem(Item.Settings settings) {
        super(settings);
    }

    @Override
    public PersistentProjectileEntity createArrow(World world, ItemStack stack, LivingEntity shooter,
                                                  ItemStack weapon) {
        return new CourierArrowEntity(world, shooter, stack.copyWithCount(1), weapon);
    }

    @Override
    public ProjectileEntity createEntity(World world, Position pos, ItemStack stack, Direction direction) {
        return new CourierArrowEntity(world, pos.getX(), pos.getY(), pos.getZ(), stack.copyWithCount(1),
                ItemStack.EMPTY);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent display,
                              Consumer<Text> tooltip, TooltipType type) {
        tooltip.accept(Text.translatable("item.ringbongos.courier_arrow.tip").formatted(Formatting.GRAY));
    }
}
