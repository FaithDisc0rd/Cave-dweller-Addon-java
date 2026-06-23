package de.cadentem.cave_dweller_addon.mixin;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import de.cadentem.cave_dweller_addon.entities.goals.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.*;
import software.bernie.geckolib.core.object.PlayState;
import software.bernie.geckolib.util.GeckoLibUtil;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

@Mixin(CaveDwellerEntity.class)
public abstract class CaveDwellerEntityMixin implements GeoEntity {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    // Track the currently playing animation to prevent resetting
    private String lastAnimation = "";

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar registrar) {
        registrar.add(new AnimationController<>(this, "controller", 10, this::predicate));
    }

private <E extends GeoEntity> PlayState predicate(AnimationState<E> state) {
        // Define entity first so it exists for the logger
        CaveDwellerEntity entity = (CaveDwellerEntity) state.getAnimatable();
        
       // Only log once every 100 ticks (approx every 5 seconds) to avoid spamming the console
if (entity.tickCount % 100 == 0) {
    CaveDwellerAddon.LOGGER.info("Current Mode ID: {}", entity.getEntityData().get(CaveDwellerAddon.DATA_MODE_ID));
}
        double speed = entity.getDeltaMovement().horizontalDistanceSqr();
        double currentHeight = entity.getBoundingBox().getYsize();

        boolean isSpottedByPlayer = entity.getEntityData().get(CaveDwellerEntity.SPOTTED_ACCESSOR);
        Player targetPlayer = entity.getTarget() instanceof Player ? (Player) entity.getTarget() : null;

        boolean isPlayerUnavailable = targetPlayer == null || 
                                      targetPlayer.isCreative() || 
                                      targetPlayer.isSpectator() || 
                                      targetPlayer.isInvisible() || 
                                      entity.distanceToSqr(targetPlayer) > 400.0;

        // Determine which animation should be playing
        String nextAnimation = getTargetAnimation(entity, isPlayerUnavailable, targetPlayer, isSpottedByPlayer, speed, currentHeight);

        // Only update the animation if it has changed
        if (!nextAnimation.equals(this.lastAnimation)) {
            state.getController().setAnimation(RawAnimation.begin().thenLoop(nextAnimation));
            this.lastAnimation = nextAnimation;
        }

        return PlayState.CONTINUE;
    }
    
    private String getTargetAnimation(CaveDwellerEntity entity, boolean isPlayerUnavailable, Player targetPlayer, boolean isSpottedByPlayer, double speed, double currentHeight) {
        if (CaveDwellerAddonModes.isCarnageMode(entity)) {
            if (isPlayerUnavailable) return speed > 0.01 ? "animation.carnage.calm_move" : "animation.carnage.calm_idle";
            if (!entity.getSensing().hasLineOfSight(targetPlayer)) return "animation.carnage.flee";
            if (isSpottedByPlayer && speed < 0.01) return "animation.carnage.spotted";
            if (currentHeight <= 1.0) return "animation.carnage.crawl";
            return currentHeight <= 2.0 ? (speed > 0.01 ? "animation.carnage.crouch_run_new" : "animation.carnage.crouch_idle") 
                                       : (speed > 0.01 ? "animation.carnage.new_run" : "animation.carnage.run_idle");
        } else {
            if (isPlayerUnavailable) return speed > 0.01 ? "animation.cave_dweller.calm_move" : "animation.cave_dweller.calm_idle";
            if (currentHeight <= 1.0) return "animation.cave_dweller.crawl";
            return currentHeight <= 2.0 ? (speed > 0.01 ? "animation.cave_dweller.crouch_run" : "animation.cave_dweller.crouch_idle") 
                                       : (speed > 0.01 ? "animation.cave_dweller.calm_move" : "animation.cave_dweller.calm_idle");
        }
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return this.cache;
    }

    @Inject(method = "doHurtTarget", at = @At("HEAD"), cancellable = true)
    private void onHurtTarget(Entity target, CallbackInfoReturnable<Boolean> cir) {
        CaveDwellerEntity entity = (CaveDwellerEntity) (Object) this;
        if (CaveDwellerAddonModes.isCarnageMode(entity) && target instanceof Player player) {
            player.setHealth(0);
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void registerAddonGoals(CallbackInfo ci) {
        CaveDwellerEntity dweller = (CaveDwellerEntity) (Object) this;
        dweller.goalSelector.addGoal(1, new HallucinationModeGoal(dweller));
        dweller.goalSelector.addGoal(2, new CarnageModeGoal(dweller));
        dweller.goalSelector.addGoal(3, new CaveDwellerStalkGoal(dweller));
    }
}