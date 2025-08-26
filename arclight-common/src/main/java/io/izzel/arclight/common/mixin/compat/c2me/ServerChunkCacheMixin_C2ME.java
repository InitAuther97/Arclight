package io.izzel.arclight.common.mixin.compat.c2me;

import io.izzel.arclight.common.bridge.core.world.server.ServerChunkProviderBridge;
import io.izzel.arclight.common.mod.mixins.annotation.LoadIfMod;
import io.izzel.arclight.common.mod.server.ArclightServer;
import io.izzel.arclight.common.mod.server.world.IllegalChunkAccessException;
import io.izzel.arclight.mixin.Decorate;
import io.izzel.arclight.mixin.DecorationOps;
import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@LoadIfMod(modid = "c2me", condition = LoadIfMod.ModCondition.PRESENT)
@Mixin(ServerChunkCache.class)
public abstract class ServerChunkCacheMixin_C2ME implements ServerChunkProviderBridge {

    // @formatter:off
    @Shadow @Final ServerLevel level;
    // @formatter:on

    @Unique
    private long arclight$currentChunkEvent = Long.MAX_VALUE;

    @Unique
    private LevelChunk arclight$currentChunk = null;

    @Inject(method = "getChunk", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/profiling/ProfilerFiller;incrementCounter(Ljava/lang/String;)V", ordinal = 1), cancellable = true)
    private void arclight$shortcutGetChunk(int chunkX, int chunkZ, ChunkStatus status, boolean generate, CallbackInfoReturnable<LevelChunk> cir) {
        if (this.arclight$currentChunkEvent != Long.MAX_VALUE) {
            if (arclight$currentChunkEvent != ChunkPos.asLong(chunkX, chunkZ)) {
                if (generate) {
                    RuntimeException ex = new IllegalChunkAccessException(this.level.dimension.location(), new ChunkPos(arclight$currentChunkEvent), new ChunkPos(chunkX, chunkZ));
                    ArclightServer.LOGGER.error("Detected an illegal chunk request during chunk event. This will lead to a dangerous undefined behaviour! Report this to the author of the respective plugin or mod!", ex);
                    throw ex;
                } else {
                    ArclightServer.LOGGER.warn("Rejected optional chunk loading during chunk event in {} at ({},{})", level.dimension.location(), chunkX, chunkZ);
                    cir.setReturnValue(null);
                }
            } else if (this.arclight$currentChunk != null) {
                cir.setReturnValue(this.arclight$currentChunk);
            }
        }
    }

    @Inject(method = "getChunkNow", at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerChunkCache;getVisibleChunkIfPresent(J)Lnet/minecraft/server/level/ChunkHolder;"), cancellable = true)
    private void arclight$shortcutGetChunkNow(int i, int j, CallbackInfoReturnable<LevelChunk> cir) {
        if (this.arclight$currentChunkEvent == ChunkPos.asLong(i, j)) {
            cir.setReturnValue(arclight$currentChunk);
        }
    }

    @Decorate(method = "isChunkLoaded", at = @At("HEAD"), inject = true)
    @Dynamic
    private void arclight$shortcutIsChunkLoaded(final int chunkX, final int chunkZ) throws Throwable {
        if (this.arclight$currentChunkEvent == ChunkPos.asLong(chunkX, chunkZ)) {
            DecorationOps.cancel().invoke(true);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Decorate(method = "getChunkUnchecked", at = @At("HEAD"), inject = true)
    @Dynamic
    public void arclight$shortcutGetChunkUnchecked(int chunkX, int chunkZ) throws Throwable {
        if (this.arclight$currentChunkEvent == ChunkPos.asLong(chunkX, chunkZ)) {
            DecorationOps.cancel().invoke(arclight$currentChunk);
            return;
        }
        DecorationOps.blackhole().invoke();
    }

    @Override
    public void arclight$setChunkEvent(long pos, LevelChunk unloading) {
        arclight$currentChunkEvent = pos;
        arclight$currentChunk = unloading;
    }
}
