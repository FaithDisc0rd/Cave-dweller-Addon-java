package de.cadentem.cave_dweller_addon.client;

import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.client.renderer.ApparitionRenderer;
import de.cadentem.cave_dweller_addon.client.renderer.AddonCarnageRenderer;
import de.cadentem.cave_dweller_addon.registry.ModEntityTypes;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class ModClientEvents {

    private ModClientEvents() {
    }

    @SubscribeEvent
    public static void registerEntityRenderers(final EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(ModEntityTypes.APPARITION.get(), ApparitionRenderer::new);
        
        // Correct base mod entity registry registration mapping
        event.registerEntityRenderer(de.cadentem.cave_dweller.registry.ModEntityTypes.CAVE_DWELLER.get(), AddonCarnageRenderer::new);
    }
}