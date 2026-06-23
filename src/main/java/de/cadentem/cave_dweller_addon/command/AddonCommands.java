package de.cadentem.cave_dweller_addon.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import de.cadentem.cave_dweller.entities.CaveDwellerEntity;
import de.cadentem.cave_dweller_addon.CaveDwellerAddon;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import de.cadentem.cave_dweller_addon.entities.goals.CarnageModeGoal;
import de.cadentem.cave_dweller_addon.entities.goals.HallucinationModeGoal;
import de.cadentem.cave_dweller_addon.registry.ModEntityTypes;
import de.cadentem.cave_dweller_addon.util.CaveDwellerAddonModes;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterClientCommandsEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class AddonCommands {

    private static final List<String> DWELLER_MODES = Arrays.asList("flee", "chase", "stare", "default", "carnage", "stalk", "berserk");
    private static final List<String> APPARITION_MODES = Arrays.asList("hallucination", "flee", "default");
    private static final List<String> ALL_CONFIG_MODES = Arrays.asList("default", "stalk", "carnage", "hallucination", "flee", "chase", "stare", "berserk");

    private static final Map<String, Integer> maxCooldownMap = new HashMap<>();
    private static final Map<String, Integer> maxTimerMap = new HashMap<>();
    private static final Map<String, Integer> liveCooldownMap = new HashMap<>();
    private static final Map<String, Integer> liveTimerMap = new HashMap<>();

    static {
        for (String mode : ALL_CONFIG_MODES) {
            maxCooldownMap.put(mode, 1200);
            maxTimerMap.put(mode, 600);
            liveCooldownMap.put(mode, 0);
            liveTimerMap.put(mode, 0);
        }
    }

    private AddonCommands() {
    }

    @SubscribeEvent
    public static void registerServerCommands(final RegisterCommandsEvent event) {
        registerAllAddonCommands(event.getDispatcher(), false);
    }

    @Mod.EventBusSubscriber(modid = CaveDwellerAddon.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static final class ClientRegistration {
        private ClientRegistration() {
        }

        @SubscribeEvent
        public static void registerClientCommands(final RegisterClientCommandsEvent event) {
            registerAllAddonCommands(event.getDispatcher(), true);
        }
    }

    private static void registerAllAddonCommands(final CommandDispatcher<CommandSourceStack> dispatcher, final boolean isClientContext) {
        dispatcher.register(
                Commands.literal("dwelleraddon")
                        .requires(source -> source.hasPermission(0))

                        // ====================================================================
                        // SPAWN LAYER
                        // ====================================================================
                        .then(Commands.literal("spawn")
                                .requires(source -> source.hasPermission(2))
                                
                                .then(Commands.literal("cave_dweller")
                                        .then(Commands.argument("mode", StringArgumentType.string())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DWELLER_MODES, builder))
                                                .executes(ctx -> executeDwellerSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), isClientContext))
                                        )
                                        .executes(ctx -> executeDwellerSpawn(ctx.getSource(), "default", isClientContext))
                                )
                                
                                .then(Commands.literal("apparition")
                                        .then(Commands.argument("mode", StringArgumentType.string())
                                                .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(APPARITION_MODES, builder))
                                                .executes(ctx -> executeApparitionSpawn(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), isClientContext))
                                        )
                                        .executes(ctx -> executeApparitionSpawn(ctx.getSource(), "default", isClientContext))
                                )
                        )

                        // ====================================================================
                        // TIMER BRANCH
                        // ====================================================================
                        .then(Commands.literal("timer")
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_CONFIG_MODES, builder))
                                        
                                        .then(Commands.literal("query")
                                                .executes(ctx -> queryLiveRundown(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), true))
                                        )
                                        .then(Commands.literal("add")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyTrackingValue(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), IntegerArgumentType.getInteger(ctx, "ticks"), true, true))
                                                )
                                        )
                                        .then(Commands.literal("negate")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyTrackingValue(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), IntegerArgumentType.getInteger(ctx, "ticks"), false, true))
                                                )
                                        )
                                        .executes(ctx -> applyLiveGoalOverride(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), isClientContext))
                                )
                        )

                        // ====================================================================
                        // COOLDOWN BRANCH
                        // ====================================================================
                        .then(Commands.literal("cooldown")
                                .then(Commands.argument("mode", StringArgumentType.string())
                                        .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(ALL_CONFIG_MODES, builder))
                                        
                                        .then(Commands.literal("query")
                                                .executes(ctx -> queryLiveRundown(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), false))
                                        )
                                        .then(Commands.literal("add")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyTrackingValue(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), IntegerArgumentType.getInteger(ctx, "ticks"), true, false))
                                                )
                                        )
                                        .then(Commands.literal("negate")
                                                .requires(source -> source.hasPermission(2))
                                                .then(Commands.argument("ticks", IntegerArgumentType.integer(1))
                                                        .executes(ctx -> modifyTrackingValue(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), IntegerArgumentType.getInteger(ctx, "ticks"), false, false))
                                                )
                                        )
                                        .executes(ctx -> applyLiveGoalOverride(ctx.getSource(), StringArgumentType.getString(ctx, "mode"), isClientContext))
                                )
                        )
        );
    }

    // ====================================================================
    // CAVE DWELLER ARCHETYPE GENERATION FACTORY
    // ====================================================================
    private static int executeDwellerSpawn(CommandSourceStack source, String mode, boolean isClientContext) {
        if (isClientContext) {
            source.sendSuccess(() -> Component.literal("Notice: Standalone spawn commands are disabled from client windows.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level == null) return 0;

        Vec3 posVec = source.getPosition();
        BlockPos spawnPos = BlockPos.containing(posVec);
        ServerPlayer playerContext = source.getPlayer();

        EntityType<?> dwellerType = ForgeRegistries.ENTITY_TYPES.getValue(new ResourceLocation("cave_dweller", "cave_dweller"));
        if (dwellerType == null) {
            source.sendFailure(Component.literal("Error: Target core key 'cave_dweller:cave_dweller' missing from registry maps.").withStyle(ChatFormatting.RED));
            return 0;
        }

        CaveDwellerEntity dweller = (CaveDwellerEntity) dwellerType.create(level);
        if (dweller == null) return 0;

        dweller.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, playerContext != null ? playerContext.getYRot() : 0.0F, 0.0F);
        if (playerContext != null) {
            dweller.setTarget(playerContext);
        }
        dweller.setPersistenceRequired();

        int nativeBehaviorMode = 1;
        CaveDwellerAddonModes.clear(dweller);
        String activeLabel = mode.toUpperCase();

        switch (mode.toLowerCase()) {
            case "stalk":
                nativeBehaviorMode = 0;
                break;
            case "flee":
                nativeBehaviorMode = 4;
                break;
            case "stare":
                nativeBehaviorMode = 3;
                break;
            case "carnage":
                nativeBehaviorMode = 2;
                CaveDwellerAddonModes.set(dweller, CaveDwellerAddonModes.Mode.CARNAGE);
                dweller.goalSelector.addGoal(1, new CarnageModeGoal(dweller));
                break;
            case "berserk":
                nativeBehaviorMode = 2;
                CaveDwellerAddonModes.set(dweller, CaveDwellerAddonModes.Mode.CARNAGE);
                dweller.goalSelector.addGoal(1, new CarnageModeGoal(dweller));
                
                dweller.setHealth(dweller.getMaxHealth() / 2.0F);
                if (dweller.getAttribute(Attributes.ATTACK_DAMAGE) != null) {
                    dweller.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(12.0D); 
                }
                activeLabel = "BERSERK CARNAGE CONFIGURATION";
                break;
            case "chase":
            case "default":
            default:
                nativeBehaviorMode = 1;
                break;
        }

        try {
            java.lang.reflect.Method setModeMethod = dweller.getClass().getMethod("setBehaviorMode", int.class);
            setModeMethod.invoke(dweller, nativeBehaviorMode);
        } catch (Exception e) {
            dweller.getEntityData().set(net.minecraft.network.syncher.EntityDataSerializers.INT.createAccessor(20), nativeBehaviorMode);
        }

        if (level.addFreshEntity(dweller)) {
            final String finalLabel = activeLabel;
            source.sendSuccess(() -> Component.literal("Injected Cave Dweller into world coordinates -> Mode: [" + finalLabel + "]")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        return 0;
    }

    // ====================================================================
    // APPARITION ARCHETYPE GENERATION FACTORY
    // ====================================================================
    private static int executeApparitionSpawn(CommandSourceStack source, String mode, boolean isClientContext) {
        if (isClientContext) {
            source.sendSuccess(() -> Component.literal("Notice: Standalone spawn commands are disabled from client windows.")
                    .withStyle(ChatFormatting.YELLOW), false);
            return 0;
        }

        ServerLevel level = source.getLevel();
        if (level == null) return 0;

        Vec3 posVec = source.getPosition();
        BlockPos spawnPos = BlockPos.containing(posVec);
        ServerPlayer playerContext = source.getPlayer();

        ApparitionEntity apparition = new ApparitionEntity(ModEntityTypes.APPARITION.get(), level);
        apparition.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, playerContext != null ? playerContext.getYRot() : 0.0F, 0.0F);
        apparition.setTarget(playerContext);
        apparition.getEntityData().set(ApparitionEntity.DATA_IS_HANGING, false);

        String activeLabel = mode.toUpperCase();

        switch (mode.toLowerCase()) {
            case "hallucination":
                activeLabel = "ACTIVE HALLUCINATION SIMULATION TREE";
                break;
            case "flee":
                activeLabel = "EVASIVE FLEE SEQUENCE LOGIC";
                break;
            case "default":
            default:
                break;
        }

        if (level.addFreshEntity(apparition)) {
            final String finalLabel = activeLabel;
            source.sendSuccess(() -> Component.literal("Injected Apparition into world coordinates -> Mode: [" + finalLabel + "]")
                    .withStyle(ChatFormatting.GREEN), true);
            return 1;
        }
        return 0;
    }

    // ====================================================================
    // LIVE GOAL OVERRIDES
    // ====================================================================
    private static int applyLiveGoalOverride(CommandSourceStack source, String modeSelection, boolean isClientContext) {
        if (isClientContext) {
            return 0; 
        }

        ServerLevel level = source.getLevel();
        if (level == null) return 0;

        try {
            String cleanedMode = modeSelection.toLowerCase();
            BlockPos targetCenter = BlockPos.containing(source.getPosition());
            AABB searchBox = new AABB(targetCenter).inflate(64.0D);
            List<CaveDwellerEntity> nearbyDwellers = level.getEntitiesOfClass(CaveDwellerEntity.class, searchBox);

            if (nearbyDwellers.isEmpty()) {
                source.sendFailure(Component.literal("Testing Engine Alert: No active Cave Dwellers located within 64 blocks of command center.")
                        .withStyle(ChatFormatting.YELLOW));
                return 0;
            }

            int forcedModeIndex = 1;
            for (CaveDwellerEntity dweller : nearbyDwellers) {
                CaveDwellerAddonModes.clear(dweller);

                switch (cleanedMode) {
                    case "stalk":
                        forcedModeIndex = 0;
                        break;
                    case "flee":
                        forcedModeIndex = 4;
                        break;
                    case "stare":
                        forcedModeIndex = 3;
                        break;
                    case "carnage":
                    case "berserk":
                        forcedModeIndex = 2;
                        CaveDwellerAddonModes.set(dweller, CaveDwellerAddonModes.Mode.CARNAGE);
                        dweller.goalSelector.addGoal(1, new CarnageModeGoal(dweller));
                        if (cleanedMode.equals("berserk")) {
                            dweller.setHealth(dweller.getMaxHealth() / 2.0F);
                        }
                        break;
                    case "chase":
                    default:
                        forcedModeIndex = 1;
                        break;
                }

                try {
                    java.lang.reflect.Method setModeMethod = dweller.getClass().getMethod("setBehaviorMode", int.class);
                    setModeMethod.invoke(dweller, forcedModeIndex);
                } catch (Exception e) {
                    dweller.getEntityData().set(net.minecraft.network.syncher.EntityDataSerializers.INT.createAccessor(20), forcedModeIndex);
                }
            }

            final int size = nearbyDwellers.size();
            source.sendSuccess(() -> Component.literal("Successfully transformed AI objective profile on (" + size + ") entities to: [" + modeSelection.toUpperCase() + "]")
                    .withStyle(ChatFormatting.GREEN), true);
            return size;

        } catch (Exception e) {
            source.sendFailure(Component.literal("Failed to execute runtime update sequence.").withStyle(ChatFormatting.RED));
            return 0;
        }
    }

    // ====================================================================
    // RUNTIME DATA LAYERS
    // ====================================================================
    private static int queryLiveRundown(CommandSourceStack source, String modeSelection, boolean isTimer) {
        String cleanedMode = modeSelection.toLowerCase();
        if (!ALL_CONFIG_MODES.contains(cleanedMode)) {
            source.sendFailure(Component.literal("Unknown runtime key entry pattern.").withStyle(ChatFormatting.RED));
            return 0;
        }

        int maxDuration = isTimer ? maxTimerMap.get(cleanedMode) : maxCooldownMap.get(cleanedMode);
        int currentLeft = isTimer ? liveTimerMap.get(cleanedMode) : liveCooldownMap.get(cleanedMode);
        int activeRunning = Math.max(0, maxDuration - currentLeft);

        final double totalSeconds = maxDuration / 20.0D;
        final double leftSeconds = currentLeft / 20.0D;
        final double runningSeconds = activeRunning / 20.0D;
        final String label = isTimer ? "SPAWN TIMER" : "COOLDOWN CYCLE";

        source.sendSuccess(() -> Component.literal("\n=== LIVE RUNTIME STATUS: " + label + " ===").withStyle(ChatFormatting.GOLD), false);
        source.sendSuccess(() -> Component.literal("• Mode Focus: ").withStyle(ChatFormatting.GRAY).append(Component.literal(modeSelection.toUpperCase()).withStyle(ChatFormatting.WHITE)), false);
        source.sendSuccess(() -> Component.literal("• Total Metric Limit: ").withStyle(ChatFormatting.GRAY).append(Component.literal(maxDuration + " Ticks").withStyle(ChatFormatting.AQUA)).append(" (" + String.format("%.2fs", totalSeconds) + ")"), false);
        source.sendSuccess(() -> Component.literal("• Progress Duration Active: ").withStyle(ChatFormatting.GRAY).append(Component.literal(activeRunning + " Ticks").withStyle(ChatFormatting.YELLOW)).append(" (" + String.format("%.2fs", runningSeconds) + ")"), false);
        source.sendSuccess(() -> Component.literal("• Time Left To Zero: ").withStyle(ChatFormatting.GRAY).append(Component.literal(currentLeft + " Ticks").withStyle(ChatFormatting.GREEN)).append(" (" + String.format("%.2fs", leftSeconds) + ")"), false);

        return currentLeft;
    }

    private static int modifyTrackingValue(CommandSourceStack source, String modeSelection, int amount, boolean isAdding, boolean isTimer) {
        String cleanedMode = modeSelection.toLowerCase();
        if (!ALL_CONFIG_MODES.contains(cleanedMode)) {
            source.sendFailure(Component.literal("Unknown runtime validation token.").withStyle(ChatFormatting.RED));
            return 0;
        }

        Map<String, Integer> targetMap = isTimer ? liveTimerMap : liveCooldownMap;
        int activeTicks = targetMap.get(cleanedMode);

        if (isAdding) activeTicks += amount;
        else activeTicks = Math.max(0, activeTicks - amount);

        targetMap.put(cleanedMode, activeTicks);

        final int outputTicks = activeTicks;
        final double displaySeconds = outputTicks / 20.0D;
        final String trackingSymbol = isAdding ? "+" : "-";
        final String systemLabel = isTimer ? "Live Spawn Timer" : "Live Cooldown Cycle";

        source.sendSuccess(() -> Component.literal("[" + modeSelection.toUpperCase() + "] " + systemLabel + " Mutated [")
                .append(Component.literal(trackingSymbol + amount + " Ticks").withStyle(ChatFormatting.LIGHT_PURPLE))
                .append("] -> Net Active: ")
                .append(Component.literal(outputTicks + " Ticks").withStyle(ChatFormatting.AQUA))
                .append(" (")
                .append(Component.literal(String.format("%.1fs", displaySeconds)).withStyle(ChatFormatting.GOLD))
                .append(")"), true);
        return outputTicks;
    }
}