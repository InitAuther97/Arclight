package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.server.ChunkHolderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import net.minecraft.server.level.ChunkHolder;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ChunkResult;
import net.minecraft.server.level.GenerationChunkHolder;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.concurrent.CompletableFuture;

@LoadIfMod(modid = "c3me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(ChunkHolder.class)
public abstract class ChunkHolderMixin_C2ME extends GenerationChunkHolder implements ChunkHolderBridge {

    @Shadow public abstract CompletableFuture<ChunkResult<LevelChunk>> getFullChunkFuture();

    protected ChunkHolderMixin_C2ME(ChunkPos chunkPos) {
        super(chunkPos);
    }

    public LevelChunk getFullChunkNow() {
        return (LevelChunk) this.getChunkIfPresent(ChunkStatus.FULL);
    }

    public LevelChunk getFullChunkUnchecked() {
        return (LevelChunk) this.getChunkIfPresentUnchecked(ChunkStatus.FULL);
    }

    @Override
    public LevelChunk bridge$getFullChunkUnchecked() {
        return getFullChunkNow();
    }

    @Override
    public LevelChunk bridge$getFullChunkNow() {
        return getFullChunkUnchecked();
    }

    protected void callEventIfUnloading(ChunkMap map) {
        ArclightServer.LOGGER.warn("callEventIfUnloading invoked. This method has no effect when C2ME is present.");
    }

    protected void callEventIfLoading(ChunkMap map) {}

    @Override
    public void bridge$callEventIfUnloading(ChunkMap manager) {
        callEventIfUnloading(manager);
    }
}