package com.watcher.thewatcher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.entity.SignBlockEntity;
import net.minecraft.world.level.block.entity.SignText;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerSleepInBedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.server.ServerLifecycleHooks;

@Mod.EventBusSubscriber(modid = TheWatcher.MODID)
public class WatcherEvents {
    private static final String PERSISTED = "PlayerPersisted";
    private static final String SIGN_KEY = "thewatcher_sign_placed";

    /** Players currently in their death jump-scare: uuid -> ticks elapsed. */
    private static final Map<UUID, Integer> SCARES = new HashMap<>();

    // ---------- Nightly spawn ----------

    @SubscribeEvent
    public static void onLevelTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel level)) return;
        if (level.dimension() != Level.OVERWORLD) return;
        if (level.getGameTime() % 40L != 0L) return;

        long t = level.getDayTime() % 24000L;
        if (t < 13000L || t >= 23000L) return;

        for (ServerPlayer player : level.players()) {
            if (!player.isAlive() || player.isCreative() || player.isSpectator()) continue;
            AABB area = player.getBoundingBox().inflate(200.0D);
            if (!level.getEntitiesOfClass(WatcherEntity.class, area).isEmpty()) continue;
            spawnNear(level, player);
        }
    }

    private static void spawnNear(ServerLevel level, ServerPlayer player) {
        RandomSource r = level.random;
        for (int i = 0; i < 12; i++) {
            double angle = r.nextDouble() * Math.PI * 2.0D;
            double dist = 24.0D + r.nextInt(12);
            int x = Mth.floor(player.getX() + Math.cos(angle) * dist);
            int z = Mth.floor(player.getZ() + Math.sin(angle) * dist);
            if (!level.hasChunk(x >> 4, z >> 4)) continue;
            int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);

            WatcherEntity watcher = new WatcherEntity(ModEntities.WATCHER.get(), level);
            watcher.moveTo(x + 0.5D, y, z + 0.5D, r.nextFloat() * 360.0F, 0.0F);
            if (!level.noCollision(watcher)) {
                watcher.discard();
                continue;
            }
            watcher.setPersistenceRequired();
            watcher.setTarget(player);
            level.addFreshEntity(watcher);
            return;
        }
    }

    // ---------- No sleeping, ever ----------

    @SubscribeEvent
    public static void onSleep(PlayerSleepInBedEvent event) {
        event.setResult(Player.BedSleepingProblem.OTHER_PROBLEM);
        event.getEntity().displayClientMessage(Component.literal("You are too terrified to sleep."), true);
    }

    // ---------- Death scare ----------

    @SubscribeEvent
    public static void onDeath(LivingDeathEvent event) {
        if (event.getEntity() instanceof ServerPlayer player
                && event.getSource() instanceof WatcherDamageSource) {
            SCARES.put(player.getUUID(), 0);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || SCARES.isEmpty()) return;
        var server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;

        Iterator<Map.Entry<UUID, Integer>> it = SCARES.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Integer> entry = it.next();
            ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
            int ticks = entry.getValue();
            if (player == null || ticks > 100) {
                it.remove();
                continue;
            }
            if (ticks == 0) {
                player.playNotifySound(SoundEvents.ENDERMAN_SCREAM, SoundSource.MASTER, 2.0F, 0.5F);
                player.playNotifySound(SoundEvents.GHAST_SCREAM, SoundSource.MASTER, 1.5F, 0.6F);
            }
            if (ticks % 14 == 0) {
                player.playNotifySound(SoundEvents.WARDEN_HEARTBEAT, SoundSource.MASTER, 2.0F, 1.0F);
            }
            entry.setValue(ticks + 1);
        }
    }

    // ---------- The sign ----------

    @SubscribeEvent
    public static void onLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        CompoundTag data = player.getPersistentData();
        CompoundTag persisted = data.getCompound(PERSISTED);
        if (persisted.getBoolean(SIGN_KEY)) return;
        persisted.putBoolean(SIGN_KEY, true);
        data.put(PERSISTED, persisted);
        placeSign(player);
    }

    private static void placeSign(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Direction dir = player.getDirection();
        BlockPos base = player.blockPosition().relative(dir, 2);
        // Text faces back toward the player
        int rot = Mth.floor((double) ((180.0F + player.getYRot()) * 16.0F / 360.0F) + 0.5D) & 15;
        BlockState state = Blocks.OAK_SIGN.defaultBlockState().setValue(StandingSignBlock.ROTATION, rot);

        for (int dy = 2; dy >= -3; dy--) {
            BlockPos pos = base.above(dy);
            if (level.getBlockState(pos).isAir() && state.canSurvive(level, pos)) {
                level.setBlock(pos, state, 3);
                if (level.getBlockEntity(pos) instanceof SignBlockEntity sign) {
                    SignText text = sign.getFrontText()
                            .setMessage(0, Component.literal("he is always"))
                            .setMessage(1, Component.literal("watching you"))
                            .setMessage(2, Component.literal("from behind."))
                            .setColor(DyeColor.RED)
                            .setHasGlowingText(true);
                    sign.setText(text, true);
                    sign.setChanged();
                    level.sendBlockUpdated(pos, state, state, 3);
                }
                return;
            }
        }
    }
}
