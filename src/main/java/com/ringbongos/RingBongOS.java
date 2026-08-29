package com.ringbongos;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.AbstractBlock;
import net.minecraft.block.Block;
import net.minecraft.block.piston.PistonBehavior;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.SpawnGroup;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroup;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ToolMaterial;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class RingBongOS implements ModInitializer {
    public static final String MOD_ID = "ringbongos";

    /** Bing Bong items that share the Ring Bong tab, if that mod is around. */
    private static final List<String> BING_BONG_ITEMS = List.of("phone", "doorbell");

    /** How far the PING app can see. */
    private static final double PING_RANGE = 64.0;

    public static final RegistryKey<Block> BONG_TERMINAL_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "bong_terminal"));

    public static final Block BONG_TERMINAL = Registry.register(
            Registries.BLOCK,
            BONG_TERMINAL_KEY,
            new BongTerminalBlock(AbstractBlock.Settings.create()
                    .registryKey(BONG_TERMINAL_KEY)
                    .strength(1.0F)
                    .sounds(BlockSoundGroup.METAL)
                    .noCollision()
                    .nonOpaque()
                    .pistonBehavior(PistonBehavior.DESTROY)
                    // The panel glows brighter while it is chiming.
                    .luminance(state -> state.get(BongTerminalBlock.STAGE) == 0 ? 5 : 11))
    );

    public static final RegistryKey<Item> BONG_TERMINAL_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "bong_terminal"));

    public static final Item BONG_TERMINAL_ITEM = Registry.register(
            Registries.ITEM,
            BONG_TERMINAL_ITEM_KEY,
            new BlockItem(BONG_TERMINAL, new Item.Settings()
                    .registryKey(BONG_TERMINAL_ITEM_KEY)
                    .useBlockPrefixedTranslationKey())
    );

    public static final RegistryKey<EntityType<?>> COURIER_ARROW_ENTITY_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "courier_arrow"));

    public static final EntityType<CourierArrowEntity> COURIER_ARROW_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            COURIER_ARROW_ENTITY_KEY,
            EntityType.Builder.<CourierArrowEntity>create(CourierArrowEntity::new, SpawnGroup.MISC)
                    .dimensions(0.5F, 0.5F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(20)
                    .build(COURIER_ARROW_ENTITY_KEY)
    );

    public static final RegistryKey<Item> COURIER_ARROW_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "courier_arrow"));

    public static final Item COURIER_ARROW = Registry.register(
            Registries.ITEM,
            COURIER_ARROW_KEY,
            new CourierArrowItem(new Item.Settings().registryKey(COURIER_ARROW_KEY))
    );

    public static final RegistryKey<Item> VILLAGER_LEG_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "villager_leg"));

    public static final Item VILLAGER_LEG = Registry.register(
            Registries.ITEM,
            VILLAGER_LEG_KEY,
            new VillagerLegItem(new Item.Settings().registryKey(VILLAGER_LEG_KEY).maxCount(16))
    );

    public static final RegistryKey<EntityType<?>> VILLAGER_LEG_ENTITY_KEY =
            RegistryKey.of(RegistryKeys.ENTITY_TYPE, Identifier.of(MOD_ID, "villager_leg"));

    public static final EntityType<ThrownVillagerLegEntity> VILLAGER_LEG_ENTITY = Registry.register(
            Registries.ENTITY_TYPE,
            VILLAGER_LEG_ENTITY_KEY,
            EntityType.Builder.<ThrownVillagerLegEntity>create(ThrownVillagerLegEntity::new, SpawnGroup.MISC)
                    .dimensions(0.25F, 0.25F)
                    .maxTrackingRange(4)
                    .trackingTickInterval(10)
                    .build(VILLAGER_LEG_ENTITY_KEY)
    );

    public static final RegistryKey<Block> ONE_CYCLE_BED_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "one_cycle_bed"));

    public static final Block ONE_CYCLE_BED = Registry.register(
            Registries.BLOCK,
            ONE_CYCLE_BED_KEY,
            new OneCycleBedBlock(AbstractBlock.Settings.create()
                    .registryKey(ONE_CYCLE_BED_KEY)
                    .strength(0.2F)
                    .sounds(BlockSoundGroup.WOOL))
    );

    public static final RegistryKey<Item> ONE_CYCLE_BED_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "one_cycle_bed"));

    public static final Item ONE_CYCLE_BED_ITEM = Registry.register(
            Registries.ITEM,
            ONE_CYCLE_BED_ITEM_KEY,
            new BlockItem(ONE_CYCLE_BED, new Item.Settings()
                    .registryKey(ONE_CYCLE_BED_ITEM_KEY)
                    .useBlockPrefixedTranslationKey())
    );

    public static final RegistryKey<Block> TRAMPOLINE_KEY =
            RegistryKey.of(RegistryKeys.BLOCK, Identifier.of(MOD_ID, "trampoline"));

    public static final Block TRAMPOLINE = Registry.register(
            Registries.BLOCK,
            TRAMPOLINE_KEY,
            new TrampolineBlock(AbstractBlock.Settings.create()
                    .registryKey(TRAMPOLINE_KEY)
                    // Five minutes with an iron pickaxe: break ticks are 30 * hardness / tool
                    // speed, an iron pickaxe is speed 6, and 5 minutes is 6000 ticks, so 1200.
                    // Blast resistance stays low - only the digging is meant to be punishing.
                    .strength(1200.0F, 6.0F)
                    .sounds(BlockSoundGroup.WOOL)
                    // Pickaxe work: see the mineable/pickaxe tag. Without a pickaxe it breaks
                    // slowly and drops nothing.
                    .requiresTool()
                    // Jumping off one under your own power gets a boost too.
                    .jumpVelocityMultiplier(1.5F))
    );

    public static final RegistryKey<Item> TRAMPOLINE_ITEM_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "trampoline"));

    public static final Item TRAMPOLINE_ITEM = Registry.register(
            Registries.ITEM,
            TRAMPOLINE_ITEM_KEY,
            new BlockItem(TRAMPOLINE, new Item.Settings()
                    .registryKey(TRAMPOLINE_ITEM_KEY)
                    .useBlockPrefixedTranslationKey())
    );

    public static final RegistryKey<Item> GIANT_PICKAXE_KEY =
            RegistryKey.of(RegistryKeys.ITEM, Identifier.of(MOD_ID, "giant_pickaxe"));

    public static final Item GIANT_PICKAXE = Registry.register(
            Registries.ITEM,
            GIANT_PICKAXE_KEY,
            new Item(new Item.Settings()
                    .registryKey(GIANT_PICKAXE_KEY)
                    .maxCount(1)
                    // Netherite grade: it has to get through diamond ore to collect any.
                    .pickaxe(ToolMaterial.NETHERITE, 5.0F, -3.0F))
    );

    /** Everything Ring Bong OS adds lives in its own creative tab. */
    public static final RegistryKey<ItemGroup> ITEM_GROUP_KEY =
            RegistryKey.of(RegistryKeys.ITEM_GROUP, Identifier.of(MOD_ID, "ring_bong_items"));

    public static final ItemGroup ITEM_GROUP = Registry.register(
            Registries.ITEM_GROUP,
            ITEM_GROUP_KEY,
            FabricItemGroup.builder()
                    .icon(() -> new ItemStack(BONG_TERMINAL_ITEM))
                    .displayName(Text.translatable("itemGroup.ringbongos.ring_bong_items"))
                    .build()
    );

    @Override
    public void onInitialize() {
        PayloadTypeRegistry.playC2S().register(OSPayloads.Refresh.ID, OSPayloads.Refresh.CODEC);
        PayloadTypeRegistry.playC2S().register(OSPayloads.RingBong.ID, OSPayloads.RingBong.CODEC);
        PayloadTypeRegistry.playC2S().register(OSPayloads.ToggleHerobrine.ID, OSPayloads.ToggleHerobrine.CODEC);
        PayloadTypeRegistry.playS2C().register(OSPayloads.TerminalState.ID, OSPayloads.TerminalState.CODEC);
        PayloadTypeRegistry.playS2C().register(OSPayloads.HerobrineSet.ID, OSPayloads.HerobrineSet.CODEC);

        ServerPlayNetworking.registerGlobalReceiver(OSPayloads.Refresh.ID, (payload, context) ->
                context.server().execute(() -> sendState(context.player(), payload.pos())));
        ServerPlayNetworking.registerGlobalReceiver(OSPayloads.RingBong.ID, (payload, context) ->
                context.server().execute(() -> {
                    ServerPlayerEntity player = context.player();
                    if (player.getEntityWorld() instanceof ServerWorld world
                            && world.getBlockState(payload.pos()).getBlock() instanceof BongTerminalBlock terminal) {
                        terminal.ringFromOS(world, payload.pos(), player);
                    }
                    sendState(player, payload.pos());
                }));

        ServerPlayNetworking.registerGlobalReceiver(OSPayloads.ToggleHerobrine.ID, (payload, context) ->
                context.server().execute(() -> HerobrineManager.toggle(context.player())));

        // Fresh joiners need to know who is already wearing the eyes; leavers stop being it.
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) ->
                HerobrineManager.sendTo(handler.player));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            HerobrineManager.forget(handler.player);
            RideablePlayers.forget(handler.player);
            StrongholdTitle.forget(handler.player);
        });

        // A dead villager leaves a leg behind.
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            if (entity.getType() == EntityType.VILLAGER && entity.getEntityWorld() instanceof ServerWorld world) {
                world.spawnEntity(new ItemEntity(world, entity.getX(), entity.getY() + 0.5, entity.getZ(),
                        new ItemStack(VILLAGER_LEG)));
            }
        });

        // Saddle a player, then climb on with a carrot on a stick.
        UseEntityCallback.EVENT.register(RideablePlayers::interact);

        // Empty hand on a baby ravager climbs aboard.
        UseEntityCallback.EVENT.register(BabyRavagers::interact);

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            PantsCommand.register(dispatcher);
            EndPortalCommand.register(dispatcher);
            DragonCommand.register(dispatcher);
            TrampolineCommand.register(dispatcher);
        });

        // One swing of the giant pickaxe eats a sphere of world, a slice per tick.
        PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
            if (world instanceof ServerWorld serverWorld && player.getMainHandStack().isOf(GIANT_PICKAXE)) {
                GiantPickaxe.swing(serverWorld, pos, player.getUuid());
            }
        });
        ServerTickEvents.END_SERVER_TICK.register(GiantPickaxe::tick);
        ServerTickEvents.END_SERVER_TICK.register(BabyRavagers::tick);
        ServerTickEvents.END_SERVER_TICK.register(StrongholdTitle::tick);
        ServerTickEvents.END_SERVER_TICK.register(Followers::tick);

        // Wake up in a stronghold, or a villager's house, rather than where you died.
        ServerPlayerEvents.AFTER_RESPAWN.register(RespawnSpot::afterRespawn);

        ItemGroupEvents.modifyEntriesEvent(ITEM_GROUP_KEY).register(entries -> {
            entries.add(BONG_TERMINAL_ITEM);
            entries.add(COURIER_ARROW);
            entries.add(GIANT_PICKAXE);
            entries.add(TRAMPOLINE_ITEM);
            entries.add(ONE_CYCLE_BED_ITEM);
            entries.add(VILLAGER_LEG);
            // Bing Bong's two items share the tab. Looked up by id rather than compiled against,
            // so this mod still loads on its own if Bing Bong isn't installed.
            for (String id : BING_BONG_ITEMS) {
                Registries.ITEM.getOptionalValue(Identifier.of("bingbong", id)).ifPresent(entries::add);
            }
        });
    }

    /** One screenful of truth for the terminal at {@code pos}, straight from the server. */
    public static void sendState(ServerPlayerEntity player, BlockPos pos) {
        if (!(player.getEntityWorld() instanceof ServerWorld world)) {
            return;
        }
        if (!(world.getBlockState(pos).getBlock() instanceof BongTerminalBlock)) {
            return;
        }

        ServerPlayNetworking.send(player, new OSPayloads.TerminalState(
                pos,
                world.getBlockState(pos).get(BongTerminalBlock.STAGE) != 0,
                TerminalLog.uptime(world, pos),
                world.getTimeOfDay(),
                world.isRaining(),
                world.getRegistryKey().getValue().getPath(),
                nearby(world, pos, player),
                TerminalLog.lines(world, pos)));
    }

    /** Everyone the terminal can see, nearest first. */
    private static List<String> nearby(ServerWorld world, BlockPos pos, ServerPlayerEntity viewer) {
        Vec3d center = Vec3d.ofCenter(pos);
        List<ServerPlayerEntity> found = new ArrayList<>();
        for (ServerPlayerEntity candidate : world.getPlayers()) {
            if (candidate.squaredDistanceTo(center) <= PING_RANGE * PING_RANGE) {
                found.add(candidate);
            }
        }
        found.sort(Comparator.comparingDouble(candidate -> candidate.squaredDistanceTo(center)));

        List<String> lines = new ArrayList<>();
        for (ServerPlayerEntity candidate : found.subList(0, Math.min(found.size(), 6))) {
            String name = candidate.getGameProfile().name();
            if (candidate == viewer) {
                name = name + " (you)";
            }
            lines.add(name + "  " + Math.round(Math.sqrt(candidate.squaredDistanceTo(center))) + "m");
        }
        return List.copyOf(lines);
    }
}
