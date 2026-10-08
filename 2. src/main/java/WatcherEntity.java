package com.watcher.thewatcher;

import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class WatcherEntity extends Monster {
    private Vec3 lastCheckPos = Vec3.ZERO;
    private int stuckSeconds = 0;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, Level level) {
        super(type, level);
        this.setMaxUpStep(2.0F);
        this.xpReward = 50;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 250.0D)
                // Player sprint speed = 0.13, so 4x = 0.52
                .add(Attributes.MOVEMENT_SPEED, 0.52D)
                .add(Attributes.ATTACK_DAMAGE, 20.0D)
                .add(Attributes.FOLLOW_RANGE, 128.0D)
                .add(Attributes.KNOCKBACK_RESISTANCE, 1.0D);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0D, true));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 64.0F));
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, 0, false, false, null));
    }

    @Override
    public boolean doHurtTarget(Entity target) {
        DamageSource source = new WatcherDamageSource(
                this.level().registryAccess().registryOrThrow(Registries.DAMAGE_TYPE)
                        .getHolderOrThrow(DamageTypes.MOB_ATTACK),
                this);
        return target.hurt(source, (float) this.getAttributeValue(Attributes.ATTACK_DAMAGE));
    }

    @Override
    protected void customServerAiStep() {
        super.customServerAiStep();
        if (this.tickCount % 20 != 0) return;

        // Vanishes at dawn
        long t = this.level().getDayTime() % 24000L;
        boolean night = t >= 13000L && t < 23000L;
        if (!night) {
            this.discard();
            return;
        }

        LivingEntity target = this.getTarget();
        if (target == null) return;

        // If boxed in (8 blocks tall is hard to path), or the player is very far, teleport closer.
        Vec3 now = this.position();
        double distSqr = this.distanceToSqr(target);
        if (distSqr > 36.0D && now.distanceToSqr(this.lastCheckPos) < 4.0D) {
            this.stuckSeconds++;
        } else {
            this.stuckSeconds = 0;
        }
        this.lastCheckPos = now;

        if (this.stuckSeconds >= 3 || distSqr > 90.0D * 90.0D) {
            this.teleportNear(target);
            this.stuckSeconds = 0;
        }
    }

    private void teleportNear(LivingEntity target) {
        for (int i = 0; i < 16; i++) {
            double angle = this.random.nextDouble() * Math.PI * 2.0D;
            double dist = 12.0D + this.random.nextInt(6);
            int x = Mth.floor(target.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(target.getZ() + Math.sin(angle) * dist);
            if (!this.level().hasChunk(x >> 4, z >> 4)) continue;
            int y = this.level().getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
            AABB box = this.getType().getDimensions().makeBoundingBox(x + 0.5D, y, z + 0.5D);
            if (this.level().noCollision(this, box)) {
                this.moveTo(x + 0.5D, y, z + 0.5D, this.getYRot(), this.getXRot());
                this.getNavigation().stop();
                return;
            }
        }
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.ENDERMAN_STARE;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENDERMAN_DEATH;
    }
}
