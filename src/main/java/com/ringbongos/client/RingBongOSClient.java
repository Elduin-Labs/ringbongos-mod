package com.ringbongos.client;

import com.ringbongos.OSPayloads;
import com.ringbongos.RingBongOS;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.EntityRendererRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.render.entity.ArrowEntityRenderer;
import net.minecraft.client.render.entity.FlyingItemEntityRenderer;
import net.minecraft.client.util.InputUtil;

import org.jetbrains.annotations.Nullable;

public class RingBongOSClient implements ClientModInitializer {
    /** Hold F to become Herobrine, press again to stop. */
    private static KeyBinding herobrineKey;

    /** Whatever the server last told us the terminal looks like. */
    public static @Nullable OSPayloads.TerminalState state;

    @Override
    public void onInitializeClient() {
        // A courier arrow is an arrow — the vanilla renderer draws it as-is.
        EntityRendererRegistry.register(RingBongOS.COURIER_ARROW_ENTITY, ArrowEntityRenderer::new);
        // A thrown villager leg is drawn as the item itself, spinning through the air.
        EntityRendererRegistry.register(RingBongOS.VILLAGER_LEG_ENTITY, FlyingItemEntityRenderer::new);

        herobrineKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.ringbongos.herobrine",
                InputUtil.Type.KEYSYM,
                InputUtil.GLFW_KEY_F,
                KeyBinding.Category.MISC));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (herobrineKey.wasPressed()) {
                ClientPlayNetworking.send(new OSPayloads.ToggleHerobrine());
            }
        });

        ClientPlayNetworking.registerGlobalReceiver(OSPayloads.HerobrineSet.ID, (payload, context) ->
                context.client().execute(() -> Herobrine.set(payload.players())));

        ClientPlayNetworking.registerGlobalReceiver(OSPayloads.TerminalState.ID, (payload, context) ->
                context.client().execute(() -> {
                    state = payload;
                    // The first packet is the terminal booting; later ones are refreshes.
                    MinecraftClient client = context.client();
                    if (!(client.currentScreen instanceof TerminalScreen)) {
                        client.setScreen(new TerminalScreen(payload.pos()));
                    }
                }));
    }
}
