package de.cadentem.cave_dweller_addon;

import com.mojang.logging.LogUtils;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.registry.ModEntityTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraftforge.event.entity.EntityEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(CaveDwellerAddon.MODID)
public class CaveDwellerAddon {
    public static final String MODID = "cave_dweller_addon";
    public static final Logger LOGGER = LogUtils.getLogger();

    // Define the accessors
    public static final EntityDataAccessor<Integer> DATA_MODE_ID = 
        SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.INT);
    public static final EntityDataAccessor<Float> DATA_RAGE_METER = 
        SynchedEntityData.defineId(CaveDwellerEntity.class, EntityDataSerializers.FLOAT);

    public CaveDwellerAddon() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 1. Register the entity types
        ModEntityTypes.ENTITIES.register(modEventBus);

        // 2. Register Config Screen (Uncomment and fix the reference if you have a config class)
        // ModLoadingContext.get().registerConfigScreen((client, parent) -> new MyConfigScreen(parent));
    }

    @Mod.EventBusSubscriber(modid = MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
    public static class ModEvents {
        @SubscribeEvent
        public static void onEntityConstructing(EntityEvent.EntityConstructing event) {
            if (event.getEntity() instanceof CaveDwellerEntity dweller) {
                dweller.getEntityData().define(DATA_MODE_ID, 0);
                dweller.getEntityData().define(DATA_RAGE_METER, 0.0f);
            }
        }
    }
}