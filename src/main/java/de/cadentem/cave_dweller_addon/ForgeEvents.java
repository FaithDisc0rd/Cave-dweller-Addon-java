package de.cadentem.cave_dweller_addon;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import de.cadentem.cave_dweller_addon.entities.goals.CarnageModeGoal;
import de.cadentem.cave_dweller_addon.entities.goals.CaveDwellerStalkGoal;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class ForgeEvents {

    private ForgeEvents() {
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(final EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) {
            return;
        }
        if (event.getEntity() instanceof ApparitionEntity apparition) {
            apparition.finalizeSpawnFromConfig();
            return;
        }
        if (event.getEntity() instanceof CaveDwellerEntity caveDweller) {
            caveDweller.goalSelector.addGoal(1, new CarnageModeGoal(caveDweller));
            caveDweller.goalSelector.addGoal(2, new CaveDwellerStalkGoal(caveDweller));
        }
    }
}
