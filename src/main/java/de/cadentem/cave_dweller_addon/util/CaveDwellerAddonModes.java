package de.cadentem.cave_dweller_addon.util;

import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public final class CaveDwellerAddonModes {
    private static final Map<Integer, Integer> fallbackModeMap = new ConcurrentHashMap<>();
    private static final Map<Integer, Float> fallbackRageMap = new ConcurrentHashMap<>();

    public enum Mode {
        NONE(0), CARNAGE(1), HALLUCINATION(2), STALKING(3), FLEEING(4);
        final int id;
        Mode(int id) { this.id = id; }
        public static Mode fromId(int id) {
            for (Mode m : values()) if (m.id == id) return m;
            return NONE;
        }
    }

public static Mode get(final CaveDwellerEntity entity) {
    if (entity == null) return Mode.NONE;
    
    // Now safe because define() is called in onEntityConstructing
    Integer modeId = entity.getEntityData().get(CaveDwellerAddon.DATA_MODE_ID);
    return Mode.fromId(modeId != null ? modeId : 0);
}

public static float getRage(final CaveDwellerEntity entity) {
    if (entity == null) return 0.0f;
    return entity.getEntityData().get(CaveDwellerAddon.DATA_RAGE_METER);
}

    public static void set(final CaveDwellerEntity entity, final Mode mode) {
        if (entity == null || mode == null) return;

        fallbackModeMap.put(entity.getId(), mode.id);
        entity.getEntityData().set(CaveDwellerAddon.DATA_MODE_ID, mode.id);
    }

    public static void setRage(final CaveDwellerEntity entity, float amount) {
        if (entity == null) return;
        float clampedAmount = Math.min(amount, 1.0f);

        fallbackRageMap.put(entity.getId(), clampedAmount);
        entity.getEntityData().set(CaveDwellerAddon.DATA_RAGE_METER, clampedAmount);
    }

    public static void clear(final CaveDwellerEntity entity) {
        if (entity == null) return;
        
        fallbackModeMap.remove(entity.getId());
        fallbackRageMap.remove(entity.getId());
        entity.getEntityData().set(CaveDwellerAddon.DATA_MODE_ID, 0);
    }

    public static boolean isCarnageMode(final CaveDwellerEntity entity) {
        return get(entity) == Mode.CARNAGE;
    }

    public static boolean isStalkingMode(final CaveDwellerEntity entity) {
        return get(entity) == Mode.STALKING;
    }

    public static void enterStalkingMode(final CaveDwellerEntity entity) {
        set(entity, Mode.STALKING);
    }

    public static boolean isBerserk(final CaveDwellerEntity entity) {
        return entity != null && entity.getHealth() <= (entity.getMaxHealth() * 0.5f);
    }
}