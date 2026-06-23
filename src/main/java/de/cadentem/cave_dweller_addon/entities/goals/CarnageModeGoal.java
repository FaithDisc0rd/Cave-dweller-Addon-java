package de.cadentem.cave_dweller_addon.entities.goals;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.config.AddonConfig;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import net.minecraft.core.BlockPos;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;

import java.util.EnumSet;
import java.util.UUID;

public class CarnageModeGoal extends Goal {
    private static final UUID CARNAGE_SPEED_UUID = UUID.fromString("d62f4812-421f-4424-bd9a-65d14457311d");
    private static final UUID CARNAGE_DAMAGE_UUID = UUID.fromString("a1b2c3d4-5e6f-7890-abcd-ef1234567890");
    private static final UUID CARNAGE_KNOCKBACK_UUID = UUID.fromString("c3d4e5f6-7890-4012-cdef-345678901234");
    private static final UUID BERSERK_DAMAGE_UUID = UUID.fromString("d4e5f607-8901-4123-def0-456789012345");
    private static final UUID BERSERK_SPEED_UUID = UUID.fromString("e5f60718-9012-4234-ef01-567890123456");

    private final CaveDwellerEntity caveDweller;
    private LivingEntity target;
    private boolean isBerserk = false;
    private int breakCooldown = 0;

    private int weaknessAmplifier = 1;
    private int miningFatigueAmplifier = 2;

    public CarnageModeGoal(CaveDwellerEntity caveDweller) {
        this.caveDweller = caveDweller;
        this.setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (caveDweller == null) return false;

        if (CaveDwellerAddonModes.isCarnageMode(caveDweller)) {
            this.target = caveDweller.getTarget();
            return this.target != null;
        }

        LivingEntity combatTarget = caveDweller.getTarget();
        if (combatTarget instanceof Player player) {
            this.target = player;
            return CaveDwellerAddonModes.getRage(caveDweller) >= 1.0f || caveDweller.getRandom().nextFloat() < 0.05f;
        }
        return false;
    }

