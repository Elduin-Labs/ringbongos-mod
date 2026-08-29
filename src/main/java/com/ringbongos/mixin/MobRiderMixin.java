package com.ringbongos.mixin;

import com.ringbongos.BabyRavagers;

import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.mob.MobEntity;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.player.PlayerEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hands the steering of a baby ravager to whoever is sitting on it.
 *
 * <p>Vanilla already knows how to let a passenger drive — that is how a saddled pig works. All it
 * asks is that the mob name its driver, which nothing does by default, so a ridden ravager just
 * carries on with its own plans. Naming the rider here is enough for the rest of vanilla's movement
 * code to take over.
 *
 * <p>Injected on {@link MobEntity} rather than {@link RavagerEntity} because the ravager does not
 * override the method, and a mixin cannot inject into a method its target class inherits.
 */
@Mixin(MobEntity.class)
public abstract class MobRiderMixin {
    @Inject(method = "getControllingPassenger", at = @At("HEAD"), cancellable = true)
    private void ringbongos$babyRavagerDriver(CallbackInfoReturnable<LivingEntity> info) {
        Object self = this;
        if (!(self instanceof RavagerEntity ravager) || !ravager.getCommandTags().contains(BabyRavagers.TAG)) {
            return;
        }
        Entity first = ravager.getFirstPassenger();
        if (first instanceof PlayerEntity player) {
            info.setReturnValue(player);
        }
    }
}
