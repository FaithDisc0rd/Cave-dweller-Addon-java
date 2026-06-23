package de.cadentem.cave_dweller_addon.events;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.config.AddonConfig;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import de.cadentem.cave_dweller_addon.util.ApparitionSpawnHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.List;

@Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ApparitionSpawnEvents {

    private static int spawnTimerTicks;
    private static int nextSpawnIntervalTicks = 6000; 

    private ApparitionSpawnEvents() {
    }

    /**
     * Blocks natural, ambient Cave Dwellers from spawning if an Apparition is currently stalking a player.
     * This ensures the custom replacement rules take absolute priority.
     */
    @SubscribeEvent
    public static void onCaveDwellerSpawnCheck(final MobSpawnEvent.FinalizeSpawn event) {
        if (event.getLevel() instanceof ServerLevel serverLevel) {
            // Only intercept natural world generation (allows your manual /summon commands and triggered swaps to pass)
            if (event.getSpawnType() == MobSpawnType.NATURAL) {
                if (event.getEntity() instanceof CaveDwellerEntity) {
                    
                    // Scan the dimension for active Apparitions
                    for (Entity entity : serverLevel.getAllEntities()) {
                        if (entity instanceof ApparitionEntity && entity.isAlive()) {
                            // Deny the standard natural Cave Dweller spawn
                            event.setResult(Event.Result.DENY);
                            event.setCanceled(true);
                            return;
                        }
                    }
                }
            }
        }
    }

      @SubscribeEvent
    public static void onServerTick(final TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        var server = event.getServer();
        if (server == null || server.getTickCount() % 400 != 0) return; // Run check every 20 seconds (400 ticks)

        // Dynamic spawn chance (e.g., 5% chance every 20 seconds)
        if (server.overworld().getRandom().nextFloat() < 0.05f) {
             ServerLevel overworld = server.overworld();
             List<ServerPlayer> players = overworld.getPlayers(ApparitionSpawnHelper::isOverworldCavePlayer);
             if (!players.isEmpty()) {
                 ServerPlayer target = players.get(overworld.getRandom().nextInt(players.size()));
                 ApparitionSpawnHelper.spawnAt(overworld, target.blockPosition(), target, false);
             }
        }
    }
}