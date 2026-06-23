package de.cadentem.cave_dweller_addon.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.config.AddonConfig;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import java.util.EnumSet;

public class CaveDwellerStalkGoal extends Goal {
    private final CaveDwellerEntity caveDweller;
    private int stalkTimer;
    private int teleportCooldown;
    private Player targetPlayer;

    public CaveDwellerStalkGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
        this.setFlags(EnumSet.of(Goal.Flag.MOVE, Goal.Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (!CaveDwellerAddonModes.isStalkingMode(caveDweller)) {
            return false;
        }
        
        LivingEntity target = caveDweller.getTarget();
        if (target instanceof Player player) {
            this.targetPlayer = player;
            double maxDistanceSq = AddonConfig.STALKING_MAX_DISTANCE.get() * AddonConfig.STALKING_MAX_DISTANCE.get();
            double minDistanceSq = AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get() * AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get();
            double currentDistanceSq = caveDweller.distanceToSqr(target);
            
            return currentDistanceSq >= minDistanceSq && currentDistanceSq <= maxDistanceSq;
        }
        return false;
    }

    @Override
    public void start() {
        this.stalkTimer = 0;
        this.teleportCooldown = 0;
        CaveDwellerAddonModes.enterStalkingMode(caveDweller);
    }

    @Override
    public void tick() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) return;

        this.stalkTimer++;
        this.teleportCooldown++;

        if (this.teleportCooldown >= AddonConfig.STALK_TELEPORT_INTERVAL.get()) {
            this.teleportCooldown = 0;
            executeSafeTeleportBehind(this.targetPlayer);
        }

        double distanceSq = caveDweller.distanceToSqr(this.targetPlayer);
        double minLimitSq = AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get() * AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get();
        boolean isBeingWatched = isPlayerLookingAtMe(this.targetPlayer);

        if (isBeingWatched && AddonConfig.STALK_FREEZE_WHEN_LOOKED_AT.get()) {
            caveDweller.getNavigation().stop();
            caveDweller.setDeltaMovement(Vec3.ZERO);
            caveDweller.getLookControl().setLookAt(this.targetPlayer, 60.0F, 60.0F);
        } else {
            if (distanceSq > minLimitSq) {
                double targetSpeed = AddonConfig.STALKING_SPEED_MULTIPLIER.get();
                caveDweller.getNavigation().moveTo(this.targetPlayer, targetSpeed);
            } else {
                caveDweller.getNavigation().stop();
            }
            caveDweller.getLookControl().setLookAt(this.targetPlayer, 30.0F, 30.0F);
        }
    }

    private boolean isPlayerLookingAtMe(Player player) {
        Vec3 playerLookVec = player.getViewVector(1.0F).normalize();
        Vec3 entityToPlayerVec = new Vec3(caveDweller.getX() - player.getX(), 
                                          caveDweller.getEyeY() - player.getEyeY(), 
                                          caveDweller.getZ() - player.getZ()).normalize();
        
        double dotProduct = playerLookVec.dot(entityToPlayerVec);
        return dotProduct > 0.5; 
    }

    private void executeSafeTeleportBehind(Player player) {
        Vec3 lookDirection = player.getViewVector(1.0F).normalize();
        double distanceBehind = AddonConfig.STALK_TELEPORT_DISTANCE_BEHIND.get();
        Vec3 destination = player.position().subtract(lookDirection.scale(distanceBehind));
        
        BlockPos basePos = BlockPos.containing(destination.x, destination.y, destination.z);
        BlockPos safeFloorPos = null;

        for (int offset : new int[]{0, -1, -2, -3, 1, 2}) {
            BlockPos testPos = basePos.above(offset);
            BlockState floorState = caveDweller.level().getBlockState(testPos.below());
            BlockState bodyState = caveDweller.level().getBlockState(testPos);
            BlockState headState = caveDweller.level().getBlockState(testPos.above());

            if (!floorState.isAir() && bodyState.isAir() && headState.isAir()) {
                safeFloorPos = testPos;
                break;
            }
        }

        if (safeFloorPos != null && !caveDweller.level().isClientSide) {
            ServerLevel serverLevel = (ServerLevel) caveDweller.level();
            emitSmokeCloud(serverLevel, caveDweller.position());
            caveDweller.moveTo(safeFloorPos.getX() + 0.5, safeFloorPos.getY(), safeFloorPos.getZ() + 0.5, player.getYRot(), player.getXRot());
            emitSmokeCloud(serverLevel, caveDweller.position());
        }
    }

    private void emitSmokeCloud(ServerLevel serverLevel, Vec3 pos) {
        for (int i = 0; i < 8; i++) {
            serverLevel.sendParticles(ParticleTypes.SQUID_INK, 
                    pos.x + (serverLevel.random.nextDouble() - 0.5), 
                    pos.y + 0.5 + (serverLevel.random.nextDouble() - 0.5), 
                    pos.z + (serverLevel.random.nextDouble() - 0.5), 
                    1, 0.0, 0.0, 0.0, 0.01);
        }
    }

    @Override
    public boolean canContinueToUse() {
        if (this.targetPlayer == null || !this.targetPlayer.isAlive()) return false;
        
        double currentDistanceSq = caveDweller.distanceToSqr(this.targetPlayer);
        double minLimitSq = AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get() * AddonConfig.STALK_MIN_PROXIMITY_LIMIT.get();
        
        if (currentDistanceSq < minLimitSq) return false;

        return CaveDwellerAddonModes.isStalkingMode(caveDweller) && this.stalkTimer < AddonConfig.STALK_MAX_GOAL_DURATION.get();
    }

    @Override
    public void stop() {
        this.targetPlayer = null;
        caveDweller.getNavigation().stop();

        if (CaveDwellerAddonModes.get(caveDweller) == CaveDwellerAddonModes.Mode.STALKING) {
            float roll = caveDweller.getRandom().nextFloat();

            if (roll < 0.33f) {
                CaveDwellerAddonModes.set(caveDweller, CaveDwellerAddonModes.Mode.CARNAGE);
            } else if (roll < 0.66f) {
                try {
                    // Triggers the base mod's internal flee behavior via reflection
                    caveDweller.getClass().getMethod("flee").invoke(caveDweller);
                } catch (Exception e) {
                    CaveDwellerAddonModes.clear(caveDweller);
                }
            } else {
                CaveDwellerAddonModes.clear(caveDweller);
            }
        }
    }
}