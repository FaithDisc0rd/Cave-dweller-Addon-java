package de.cadentem.cave_dweller_addon;

import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import de.cadentem.cave_dweller_addon.registry.ModEntityTypes;
import net.minecraftforge.event.entity.EntityAttributeCreationEvent;
import net.minecraftforge.event.entity.SpawnPlacementRegisterEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraft.world.entity.SpawnPlacements;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.level.levelgen.Heightmap;

@Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ModEvents {

    private ModEvents() {
    }

    @SubscribeEvent
public static void registerEntityAttributes(final EntityAttributeCreationEvent event) {
    // The issue is that APPARITION is a RegistryObject
    // We check if it is ready before calling .get()
    if (ModEntityTypes.APPARITION.isPresent()) {
        event.put(
            ModEntityTypes.APPARITION.get(),
            ApparitionEntity.createApparitionAttributes().build()
        );
    } else {
        // Log an error so you can see why it's failing without a crash
        CaveDwellerAddon.LOGGER.error("APPARITION is not yet present in the registry! Check your registration order.");
    }
}

    @SubscribeEvent
    public static void registerSpawnPlacements(final SpawnPlacementRegisterEvent event) {
        event.register(
                ModEntityTypes.APPARITION.get(),
                SpawnPlacements.Type.ON_GROUND,
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                Monster::checkMonsterSpawnRules,
                SpawnPlacementRegisterEvent.Operation.OR
        );
    }
}
