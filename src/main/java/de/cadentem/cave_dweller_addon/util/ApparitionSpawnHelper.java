package de.cadentem.cave_dweller_addon.util;

import de.cadentem.cave_dweller_addon.config.AddonConfig;
import de.cadentem.cave_dweller_addon.entities.ApparitionEntity;
import de.cadentem.cave_dweller_addon.registry.ModEntityTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.Optional;

public final class ApparitionSpawnHelper {

    public static final int GLOBAL_CAP = 1;

    private ApparitionSpawnHelper() {
    }

    public static int countGlobalApparitions(final MinecraftServer server) {
        int count = 0;
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ApparitionEntity) {
                    count++;
                }
            }
        }
        return count;
    }

    public static boolean canSpawnAnother(final MinecraftServer server) {
        return countGlobalApparitions(server) < GLOBAL_CAP;
    }

    public static boolean isValidCaveBlock(final BlockState state) {
        if (state.is(Blocks.STONE) || state.is(Blocks.DEEPSLATE) || state.is(Blocks.TUFF)
                || state.is(Blocks.GRANITE) || state.is(Blocks.DIORITE) || state.is(Blocks.ANDESITE)) {
            return true;
        }
        if (state.is(BlockTags.BASE_STONE_OVERWORLD)) {
            String path = net.minecraftforge.registries.ForgeRegistries.BLOCKS.getKey(state.getBlock()).getPath();
            return path.contains("deepslate") || path.equals("stone") || path.contains("tuff");
        }
        return false;
    }

    /**
     * {@code ceilingPos} is the solid stone/deepslate block; the apparition hangs in the air below it.
     */
    public static boolean isValidSpawnPosition(final ServerLevel level, final BlockPos ceilingPos) {
        if (ceilingPos.getY() > AddonConfig.ABSOLUTE_MAX_Y.get()) {
            return false;
        }

        if (!isValidCaveBlock(level.getBlockState(ceilingPos))) {
            return false;
        }

        for (int i = 1; i <= 3; i++) {
            BlockState space = level.getBlockState(ceilingPos.below(i));
            if (!space.isAir()) {
                return false;
            }
        }

        return true;
    }

  @Nullable
    public static BlockPos findCeilingSpawnPos(final ServerLevel level, final BlockPos origin, final int radius, final boolean preferDeep) {
        int preferredY = AddonConfig.PREFERRED_MAX_Y.get();
        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();

        for (int attempt = 0; attempt < 48; attempt++) {
            int x = origin.getX() + level.random.nextInt(radius * 2 + 1) - radius;
            int z = origin.getZ() + level.random.nextInt(radius * 2 + 1) - radius;
            int y;

            if (preferDeep && level.random.nextFloat() < 0.75f) {
                y = level.random.nextIntBetweenInclusive(Math.min(preferredY, -1), preferredY);
            } else {
                y = level.random.nextIntBetweenInclusive(-32, AddonConfig.ABSOLUTE_MAX_Y.get());
            }

            cursor.set(x, y, z);
            if (isValidSpawnPosition(level, cursor)) {
                return cursor.immutable();
            }
        }

        return null;
    }

    public static Optional<ApparitionEntity> spawnAt(
            final ServerLevel level,
            final BlockPos origin,
            @Nullable final ServerPlayer contextPlayer,
            final boolean commandSpawn
    ) {
        MinecraftServer server = level.getServer();
        if (server == null) {
            return Optional.empty();
        }
        if (!commandSpawn && !canSpawnAnother(server)) {
            return Optional.empty();
        }
        if (commandSpawn && !canSpawnAnother(server)) {
            removeAllApparitions(server);
        }

        boolean preferDeep = origin.getY() <= AddonConfig.PREFERRED_MAX_Y.get();
        BlockPos spawnPos = findCeilingSpawnPos(level, origin, 12, preferDeep);
        if (spawnPos == null) {
            spawnPos = findCeilingSpawnPos(level, origin, 24, true);
        }
        if (spawnPos == null && commandSpawn) {
            spawnPos = findCeilingSpawnPos(level, origin, 6, false);
        }
        if (spawnPos == null && commandSpawn && contextPlayer != null) {
            spawnPos = origin.above(2);
        }
        if (spawnPos == null) {
            return Optional.empty();
        }

        ApparitionEntity apparition = ModEntityTypes.APPARITION.get().create(level);
        if (apparition == null) {
            return Optional.empty();
        }

        BlockPos hangPos = isValidSpawnPosition(level, spawnPos) ? spawnPos.below() : spawnPos;
        Vec3 vec = Vec3.atCenterOf(hangPos);
        apparition.moveTo(vec.x, vec.y, vec.z, level.random.nextFloat() * 360f, 0f);
        apparition.finalizeSpawnFromConfig();

        if (!level.addFreshEntity(apparition)) {
            return Optional.empty();
        }

        if (contextPlayer != null) {
            apparition.setLookTarget(contextPlayer);
        }

        return Optional.of(apparition);
    }

    public static void removeAllApparitions(final MinecraftServer server) {
        for (ServerLevel level : server.getAllLevels()) {
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ApparitionEntity apparition) {
                    apparition.discard();
                }
            }
        }
    }

    public static boolean isOverworldCavePlayer(final ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return false;
        }
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        if (!level.dimension().location().getPath().equals("overworld")) {
            return false;
        }
        return player.getY() <= AddonConfig.ABSOLUTE_MAX_Y.get();
    }
}
