package com.watcher.thewatcher;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Damage dealt by The Watcher. Its death message replaces the vanilla one. */
public class WatcherDamageSource extends DamageSource {
    public WatcherDamageSource(Holder<DamageType> type, Entity attacker) {
        super(type, attacker, attacker);
    }

    @Override
    public Component getLocalizedDeathMessage(LivingEntity victim) {
        return Component.translatable("death.thewatcher.found");
    }
}
