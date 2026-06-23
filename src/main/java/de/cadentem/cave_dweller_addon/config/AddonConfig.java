package de.cadentem.cave_dweller_addon.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class AddonConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    // --- Spawn Timing ---
    public static final ForgeConfigSpec.IntValue SPAWN_TIMER_MIN, SPAWN_TIMER_MAX, COOLDOWN_AFTER_RESOLUTION;
    public static final ForgeConfigSpec.DoubleValue SURFACE_TIMER_MULT;

    // --- Spawn Environment ---
    public static final ForgeConfigSpec.IntValue PREFERRED_MAX_Y, ABSOLUTE_MAX_Y;
    public static final ForgeConfigSpec.DoubleValue SPAWN_OVERRIDE_CHANCE;
    public static final ForgeConfigSpec.BooleanValue ALLOW_SURFACE_SPAWN;

    // --- Apparition Settings ---
    public static final ForgeConfigSpec.DoubleValue APPARITION_HEALTH, APPARITION_SPEED;
    public static final ForgeConfigSpec.BooleanValue APPARITION_GLOW_EYES;

    // --- Hallucination Mode ---
    public static final ForgeConfigSpec.IntValue HALLUCINATION_MAX_DURATION, BLINDNESS_EFFECT_INTERVAL;
    public static final ForgeConfigSpec.DoubleValue RESOLUTION_REAL_MOB_CHANCE, HALLUCINATION_EFFECT_RADIUS;
    public static final ForgeConfigSpec.BooleanValue APPLY_BLINDNESS_EFFECTS;
    public static final ForgeConfigSpec.DoubleValue WEIGHT_INSTANT_CARNAGE_BEHIND, WEIGHT_TELEPORT_LOW_CARNAGE_DWELLER, WEIGHT_WITHER_HALLUCINATION_ONLY;

    // --- Stalking Mode ---
    public static final ForgeConfigSpec.DoubleValue STALKING_CHANCE_WEIGHT, STALKING_SPEED_MULTIPLIER, STALK_TELEPORT_DISTANCE_BEHIND, STALK_MIN_PROXIMITY_LIMIT;
    public static final ForgeConfigSpec.IntValue STALKING_MAX_DISTANCE, STALK_MAX_GOAL_DURATION, STALK_TELEPORT_INTERVAL;
    public static final ForgeConfigSpec.BooleanValue STALK_FREEZE_WHEN_LOOKED_AT;

    // --- Carnage Mode & Berserk ---
    public static final ForgeConfigSpec.DoubleValue CARNAGE_MODE_CHANCE, CARNAGE_HEALTH_BOOST, CARNAGE_SPEED_BOOST, CARNAGE_DAMAGE_BOOST, CARNAGE_KNOCKBACK_RESIST;
    public static final ForgeConfigSpec.BooleanValue CARNAGE_BREAK_LIGHTS, CARNAGE_SCREEN_SHAKE, ENABLE_BERSERK, BERSERK_PARTICLES;
    public static final ForgeConfigSpec.IntValue CARNAGE_EFFECTS_DURATION;
    public static final ForgeConfigSpec.DoubleValue BERSERK_TRIGGER_PCT, BERSERK_DAMAGE_MULT, BERSERK_SPEED_MULT;
    public static final ForgeConfigSpec.DoubleValue RAGE_GAIN_MOVING, RAGE_GAIN_IDLE; // NEW

    static {
        BUILDER.push("Spawn Timing & Frequency");
        SPAWN_TIMER_MIN = BUILDER.comment("Minimum time (in seconds) between potential spawn checks").defineInRange("spawn_timer_min", 120, 10, 86400);
        SPAWN_TIMER_MAX = BUILDER.comment("Maximum time (in seconds) between potential spawn checks").defineInRange("spawn_timer_max", 300, 10, 86400);
        COOLDOWN_AFTER_RESOLUTION = BUILDER.comment("Cooldown (in seconds) before the Dweller can spawn again after an encounter").defineInRange("cooldown_after_resolution", 1200, 0, 120000);
        SURFACE_TIMER_MULT = BUILDER.comment("Multiplier applied to spawn timers if the player is on the surface").defineInRange("surface_timer_multiplier", 1.21, 0.0, 5.0);
        BUILDER.pop();

        BUILDER.push("Spawn Environment Restrictions");
        PREFERRED_MAX_Y = BUILDER.defineInRange("preferred_max_y", 0, -64, 320);
        ABSOLUTE_MAX_Y = BUILDER.defineInRange("absolute_max_y", 32, -64, 320);
        SPAWN_OVERRIDE_CHANCE = BUILDER.defineInRange("spawn_override_chance", 0.15, 0.0, 1.0);
        ALLOW_SURFACE_SPAWN = BUILDER.define("allow_surface_spawn", true);
        BUILDER.pop();

        BUILDER.push("Apparition Attributes");
        APPARITION_HEALTH = BUILDER.defineInRange("apparition_health", 20.0, 1.0, 10000.0);
        APPARITION_SPEED = BUILDER.defineInRange("apparition_speed", 0.15, 0.0, 5.0);
        APPARITION_GLOW_EYES = BUILDER.define("apparition_glow_eyes", true);
        BUILDER.pop();

        BUILDER.push("Hallucination Mode");
        HALLUCINATION_MAX_DURATION = BUILDER.defineInRange("hallucination_max_duration", 400, 40, 12000);
        RESOLUTION_REAL_MOB_CHANCE = BUILDER.defineInRange("resolution_real_mob_chance", 0.75, 0.0, 1.0);
        APPLY_BLINDNESS_EFFECTS = BUILDER.define("apply_blindness_effects", true);
        BLINDNESS_EFFECT_INTERVAL = BUILDER.defineInRange("blindness_effect_interval", 20, 5, 200);
        HALLUCINATION_EFFECT_RADIUS = BUILDER.defineInRange("radius", 6.0D, 1.0D, 32.0D);
        WEIGHT_INSTANT_CARNAGE_BEHIND = BUILDER.defineInRange("weight_ambush", 1.0D, 0.0D, 100.0D);
        WEIGHT_TELEPORT_LOW_CARNAGE_DWELLER = BUILDER.defineInRange("weight_spawn", 1.0D, 0.0D, 100.0D);
        WEIGHT_WITHER_HALLUCINATION_ONLY = BUILDER.defineInRange("weight_wither", 1.0D, 0.0D, 100.0D);
        BUILDER.pop();

        BUILDER.push("Stalking Mechanics");
        STALKING_CHANCE_WEIGHT = BUILDER.defineInRange("stalking_chance_weight", 0.35, 0.0, 1.0);
        STALKING_MAX_DISTANCE = BUILDER.defineInRange("stalking_max_distance", 32, 8, 128);
        STALKING_SPEED_MULTIPLIER = BUILDER.defineInRange("stalking_speed_multiplier", 1.0, 0.1, 5.0);
        STALK_MAX_GOAL_DURATION = BUILDER.defineInRange("stalk_max_goal_duration", 600, 100, 12000);
        STALK_TELEPORT_INTERVAL = BUILDER.defineInRange("stalk_teleport_interval", 200, 40, 2000);
        STALK_TELEPORT_DISTANCE_BEHIND = BUILDER.defineInRange("stalk_teleport_distance_behind", 6.0, 2.0, 20.0);
        STALK_FREEZE_WHEN_LOOKED_AT = BUILDER.define("stalk_freeze_when_looked_at", true);
        STALK_MIN_PROXIMITY_LIMIT = BUILDER.defineInRange("stalk_min_proximity_limit", 6.0, 2.0, 32.0);
        BUILDER.pop();
        
		BUILDER.push("Carnage & Berserk Mode");
		CARNAGE_MODE_CHANCE = BUILDER.defineInRange("carnage_mode_chance", 0.35, 0.0, 1.0);
		CARNAGE_HEALTH_BOOST = BUILDER.defineInRange("carnage_health_boost", 40.0, 0.0, 1000.0);
		CARNAGE_SPEED_BOOST = BUILDER.defineInRange("carnage_speed_boost", 0.12, 0.0, 2.0);
		CARNAGE_DAMAGE_BOOST = BUILDER.defineInRange("carnage_damage_boost", 6.0, 0.0, 100.0);
		CARNAGE_KNOCKBACK_RESIST = BUILDER.defineInRange("knockback_resistance", 0.85, 0.0, 1.0);
		CARNAGE_BREAK_LIGHTS = BUILDER.define("carnage_break_lights", true);
		CARNAGE_SCREEN_SHAKE = BUILDER.define("carnage_screen_shake", true);
		CARNAGE_EFFECTS_DURATION = BUILDER.defineInRange("carnage_effects_duration", 7, 0, 60);
		ENABLE_BERSERK = BUILDER.define("enable_berserk", true);
		BERSERK_TRIGGER_PCT = BUILDER.defineInRange("berserk_trigger_health_pct", 0.30, 0.05, 0.95);
		BERSERK_DAMAGE_MULT = BUILDER.defineInRange("berserk_damage_multiplier", 1.5, 1.0, 10.0);
		BERSERK_SPEED_MULT = BUILDER.defineInRange("berserk_speed_multiplier", 1.35, 1.0, 5.0);
		BERSERK_PARTICLES = BUILDER.define("berserk_particles", true);

		// Initialize the new Rage configs
		RAGE_GAIN_MOVING = BUILDER.comment("Rage gained per tick while actively chasing the player").defineInRange("rage_gain_moving", 0.001, 0.0, 1.0);
		RAGE_GAIN_IDLE = BUILDER.comment("Rage gained per tick while waiting (navigation done or LOS broken)").defineInRange("rage_gain_idle", 0.002, 0.0, 1.0);
		
		BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
}