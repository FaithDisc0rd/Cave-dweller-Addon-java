package de.cadentem.cave_dweller_addon.registry;

import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITIES = 
        DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, CaveDwellerAddon.MODID);

    public static final RegistryObject<EntityType<ApparitionEntity>> APPARITION = 
        ENTITIES.register("apparition", () -> EntityType.Builder.of(ApparitionEntity::new, MobCategory.MONSTER)
            .sized(0.6F, 1.95F)
            .clientTrackingRange(8)
            .updateInterval(3)
            .build(ResourceLocation.fromNamespaceAndPath(CaveDwellerAddon.MODID, "apparition").toString()));
}