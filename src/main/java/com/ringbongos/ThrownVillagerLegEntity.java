package com.ringbongos;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.SpawnReason;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.mob.RavagerEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A villager leg in flight. Wherever it lands, a baby ravager turns up for it.
 *
 * <p>Vanilla has no baby ravager, so this is a normal ravager with its scale attribute halved and
 * a {@code baby_ravager} tag on it, which is enough to find them again later.
 */
public class ThrownVillagerLegEntity extends ThrownItemEntity {
    /** Half size. Vanilla babies are usually half, and a ravager is big enough that it reads. */
    private static final double BABY_SCALE = 0.5;

    public ThrownVillagerLegEntity(EntityType<? extends ThrownVillagerLegEntity> type, World world) {
        super(type, world);
    }

    public ThrownVillagerLegEntity(World world, LivingEntity owner, ItemStack stack) {
        super(RingBongOS.VILLAGER_LEG_ENTITY, owner, world, stack);
    }

    @Override
    protected Item getDefaultItem() {
        return RingBongOS.VILLAGER_LEG;
    }

    @Override
    protected void onCollision(HitResult hit) {
        super.onCollision(hit);
        if (!(getEntityWorld() instanceof ServerWorld world)) {
            return;
        }

        Vec3d where = hit.getPos();
        RavagerEntity baby = EntityType.RAVAGER.spawn(world, BlockPos.ofFloored(where), SpawnReason.SPAWN_ITEM_USE);
        if (baby != null) {
            EntityAttributeInstance scale = baby.getAttributeInstance(EntityAttributes.SCALE);
            if (scale != null) {
                scale.setBaseValue(BABY_SCALE);
            }
            baby.addCommandTag(BabyRavagers.TAG);
            world.playSound(null, baby.getBlockPos(), SoundEvents.ENTITY_RAVAGER_CELEBRATE,
                    SoundCategory.HOSTILE, 1.0F, 1.6F);
        }

        // Whatever babies are already about should come running to it too.
        BabyRavagers.legLanded(world, where, getOwner() == null ? null : getOwner().getUuid());

        world.spawnParticles(ParticleTypes.ITEM_SLIME, where.x, where.y + 0.2, where.z, 8, 0.2, 0.2, 0.2, 0.0);
        discard();
    }
}
