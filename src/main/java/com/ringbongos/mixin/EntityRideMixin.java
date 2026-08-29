package com.ringbongos.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a player be ridden.
 *
 * <p>{@code Entity.startRiding} gives up server-side when the vehicle's type is not saveable, and
 * {@code EntityType.PLAYER} is built with saving switched off — so mounting a player returns false
 * every time, silently, with nothing logged. The check is only reached off-thread of the client
 * ({@code world.isClient()} skips it), which is why this looks like it works until it is tried on
 * a server.
 *
 * <p>This redirects that one call, and only that one: every other entity type still answers for
 * itself.
 */
@Mixin(Entity.class)
public class EntityRideMixin {
    @Redirect(
            method = "startRiding(Lnet/minecraft/entity/Entity;ZZ)Z",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/EntityType;isSaveable()Z"))
    private boolean ringbongos$letPlayersBeRidden(EntityType<?> type) {
        return type == EntityType.PLAYER || type.isSaveable();
    }
}
