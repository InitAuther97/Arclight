package io.izzel.arclight.common.mixin.core.server.level;

import io.izzel.arclight.common.bridge.core.world.WorldBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.bridge.core.world.server.ChunkMapBridge;
import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.bridge.core.world.server.TicketManagerBridge;
import net.minecraft.server.level.*;
import net.minecraft.util.thread.BlockableEventLoop;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.storage.LevelData;
import org.bukkit.entity.SpawnCategory;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;
import org.spongepowered.asm.mixin.injection.*;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.function.BooleanSupplier;

@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin implements ServerChunkProviderBridge {

    // @formatter:off
    @Shadow public abstract void save(boolean flush);
    @Shadow @Final ThreadedLevelLightEngine lightEngine;
    @Shadow @Final public ChunkMap chunkMap;
    @Shadow @Final public ServerLevel level;
    @Shadow @Final private DistanceManager distanceManager;
    @Shadow protected abstract void clearCache();
    @Shadow @Nullable protected abstract ChunkHolder getVisibleChunkIfPresent(long chunkPosIn);
    @Invoker("runDistanceManagerUpdates") public abstract boolean bridge$tickDistanceManager();
    @Accessor("lightEngine") public abstract ThreadedLevelLightEngine bridge$getLightManager();
    // @formatter:on

    private BlockableEventLoop<Runnable> arclight$mainThreadExecutor;

    public boolean isChunkLoaded(final int chunkX, final int chunkZ) {
        //bridge$chunkHolderAt is getUpdatingChunkIfPresent
        ChunkHolder chunk = ((ChunkMapBridge) this.chunkMap).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        return chunk != null && ((ChunkHolderBridge) chunk).bridge$getFullChunkNow() != null;
    }

    public LevelChunk getChunkUnchecked(int chunkX, int chunkZ) {
        ChunkHolder chunk = ((ChunkMapBridge) this.chunkMap).bridge$chunkHolderAt(ChunkPos.asLong(chunkX, chunkZ));
        if (chunk == null) {
            return null;
        }
        return ((ChunkHolderBridge) chunk).bridge$getFullChunkUnchecked();
    }

    @Override
    public boolean bridge$isChunkLoaded(int x, int z) {
        return isChunkLoaded(x, z);
    }

    @Override
    public void arclight$setMainThreadExecutor(BlockableEventLoop<Runnable> executor) {
        this.arclight$mainThreadExecutor = executor;
    }

    @Override
    public void arclight$managedBlockOnExecutor(BooleanSupplier until) {
        arclight$mainThreadExecutor.managedBlock(until);
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/GameRules;getBoolean(Lnet/minecraft/world/level/GameRules$Key;)Z"))
    private boolean arclight$noPlayer(GameRules gameRules, GameRules.Key<GameRules.BooleanValue> key) {
        return gameRules.getBoolean(key) && !this.level.players().isEmpty();
    }

    @Redirect(method = "tickChunks", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/storage/LevelData;getGameTime()J"))
    private long arclight$ticksPer(LevelData worldInfo) {
        long gameTime = worldInfo.getGameTime();
        long ticksPer = ((WorldBridge) this.level).bridge$ticksPerSpawnCategory().getLong(SpawnCategory.ANIMAL);
        return (ticksPer != 0L && gameTime % ticksPer == 0) ? 0 : 1;
    }

    public void close(boolean save) throws IOException {
        if (save) {
            this.save(true);
        }
        this.lightEngine.close();
        this.chunkMap.close();
    }

    public void purgeUnload() {
        this.level.getProfiler().push("purge");
        ((TicketManagerBridge) this.distanceManager).bridge$tick();
        this.bridge$tickDistanceManager();
        this.level.getProfiler().popPush("unload");
        ((ChunkMapBridge) this.chunkMap).bridge$tick(() -> true);
        this.level.getProfiler().pop();
        this.clearCache();
    }

    @Override
    public void bridge$close(boolean save) throws IOException {
        this.close(save);
    }

    @Override
    public void bridge$purgeUnload() {
        this.purgeUnload();
    }
}