    @Override
    public void start() {
        if (!CaveDwellerAddonModes.isCarnageMode(caveDweller)) {
            CaveDwellerAddonModes.set(caveDweller, CaveDwellerAddonModes.Mode.CARNAGE);
        }

        this.isBerserk = false;
        this.weaknessAmplifier = 1;
        this.miningFatigueAmplifier = 2;

        applyModifier(Attributes.MOVEMENT_SPEED, CARNAGE_SPEED_UUID, AddonConfig.CARNAGE_SPEED_BOOST.get() - 1.0, AttributeModifier.Operation.MULTIPLY_BASE);
        applyModifier(Attributes.ATTACK_DAMAGE, CARNAGE_DAMAGE_UUID, AddonConfig.CARNAGE_DAMAGE_BOOST.get(), AttributeModifier.Operation.ADDITION);
        applyModifier(Attributes.KNOCKBACK_RESISTANCE, CARNAGE_KNOCKBACK_UUID, AddonConfig.CARNAGE_KNOCKBACK_RESIST.get(), AttributeModifier.Operation.ADDITION);

        caveDweller.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, -1, 1, false, false));
    }

    @Override
    public void tick() {
        if (target == null) return;

        // Throttled logic updates
        if (caveDweller.tickCount % 5 == 0) {
            if (!caveDweller.level().isClientSide()) {
                float rage = CaveDwellerAddonModes.getRage(caveDweller);
                double gain = (caveDweller.getNavigation().isDone() || !caveDweller.getSensing().hasLineOfSight(target))
                            ? AddonConfig.RAGE_GAIN_IDLE.get()
                            : AddonConfig.RAGE_GAIN_MOVING.get();

                CaveDwellerAddonModes.setRage(caveDweller, rage + (float) gain);
            }

            if (!isBerserk && caveDweller.getHealth() < (caveDweller.getMaxHealth() * 0.5)) {
                triggerBerserk();
            }
        }

        caveDweller.getNavigation().moveTo(target, 1.5D);
        caveDweller.getLookControl().setLookAt(target, 30.0F, 30.0F);

        // Throttled block breaking
        if (caveDweller.horizontalCollision && breakCooldown <= 0) {
            BlockPos pos = caveDweller.blockPosition().relative(caveDweller.getDirection());
            caveDweller.level().destroyBlock(pos, false);
            caveDweller.level().destroyBlock(pos.above(), false);
            breakCooldown = 40;
        } else if (breakCooldown > 0) {
            breakCooldown--;
        }

        // Effect application
        if (!target.hasEffect(MobEffects.WEAKNESS) || target.getEffect(MobEffects.WEAKNESS).getAmplifier() < weaknessAmplifier) {
            target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, -1, weaknessAmplifier, false, true));
        }
        if (!target.hasEffect(MobEffects.DIG_SLOWDOWN) || target.getEffect(MobEffects.DIG_SLOWDOWN).getAmplifier() < miningFatigueAmplifier) {
            target.addEffect(new MobEffectInstance(MobEffects.DIG_SLOWDOWN, -1, miningFatigueAmplifier, false, true));
        }
        if (isBerserk && !target.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)) {
            target.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SLOWDOWN, -1, 2, false, true));
        }
    }

    private void triggerBerserk() {
        this.isBerserk = true;
        this.weaknessAmplifier = 2;
        this.miningFatigueAmplifier = 3;

        if (this.target != null) {
            this.target.removeEffect(MobEffects.WEAKNESS);
            this.target.removeEffect(MobEffects.DIG_SLOWDOWN);
        }

        applyModifier(Attributes.ATTACK_DAMAGE, BERSERK_DAMAGE_UUID, 50.0, AttributeModifier.Operation.ADDITION);
        applyModifier(Attributes.MOVEMENT_SPEED, BERSERK_SPEED_UUID, 0.5, AttributeModifier.Operation.ADDITION);
    }

    private void applyModifier(net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid, double amount, AttributeModifier.Operation operation) {
        var attr = caveDweller.getAttribute(attribute);
        if (attr != null && attr.getModifier(uuid) == null) {
            attr.addPermanentModifier(new AttributeModifier(uuid, "Carnage", amount, operation));
        }
    }

    @Override
    public boolean canContinueToUse() {
        return CaveDwellerAddonModes.isCarnageMode(caveDweller) && target != null && target.isAlive();
    }

    @Override
    public void stop() {
        if (this.target != null) {
            this.target.removeEffect(MobEffects.WEAKNESS);
            this.target.removeEffect(MobEffects.DIG_SLOWDOWN);
            this.target.removeEffect(MobEffects.MOVEMENT_SLOWDOWN);
        }

        removeModifier(Attributes.MOVEMENT_SPEED, CARNAGE_SPEED_UUID);
        removeModifier(Attributes.ATTACK_DAMAGE, CARNAGE_DAMAGE_UUID);
        removeModifier(Attributes.KNOCKBACK_RESISTANCE, CARNAGE_KNOCKBACK_UUID);
        removeModifier(Attributes.ATTACK_DAMAGE, BERSERK_DAMAGE_UUID);
        removeModifier(Attributes.MOVEMENT_SPEED, BERSERK_SPEED_UUID);

        caveDweller.removeEffect(MobEffects.DAMAGE_RESISTANCE);
        this.target = null;
        CaveDwellerAddonModes.set(caveDweller, CaveDwellerAddonModes.Mode.NONE);
    }

    private void removeModifier(net.minecraft.world.entity.ai.attributes.Attribute attribute, UUID uuid) {
        var attr = caveDweller.getAttribute(attribute);
        if (attr != null && attr.getModifier(uuid) != null) {
            attr.removeModifier(uuid);
        }
    }
}