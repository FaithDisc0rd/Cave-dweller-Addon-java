package de.cadentem.cave_dweller_addon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer; // Added this import
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.client.model.AddonCarnageModel;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;

public class AddonCarnageRenderer extends GeoEntityRenderer<CaveDwellerEntity> {

    public AddonCarnageRenderer(EntityRendererProvider.Context renderManager) {
        super(renderManager, new AddonCarnageModel());
    }

    @Override
    public ResourceLocation getTextureLocation(CaveDwellerEntity animatable) {
        if (CaveDwellerAddonModes.isCarnageMode(animatable)) {
            return ResourceLocation.fromNamespaceAndPath("cave_dweller_addon", 
                    "textures/entity/cave_dweller_texture_carnage.png");
        }
        return ResourceLocation.fromNamespaceAndPath("cave_dweller", 
                "textures/entity/cave_dweller_texture.png");
    }

    @Override
    public void preRender(PoseStack poseStack, CaveDwellerEntity animatable, BakedGeoModel model, 
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, 
                          float partialTick, int packedLight, int packedOverlay, 
                          float red, float green, float blue, float alpha) {
        
        // 1. Configure bone visibility
        boolean isCarnage = CaveDwellerAddonModes.isCarnageMode(animatable);
        model.getBone("group_base_form").ifPresent(bone -> bone.setHidden(isCarnage));
        model.getBone("group_carnage_form").ifPresent(bone -> bone.setHidden(!isCarnage));

        // 2. Always call super last
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, 
                        partialTick, packedLight, packedOverlay, red, green, blue, alpha);
    }

    @Override
    public void render(CaveDwellerEntity entity, float entityYaw, float partialTick, 
                       PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }
}