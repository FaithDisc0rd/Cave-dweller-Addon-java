package de.cadentem.cave_dweller_addon.entities;

import de.cadentem.cave_dweller_addon.config.AddonConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import software.bernie.geckolib.animatable.GeoEntity;
import software.bernie.geckolib.core.animatable.instance.AnimatableInstanceCache;
import software.bernie.geckolib.core.animation.AnimatableManager;
import software.bernie.geckolib.core.animation.AnimationController;
import software.bernie.geckolib.core.animation.RawAnimation;
import software.bernie.geckolib.util.GeckoLibUtil;

import javax.annotation.Nullable;
import java.util.OptionalInt;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;

public class ApparitionEntity extends Monster implements GeoEntity {

    public static final EntityDataAccessor<Boolean> DATA_IS_HANGING = 
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.BOOLEAN);
            
    public static final EntityDataAccessor<OptionalInt> DATA_LOOK_TARGET_ID = 
            SynchedEntityData.defineId(ApparitionEntity.class, EntityDataSerializers.OPTIONAL_UNSIGNED_INT);

    private static final RawAnimation HANGING = RawAnimation.begin().thenLoop("animation.apparition.hanging");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);
    private int lifetime = 600;
    
    @Nullable
    private LivingEntity lookTarget;

    public ApparitionEntity(EntityType<? extends ApparitionEntity> entityType, Level level) {
        super(entityType, level);
        this.setInvulnerable(true);
        this.setNoGravity(true);
        this.noPhysics = true; // Must be true so it doesn't get stuck in the ceiling when floating up
    }

    @Override
    protected void registerGoals() {
        super.registerGoals();
        this.goalSelector.addGoal(1, new LookAtPlayerGoal(this, Player.class, 128.0F, 1.0F) {
            @Override
            public boolean canUse() {
                if (ApparitionEntity.this.lookTarget == null) {
                    Player nearest = ApparitionEntity.this.level().getNearestPlayer(ApparitionEntity.this, 128.0);
                    if (nearest != null) {
                        ApparitionEntity.this.setLookTarget(nearest);
                    }
                }
                return ApparitionEntity.this.lookTarget != null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                if (ApparitionEntity.this.lookTarget == null || this.lookAt == null) {
                    return false;
                }
                return ApparitionEntity.this.lookTarget.isAlive() && 
                       ApparitionEntity.this.distanceToSqr(ApparitionEntity.this.lookTarget) <= 16384.0D && 
                       super.canContinueToUse();
            }
        });
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(DATA_IS_HANGING, true); 
        this.entityData.define(DATA_LOOK_TARGET_ID, OptionalInt.empty());
    }

    public static AttributeSupplier.Builder createApparitionAttributes() {
        return Monster.createMonsterAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.15)
                .add(Attributes.ATTACK_DAMAGE, 0.0)
                .add(Attributes.FOLLOW_RANGE, 64.0);
    }

    @Nullable
    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor levelAccessor, DifficultyInstance difficulty, MobSpawnType spawnType, @Nullable SpawnGroupData spawnGroupData, @Nullable CompoundTag tag) {
        if (spawnType == MobSpawnType.NATURAL) {
            Player targetPlayer = levelAccessor.getNearestPlayer(this, 128.0D);
            if (targetPlayer != null) {
                BlockPos spawnPos = findPathSpawnPositionNearPlayer(targetPlayer);
                if (spawnPos != null) {
                    this.moveTo(spawnPos.getX() + 0.5D, spawnPos.getY(), spawnPos.getZ() + 0.5D);
                }
            }
        }
        
        finalizeSpawnFromConfig();
        return super.finalizeSpawn(levelAccessor, difficulty, spawnType, spawnGroupData, tag);
    }

    public void finalizeSpawnFromConfig() {
        double health = AddonConfig.APPARITION_HEALTH.get();
        double speed = AddonConfig.APPARITION_SPEED.get();
        if (getAttribute(Attributes.MAX_HEALTH) != null) {
            getAttribute(Attributes.MAX_HEALTH).setBaseValue(health);
            setHealth((float) health);
        }
        if (getAttribute(Attributes.MOVEMENT_SPEED) != null) {
            getAttribute(Attributes.MOVEMENT_SPEED).setBaseValue(speed);
        }
    }

    public void setLookTarget(@Nullable final LivingEntity target) {
        this.lookTarget = target;
        if (!this.level().isClientSide()) {
            if (target != null) {
                this.entityData.set(DATA_LOOK_TARGET_ID, OptionalInt.of(target.getId()));
            } else {
                this.entityData.set(DATA_LOOK_TARGET_ID, OptionalInt.empty());
            }
        }
    }

    @Nullable
    public LivingEntity getLookTarget() {
        if (this.level().isClientSide()) {
            OptionalInt targetIdOpt = this.entityData.get(DATA_LOOK_TARGET_ID);
            if (targetIdOpt.isPresent()) {
                Entity found = this.level().getEntity(targetIdOpt.getAsInt());
                if (found instanceof LivingEntity living) {
                    return living;
                }
            }
            return null;
        }
        return this.lookTarget;
    }

    @Override
    public void tick() {
        super.tick();

        if (!this.level().isClientSide()) {
            lifetime--;
            if (lifetime <= 0) {
                this.discard();
                return;
            }

            // The Float Up Mechanic
            // Gradually adds upward momentum so the apparition drifts into the air over time
            this.setDeltaMovement(this.getDeltaMovement().add(0, 0.015D, 0));
        }
    }

    @Override
    public boolean hurt(net.minecraft.world.damagesource.DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(net.minecraft.world.damagesource.DamageSource damageSource) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    public boolean isNoGravity() {
        return true;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>(this, "controller", 2, event -> event.setAndContinue(HANGING)));
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public double getTick(Object object) {
        return this.tickCount;
    }

    @Override
    protected boolean canRide(net.minecraft.world.entity.Entity entity) {
        return false;
    }

    @Nullable
    public BlockPos findPathSpawnPositionNearPlayer(Player player) {
        for (int i = 0; i < 20; i++) {
            BlockPos randomPos = player.blockPosition().offset(
                this.getRandom().nextInt(10) - 5,
                this.getRandom().nextInt(4) - 2,
                this.getRandom().nextInt(10) - 5
            );
            
            if (this.level().getBlockState(randomPos).isAir()) {
                return randomPos;
            }
        }
        return player.blockPosition();
    }
}