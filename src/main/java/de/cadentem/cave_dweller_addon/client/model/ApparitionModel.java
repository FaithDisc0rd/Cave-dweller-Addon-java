package de.cadentem.cave_dweller_addon.client.model;

import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import software.bernie.geckolib.cache.object.GeoBone;
import software.bernie.geckolib.core.animation.AnimationState;
import software.bernie.geckolib.model.GeoModel;

public class ApparitionModel extends GeoModel<ApparitionEntity> {

    private static final ResourceLocation MODEL =
            new ResourceLocation(CaveDwellerAddon.MODID, "geo/apparition.geo.json");
    private static final ResourceLocation ANIMATION =
            new ResourceLocation(CaveDwellerAddon.MODID, "animations/apparition.animation.json");
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(CaveDwellerAddon.MODID, "textures/entity/apparition_texture.png");
    private static final ResourceLocation EYES_TEXTURE =
            new ResourceLocation(CaveDwellerAddon.MODID, "textures/entity/apparition_eyes_texture.png");

    @Override
    public ResourceLocation getModelResource(final ApparitionEntity animatable) {
        return MODEL;
    }

    @Override
    public ResourceLocation getTextureResource(final ApparitionEntity animatable) {
        return TEXTURE;
    }

    @Override
    public ResourceLocation getAnimationResource(final ApparitionEntity animatable) {
        return ANIMATION;
    }

    public ResourceLocation getEyesTextureResource() {
        return EYES_TEXTURE;
    }

    @Override
    public void setCustomAnimations(ApparitionEntity animatable, long instanceId, AnimationState<ApparitionEntity> animationState) {
        super.setCustomAnimations(animatable, instanceId, animationState);

        GeoBone head = RoyalBoneFetch(this.getAnimationProcessor().getBone("head"));
        if (head != null) {
            LivingEntity target = animatable.getLookTarget();
            if (target != null) {
                double dX = target.getX() - animatable.getX();
                double dY = target.getEyeY() - animatable.getEyeY();
                double dZ = target.getZ() - animatable.getZ();
                double horizontalDist = Math.sqrt(dX * dX + dZ * dZ);

                float targetYaw = (float) (Mth.atan2(dZ, dX) * (180D / Math.PI)) - 90.0F;
                float targetPitch = (float) (-(Mth.atan2(dY, horizontalDist) * (180D / Math.PI)));

                float yawBodyDiff = Mth.wrapDegrees(targetYaw - animatable.yBodyRot);
                float pitchClamp = Mth.clamp(targetPitch, -90.0F, 90.0F);

                // Multiply by -1 to lock view vectors against upside-down bones cleanly
                head.setRotX(-pitchClamp * ((float) Math.PI / 180F));
                head.setRotY(-yawBodyDiff * ((float) Math.PI / 180F));
            }
        }
    }

    private GeoBone RoyalBoneFetch(Object boneObj) {
        if (boneObj instanceof java.util.Optional) {
            return (GeoBone) ((java.util.Optional<?>) boneObj).orElse(null);
        }
        return (GeoBone) boneObj;
    }
}