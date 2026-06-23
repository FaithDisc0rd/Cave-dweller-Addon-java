package de.cadentem.cave_dweller_addon.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.client.model.ApparitionModel;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;
import software.bernie.geckolib.cache.object.BakedGeoModel;
import software.bernie.geckolib.renderer.GeoEntityRenderer;
import software.bernie.geckolib.renderer.layer.GeoRenderLayer;

public class ApparitionRenderer extends GeoEntityRenderer<ApparitionEntity> {

    private static final ResourceLocation EMISSIVE_TEXTURE = 
            ResourceLocation.fromNamespaceAndPath(CaveDwellerAddon.MODID, "textures/entity/apparition_glow.png");

    public ApparitionRenderer(EntityRendererProvider.Context context) {
        super(context, new ApparitionModel());
        this.shadowRadius = 0.0F;
        this.addRenderLayer(new ApparitionEmissiveLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(ApparitionEntity animatable) {
        return this.model.getTextureResource(animatable);
    }

    @Override
    public RenderType getRenderType(ApparitionEntity animatable, ResourceLocation texture, @Nullable MultiBufferSource bufferSource, float partialTick) {
        return RenderType.entityTranslucent(texture);
    }

    @Override
    public void preRender(PoseStack poseStack, ApparitionEntity animatable, BakedGeoModel model, 
                          MultiBufferSource bufferSource, VertexConsumer buffer, boolean isReRender, 
                          float partialTick, int packedLight, int packedOverlay, 
                          float red, float green, float blue, float alpha) {
        
        // Use 0.35F as the alpha instead of the default 1.0F
        super.preRender(poseStack, animatable, model, bufferSource, buffer, isReRender, 
                        partialTick, packedLight, packedOverlay, red, green, blue, 0.35F);
    }

    private static class ApparitionEmissiveLayer extends GeoRenderLayer<ApparitionEntity> {
        public ApparitionEmissiveLayer(GeoEntityRenderer<ApparitionEntity> entityRenderer) {
            super(entityRenderer);
        }

        @Override
        public void render(PoseStack poseStack, ApparitionEntity animatable, BakedGeoModel bakedModel, 
                           RenderType renderType, MultiBufferSource bufferSource, VertexConsumer buffer, 
                           float partialTick, int packedLight, int packedOverlay) {
            
            ApparitionModel modelProvider = (ApparitionModel) this.getRenderer().getGeoModel();
            int fullBright = 15728880; 

            // Render Glow Layer
            RenderType emissiveRenderType = RenderType.entityTranslucentEmissive(EMISSIVE_TEXTURE);
            VertexConsumer emissiveBuilder = bufferSource.getBuffer(emissiveRenderType);
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, emissiveRenderType, emissiveBuilder, partialTick, fullBright, packedOverlay, 1.0F, 1.0F, 1.0F, 0.35F);

            // Render Eyes Layer
            ResourceLocation eyesFile = modelProvider.getEyesTextureResource();
            RenderType eyesRenderType = RenderType.entityTranslucentEmissive(eyesFile);
            VertexConsumer eyesBuilder = bufferSource.getBuffer(eyesRenderType);
            this.getRenderer().reRender(bakedModel, poseStack, bufferSource, animatable, eyesRenderType, eyesBuilder, partialTick, fullBright, packedOverlay, 1.0F, 1.0F, 1.0F, 0.35F);
        }
    }
}