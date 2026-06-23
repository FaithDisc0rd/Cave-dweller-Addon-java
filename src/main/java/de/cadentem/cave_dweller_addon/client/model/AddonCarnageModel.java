package de.cadentem.cave_dweller_addon.client.model;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.model.GeoModel;

public class AddonCarnageModel extends GeoModel<CaveDwellerEntity> {
    
    @Override
    public ResourceLocation getModelResource(CaveDwellerEntity animatable) {
        // Point both forms to your single unified geo file
        return ResourceLocation.fromNamespaceAndPath("cave_dweller_addon", "geo/dweller_unified.geo.json");
    }

    @Override
    public ResourceLocation getTextureResource(CaveDwellerEntity animatable) {
        if (CaveDwellerAddonModes.get(animatable) == CaveDwellerAddonModes.Mode.CARNAGE) {
            return ResourceLocation.fromNamespaceAndPath("cave_dweller_addon", "textures/entity/cave_dweller_texture_carnage.png");
        }
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", "textures/entity/cave_dweller_texture.png");
    }

    @Override
    public ResourceLocation getAnimationResource(CaveDwellerEntity animatable) {
        // Point both forms to your single unified animation file
        return ResourceLocation.fromNamespaceAndPath("cave_dweller_addon", "animations/dweller_unified.animation.json");
    }
}