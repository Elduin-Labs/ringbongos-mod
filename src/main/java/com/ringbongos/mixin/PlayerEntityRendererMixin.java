package com.ringbongos.mixin;

import com.ringbongos.client.Herobrine;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Swaps the skin of anyone the server has flagged as Herobrine. The render state carries the
 * entity id, which is enough to find out who is being drawn.
 */
@Mixin(PlayerEntityRenderer.class)
public class PlayerEntityRendererMixin {
    @Inject(method = "getTexture(Lnet/minecraft/client/render/entity/state/PlayerEntityRenderState;)Lnet/minecraft/util/Identifier;",
            at = @At("HEAD"), cancellable = true)
    private void ringbongos$herobrineSkin(PlayerEntityRenderState state,
                                          CallbackInfoReturnable<Identifier> info) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null) {
            return;
        }
        Entity entity = client.world.getEntityById(state.id);
        if (entity != null && Herobrine.is(entity.getUuid())) {
            info.setReturnValue(Herobrine.SKIN);
        }
    }
}
